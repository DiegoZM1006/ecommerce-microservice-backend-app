package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Implementation Tests")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private PaymentDto paymentDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        orderDto = OrderDto.builder()
                .orderId(1)
                .build();

        payment = Payment.builder()
                .paymentId(1)
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
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
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findAll()).thenReturn(payments);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(orderDto);

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(payment.getPaymentId(), result.get(0).getPaymentId());
        assertEquals(payment.getIsPayed(), result.get(0).getIsPayed());
        assertEquals(payment.getPaymentStatus(), result.get(0).getPaymentStatus());
        
        verify(paymentRepository).findAll();
        verify(restTemplate).getForObject(anyString(), eq(OrderDto.class));
    }

    @Test
    @DisplayName("Should return empty list when no payments exist")
    void findAll_ShouldReturnEmptyList_WhenNoPaymentsExist() {
        // Given
        when(paymentRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(paymentRepository).findAll();
        verify(restTemplate, never()).getForObject(anyString(), eq(OrderDto.class));
    }

    @Test
    @DisplayName("Should return payment by id successfully")
    void findById_ShouldReturnPayment_WhenPaymentExists() {
        // Given
        Integer paymentId = 1;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(orderDto);

        // When
        PaymentDto result = paymentService.findById(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(payment.getPaymentId(), result.getPaymentId());
        assertEquals(payment.getIsPayed(), result.getIsPayed());
        assertEquals(payment.getPaymentStatus(), result.getPaymentStatus());
        assertNotNull(result.getOrderDto());
        
        verify(paymentRepository).findById(paymentId);
        verify(restTemplate).getForObject(anyString(), eq(OrderDto.class));
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when payment not found")
    void findById_ShouldThrowException_WhenPaymentNotFound() {
        // Given
        Integer paymentId = 999;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        PaymentNotFoundException exception = assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.findById(paymentId)
        );

        assertEquals("Payment with id: 999 not found", exception.getMessage());
        verify(paymentRepository).findById(paymentId);
        verify(restTemplate, never()).getForObject(anyString(), eq(OrderDto.class));
    }

    @Test
    @DisplayName("Should save payment successfully")
    void save_ShouldSavePayment_WhenValidPaymentDto() {
        // Given
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        PaymentDto result = paymentService.save(paymentDto);

        // Then
        assertNotNull(result);
        assertEquals(payment.getPaymentId(), result.getPaymentId());
        assertEquals(payment.getIsPayed(), result.getIsPayed());
        assertEquals(payment.getPaymentStatus(), result.getPaymentStatus());
        
        verify(paymentRepository).save(any(Payment.class));
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

        Payment updatedPayment = Payment.builder()
                .paymentId(1)
                .orderId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(updatedPayment);

        // When
        PaymentDto result = paymentService.update(updatedPaymentDto);

        // Then
        assertNotNull(result);
        assertEquals(updatedPayment.getPaymentId(), result.getPaymentId());
        assertEquals(updatedPayment.getIsPayed(), result.getIsPayed());
        assertEquals(updatedPayment.getPaymentStatus(), result.getPaymentStatus());
        
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should delete payment by id successfully")
    void deleteById_ShouldDeletePayment_WhenValidId() {
        // Given
        Integer paymentId = 1;

        // When
        paymentService.deleteById(paymentId);

        // Then
        verify(paymentRepository).deleteById(paymentId);
    }

    @Test
    @DisplayName("Should handle null payment dto gracefully")
    void save_ShouldHandleNullPaymentDto() {
        // Given
        PaymentDto nullPaymentDto = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> paymentService.save(nullPaymentDto));
    }

    @Test
    @DisplayName("Should handle payment with different statuses")
    void findById_ShouldHandlePaymentWithDifferentStatuses() {
        // Given
        Payment inProgressPayment = Payment.builder()
                .paymentId(2)
                .orderId(2)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .build();

        when(paymentRepository.findById(2)).thenReturn(Optional.of(inProgressPayment));
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(orderDto);

        // When
        PaymentDto result = paymentService.findById(2);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.IN_PROGRESS, result.getPaymentStatus());
        assertFalse(result.getIsPayed());
    }
}
