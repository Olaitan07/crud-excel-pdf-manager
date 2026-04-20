package io.lukmandev.excel_pdf_crud.employee.mapper;

import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeePartialUpdateDto;
import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeeRequestDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.EmployeeResponseDto;
import io.lukmandev.excel_pdf_crud.employee.entity.Employee;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    Employee toEntity(EmployeeRequestDto dto);

    EmployeeResponseDto toDto(Employee employee);

    List<EmployeeResponseDto> toDtoList(List<Employee> employees);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(EmployeePartialUpdateDto dto, @MappingTarget Employee employee);
}
