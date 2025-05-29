FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy locust files
COPY . .

# Expose locust web interface port
EXPOSE 8089

# Run locust
ENTRYPOINT ["locust"]
