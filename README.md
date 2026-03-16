# CliniQ Backend

Clinic Queue & Token Management System — a Spring Boot REST API backend for managing patient queues in small to mid-sized clinics with walk-in patients.

## About the Project

CliniQ automates the patient queue workflow at clinics. Receptionists register walk-in patients and generate sequential daily tokens. Doctors manage the queue by calling patients, marking visits as completed or no-show. A public display screen shows the current queue status without exposing any personal patient data.

The system is built following Agile Scrum practices as part of the MSc Software Engineering curriculum at SLIIT.

## Tech Stack

- Java 17
- Spring Boot 3.4.3
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA / Hibernate
- PostgreSQL
- Lombok
- Maven

## API Endpoints

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/login` | No | Login with username/password, returns JWT |

### Patient Registration
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/patients/register` | RECEPTIONIST, DOCTOR | Register walk-in patient, generates token |

### Queue Management
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/queue/display` | No | Public queue display (tokens only) |
| GET | `/api/queue/today` | RECEPTIONIST, DOCTOR | Staff view with patient details |
| POST | `/api/queue/manage/call-next` | DOCTOR | Call next waiting patient |
| PATCH | `/api/queue/manage/visits/{id}/status` | DOCTOR | Update visit status |

## Project Structure

```
be-cliniQ/
├── src/main/java/com/cliniq/
│   ├── CliniqBackendApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── CorsConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── PatientController.java
│   │   └── QueueController.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── PatientRegistrationRequest.java
│   │   ├── PatientRegistrationResponse.java
│   │   ├── QueueDisplayResponse.java
│   │   ├── StatusUpdateRequest.java
│   │   └── VisitResponse.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Patient.java
│   │   └── Visit.java
│   ├── enums/
│   │   ├── Role.java
│   │   └── VisitStatus.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── PatientRepository.java
│   │   └── VisitRepository.java
│   ├── security/
│   │   ├── JwtUtils.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   ├── CustomUserDetails.java
│   │   └── CustomUserDetailsService.java
│   └── service/
│       ├── PatientService.java
│       └── QueueService.java
├── src/main/resources/
│   └── application.properties
├── schema.sql
├── pom.xml
├── Dockerfile
├── run.sh
└── CliniQ.postman_collection.json
```

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

## Database Setup

1. Create the database (if it doesn't exist):
```sql
CREATE DATABASE msc;
```

2. Run the schema script against the `msc` database:
```bash
psql -U postgres -d msc -f schema.sql
```

This creates the `cliniq` schema, all tables, indexes, and seeds two default users:
- **Doctor:** username `admin`, password `admin123`
- **Receptionist:** username `reception`, password `reception123`

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/msc?currentSchema=cliniq` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `postgres` |
| `JWT_SECRET` | Base64-encoded 256-bit key | **required, no default** |
| `JWT_EXPIRATION_MS` | Token expiry in milliseconds | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins | `http://localhost:3000` |
| `SERVER_PORT` | Server port | `8080` |
| `JPA_DDL_AUTO` | Hibernate DDL mode | `validate` |
| `JPA_SHOW_SQL` | Log SQL queries | `false` |

### Generating a JWT Secret

```bash
openssl rand -base64 32
```

## Running the Application

### Using the run script
```bash
chmod +x run.sh
./run.sh
```

The `run.sh` script exports all required environment variables and starts the application with Maven.

### Using Maven directly
```bash
export JWT_SECRET=$(openssl rand -base64 32)
./mvnw spring-boot:run
```

### Using Docker
```bash
docker build -t cliniq-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/msc?currentSchema=cliniq \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e JWT_SECRET=$(openssl rand -base64 32) \
  cliniq-backend
```

## Testing with Postman

Import `CliniQ.postman_collection.json` into Postman. The collection includes 14 requests across 4 folders with test scripts that auto-save the JWT token for authenticated requests.

Recommended test flow:
1. Login as Receptionist
2. Register 2-3 patients (tokens are auto-generated: 1, 2, 3...)
3. Login as Doctor
4. Call next patient
5. Mark as completed or no-show
6. Check public display (no auth needed)
