package io.lukmandev.excel_pdf_crud.employee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Payload for partially updating an employee — only provided fields are updated")
public class EmployeePartialUpdateDto {

    @Schema(description = "Employee's first name", example = "Jane", maxLength = 50)
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Schema(description = "Employee's last name", example = "Smith", maxLength = 50)
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Schema(description = "Unique email address", example = "jane.smith@example.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Department the employee belongs to", example = "Marketing", maxLength = 100)
    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Schema(description = "Monthly salary in base currency", example = "80000.00", minimum = "0.00")
    @DecimalMin(value = "0.00", message = "Salary must be at least 0.00")
    private BigDecimal salary;

    @Schema(description = "Date the employee joined (cannot be in the future)", example = "2024-06-01")
    @PastOrPresent(message = "Date of joining cannot be in the future")
    private LocalDate dateOfJoining;

    @Schema(description = "Whether the employee is currently active", example = "false")
    private Boolean active;
}
