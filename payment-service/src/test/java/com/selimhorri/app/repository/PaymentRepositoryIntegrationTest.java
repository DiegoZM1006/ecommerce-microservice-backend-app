package com.selimhorri.app.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Payment Repository Integration Tests")
class PaymentRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .orderId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();
    }

    @Test
    @DisplayName("Should save payment successfully")
    void save_ShouldPersistPayment() {
        // When
        Payment savedPayment = paymentRepository.save(testPayment);

        // Then
        assertNotNull(savedPayment.getPaymentId());
        assertEquals(testPayment.getOrderId(), savedPayment.getOrderId());
        assertEquals(testPayment.getIsPayed(), savedPayment.getIsPayed());
        assertEquals(testPayment.getPaymentStatus(), savedPayment.getPaymentStatus());

        // Verify it's actually persisted
        Payment foundPayment = entityManager.find(Payment.class, savedPayment.getPaymentId());
        assertNotNull(foundPayment);
        assertEquals(savedPayment.getPaymentId(), foundPayment.getPaymentId());
    }

    @Test
    @DisplayName("Should find payment by id successfully")
    void findById_ShouldReturnPayment_WhenPaymentExists() {
        // Given
        Payment persistedPayment = entityManager.persistAndFlush(testPayment);

        // When
        Optional<Payment> foundPayment = paymentRepository.findById(persistedPayment.getPaymentId());

        // Then
        assertTrue(foundPayment.isPresent());
        assertEquals(persistedPayment.getPaymentId(), foundPayment.get().getPaymentId());
        assertEquals(persistedPayment.getOrderId(), foundPayment.get().getOrderId());
        assertEquals(persistedPayment.getIsPayed(), foundPayment.get().getIsPayed());
        assertEquals(persistedPayment.getPaymentStatus(), foundPayment.get().getPaymentStatus());
    }

    @Test
    @DisplayName("Should return empty optional when payment not found")
    void findById_ShouldReturnEmpty_WhenPaymentNotExists() {
        // When
        Optional<Payment> foundPayment = paymentRepository.findById(999);

        // Then
        assertFalse(foundPayment.isPresent());
    }

    @Test
    @DisplayName("Should find all payments successfully")
    void findAll_ShouldReturnAllPayments() {
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

        entityManager.persistAndFlush(payment1);
        entityManager.persistAndFlush(payment2);

        // When
        List<Payment> payments = paymentRepository.findAll();

        // Then
        assertEquals(2, payments.size());
        assertTrue(payments.stream().anyMatch(p -> p.getOrderId().equals(1)));
        assertTrue(payments.stream().anyMatch(p -> p.getOrderId().equals(2)));
    }

    // @Test
    // @DisplayName("Should update payment successfully")
    // void save_ShouldUpdateExistingPayment() {
    //     // Given
    //     Payment persistedPayment = entityManager.persistAndFlush(testPayment);

    //     // When
    //     persistedPayment.setIsPayed(true);
    //     persistedPayment.setPaymentStatus(PaymentStatus.COMPLETED);
    //     Payment updatedPayment = paymentRepository.save(persistedPayment);

    //     // Then
    //     assertEquals(persistedPayment.getPaymentId(), updatedPayment.getPaymentId());
    //     assertTrue(updatedPayment.getIsPayed());
    //     assertEquals(PaymentStatus.COMPLETED, updatedPayment.getPaymentStatus());

    //     // Verify in database
    //     entityManager.clear();
    //     Payment foundPayment = entityManager.find(Payment.class, persistedPayment.getPaymentId());
    //     assertTrue(foundPayment.getIsPayed());
    //     assertEquals(PaymentStatus.COMPLETED, foundPayment.getPaymentStatus());
    // }

    @Test
    @DisplayName("Should delete payment successfully")
    void deleteById_ShouldRemovePayment() {
        // Given
        Payment persistedPayment = entityManager.persistAndFlush(testPayment);
        Integer paymentId = persistedPayment.getPaymentId();

        // Verify payment exists
        assertTrue(paymentRepository.existsById(paymentId));

        // When
        paymentRepository.deleteById(paymentId);

        // Then
        assertFalse(paymentRepository.existsById(paymentId));
        
        // Verify from entity manager
        entityManager.clear();
        Payment foundPayment = entityManager.find(Payment.class, paymentId);
        assertNull(foundPayment);
    }

    @Test
    @DisplayName("Should handle different payment statuses")
    void save_ShouldHandleAllPaymentStatuses() {
        for (PaymentStatus status : PaymentStatus.values()) {
            // Given
            Payment payment = Payment.builder()
                    .orderId(100 + status.ordinal())
                    .isPayed(status == PaymentStatus.COMPLETED)
                    .paymentStatus(status)
                    .build();

            // When
            Payment savedPayment = paymentRepository.save(payment);

            // Then
            assertNotNull(savedPayment.getPaymentId());
            assertEquals(status, savedPayment.getPaymentStatus());
            assertEquals(status == PaymentStatus.COMPLETED, savedPayment.getIsPayed());
        }
    }

    @Test
    @DisplayName("Should count payments correctly")
    void count_ShouldReturnCorrectCount() {
        // Given - no payments initially
        assertEquals(0, paymentRepository.count());

        // When
        entityManager.persistAndFlush(testPayment);

        // Then
        assertEquals(1, paymentRepository.count());

        // Add another payment
        Payment payment2 = Payment.builder()
                .orderId(2)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();
        entityManager.persistAndFlush(payment2);

        assertEquals(2, paymentRepository.count());
    }

    @Test
    @DisplayName("Should handle null values appropriately")
    void save_ShouldHandleNullValues() {
        // Given
        Payment paymentWithNulls = Payment.builder()
                .orderId(1)
                .isPayed(null)
                .paymentStatus(null)
                .build();

        // When
        Payment savedPayment = paymentRepository.save(paymentWithNulls);

        // Then
        assertNotNull(savedPayment.getPaymentId());
        assertEquals(1, savedPayment.getOrderId());
        assertNull(savedPayment.getIsPayed());
        assertNull(savedPayment.getPaymentStatus());
    }

    // @Test
    // @DisplayName("Should verify payment entity relationships")
    // void save_ShouldMaintainEntityIntegrity() {
    //     // Given
    //     Payment payment = Payment.builder()
    //             .orderId(123)
    //             .isPayed(false)
    //             .paymentStatus(PaymentStatus.IN_PROGRESS)
    //             .build();

    //     // When
    //     Payment savedPayment = paymentRepository.save(payment);

    //     // Then
    //     assertNotNull(savedPayment.getPaymentId());
    //     assertEquals(123, savedPayment.getOrderId());
    //       // Verify audit fields if they exist (from AbstractMappedEntity)
    //     assertNotNull(savedPayment.getCreatedAt());
    //     assertNotNull(savedPayment.getUpdatedAt());
    // }
}
