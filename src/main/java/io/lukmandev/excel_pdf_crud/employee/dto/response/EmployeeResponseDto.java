package io.lukmandev.excel_pdf_crud.employee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee details returned by the API")
public class EmployeeResponseDto {

    @Schema(description = "Unique identifier", example = "1")
    private Long id;

    @Schema(description = "Employee's first name", example = "John")
    private String firstName;

    @Schema(description = "Employee's last name", example = "Doe")
    private String lastName;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Department", example = "Engineering")
    private String department;

    @Schema(description = "Monthly salary", example = "75000.00")
    private BigDecimal salary;

    @Schema(description = "Date the employee joined", example = "2023-01-15")
    private LocalDate dateOfJoining;

    @Schema(description = "Whether the employee is active", example = "true")
    private Boolean active;

    @Schema(description = "Record creation timestamp", example = "2023-01-15T09:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-06-01T14:30:00")
    private LocalDateTime updatedAt;
}
