package io.lukmandev.excel_pdf_crud.employee.controller;

import io.lukmandev.excel_pdf_crud.common.ApiResponse;
import io.lukmandev.excel_pdf_crud.employee.dto.response.ImportResultDto;
import io.lukmandev.excel_pdf_crud.employee.service.EmployeeImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Import", description = "Bulk employee import from Excel")
public class EmployeeImportController {

    private final EmployeeImportService employeeImportService;

    @Operation(summary = "Bulk import employees from an .xlsx file")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Import completed (check successCount/failureCount)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error reading the Excel file")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResultDto>> importEmployees(
            @RequestParam("file") MultipartFile file) {
        ImportResultDto result = employeeImportService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.ok("Import completed", result));
    }
}
