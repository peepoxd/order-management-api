# Order Management API

Delivery order management system with **order state machine**, inventory control, and restaurant management.

## Key Concepts

### Order State Machine

Orders follow a strict state transition — invalid transitions are rejected:

```
PENDING → CONFIRMED → PREPARING → READY_FOR_PICKUP → OUT_FOR_DELIVERY → DELIVERED
   ↓          ↓           ↓
CANCELLED  CANCELLED  CANCELLED     (terminal states cannot transition)
```

Implemented as a `switch` expression in `OrderService.validateStatusTransition()` — no external state machine library needed.

### Inventory Management

- Stock is **decreased** when order is created (per menu item quantity)
- Stock is **restored** when order is cancelled (`restoreInventory()`)
- Validates: item belongs to restaurant, item is available, sufficient stock
- Cross-restaurant ordering is blocked at validation layer

### Domain Model

```
Restaurant (1) ──→ (*) MenuItem    (menu with stock tracking)
     │                    │
     ↓                    ↓
  Order (1) ──────→ (*) OrderItem  (price captured at order time)
```

- `priceAtOrder` — captures price at purchase time (price can change later)
- `OrderNumberGenerator` — generates unique order numbers
- `BigDecimal` for all monetary calculations (no floating point)

## Tech Stack

- Java 21, Spring Boot 3.3.0
- Spring Data JPA + PostgreSQL
- Lombok, Swagger/OpenAPI
- Docker Compose (PostgreSQL + Redis)

## API Endpoints

```
POST   /api/restaurants              Create restaurant
GET    /api/restaurants              List restaurants
GET    /api/restaurants/{id}/menu    Get menu items

POST   /api/orders                   Create order (validates stock, restaurant hours)
GET    /api/orders                   List orders (paginated)
PATCH  /api/orders/{id}/status       Update status (state machine enforced)
POST   /api/orders/{id}/cancel       Cancel + restore inventory

GET    /api/orders/statistics        Order count by status
```

## Run

```bash
docker-compose up -d          # PostgreSQL + Redis
./mvnw spring-boot:run        # Start API on :8080
```

Swagger UI: `http://localhost:8080/swagger-ui.html`
