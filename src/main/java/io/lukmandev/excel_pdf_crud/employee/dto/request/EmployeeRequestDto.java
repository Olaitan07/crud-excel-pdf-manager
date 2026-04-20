package io.lukmandev.excel_pdf_crud.employee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for creating or fully updating an employee")
public class EmployeeRequestDto {

    @Schema(description = "Employee's first name", example = "John", maxLength = 50)
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Schema(description = "Employee's last name", example = "Doe", maxLength = 50)
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Schema(description = "Unique email address", example = "john.doe@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Department the employee belongs to", example = "Engineering", maxLength = 100)
    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Schema(description = "Monthly salary in base currency", example = "75000.00", minimum = "0.00")
    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.00", message = "Salary must be at least 0.00")
    private BigDecimal salary;

    @Schema(description = "Date the employee joined (cannot be in the future)", example = "2023-01-15")
    @NotNull(message = "Date of joining is required")
    @PastOrPresent(message = "Date of joining cannot be in the future")
    private LocalDate dateOfJoining;

    @Schema(description = "Whether the employee is currently active", example = "true", defaultValue = "true")
    @NotNull(message = "Active status is required")
    private Boolean active = true;
}
