package io.lukmandev.excel_pdf_crud.employee.exception;

public class ExcelProcessingException extends RuntimeException {
    public ExcelProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
