package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeeRequestDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.EmployeeResponseDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.ImportResultDto;
import io.lukmandev.excel_pdf_crud.employee.exception.DuplicateEmailException;
import io.lukmandev.excel_pdf_crud.employee.exception.InvalidFileFormatException;
import io.lukmandev.excel_pdf_crud.employee.exception.SalaryFloorException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EmployeeImportServiceImplTest {

    @Mock
    private EmployeeService employeeService;

    private EmployeeImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        importService = new EmployeeImportServiceImpl(employeeService, validator);
    }

    // ── NEGATIVE: wrong file extension ───────────────────────────────────────

    @Test
    void rejectsXlsFile() {
        MockMultipartFile file = new MockMultipartFile("file", "employees.xls",
                "application/vnd.ms-excel", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(InvalidFileFormatException.class);
    }

    @Test
    void rejectsNonSpreadsheetFile() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv",
                "text/csv", "a,b,c".getBytes());

        assertThatThrownBy(() -> importService.importFromExcel(file))
                .isInstanceOf(InvalidFileFormatException.class);
    }

    // ── POSITIVE: all rows valid ──────────────────────────────────────────────

    @Test
    void importsAllValidRows() throws Exception {
        MockMultipartFile file = buildXlsx(new Object[][]{
                {"John", "Doe", "john@example.com", "Engineering", 50000.00, "2022-01-10", true},
                {"Jane", "Smith", "jane@example.com", "HR", 35000.00, "2021-06-15", true}
        });

        when(employeeService.createEmployee(any())).thenReturn(new EmployeeResponseDto());

        ImportResultDto result = importService.importFromExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getErrors()).isEmpty();
    }

    // ── NEGATIVE: validation failure rows ────────────────────────────────────

    @Test
    void reportsValidationErrorsWithoutSaving() throws Exception {
        // Row with blank firstName and invalid email
        MockMultipartFile file = buildXlsx(new Object[][]{
                {"", "Doe", "not-an-email", "Engineering", 50000.00, "2022-01-10", true}
        });

        ImportResultDto result = importService.importFromExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Row 1"));
    }

    // ── NEGATIVE: business rule failures (salary / duplicate email) ───────────

    @Test
    void reportsSalaryFloorViolationAsRowError() throws Exception {
        MockMultipartFile file = buildXlsx(new Object[][]{
                {"John", "Doe", "john@example.com", "Engineering", 50000.00, "2022-01-10", true}
        });

        when(employeeService.createEmployee(any()))
                .thenThrow(new SalaryFloorException("Engineering", BigDecimal.valueOf(50000), BigDecimal.valueOf(30000)));

        ImportResultDto result = importService.importFromExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Row 1");
    }

    @Test
    void reportsDuplicateEmailAsRowError() throws Exception {
        MockMultipartFile file = buildXlsx(new Object[][]{
                {"John", "Doe", "dup@example.com", "Engineering", 50000.00, "2022-01-10", true}
        });

        when(employeeService.createEmployee(any()))
                .thenThrow(new DuplicateEmailException("dup@example.com"));

        ImportResultDto result = importService.importFromExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
    }

    // ── POSITIVE: partial success (some rows fail, some pass) ─────────────────

    @Test
    void partialSuccessWhenSomeRowsInvalid() throws Exception {
        MockMultipartFile file = buildXlsx(new Object[][]{
                {"John", "Doe", "john@example.com", "Engineering", 50000.00, "2022-01-10", true},  // valid
                {"", "", "bad-email", "", 50000.00, "2022-01-10", true}                             // invalid
        });

        when(employeeService.createEmployee(any())).thenReturn(new EmployeeResponseDto());

        ImportResultDto result = importService.importFromExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).isNotEmpty();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Builds an in-memory .xlsx file with a header row and the given data rows.
     * Columns: firstName, lastName, email, department, salary, dateOfJoining, active
     */
    private MockMultipartFile buildXlsx(Object[][] dataRows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet();

            // header row (index 0) — skipped by the service
            var header = sheet.createRow(0);
            String[] headers = {"firstName", "lastName", "email", "department", "salary", "dateOfJoining", "active"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < dataRows.length; r++) {
                var row = sheet.createRow(r + 1);
                Object[] cols = dataRows[r];
                for (int c = 0; c < cols.length; c++) {
                    var cell = row.createCell(c);
                    Object val = cols[c];
                    if (val instanceof String s)       cell.setCellValue(s);
                    else if (val instanceof Double d)  cell.setCellValue(d);
                    else if (val instanceof Boolean b) cell.setCellValue(b);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "employees.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }
}
