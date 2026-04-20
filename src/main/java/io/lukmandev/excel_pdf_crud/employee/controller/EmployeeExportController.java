package io.lukmandev.excel_pdf_crud.employee.controller;

import io.lukmandev.excel_pdf_crud.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Export", description = "Export employees to Excel")
public class EmployeeExportController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final EmployeeService employeeService;

    @Operation(summary = "Export employees to .xlsx",
               description = "Streams all employees (optionally filtered) directly as an Excel file.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Excel file streamed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error generating Excel file")
    })
    @GetMapping("/export/excel")
    public void exportExcel(
            @Parameter(description = "Filter by department (optional)", example = "Engineering")
            @RequestParam(required = false) String department,
            @Parameter(description = "Filter by active status (optional)", example = "true")
            @RequestParam(required = false) Boolean active,
            HttpServletResponse response) throws IOException {

        String filename = "employees_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".xlsx";
        response.setContentType(XLSX_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString());

        employeeService.exportToExcel(department, active, response.getOutputStream());
    }

    @Operation(summary = "Export employees to PDF",
               description = "Streams all employees (optionally filtered) directly as a PDF report.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF report streamed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error generating PDF report")
    })
    @GetMapping("/export/pdf")
    public void exportPdf(
            @Parameter(description = "Filter by department (optional)", example = "Engineering")
            @RequestParam(required = false) String department,
            @Parameter(description = "Filter by active status (optional)", example = "true")
            @RequestParam(required = false) Boolean active,
            HttpServletResponse response) throws IOException {

        String filename = "employee_report_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString());

        employeeService.exportToPdf(department, active, response.getOutputStream());
    }
}
