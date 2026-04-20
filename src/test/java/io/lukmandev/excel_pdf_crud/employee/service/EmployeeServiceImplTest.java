package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeePartialUpdateDto;
import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeeRequestDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.EmployeeResponseDto;
import io.lukmandev.excel_pdf_crud.employee.entity.Employee;
import io.lukmandev.excel_pdf_crud.employee.exception.DuplicateEmailException;
import io.lukmandev.excel_pdf_crud.employee.exception.EmployeeNotFoundException;
import io.lukmandev.excel_pdf_crud.employee.exception.SalaryFloorException;
import io.lukmandev.excel_pdf_crud.employee.mapper.EmployeeMapper;
import io.lukmandev.excel_pdf_crud.employee.repository.EmployeeRespository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmployeeServiceImplTest {

    @Mock
    private EmployeeRespository employeeRespository;

    @Mock
    private EmployeeMapper employeeMapper;

    private EmployeeServiceImpl service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static EmployeeRequestDto validRequest() {
        return new EmployeeRequestDto(
                "John", "Doe", "john@example.com", "Engineering",
                new BigDecimal("50000"), LocalDate.of(2022, 1, 10), true);
    }

    private static Employee employeeEntity(Long id) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName("John");
        e.setLastName("Doe");
        e.setEmail("john@example.com");
        e.setDepartment("Engineering");
        e.setSalary(new BigDecimal("50000"));
        e.setDateOfJoining(LocalDate.of(2022, 1, 10));
        e.setActive(true);
        return e;
    }

    private static EmployeeResponseDto responseDto(Long id) {
        return new EmployeeResponseDto(id, "John", "Doe", "john@example.com",
                "Engineering", new BigDecimal("50000"), LocalDate.of(2022, 1, 10),
                true, null, null);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EmployeeServiceImpl(employeeRespository, employeeMapper);
    }

    // ── createEmployee ────────────────────────────────────────────────────────

    @Test
    void createEmployee_success() {
        EmployeeRequestDto dto = validRequest();
        Employee entity = employeeEntity(1L);
        EmployeeResponseDto expected = responseDto(1L);

        when(employeeRespository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(employeeMapper.toEntity(dto)).thenReturn(entity);
        when(employeeRespository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDto(entity)).thenReturn(expected);

        EmployeeResponseDto result = service.createEmployee(dto);

        assertThat(result).isEqualTo(expected);
        verify(employeeRespository).save(entity);
    }

    @Test
    void createEmployee_throwsSalaryFloor_whenBelowDefault() {
        EmployeeRequestDto dto = new EmployeeRequestDto(
                "John", "Doe", "john@example.com", "Engineering",
                new BigDecimal("10000"), LocalDate.of(2022, 1, 10), true);

        assertThatThrownBy(() -> service.createEmployee(dto))
                .isInstanceOf(SalaryFloorException.class);

        verifyNoInteractions(employeeRespository);
    }

    @Test
    void createEmployee_throwsSalaryFloor_whenInternBelowInternFloor() {
        EmployeeRequestDto dto = new EmployeeRequestDto(
                "John", "Doe", "john@example.com", "intern",
                new BigDecimal("5000"), LocalDate.of(2022, 1, 10), true);

        assertThatThrownBy(() -> service.createEmployee(dto))
                .isInstanceOf(SalaryFloorException.class);
    }

    @Test
    void createEmployee_allowsInternAboveInternFloor() {
        EmployeeRequestDto dto = new EmployeeRequestDto(
                "John", "Doe", "john@example.com", "intern",
                new BigDecimal("15000"), LocalDate.of(2022, 1, 10), true);
        Employee entity = employeeEntity(1L);

        when(employeeRespository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(employeeMapper.toEntity(dto)).thenReturn(entity);
        when(employeeRespository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDto(entity)).thenReturn(responseDto(1L));

        assertThatNoException().isThrownBy(() -> service.createEmployee(dto));
    }

    @Test
    void createEmployee_throwsDuplicateEmail() {
        EmployeeRequestDto dto = validRequest();

        when(employeeRespository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.createEmployee(dto))
                .isInstanceOf(DuplicateEmailException.class);

        verify(employeeRespository, never()).save(any());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllEmployees() {
        List<Employee> entities = List.of(employeeEntity(1L), employeeEntity(2L));
        List<EmployeeResponseDto> expected = List.of(responseDto(1L), responseDto(2L));

        when(employeeRespository.findAll()).thenReturn(entities);
        when(employeeMapper.toDtoList(entities)).thenReturn(expected);

        assertThat(service.findAll()).isEqualTo(expected);
    }

    @Test
    void findAll_returnsEmptyList_whenNoEmployees() {
        when(employeeRespository.findAll()).thenReturn(List.of());
        when(employeeMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_returnsEmployee() {
        Employee entity = employeeEntity(1L);
        EmployeeResponseDto expected = responseDto(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(employeeMapper.toDto(entity)).thenReturn(expected);

        assertThat(service.findById(1L)).isEqualTo(expected);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(employeeRespository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // ── findBySalaryRange ─────────────────────────────────────────────────────

    @Test
    void findBySalaryRange_returnsMatchingEmployees() {
        BigDecimal min = new BigDecimal("30000");
        BigDecimal max = new BigDecimal("60000");
        List<Employee> entities = List.of(employeeEntity(1L));
        List<EmployeeResponseDto> expected = List.of(responseDto(1L));

        when(employeeRespository.findBySalaryBetween(min, max)).thenReturn(entities);
        when(employeeMapper.toDtoList(entities)).thenReturn(expected);

        assertThat(service.findBySalaryRange(min, max)).isEqualTo(expected);
    }

    @Test
    void findBySalaryRange_returnsEmpty_whenNoMatch() {
        BigDecimal min = new BigDecimal("100000");
        BigDecimal max = new BigDecimal("200000");

        when(employeeRespository.findBySalaryBetween(min, max)).thenReturn(List.of());
        when(employeeMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(service.findBySalaryRange(min, max)).isEmpty();
    }

    // ── fullUpdate ────────────────────────────────────────────────────────────

    @Test
    void fullUpdate_success() {
        EmployeeRequestDto dto = validRequest();
        Employee entity = employeeEntity(1L);
        EmployeeResponseDto expected = responseDto(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(employeeRespository.existsByEmailAndIdNot(dto.getEmail(), 1L)).thenReturn(false);
        when(employeeRespository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDto(entity)).thenReturn(expected);

        assertThat(service.fullUpdate(1L, dto)).isEqualTo(expected);
        verify(employeeRespository).save(entity);
    }

    @Test
    void fullUpdate_throwsNotFound_whenMissing() {
        when(employeeRespository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fullUpdate(99L, validRequest()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void fullUpdate_throwsSalaryFloor_whenBelowMinimum() {
        EmployeeRequestDto dto = new EmployeeRequestDto(
                "John", "Doe", "john@example.com", "Engineering",
                new BigDecimal("5000"), LocalDate.of(2022, 1, 10), true);
        Employee entity = employeeEntity(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.fullUpdate(1L, dto))
                .isInstanceOf(SalaryFloorException.class);

        verify(employeeRespository, never()).save(any());
    }

    @Test
    void fullUpdate_throwsDuplicateEmail_whenEmailTakenByAnotherEmployee() {
        EmployeeRequestDto dto = validRequest();
        Employee entity = employeeEntity(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(employeeRespository.existsByEmailAndIdNot(dto.getEmail(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.fullUpdate(1L, dto))
                .isInstanceOf(DuplicateEmailException.class);

        verify(employeeRespository, never()).save(any());
    }

    // ── partialUpdate ─────────────────────────────────────────────────────────

    @Test
    void partialUpdate_success_withNewSalary() {
        EmployeePartialUpdateDto dto = new EmployeePartialUpdateDto();
        dto.setSalary(new BigDecimal("60000"));
        Employee entity = employeeEntity(1L);
        EmployeeResponseDto expected = responseDto(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(employeeRespository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDto(entity)).thenReturn(expected);

        assertThat(service.partialUpdate(1L, dto)).isEqualTo(expected);
        verify(employeeMapper).partialUpdate(dto, entity);
    }

    @Test
    void partialUpdate_useExistingSalary_whenNotProvided() {
        EmployeePartialUpdateDto dto = new EmployeePartialUpdateDto();
        dto.setFirstName("Jane");
        Employee entity = employeeEntity(1L); // salary=50000, dept=Engineering
        EmployeeResponseDto expected = responseDto(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(employeeRespository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDto(entity)).thenReturn(expected);

        assertThatNoException().isThrownBy(() -> service.partialUpdate(1L, dto));
    }

    @Test
    void partialUpdate_throwsNotFound_whenMissing() {
        when(employeeRespository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.partialUpdate(99L, new EmployeePartialUpdateDto()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void partialUpdate_throwsSalaryFloor_whenNewSalaryBelowFloor() {
        EmployeePartialUpdateDto dto = new EmployeePartialUpdateDto();
        dto.setSalary(new BigDecimal("1000"));
        Employee entity = employeeEntity(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.partialUpdate(1L, dto))
                .isInstanceOf(SalaryFloorException.class);

        verify(employeeRespository, never()).save(any());
    }

    @Test
    void partialUpdate_throwsDuplicateEmail_whenEmailTaken() {
        EmployeePartialUpdateDto dto = new EmployeePartialUpdateDto();
        dto.setEmail("taken@example.com");
        Employee entity = employeeEntity(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(employeeRespository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.partialUpdate(1L, dto))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // ── softDelete ────────────────────────────────────────────────────────────

    @Test
    void softDelete_success() {
        Employee entity = employeeEntity(1L);

        when(employeeRespository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));

        service.softDelete(1L);

        verify(employeeRespository).softDeleteById(1L);
    }

    @Test
    void softDelete_throwsNotFound_whenMissing() {
        when(employeeRespository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(99L))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRespository, never()).softDeleteById(any());
    }

    // ── hardDelete ────────────────────────────────────────────────────────────

    @Test
    void hardDelete_success() {
        when(employeeRespository.existsById(1L)).thenReturn(true);

        service.hardDelete(1L);

        verify(employeeRespository).deleteById(1L);
    }

    @Test
    void hardDelete_throwsNotFound_whenMissing() {
        when(employeeRespository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.hardDelete(99L))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRespository, never()).deleteById(any());
    }

    // ── exportToExcel ─────────────────────────────────────────────────────────

    @Test
    void exportToExcel_writesNonEmptyOutput() throws IOException {
        when(employeeRespository.findByFilters(null, null))
                .thenReturn(List.of(employeeEntity(1L)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportToExcel(null, null, out);

        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    void exportToExcel_worksWithEmptyList() throws IOException {
        when(employeeRespository.findByFilters("HR", true)).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatNoException().isThrownBy(() -> service.exportToExcel("HR", true, out));
        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    void exportToExcel_throwsRuntimeException_onIOError() {
        when(employeeRespository.findByFilters(null, null)).thenReturn(List.of());

        OutputStream broken = new OutputStream() {
            @Override
            public void write(int b) throws IOException { throw new IOException("disk full"); }
            @Override
            public void write(byte[] b, int off, int len) throws IOException { throw new IOException("disk full"); }
        };

        // POI wraps the IOException in OpenXML4JRuntimeException before our catch block fires,
        // so the thrown exception is a RuntimeException (not necessarily ExcelProcessingException).
        assertThatThrownBy(() -> service.exportToExcel(null, null, broken))
                .isInstanceOf(RuntimeException.class);
    }

    // ── exportToPdf ───────────────────────────────────────────────────────────

    @Test
    void exportToPdf_writesNonEmptyOutput() {
        when(employeeRespository.findByFilters(null, null))
                .thenReturn(List.of(employeeEntity(1L)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportToPdf(null, null, out);

        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    void exportToPdf_worksWithEmptyList() {
        when(employeeRespository.findByFilters("Engineering", false)).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatNoException().isThrownBy(() -> service.exportToPdf("Engineering", false, out));
        assertThat(out.size()).isGreaterThan(0);
    }
}
