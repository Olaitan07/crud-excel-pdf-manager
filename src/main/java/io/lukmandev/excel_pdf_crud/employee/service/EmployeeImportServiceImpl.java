package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeeRequestDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.ImportResultDto;
import io.lukmandev.excel_pdf_crud.employee.exception.DuplicateEmailException;
import io.lukmandev.excel_pdf_crud.employee.exception.ExcelProcessingException;
import io.lukmandev.excel_pdf_crud.employee.exception.InvalidFileFormatException;
import io.lukmandev.excel_pdf_crud.employee.exception.SalaryFloorException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeImportServiceImpl implements EmployeeImportService {

    private final EmployeeService employeeService;
    private final Validator validator;

    @Override
    @Transactional(noRollbackFor = {DuplicateEmailException.class, SalaryFloorException.class})
    public ImportResultDto importFromExcel(MultipartFile file) {
        validateFileExtension(file);

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                List<String> rowErrors = new ArrayList<>();
                EmployeeRequestDto dto = mapRowToDto(row, rowIndex, rowErrors);

                if (rowErrors.isEmpty()) {
                    Set<ConstraintViolation<EmployeeRequestDto>> violations = validator.validate(dto);
                    for (ConstraintViolation<EmployeeRequestDto> v : violations) {
                        rowErrors.add("Row " + rowIndex + ": " + v.getPropertyPath() + " – " + v.getMessage());
                    }
                }

                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    failureCount++;
                    continue;
                }

                try {
                    employeeService.createEmployee(dto);
                    successCount++;
                } catch (DuplicateEmailException | SalaryFloorException ex) {
                    errors.add("Row " + rowIndex + ": " + ex.getMessage());
                    failureCount++;
                }
            }
        } catch (IOException ex) {
            throw new ExcelProcessingException("Failed to read Excel file: " + ex.getMessage(), ex);
        }

        return new ImportResultDto(successCount, failureCount, errors);
    }

    private void validateFileExtension(MultipartFile file) {
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        if (!filename.toLowerCase().endsWith(".xlsx")) {
            throw new InvalidFileFormatException("Only .xlsx files are accepted. Got: " + filename);
        }
    }

    private EmployeeRequestDto mapRowToDto(Row row, int rowIndex, List<String> rowErrors) {
        EmployeeRequestDto dto = new EmployeeRequestDto();
        trySet(() -> dto.setFirstName(getStringCell(row, 0)),     rowIndex, rowErrors);
        trySet(() -> dto.setLastName(getStringCell(row, 1)),      rowIndex, rowErrors);
        trySet(() -> dto.setEmail(getStringCell(row, 2)),         rowIndex, rowErrors);
        trySet(() -> dto.setDepartment(getStringCell(row, 3)),    rowIndex, rowErrors);
        trySet(() -> dto.setSalary(getDecimalCell(row, 4)),       rowIndex, rowErrors);
        trySet(() -> dto.setDateOfJoining(getDateCell(row, 5)),   rowIndex, rowErrors);
        trySet(() -> dto.setActive(getBooleanCell(row, 6)),       rowIndex, rowErrors);
        return dto;
    }

    private void trySet(Runnable setter, int rowIndex, List<String> rowErrors) {
        try {
            setter.run();
        } catch (Exception ex) {
            rowErrors.add("Row " + rowIndex + ": " + ex.getMessage());
        }
    }

    private String getStringCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    private BigDecimal getDecimalCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try { yield new BigDecimal(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("column " + col + " is not a valid number"); }
            }
            default -> null;
        };
    }

    private LocalDate getDateCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                if (!DateUtil.isCellDateFormatted(cell)) {
                    throw new IllegalArgumentException("column " + col + " is not a valid date");
                }
                yield cell.getLocalDateTimeCellValue().toLocalDate();
            }
            case STRING -> {
                try { yield LocalDate.parse(cell.getStringCellValue().trim()); }
                catch (Exception e) { throw new IllegalArgumentException("column " + col + " is not a valid date (expected YYYY-MM-DD)"); }
            }
            default -> null;
        };
    }

    private Boolean getBooleanCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> {
                String val = cell.getStringCellValue().trim().toLowerCase();
                yield "true".equals(val) || "1".equals(val) || "yes".equals(val);
            }
            case NUMERIC -> cell.getNumericCellValue() != 0;
            default -> null;
        };
    }
}
