#!/bin/bash
set -e

echo "=========================================="
echo "LearnHub Backend - Docker Startup Script"
echo "=========================================="
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Copy .env.example to .env and fill in required values"
    echo "Example:"
    echo "  cp .env.example .env"
    echo "  nano .env  # Edit with your values"
    exit 1
fi

# Load environment
set -a
source .env
set +a

echo "Starting services..."
echo "- PostgreSQL 15 (port 5432)"
echo "- Redis 7 (port 6379)"
echo "- LearnHub Backend (port 8080)"
echo ""

# Start Docker Compose
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to be healthy..."
sleep 3

# Check PostgreSQL
RETRIES=0
until docker-compose exec -T postgres pg_isready -U learnhub > /dev/null 2>&1; do
    RETRIES=$((RETRIES + 1))
    if [ $RETRIES -gt 30 ]; then
        echo "ERROR: PostgreSQL failed to start"
        docker-compose logs postgres
        exit 1
    fi
    echo "  Waiting for PostgreSQL... ($RETRIES/30)"
    sleep 1
done
echo "✓ PostgreSQL ready"

# Check Redis
RETRIES=0
until docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; do
    RETRIES=$((RETRIES + 1))
    if [ $RETRIES -gt 30 ]; then
        echo "ERROR: Redis failed to start"
        docker-compose logs redis
        exit 1
    fi
    echo "  Waiting for Redis... ($RETRIES/30)"
    sleep 1
done
echo "✓ Redis ready"

# Check Backend
RETRIES=0
until curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; do
    RETRIES=$((RETRIES + 1))
    if [ $RETRIES -gt 60 ]; then
        echo "ERROR: Backend failed to start"
        docker-compose logs learnhub-app
        exit 1
    fi
    echo "  Waiting for Backend... ($RETRIES/60)"
    sleep 1
done
echo "✓ Backend ready"

echo ""
echo "=========================================="
echo "✓ All services started successfully!"
echo "=========================================="
echo ""
echo "LearnHub Backend is running at:"
echo "  http://localhost:8080"
echo ""
echo "API Documentation:"
echo "  http://localhost:8080/swagger-ui.html"
echo ""
echo "Useful commands:"
echo "  View logs:         docker-compose logs -f learnhub-app"
echo "  Stop services:     docker-compose down"
echo "  Stop & clean:      docker-compose down -v"
echo "  Database shell:    docker-compose exec postgres psql -U learnhub -d learnhub"
echo "  Redis CLI:         docker-compose exec redis redis-cli"
echo ""
echo "=========================================="
