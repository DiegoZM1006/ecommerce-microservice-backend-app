from locust import HttpUser, task, between
import random
import json

class PaymentServiceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Called when a user starts"""
        self.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
        self.base_url = "/payment-service/api"
    
    @task(3)
    def get_all_payments(self):
        """Get all payments"""
        self.client.get(f"{self.base_url}/payments", headers=self.headers)
    
    @task(2)
    def create_payment(self):
        """Create a new payment"""
        payment_statuses = ["ACCEPTED", "REJECTED", "PENDING", "COMPLETED"]
        
        payment_data = {
            "paymentStatus": random.choice(payment_statuses),
            "orderId": random.randint(1, 100),
            "userId": random.randint(1, 50),
            "amount": round(random.uniform(10.0, 500.0), 2)
        }
        
        response = self.client.post(
            f"{self.base_url}/payments",
            json=payment_data,
            headers=self.headers
        )
        
        if response.status_code == 201:
            payment_id = response.json().get("paymentId")
            if payment_id:
                # Get the created payment
                self.client.get(f"{self.base_url}/payments/{payment_id}", headers=self.headers)
    
    @task(2)
    def get_payments_by_user(self):
        """Get payments by user ID"""
        user_id = random.randint(1, 50)
        self.client.get(f"{self.base_url}/payments/user/{user_id}", headers=self.headers)
    
    @task(2)
    def get_payments_by_order(self):
        """Get payments by order ID"""
        order_id = random.randint(1, 100)
        self.client.get(f"{self.base_url}/payments/order/{order_id}", headers=self.headers)
    
    @task(1)
    def get_payment_by_id(self):
        """Get payment by ID"""
        payment_id = random.randint(1, 20)
        self.client.get(f"{self.base_url}/payments/{payment_id}", headers=self.headers)
    
    @task(1)
    def update_payment_status(self):
        """Update payment status"""
        payment_id = random.randint(1, 20)
        payment_statuses = ["ACCEPTED", "REJECTED", "PENDING", "COMPLETED"]
        
        update_data = {
            "paymentStatus": random.choice(payment_statuses)
        }
        
        self.client.put(
            f"{self.base_url}/payments/{payment_id}",
            json=update_data,
            headers=self.headers
        )
    
    @task(1)
    def delete_payment(self):
        """Delete a payment"""
        payment_id = random.randint(1, 20)
        self.client.delete(f"{self.base_url}/payments/{payment_id}", headers=self.headers)
    
    @task(1)
    def health_check(self):
        """Check service health"""
        self.client.get(f"{self.base_url}/actuator/health", headers=self.headers)
