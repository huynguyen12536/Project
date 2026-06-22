#!/bin/bash
set -e

echo "=========================================="
echo "LearnHub Backend - JAR Startup Script"
echo "=========================================="
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Copy .env.example to .env and fill in required values"
    exit 1
fi

# Load environment
set -a
source .env
set +a

# Check Java version
echo "Checking Java installation..."
JAVA_VERSION=$(java -version 2>&1 | grep "version" | head -1)
echo "  $JAVA_VERSION"
echo ""

# Verify required services are accessible
echo "Checking required services..."

if ! pg_isready -h "${SPRING_DATASOURCE_URL#*://}" -U learnhub > /dev/null 2>&1; then
    echo "ERROR: PostgreSQL is not accessible!"
    echo "Expected URL: $SPRING_DATASOURCE_URL"
    echo "Make sure PostgreSQL is running"
    exit 1
fi
echo "✓ PostgreSQL is accessible"

if ! redis-cli -h localhost ping > /dev/null 2>&1; then
    echo "ERROR: Redis is not accessible!"
    echo "Expected URL: $REDIS_URL"
    echo "Make sure Redis is running"
    exit 1
fi
echo "✓ Redis is accessible"

echo ""
echo "Building application..."
cd learnhub-backend
mvn clean package -DskipTests -q
JAR_FILE=$(ls target/learnhub-backend-*.jar 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Failed to build JAR"
    mvn clean package -DskipTests
    exit 1
fi

echo "✓ Built: $JAR_FILE"
echo ""
echo "=========================================="
echo "Starting LearnHub Backend..."
echo "=========================================="
echo ""

java -jar "$JAR_FILE"
