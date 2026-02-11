# Development

## Prerequisites
- JDK 21
- Docker (for containerized deployment)

## Build
```bash
./gradlew bootJar
```

## Run locally
```bash
java -jar build/libs/pension-engine-1.0.0.jar
```
REST server starts on port 8080, gRPC server on port 9090.

## Run with Docker
```bash
docker build -t pension-engine .
docker run -p 8080:8080 -p 9090:9090 pension-engine
```

## Test

Run the test suite:
```bash
bash test-cases/run-tests.sh
```

Run the Python test script (REST only):
```bash
python3 test_api.py --mode rest
```

Run the Python test script (REST + gRPC):
```bash
pip install grpcio grpcio-tools requests
python3 test_api.py --mode both
```

Quick smoke test:
```bash
curl -X POST http://localhost:8080/calculation-requests \
  -H "Content-Type: application/json" \
  -d '{
    "tenant_id": "test",
    "calculation_instructions": {
      "mutations": [{
        "mutation_id": "00000000-0000-0000-0000-000000000001",
        "mutation_definition_name": "create_dossier",
        "mutation_type": "DOSSIER_CREATION",
        "actual_at": "2025-01-01",
        "mutation_properties": {
          "dossier_id": "11111111-1111-1111-1111-111111111111",
          "person_id": "22222222-2222-2222-2222-222222222222",
          "name": "Test Person",
          "birth_date": "1965-01-01"
        }
      }]
    }
  }'
```

## Environment Variables
| Variable | Description | Default |
|---|---|---|
| `PORT` | REST server port | `8080` |
| `GRPC_PORT` | gRPC server port | `9090` |
| `SCHEME_REGISTRY_URL` | External scheme registry base URL (bonus feature) | not set (uses default accrual rate 0.02) |
