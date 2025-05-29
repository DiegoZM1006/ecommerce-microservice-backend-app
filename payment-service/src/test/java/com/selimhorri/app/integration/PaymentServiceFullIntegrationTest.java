package com.selimhorri.app.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.repository.PaymentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Payment Service Integration Tests with TestRestTemplate")
class PaymentServiceFullIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private RestTemplate externalRestTemplate;

    private String baseUrl;
    private OrderDto mockOrderDto;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/payments";
        
        // Clean repository
        paymentRepository.deleteAll();

        // Setup mock external service response
        mockOrderDto = OrderDto.builder()
                .orderId(1)
                .build();

        when(externalRestTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(mockOrderDto);
    }

    @Test
    @DisplayName("Should get all payments - End to End")
    void getAllPayments_EndToEnd() {
        // Given
        Payment payment1 = Payment.builder()
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        Payment payment2 = Payment.builder()
                .orderId(2)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        paymentRepository.save(payment1);
        paymentRepository.save(payment2);

        // When
        ResponseEntity<DtoCollectionResponse<PaymentDto>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<DtoCollectionResponse<PaymentDto>>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        List<PaymentDto> payments = (List<PaymentDto>) response.getBody().getCollection();
        assertEquals(2, payments.size());
        
        // Verify both payments are returned with correct data
        assertTrue(payments.stream().anyMatch(p -> 
            p.getPaymentStatus() == PaymentStatus.NOT_STARTED && !p.getIsPayed()));
        assertTrue(payments.stream().anyMatch(p -> 
            p.getPaymentStatus() == PaymentStatus.COMPLETED && p.getIsPayed()));
    }

    @Test
    @DisplayName("Should get payment by id - End to End")
    void getPaymentById_EndToEnd() {
        // Given
        Payment savedPayment = paymentRepository.save(Payment.builder()
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build());

        // When
        ResponseEntity<PaymentDto> response = restTemplate.getForEntity(
                baseUrl + "/" + savedPayment.getPaymentId(),
                PaymentDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        PaymentDto payment = response.getBody();
        assertEquals(savedPayment.getPaymentId(), payment.getPaymentId());
        assertEquals(savedPayment.getIsPayed(), payment.getIsPayed());
        assertEquals(savedPayment.getPaymentStatus(), payment.getPaymentStatus());
        assertNotNull(payment.getOrderDto());
        assertEquals(1, payment.getOrderDto().getOrderId());
    }

    @Test
    @DisplayName("Should create payment - End to End")
    void createPayment_EndToEnd() {
        // Given
        PaymentDto newPayment = PaymentDto.builder()
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(mockOrderDto)
                .build();

        // When
        ResponseEntity<PaymentDto> response = restTemplate.postForEntity(
                baseUrl,
                newPayment,
                PaymentDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        PaymentDto createdPayment = response.getBody();
        assertNotNull(createdPayment.getPaymentId());
        assertEquals(newPayment.getIsPayed(), createdPayment.getIsPayed());
        assertEquals(newPayment.getPaymentStatus(), createdPayment.getPaymentStatus());
        
        // Verify it was actually saved in the database
        assertEquals(1, paymentRepository.count());
    }

    @Test
    @DisplayName("Should update payment - End to End")
    void updatePayment_EndToEnd() {
        // Given
        Payment savedPayment = paymentRepository.save(Payment.builder()
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build());

        PaymentDto updatePayment = PaymentDto.builder()
                .paymentId(savedPayment.getPaymentId())
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(mockOrderDto)
                .build();

        HttpEntity<PaymentDto> requestEntity = new HttpEntity<>(updatePayment);

        // When
        ResponseEntity<PaymentDto> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.PUT,
                requestEntity,
                PaymentDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        PaymentDto updatedPayment = response.getBody();
        assertEquals(savedPayment.getPaymentId(), updatedPayment.getPaymentId());
        assertTrue(updatedPayment.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, updatedPayment.getPaymentStatus());
        
        // Verify it was actually updated in the database
        Payment dbPayment = paymentRepository.findById(savedPayment.getPaymentId()).orElse(null);
        assertNotNull(dbPayment);
        assertTrue(dbPayment.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, dbPayment.getPaymentStatus());
    }

    @Test
    @DisplayName("Should delete payment - End to End")
    void deletePayment_EndToEnd() {
        // Given
        Payment savedPayment = paymentRepository.save(Payment.builder()
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build());

        assertEquals(1, paymentRepository.count());

        // When
        ResponseEntity<Boolean> response = restTemplate.exchange(
                baseUrl + "/" + savedPayment.getPaymentId(),
                HttpMethod.DELETE,
                null,
                Boolean.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
        
        // Verify it was actually deleted from the database
        assertEquals(0, paymentRepository.count());
        assertFalse(paymentRepository.existsById(savedPayment.getPaymentId()));
    }

    @Test
    @DisplayName("Should handle payment not found - End to End")
    void getPaymentById_ShouldReturnError_WhenNotFound() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/999",
                String.class
        );

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("Should handle payment workflow - End to End")
    void paymentWorkflow_EndToEnd() {
        // Step 1: Create a new payment
        PaymentDto newPayment = PaymentDto.builder()
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(mockOrderDto)
                .build();

        ResponseEntity<PaymentDto> createResponse = restTemplate.postForEntity(
                baseUrl,
                newPayment,
                PaymentDto.class
        );

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        PaymentDto createdPayment = createResponse.getBody();
        assertNotNull(createdPayment);
        Integer paymentId = createdPayment.getPaymentId();

        // Step 2: Update payment to IN_PROGRESS
        PaymentDto inProgressPayment = PaymentDto.builder()
                .paymentId(paymentId)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(mockOrderDto)
                .build();

        HttpEntity<PaymentDto> inProgressEntity = new HttpEntity<>(inProgressPayment);
        ResponseEntity<PaymentDto> inProgressResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.PUT,
                inProgressEntity,
                PaymentDto.class
        );

        assertEquals(HttpStatus.OK, inProgressResponse.getStatusCode());
        assertEquals(PaymentStatus.IN_PROGRESS, inProgressResponse.getBody().getPaymentStatus());

        // Step 3: Complete the payment
        PaymentDto completedPayment = PaymentDto.builder()
                .paymentId(paymentId)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(mockOrderDto)
                .build();

        HttpEntity<PaymentDto> completedEntity = new HttpEntity<>(completedPayment);
        ResponseEntity<PaymentDto> completedResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.PUT,
                completedEntity,
                PaymentDto.class
        );

        assertEquals(HttpStatus.OK, completedResponse.getStatusCode());
        assertEquals(PaymentStatus.COMPLETED, completedResponse.getBody().getPaymentStatus());
        assertTrue(completedResponse.getBody().getIsPayed());

        // Step 4: Verify the final state
        ResponseEntity<PaymentDto> finalResponse = restTemplate.getForEntity(
                baseUrl + "/" + paymentId,
                PaymentDto.class
        );

        assertEquals(HttpStatus.OK, finalResponse.getStatusCode());
        PaymentDto finalPayment = finalResponse.getBody();
        assertEquals(PaymentStatus.COMPLETED, finalPayment.getPaymentStatus());
        assertTrue(finalPayment.getIsPayed());
    }

    @Test
    @DisplayName("Should handle empty payments list - End to End")
    void getAllPayments_ShouldReturnEmptyList_WhenNoPayments() {
        // When
        ResponseEntity<DtoCollectionResponse<PaymentDto>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<DtoCollectionResponse<PaymentDto>>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getCollection().isEmpty());
    }

    @Test
    @DisplayName("Should handle concurrent payment operations - End to End")
    void concurrentPaymentOperations_EndToEnd() {
        // Given
        Payment payment1 = paymentRepository.save(Payment.builder()
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build());

        Payment payment2 = paymentRepository.save(Payment.builder()
                .orderId(2)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build());

        // When - Update both payments to completed
        PaymentDto update1 = PaymentDto.builder()
                .paymentId(payment1.getPaymentId())
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(mockOrderDto)
                .build();

        PaymentDto update2 = PaymentDto.builder()
                .paymentId(payment2.getPaymentId())
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(mockOrderDto)
                .build();

        HttpEntity<PaymentDto> entity1 = new HttpEntity<>(update1);
        HttpEntity<PaymentDto> entity2 = new HttpEntity<>(update2);

        ResponseEntity<PaymentDto> response1 = restTemplate.exchange(
                baseUrl, HttpMethod.PUT, entity1, PaymentDto.class);
        ResponseEntity<PaymentDto> response2 = restTemplate.exchange(
                baseUrl, HttpMethod.PUT, entity2, PaymentDto.class);

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        
        assertTrue(response1.getBody().getIsPayed());
        assertTrue(response2.getBody().getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, response1.getBody().getPaymentStatus());
        assertEquals(PaymentStatus.COMPLETED, response2.getBody().getPaymentStatus());
    }
}
