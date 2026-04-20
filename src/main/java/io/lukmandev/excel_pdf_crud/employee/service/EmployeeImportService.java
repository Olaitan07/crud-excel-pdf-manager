package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.dto.response.ImportResultDto;
import org.springframework.web.multipart.MultipartFile;

public interface EmployeeImportService {
    ImportResultDto importFromExcel(MultipartFile file);
}
