from locust import HttpUser, task, between
import random
import json
from faker import Faker

fake = Faker()

class EcommerceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Called when a user starts"""
        self.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    
    @task(3)
    def test_health_check(self):
        """Test health endpoints for all services"""
        services = [
            "/api/payment-service/actuator/health",
            "/api/order-service/actuator/health", 
            "/api/favourite-service/actuator/health"
        ]
        
        service = random.choice(services)
        self.client.get(service, headers=self.headers)
    
    @task(2)
    def test_payment_operations(self):
        """Test payment service operations"""
        # Get all payments
        self.client.get("/api/payment-service/payments", headers=self.headers)
        
        # Create a new payment
        payment_data = {
            "paymentStatus": "ACCEPTED",
            "orderId": random.randint(1, 100),
            "userId": random.randint(1, 50)
        }
        
        response = self.client.post(
            "/api/payment-service/payments",
            json=payment_data,
            headers=self.headers
        )
        
        if response.status_code == 201:
            # Try to get the created payment
            payment_id = response.json().get("paymentId")
            if payment_id:
                self.client.get(f"/api/payment-service/payments/{payment_id}", headers=self.headers)
    
    @task(2)
    def test_order_operations(self):
        """Test order service operations"""
        # Get all orders
        self.client.get("/api/order-service/orders", headers=self.headers)
        
        # Create a new order
        order_data = {
            "orderStatus": "PENDING",
            "orderDate": fake.date_time().isoformat(),
            "userId": random.randint(1, 50)
        }
        
        response = self.client.post(
            "/api/order-service/orders",
            json=order_data,
            headers=self.headers
        )
        
        if response.status_code == 201:
            # Try to get the created order
            order_id = response.json().get("orderId")
            if order_id:
                self.client.get(f"/api/order-service/orders/{order_id}", headers=self.headers)
    
    @task(1)
    def test_favourite_operations(self):
        """Test favourite service operations"""
        # Get all favourites
        self.client.get("/api/favourite-service/favourites", headers=self.headers)
        
        # Create a new favourite
        favourite_data = {
            "userId": random.randint(1, 50),
            "productId": random.randint(1, 100)
        }
        
        response = self.client.post(
            "/api/favourite-service/favourites",
            json=favourite_data,
            headers=self.headers
        )
        
        if response.status_code == 201:
            # Try to get favourites by user
            user_id = favourite_data["userId"]
            self.client.get(f"/api/favourite-service/favourites/user/{user_id}", headers=self.headers)
