# Excel PDF CRUD — Employee Management API

Spring Boot REST API for managing employees with Excel bulk import support.

## Tech Stack
- Java 17, Spring Boot 4.0.5
- H2 (in-memory), JPA / Hibernate
- Apache POI 5.4.1 (xlsx parsing)
- MapStruct 1.6.3, Lombok
- SpringDoc OpenAPI 3 (Swagger UI)

## Running

```bash
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## API Reference

### Employee CRUD — `POST /api/employees`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/employees` | Create employee |
| GET | `/api/employees` | List all employees |
| GET | `/api/employees/{id}` | Get by ID |
| GET | `/api/employees/salary-range?min=&max=` | Filter by salary range |
| PUT | `/api/employees/{id}` | Full update |
| PATCH | `/api/employees/{id}` | Partial update |
| DELETE | `/api/employees/{id}` | Soft-delete |
| DELETE | `/api/employees/{id}/hard` | Hard-delete |

### Bulk Import / Export — v1

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/employees/import` | Import employees from `.xlsx` |
| GET | `/api/v1/employees/export/excel` | Export employees to `.xlsx` |
| GET | `/api/v1/employees/export/pdf` | Export employees to `.pdf` |

#### Export Query Params

| Param | Type | Description |
|-------|------|-------------|
| `department` | string | Filter by department (optional) |
| `active` | boolean | Filter by active status (optional) |

Response headers:
- `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- `Content-Disposition: attachment; filename="employees_<yyyyMMdd_HHmmss>.xlsx"`

Excel columns: ID, First Name, Last Name, Email, Department, Salary, Date of Joining, Active, Created At

---

#### Import Rules
- **File field name**: `file` (multipart/form-data)
- Only `.xlsx` accepted — `.xls` and other formats return HTTP 400
- Row 0 is treated as the header and skipped
- Each data row is validated against all Bean Validation constraints before saving
- All errors per row are collected (no fail-fast)
- Business rule failures (duplicate email, salary below floor) are reported per-row
- Only system errors cause a transaction rollback

#### Excel Column Order

| Col | Field | Constraints |
|-----|-------|-------------|
| 0 | firstName | Not blank, max 50 |
| 1 | lastName | Not blank, max 50 |
| 2 | email | Not blank, valid email |
| 3 | department | Not blank, max 100 |
| 4 | salary | Not null, ≥ 0.00; floor: 15 000 (Intern), 30 000 (others) |
| 5 | dateOfJoining | Not null, past or present (YYYY-MM-DD or Excel date) |
| 6 | active | Not null (TRUE/FALSE or 1/0) |

#### Response

```json
{
  "success": true,
  "message": "Import completed",
  "data": {
    "successCount": 3,
    "failureCount": 1,
    "errors": [
      "Row 2: email – Invalid email format"
    ]
  }
}
```

---

## Versions

| Version | Changes |
|---------|---------|
| 0.0.1 | Initial CRUD endpoints |
| 0.0.2 | Bulk Excel import (`POST /api/v1/employees/import`) |
| 0.0.3 | Excel export with optional filters (`GET /api/v1/employees/export/excel`) |
| 0.0.4 | PDF report export with optional filters (`GET /api/v1/employees/export/pdf`) |
