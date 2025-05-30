package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Payment Resource Integration Tests")
class PaymentResourceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Payment testPayment;
    private OrderDto mockOrderDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean repository
        paymentRepository.deleteAll();

        // Setup test data
        testPayment = Payment.builder()
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        mockOrderDto = OrderDto.builder()
                .orderId(1)
                .build();

        // Mock external service call
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(mockOrderDto);
    }

    @Test
    @DisplayName("Should get all payments successfully")
    void getAllPayments_ShouldReturnPaymentsList() throws Exception {
        // Given
        Payment savedPayment = paymentRepository.save(testPayment);

        // When & Then
        mockMvc.perform(get("/api/payments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[0].paymentId", is(savedPayment.getPaymentId())))
                .andExpect(jsonPath("$.collection[0].isPayed", is(false)))
                .andExpect(jsonPath("$.collection[0].paymentStatus", is("NOT_STARTED")));
    }

    @Test
    @DisplayName("Should get payment by id successfully")
    void getPaymentById_ShouldReturnPayment_WhenPaymentExists() throws Exception {
        // Given
        Payment savedPayment = paymentRepository.save(testPayment);

        // When & Then
        mockMvc.perform(get("/api/payments/{id}", savedPayment.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paymentId", is(savedPayment.getPaymentId())))
                .andExpect(jsonPath("$.isPayed", is(false)))
                .andExpect(jsonPath("$.paymentStatus", is("NOT_STARTED")))
                .andExpect(jsonPath("$.order.orderId", is(1)));
    }

    // @Test
    // @DisplayName("Should return 500 when payment not found")
    // void getPaymentById_ShouldReturnNotFound_WhenPaymentDoesNotExist() throws Exception {
    //     // When & Then
    //     mockMvc.perform(get("/api/payments/{id}", 999)
    //             .contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(status().is5xxServerError());
    // }

    @Test
    @DisplayName("Should create payment successfully")
    void createPayment_ShouldReturnCreatedPayment() throws Exception {
        // Given
        PaymentDto paymentDto = PaymentDto.builder()
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(mockOrderDto)
                .build();

        String paymentJson = objectMapper.writeValueAsString(paymentDto);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isPayed", is(false)))
                .andExpect(jsonPath("$.paymentStatus", is("NOT_STARTED")));

        // Verify payment was saved
        assertEquals(1, paymentRepository.count());
    }

    @Test
    @DisplayName("Should update payment successfully")
    void updatePayment_ShouldReturnUpdatedPayment() throws Exception {
        // Given
        Payment savedPayment = paymentRepository.save(testPayment);

        PaymentDto updateDto = PaymentDto.builder()
                .paymentId(savedPayment.getPaymentId())
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(mockOrderDto)
                .build();

        String updateJson = objectMapper.writeValueAsString(updateDto);

        // When & Then
        mockMvc.perform(put("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paymentId", is(savedPayment.getPaymentId())))
                .andExpect(jsonPath("$.isPayed", is(true)))
                .andExpect(jsonPath("$.paymentStatus", is("COMPLETED")));

        // Verify payment was updated
        Optional<Payment> updatedPayment = paymentRepository.findById(savedPayment.getPaymentId());
        assertTrue(updatedPayment.isPresent());
        assertTrue(updatedPayment.get().getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, updatedPayment.get().getPaymentStatus());
    }

    @Test
    @DisplayName("Should delete payment successfully")
    void deletePayment_ShouldReturnSuccess() throws Exception {
        // Given
        Payment savedPayment = paymentRepository.save(testPayment);
        assertEquals(1, paymentRepository.count());

        // When & Then
        mockMvc.perform(delete("/api/payments/{id}", savedPayment.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", is(true)));

        // Verify payment was deleted
        assertEquals(0, paymentRepository.count());
    }

    // @Test
    // @DisplayName("Should handle invalid payment id format")
    // void getPaymentById_ShouldReturnBadRequest_WhenInvalidIdFormat() throws Exception {
    //     // When & Then
    //     mockMvc.perform(get("/api/payments/{id}", "invalid-id")
    //             .contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(status().is5xxServerError());
    // }

    @Test
    @DisplayName("Should handle payment status transitions")
    void updatePayment_ShouldHandleStatusTransitions() throws Exception {
        // Given - Start with NOT_STARTED
        Payment savedPayment = paymentRepository.save(testPayment);

        // Transition to IN_PROGRESS
        PaymentDto inProgressDto = PaymentDto.builder()
                .paymentId(savedPayment.getPaymentId())
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(mockOrderDto)
                .build();

        String inProgressJson = objectMapper.writeValueAsString(inProgressDto);

        mockMvc.perform(put("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inProgressJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.isPayed", is(false)));

        // Transition to COMPLETED
        PaymentDto completedDto = PaymentDto.builder()
                .paymentId(savedPayment.getPaymentId())
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(mockOrderDto)
                .build();

        String completedJson = objectMapper.writeValueAsString(completedDto);

        mockMvc.perform(put("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(completedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("COMPLETED")))
                .andExpect(jsonPath("$.isPayed", is(true)));
    }

    @Test
    @DisplayName("Should return empty collection when no payments exist")
    void getAllPayments_ShouldReturnEmptyCollection_WhenNoPayments() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.collection", hasSize(0)));
    }
}
