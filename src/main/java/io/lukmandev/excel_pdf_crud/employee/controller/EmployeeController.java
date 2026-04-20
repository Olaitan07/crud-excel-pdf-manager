package io.lukmandev.excel_pdf_crud.employee.controller;

import io.lukmandev.excel_pdf_crud.common.ApiResponse;
import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeePartialUpdateDto;
import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeeRequestDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.EmployeeResponseDto;
import io.lukmandev.excel_pdf_crud.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management operations")
public class EmployeeController {

    private final EmployeeService employeeService;

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Operation(summary = "Create a new employee")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Employee created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> createEmployee(@Valid @RequestBody EmployeeRequestDto dto) {
        EmployeeResponseDto created = employeeService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee created successfully", created));
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Get all employees")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of all employees")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponseDto>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.findAll()));
    }

    @Operation(summary = "Get employee by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> findById(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.findById(id)));
    }

    @Operation(summary = "Get employees by salary range")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employees within the salary range"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid salary range parameters")
    })
    @GetMapping("/salary-range")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDto>>> findBySalaryRange(
            @Parameter(description = "Minimum salary (inclusive)", required = true, example = "50000")
            @RequestParam BigDecimal min,
            @Parameter(description = "Maximum salary (inclusive)", required = true, example = "100000")
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.findBySalaryRange(min, max)));
    }

    // ── FULL UPDATE (PUT) ─────────────────────────────────────────────────────

    @Operation(summary = "Fully update an employee", description = "Replaces all employee fields. All fields are required.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already in use by another employee")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> fullUpdate(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Employee updated successfully", employeeService.fullUpdate(id, dto)));
    }

    // ── PARTIAL UPDATE (PATCH) ────────────────────────────────────────────────

    @Operation(summary = "Partially update an employee", description = "Updates only the provided fields. Omitted fields remain unchanged.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee partially updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed on provided fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already in use by another employee")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> partialUpdate(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody EmployeePartialUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Employee updated successfully", employeeService.partialUpdate(id, dto)));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Operation(summary = "Soft-delete an employee", description = "Marks the employee as inactive. The record is retained in the database.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Employee deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id) {
        employeeService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Hard-delete an employee", description = "Permanently removes the employee record from the database.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Employee permanently deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id) {
        employeeService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
