.PHONY: build run stop test clean logs

## Build and run with Docker Compose
run:
	docker-compose up --build -d

## Stop all containers
stop:
	docker-compose down

## Stop and remove volumes
clean:
	docker-compose down -v

## View logs
logs:
	docker-compose logs -f app

## Run tests locally (requires Docker for Testcontainers)
test:
	./mvnw test

## Run integration tests
integration-test:
	./mvnw verify

## Build JAR only
build:
	./mvnw package -DskipTests
