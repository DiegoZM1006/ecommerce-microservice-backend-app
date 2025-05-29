from locust import HttpUser, task, between
import random
import json
from faker import Faker

fake = Faker()

class OrderServiceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Called when a user starts"""
        self.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
        self.base_url = "/order-service/api"
    
    @task(3)
    def get_all_orders(self):
        """Get all orders"""
        self.client.get(f"{self.base_url}/orders", headers=self.headers)
    
    @task(2)
    def create_order(self):
        """Create a new order"""
        order_statuses = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"]
        
        order_data = {
            "orderStatus": random.choice(order_statuses),
            "orderDate": fake.date_time().isoformat(),
            "userId": random.randint(1, 50),
            "totalAmount": round(random.uniform(10.0, 500.0), 2)
        }
        
        response = self.client.post(
            f"{self.base_url}/orders",
            json=order_data,
            headers=self.headers
        )
        
        if response.status_code == 201:
            order_id = response.json().get("orderId")
            if order_id:
                # Get the created order
                self.client.get(f"{self.base_url}/orders/{order_id}", headers=self.headers)
    
    @task(2)
    def get_orders_by_user(self):
        """Get orders by user ID"""
        user_id = random.randint(1, 50)
        self.client.get(f"{self.base_url}/orders/user/{user_id}", headers=self.headers)
    
    @task(1)
    def get_order_by_id(self):
        """Get order by ID"""
        order_id = random.randint(1, 20)
        self.client.get(f"{self.base_url}/orders/{order_id}", headers=self.headers)
    
    @task(1)
    def update_order_status(self):
        """Update order status"""
        order_id = random.randint(1, 20)
        order_statuses = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"]
        
        update_data = {
            "orderStatus": random.choice(order_statuses)
        }
        
        self.client.put(
            f"{self.base_url}/orders/{order_id}",
            json=update_data,
            headers=self.headers
        )
    
    @task(1)
    def delete_order(self):
        """Delete an order"""
        order_id = random.randint(1, 20)
        self.client.delete(f"{self.base_url}/orders/{order_id}", headers=self.headers)
    
    @task(1)
    def health_check(self):
        """Check service health"""
        self.client.get(f"{self.base_url}/actuator/health", headers=self.headers)
