from locust import HttpUser, task, between
import random
import json

class FavouriteServiceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Called when a user starts"""
        self.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
        self.base_url = "/favourite-service/api"
    
    @task(3)
    def get_all_favourites(self):
        """Get all favourites"""
        self.client.get(f"{self.base_url}/favourites", headers=self.headers)
    
    @task(2)
    def create_favourite(self):
        """Create a new favourite"""
        favourite_data = {
            "userId": random.randint(1, 50),
            "productId": random.randint(1, 100)
        }
        
        response = self.client.post(
            f"{self.base_url}/favourites",
            json=favourite_data,
            headers=self.headers
        )
        
        if response.status_code == 201:
            favourite_id = response.json().get("favouriteId")
            if favourite_id:
                # Get the created favourite
                self.client.get(f"{self.base_url}/favourites/{favourite_id}", headers=self.headers)
    
    @task(2)
    def get_favourites_by_user(self):
        """Get favourites by user ID"""
        user_id = random.randint(1, 50)
        self.client.get(f"{self.base_url}/favourites/user/{user_id}", headers=self.headers)
    
    @task(1)
    def get_favourite_by_id(self):
        """Get favourite by ID"""
        favourite_id = random.randint(1, 20)
        self.client.get(f"{self.base_url}/favourites/{favourite_id}", headers=self.headers)
    
    @task(1)
    def delete_favourite(self):
        """Delete a favourite"""
        favourite_id = random.randint(1, 20)
        self.client.delete(f"{self.base_url}/favourites/{favourite_id}", headers=self.headers)
    
    @task(1)
    def health_check(self):
        """Check service health"""
        self.client.get(f"{self.base_url}/actuator/health", headers=self.headers)
