.PHONY: build run stop test clean logs

## Build and run with Docker Compose
run:
	docker compose up --build -d

## Stop all containers
stop:
	docker compose down

## Stop and remove volumes
clean:
	docker compose down -v

## View logs
logs:
	docker compose logs -f app

## Run tests locally (requires Maven + Docker for Testcontainers)
test:
	mvn test

## Run integration tests
integration-test:
	mvn verify

## Build JAR only
build:
	mvn package -DskipTests