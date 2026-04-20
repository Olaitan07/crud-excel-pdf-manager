package io.lukmandev.excel_pdf_crud.employee.exception;

import java.math.BigDecimal;

public class SalaryFloorException extends RuntimeException {

    public SalaryFloorException(String department, BigDecimal salary, BigDecimal floor) {
        super(String.format(
                "Salary %.2f is below the minimum of %.2f for department '%s'",
                salary, floor, department));
    }
}
