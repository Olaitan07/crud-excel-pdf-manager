package io.lukmandev.excel_pdf_crud.employee.exception;

public class PdfProcessingException extends RuntimeException {
    public PdfProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
