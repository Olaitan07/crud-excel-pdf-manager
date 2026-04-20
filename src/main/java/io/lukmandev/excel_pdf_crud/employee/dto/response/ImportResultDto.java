package io.lukmandev.excel_pdf_crud.employee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDto {
    private int successCount;
    private int failureCount;
    private List<String> errors;
}
