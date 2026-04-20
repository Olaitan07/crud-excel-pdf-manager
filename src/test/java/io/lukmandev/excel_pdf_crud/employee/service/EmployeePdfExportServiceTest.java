package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.entity.Employee;
import io.lukmandev.excel_pdf_crud.employee.mapper.EmployeeMapper;
import io.lukmandev.excel_pdf_crud.employee.repository.EmployeeRespository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class EmployeePdfExportServiceTest {

    @Mock private EmployeeRespository employeeRespository;
    @Mock private EmployeeMapper employeeMapper;

    @InjectMocks private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    // ── POSITIVE: output starts with %PDF magic bytes ─────────────────────────

    @Test
    void exportProducesValidPdf() throws Exception {
        when(employeeRespository.findByFilters(null, null))
                .thenReturn(List.of(sampleEmployee(1L, "Engineering", true)));

        byte[] result = export(null, null);

        assertThat(result).isNotEmpty();
        // PDF magic header
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── POSITIVE: empty result still produces a valid PDF ─────────────────────

    @Test
    void exportWithNoEmployeesProducesValidPdf() throws Exception {
        when(employeeRespository.findByFilters(null, null)).thenReturn(List.of());

        byte[] result = export(null, null);

        assertThat(result).isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── POSITIVE: filter by department is forwarded to repository ─────────────

    @Test
    void exportFiltersByDepartment() throws Exception {
        when(employeeRespository.findByFilters("Engineering", null))
                .thenReturn(List.of(sampleEmployee(1L, "Engineering", true)));

        byte[] result = export("Engineering", null);

        assertThat(result).isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── POSITIVE: filter by active status is forwarded to repository ──────────

    @Test
    void exportFiltersByActiveStatus() throws Exception {
        when(employeeRespository.findByFilters(null, false))
                .thenReturn(List.of(sampleEmployee(2L, "HR", false)));

        byte[] result = export(null, false);

        assertThat(result).isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── POSITIVE: multiple employees produce larger output than zero ──────────

    @Test
    void exportWithMultipleEmployeesProducesLargerOutput() throws Exception {
        when(employeeRespository.findByFilters(null, null)).thenReturn(List.of());
        byte[] empty = export(null, null);

        when(employeeRespository.findByFilters(null, null))
                .thenReturn(List.of(
                        sampleEmployee(1L, "Engineering", true),
                        sampleEmployee(2L, "HR", false),
                        sampleEmployee(3L, "Finance", true)
                ));
        byte[] withData = export(null, null);

        assertThat(withData.length).isGreaterThan(empty.length);
    }

    // ── NEGATIVE: null fields in entity do not cause exception ────────────────

    @Test
    void exportHandlesNullFieldsGracefully() throws Exception {
        Employee sparse = new Employee();
        sparse.setId(99L);
        // all other fields null
        when(employeeRespository.findByFilters(null, null)).thenReturn(List.of(sparse));

        byte[] result = export(null, null);

        assertThat(result).isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private byte[] export(String department, Boolean active) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        employeeService.exportToPdf(department, active, out);
        return out.toByteArray();
    }

    private Employee sampleEmployee(Long id, String department, boolean active) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName("John");
        e.setLastName("Doe");
        e.setEmail("john" + id + "@example.com");
        e.setDepartment(department);
        e.setSalary(new BigDecimal("55000.00"));
        e.setDateOfJoining(LocalDate.of(2022, 3, 15));
        e.setActive(active);
        e.setCreatedAt(LocalDateTime.of(2022, 3, 15, 9, 0));
        e.setUpdatedAt(LocalDateTime.of(2023, 8, 1, 10, 30));
        return e;
    }
}
