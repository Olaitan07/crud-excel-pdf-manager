package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeePartialUpdateDto;
import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeeRequestDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.EmployeeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;

public interface EmployeeService {

    EmployeeResponseDto createEmployee(EmployeeRequestDto dto);

    Page<EmployeeResponseDto> findAll(Pageable pageable);

    EmployeeResponseDto findById(Long id);

    EmployeeResponseDto fullUpdate(Long id, EmployeeRequestDto dto);

    EmployeeResponseDto partialUpdate(Long id, EmployeePartialUpdateDto dto);

    void softDelete(Long id);

    void hardDelete(Long id);

    List<EmployeeResponseDto> findBySalaryRange(BigDecimal min, BigDecimal max);

    void exportToExcel(String department, Boolean active, java.io.OutputStream outputStream);

    void exportToPdf(String department, Boolean active, java.io.OutputStream outputStream);
}
