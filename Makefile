.PHONY: help up infra down

infra: 
	@if command -v docker >/dev/null 2>&1; then docker compose up -d; else podman compose up -d; fi

up: 
	@./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

down: 
	@if command -v docker >/dev/null 2>&1; then docker compose down; else podman compose down; fi
