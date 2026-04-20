package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.entity.Employee;
import io.lukmandev.excel_pdf_crud.employee.mapper.EmployeeMapper;
import io.lukmandev.excel_pdf_crud.employee.repository.EmployeeRespository;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class EmployeeExportServiceTest {

    @Mock private EmployeeRespository employeeRespository;
    @Mock private EmployeeMapper employeeMapper;

    @InjectMocks private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    // ── POSITIVE: valid XLSX bytes produced ───────────────────────────────────

    @Test
    void exportProducesValidXlsx() throws Exception {
        when(employeeRespository.findByFilters(null, null))
                .thenReturn(List.of(sampleEmployee(1L, "Engineering", true)));

        byte[] result = export(null, null);

        assertThat(result).isNotEmpty();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Employees");
        }
    }

    // ── POSITIVE: header row is bold with dark fill ───────────────────────────

    @Test
    void headerRowIsBoldWithDarkFill() throws Exception {
        when(employeeRespository.findByFilters(null, null)).thenReturn(List.of());

        byte[] result = export(null, null);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var headerRow = wb.getSheetAt(0).getRow(0);
            var headerCell = headerRow.getCell(0);
            assertThat(headerCell.getCellStyle().getFont().getBold()).isTrue();
            assertThat(headerCell.getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
        }
    }

    // ── POSITIVE: 10 header columns including updatedAt ───────────────────────

    @Test
    void headerContainsTenColumns() throws Exception {
        when(employeeRespository.findByFilters(null, null)).thenReturn(List.of());

        byte[] result = export(null, null);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var headerRow = wb.getSheetAt(0).getRow(0);
            assertThat(headerRow.getLastCellNum()).isEqualTo((short) 10);
            assertThat(headerRow.getCell(9).getStringCellValue()).isEqualTo("Updated At");
        }
    }

    // ── POSITIVE: alternating row styles are different ────────────────────────

    @Test
    void alternatingRowStylesDiffer() throws Exception {
        when(employeeRespository.findByFilters(null, null))
                .thenReturn(List.of(
                        sampleEmployee(1L, "Eng", true),
                        sampleEmployee(2L, "HR",  false)
                ));

        byte[] result = export(null, null);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var sheet = wb.getSheetAt(0);
            var row1Style = sheet.getRow(1).getCell(1).getCellStyle();
            var row2Style = sheet.getRow(2).getCell(1).getCellStyle();
            // The fill colour indexes must differ
            assertThat(row1Style.getFillForegroundColor())
                    .isNotEqualTo(row2Style.getFillForegroundColor());
        }
    }

    // ── POSITIVE: salary cell has a data format ───────────────────────────────

    @Test
    void salaryCellHasNumericFormat() throws Exception {
        when(employeeRespository.findByFilters(null, null))
                .thenReturn(List.of(sampleEmployee(1L, "Engineering", true)));

        byte[] result = export(null, null);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var salaryCell = wb.getSheetAt(0).getRow(1).getCell(5);
            String fmt = salaryCell.getCellStyle().getDataFormatString();
            assertThat(fmt).isEqualTo("0.00");
        }
    }

    // ── POSITIVE: filter by department ───────────────────────────────────────

    @Test
    void filterByDepartmentReturnsMatchingRows() throws Exception {
        when(employeeRespository.findByFilters("Engineering", null))
                .thenReturn(List.of(sampleEmployee(1L, "Engineering", true)));

        byte[] result = export("Engineering", null);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(1);
        }
    }

    // ── NEGATIVE: no matches returns header-only sheet ────────────────────────

    @Test
    void noMatchReturnsHeaderOnly() throws Exception {
        when(employeeRespository.findByFilters("Ghost", null)).thenReturn(List.of());

        byte[] result = export("Ghost", null);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private byte[] export(String department, Boolean active) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        employeeService.exportToExcel(department, active, out);
        return out.toByteArray();
    }

    private Employee sampleEmployee(Long id, String department, boolean active) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName("John");
        e.setLastName("Doe");
        e.setEmail("john" + id + "@example.com");
        e.setDepartment(department);
        e.setSalary(new BigDecimal("50000.00"));
        e.setDateOfJoining(LocalDate.of(2022, 1, 10));
        e.setActive(active);
        e.setCreatedAt(LocalDateTime.of(2022, 1, 10, 9, 0));
        e.setUpdatedAt(LocalDateTime.of(2023, 6, 1, 12, 0));
        return e;
    }
}
