package com.selimhorri.app.resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.service.PaymentService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Resource Tests")
class PaymentResourceTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentResource paymentResource;

    private PaymentDto paymentDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        orderDto = OrderDto.builder()
                .orderId(1)
                .build();

        paymentDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(orderDto)
                .build();
    }

    @Test
    @DisplayName("Should return all payments successfully")
    void findAll_ShouldReturnAllPayments() {
        // Given
        List<PaymentDto> payments = Arrays.asList(paymentDto);
        when(paymentService.findAll()).thenReturn(payments);

        // When
        ResponseEntity<DtoCollectionResponse<PaymentDto>> response = paymentResource.findAll();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());        assertEquals(1, response.getBody().getCollection().size());
        PaymentDto firstPayment = response.getBody().getCollection().iterator().next();
        assertEquals(paymentDto.getPaymentId(), firstPayment.getPaymentId());
        
        verify(paymentService).findAll();
    }

    @Test
    @DisplayName("Should return empty collection when no payments exist")
    void findAll_ShouldReturnEmptyCollection_WhenNoPayments() {
        // Given
        when(paymentService.findAll()).thenReturn(Arrays.asList());

        // When
        ResponseEntity<DtoCollectionResponse<PaymentDto>> response = paymentResource.findAll();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getCollection().isEmpty());
        
        verify(paymentService).findAll();
    }

    @Test
    @DisplayName("Should return payment by id successfully")
    void findById_ShouldReturnPayment_WhenValidId() {
        // Given
        String paymentId = "1";
        when(paymentService.findById(1)).thenReturn(paymentDto);

        // When
        ResponseEntity<PaymentDto> response = paymentResource.findById(paymentId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(paymentDto.getPaymentId(), response.getBody().getPaymentId());
        assertEquals(paymentDto.getIsPayed(), response.getBody().getIsPayed());
        assertEquals(paymentDto.getPaymentStatus(), response.getBody().getPaymentStatus());
        
        verify(paymentService).findById(1);
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void findById_ShouldThrowException_WhenPaymentNotFound() {
        // Given
        String paymentId = "999";
        when(paymentService.findById(999)).thenThrow(new PaymentNotFoundException("Payment with id: 999 not found"));

        // When & Then
        assertThrows(PaymentNotFoundException.class, () -> paymentResource.findById(paymentId));
        verify(paymentService).findById(999);
    }

    @Test
    @DisplayName("Should save payment successfully")
    void save_ShouldSavePayment_WhenValidPaymentDto() {
        // Given
        when(paymentService.save(any(PaymentDto.class))).thenReturn(paymentDto);

        // When
        ResponseEntity<PaymentDto> response = paymentResource.save(paymentDto);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(paymentDto.getPaymentId(), response.getBody().getPaymentId());
        
        verify(paymentService).save(paymentDto);
    }

    @Test
    @DisplayName("Should update payment successfully")
    void update_ShouldUpdatePayment_WhenValidPaymentDto() {
        // Given
        PaymentDto updatedPaymentDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(orderDto)
                .build();

        when(paymentService.update(any(PaymentDto.class))).thenReturn(updatedPaymentDto);

        // When
        ResponseEntity<PaymentDto> response = paymentResource.update(updatedPaymentDto);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(updatedPaymentDto.getPaymentId(), response.getBody().getPaymentId());
        assertTrue(response.getBody().getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, response.getBody().getPaymentStatus());
        
        verify(paymentService).update(updatedPaymentDto);
    }

    @Test
    @DisplayName("Should delete payment successfully")
    void deleteById_ShouldDeletePayment_WhenValidId() {
        // Given
        String paymentId = "1";
        doNothing().when(paymentService).deleteById(1);

        // When
        ResponseEntity<Boolean> response = paymentResource.deleteById(paymentId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
        
        verify(paymentService).deleteById(1);
    }

    @Test
    @DisplayName("Should handle string to integer conversion for payment id")
    void findById_ShouldConvertStringToInteger() {
        // Given
        String paymentId = "123";
        when(paymentService.findById(123)).thenReturn(paymentDto);

        // When
        ResponseEntity<PaymentDto> response = paymentResource.findById(paymentId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService).findById(123);
    }

    @Test
    @DisplayName("Should handle NumberFormatException for invalid payment id")
    void findById_ShouldThrowException_WhenInvalidId() {
        // Given
        String invalidPaymentId = "invalid";

        // When & Then
        assertThrows(NumberFormatException.class, () -> paymentResource.findById(invalidPaymentId));
        verify(paymentService, never()).findById(anyInt());
    }

    @Test
    @DisplayName("Should handle different payment statuses in save operation")
    void save_ShouldHandleDifferentPaymentStatuses() {
        // Test for each payment status
        for (PaymentStatus status : PaymentStatus.values()) {
            // Given
            PaymentDto testPaymentDto = PaymentDto.builder()
                    .paymentId(1)
                    .isPayed(status == PaymentStatus.COMPLETED)
                    .paymentStatus(status)
                    .orderDto(orderDto)
                    .build();

            when(paymentService.save(any(PaymentDto.class))).thenReturn(testPaymentDto);

            // When
            ResponseEntity<PaymentDto> response = paymentResource.save(testPaymentDto);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(status, response.getBody().getPaymentStatus());
        }
    }
}
