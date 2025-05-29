package com.selimhorri.app.helper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;

@DisplayName("Payment Mapping Helper Tests")
class PaymentMappingHelperTest {

    private Payment payment;
    private PaymentDto paymentDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .paymentId(1)
                .orderId(100)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        orderDto = OrderDto.builder()
                .orderId(100)
                .build();

        paymentDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(orderDto)
                .build();
    }

    @Test
    @DisplayName("Should map Payment entity to PaymentDto successfully")
    void map_ShouldMapPaymentToPaymentDto() {
        // When
        PaymentDto result = PaymentMappingHelper.map(payment);

        // Then
        assertNotNull(result);
        assertEquals(payment.getPaymentId(), result.getPaymentId());
        assertEquals(payment.getIsPayed(), result.getIsPayed());
        assertEquals(payment.getPaymentStatus(), result.getPaymentStatus());
        
        assertNotNull(result.getOrderDto());
        assertEquals(payment.getOrderId(), result.getOrderDto().getOrderId());
    }

    @Test
    @DisplayName("Should map PaymentDto to Payment entity successfully")
    void map_ShouldMapPaymentDtoToPayment() {
        // When
        Payment result = PaymentMappingHelper.map(paymentDto);

        // Then
        assertNotNull(result);
        assertEquals(paymentDto.getPaymentId(), result.getPaymentId());
        assertEquals(paymentDto.getIsPayed(), result.getIsPayed());
        assertEquals(paymentDto.getPaymentStatus(), result.getPaymentStatus());
        assertEquals(paymentDto.getOrderDto().getOrderId(), result.getOrderId());
    }

    @Test
    @DisplayName("Should handle payment with COMPLETED status")
    void map_ShouldHandleCompletedPayment() {
        // Given
        Payment completedPayment = Payment.builder()
                .paymentId(2)
                .orderId(200)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        // When
        PaymentDto result = PaymentMappingHelper.map(completedPayment);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        assertTrue(result.getIsPayed());
        assertEquals(200, result.getOrderDto().getOrderId());
    }

    @Test
    @DisplayName("Should handle payment with IN_PROGRESS status")
    void map_ShouldHandleInProgressPayment() {
        // Given
        PaymentDto inProgressDto = PaymentDto.builder()
                .paymentId(3)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder().orderId(300).build())
                .build();

        // When
        Payment result = PaymentMappingHelper.map(inProgressDto);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.IN_PROGRESS, result.getPaymentStatus());
        assertFalse(result.getIsPayed());
        assertEquals(300, result.getOrderId());
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void map_ShouldHandleNullPaymentId() {
        // Given
        Payment paymentWithNullId = Payment.builder()
                .paymentId(null)
                .orderId(400)
                .isPayed(null)
                .paymentStatus(null)
                .build();

        // When
        PaymentDto result = PaymentMappingHelper.map(paymentWithNullId);

        // Then
        assertNotNull(result);
        assertNull(result.getPaymentId());
        assertNull(result.getIsPayed());
        assertNull(result.getPaymentStatus());
        assertEquals(400, result.getOrderDto().getOrderId());
    }

    @Test
    @DisplayName("Should preserve all payment statuses during mapping")
    void map_ShouldPreserveAllPaymentStatuses() {
        for (PaymentStatus status : PaymentStatus.values()) {
            // Given
            Payment testPayment = Payment.builder()
                    .paymentId(1)
                    .orderId(100)
                    .isPayed(status == PaymentStatus.COMPLETED)
                    .paymentStatus(status)
                    .build();

            // When
            PaymentDto result = PaymentMappingHelper.map(testPayment);

            // Then
            assertEquals(status, result.getPaymentStatus());
            assertEquals(status == PaymentStatus.COMPLETED, result.getIsPayed());
        }
    }
}
