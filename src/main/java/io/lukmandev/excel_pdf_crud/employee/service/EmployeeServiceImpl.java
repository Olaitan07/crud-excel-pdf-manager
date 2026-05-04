package io.lukmandev.excel_pdf_crud.employee.service;

import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeePartialUpdateDto;
import io.lukmandev.excel_pdf_crud.employee.dto.request.EmployeeRequestDto;
import io.lukmandev.excel_pdf_crud.employee.dto.response.EmployeeResponseDto;
import io.lukmandev.excel_pdf_crud.employee.entity.Employee;
import io.lukmandev.excel_pdf_crud.employee.exception.DuplicateEmailException;
import io.lukmandev.excel_pdf_crud.employee.exception.EmployeeNotFoundException;
import io.lukmandev.excel_pdf_crud.employee.exception.ExcelProcessingException;
import io.lukmandev.excel_pdf_crud.employee.exception.PdfProcessingException;
import io.lukmandev.excel_pdf_crud.employee.exception.SalaryFloorException;
import io.lukmandev.excel_pdf_crud.employee.mapper.EmployeeMapper;
import io.lukmandev.excel_pdf_crud.employee.repository.EmployeeRespository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRespository employeeRespository;
    private final EmployeeMapper employeeMapper;

    // ── SALARY FLOOR ─────────────────────────────────────────────────────────

    private static final BigDecimal INTERN_FLOOR  = new BigDecimal("15000");
    private static final BigDecimal DEFAULT_FLOOR = new BigDecimal("30000");

    private void validateSalaryFloor(String department, BigDecimal salary) {
        BigDecimal floor = "intern".equalsIgnoreCase(department) ? INTERN_FLOOR : DEFAULT_FLOOR;
        if (salary.compareTo(floor) < 0) {
            throw new SalaryFloorException(department, salary, floor);
        }
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeRequestDto dto) {
        validateSalaryFloor(dto.getDepartment(), dto.getSalary());
        if (employeeRespository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEmailException(dto.getEmail());
        }
        Employee saved = employeeRespository.save(employeeMapper.toEntity(dto));
        return employeeMapper.toDto(saved);
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDto> findAll(Pageable pageable) {
        return employeeRespository.findAll(pageable).map(employeeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto findById(Long id) {
        return employeeMapper.toDto(
                employeeRespository.findByIdAndActiveTrue(id)
                        .orElseThrow(() -> new EmployeeNotFoundException(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> findBySalaryRange(BigDecimal min, BigDecimal max) {
        return employeeMapper.toDtoList(employeeRespository.findBySalaryBetween(min, max));
    }

    // ── FULL UPDATE (PUT) ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public EmployeeResponseDto fullUpdate(Long id, EmployeeRequestDto dto) {
        Employee employee = employeeRespository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        validateSalaryFloor(dto.getDepartment(), dto.getSalary());
        if (employeeRespository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new DuplicateEmailException(dto.getEmail());
        }

        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setDepartment(dto.getDepartment());
        employee.setSalary(dto.getSalary());
        employee.setDateOfJoining(dto.getDateOfJoining());
        employee.setActive(dto.getActive());

        return employeeMapper.toDto(employeeRespository.save(employee));
    }

    // ── PARTIAL UPDATE (PATCH) ────────────────────────────────────────────────

    @Override
    @Transactional
    public EmployeeResponseDto partialUpdate(Long id, EmployeePartialUpdateDto dto) {
        Employee employee = employeeRespository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        BigDecimal effectiveSalary     = dto.getSalary()     != null ? dto.getSalary()     : employee.getSalary();
        String     effectiveDepartment = dto.getDepartment() != null ? dto.getDepartment() : employee.getDepartment();
        validateSalaryFloor(effectiveDepartment, effectiveSalary);

        if (dto.getEmail() != null && employeeRespository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new DuplicateEmailException(dto.getEmail());
        }

        employeeMapper.partialUpdate(dto, employee);

        return employeeMapper.toDto(employeeRespository.save(employee));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void softDelete(Long id) {
        employeeRespository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        employeeRespository.softDeleteById(id);
    }

    // ── EXPORT ───────────────────────────────────────────────────────────────

    private static final String[] EXPORT_HEADERS = {
        "ID", "First Name", "Last Name", "Email", "Department",
        "Salary", "Date of Joining", "Active", "Created At", "Updated At"
    };

    @Override
    @Transactional(readOnly = true)
    public void exportToExcel(String department, Boolean active, OutputStream outputStream) {
        List<Employee> employees = employeeRespository.findByFilters(department, active);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Employees");

            XSSFCellStyle headerStyle  = buildHeaderStyle(workbook);
            XSSFCellStyle evenStyle    = buildFillStyle(workbook, IndexedColors.WHITE);
            XSSFCellStyle oddStyle     = buildFillStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE);
            XSSFCellStyle evenSalStyle = buildSalaryStyle(workbook, evenStyle);
            XSSFCellStyle oddSalStyle  = buildSalaryStyle(workbook, oddStyle);

            // Header row (index 0)
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows (index 1+)
            int rowNum = 1;
            for (Employee e : employees) {
                XSSFRow row = sheet.createRow(rowNum);
                boolean odd = (rowNum % 2 != 0);
                XSSFCellStyle base = odd ? oddStyle    : evenStyle;
                XSSFCellStyle sal  = odd ? oddSalStyle : evenSalStyle;

                writeCell(row, 0, e.getId() != null ? (double) e.getId() : 0.0, base);
                writeCell(row, 1, e.getFirstName(),    base);
                writeCell(row, 2, e.getLastName(),     base);
                writeCell(row, 3, e.getEmail(),        base);
                writeCell(row, 4, e.getDepartment(),   base);
                writeCell(row, 5, e.getSalary() != null ? e.getSalary().doubleValue() : 0.0, sal);
                writeCell(row, 6, e.getDateOfJoining() != null ? e.getDateOfJoining().toString() : "", base);
                writeCell(row, 7, e.getActive() != null && e.getActive(), base);
                writeCell(row, 8, e.getCreatedAt() != null ? e.getCreatedAt().toString() : "", base);
                writeCell(row, 9, e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : "", base);
                rowNum++;
            }

            // Auto-size all columns after data is written
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            outputStream.flush();

        } catch (IOException ex) {
            throw new ExcelProcessingException("Failed to generate Excel export", ex);
        }
    }

    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 31, (byte) 73, (byte) 125}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle buildFillStyle(XSSFWorkbook wb, IndexedColors color) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private XSSFCellStyle buildSalaryStyle(XSSFWorkbook wb, XSSFCellStyle base) {
        XSSFCellStyle style = wb.createCellStyle();
        style.cloneStyleFrom(base);
        style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        return style;
    }

    private void writeCell(XSSFRow row, int col, Object value, XSSFCellStyle style) {
        XSSFCell cell = row.createCell(col);
        if (value instanceof String s)       cell.setCellValue(s);
        else if (value instanceof Double d)  cell.setCellValue(d);
        else if (value instanceof Boolean b) cell.setCellValue(b);
        cell.setCellStyle(style);
    }

    // ── PDF EXPORT ────────────────────────────────────────────────────────────

    private static final Color HEADER_BG    = new Color(31, 73, 125);
    private static final Color ROW_ODD_BG   = new Color(189, 215, 238);
    private static final Color ROW_EVEN_BG  = Color.WHITE;
    private static final Font  HEADER_FONT  = new Font(Font.HELVETICA, 9f, Font.BOLD, Color.WHITE);
    private static final Font  DATA_FONT    = new Font(Font.HELVETICA, 9f, Font.NORMAL, Color.BLACK);
    private static final Font  TITLE_FONT   = new Font(Font.HELVETICA, 14f, Font.BOLD, Color.BLACK);

    @Override
    @Transactional(readOnly = true)
    public void exportToPdf(String department, Boolean active, OutputStream outputStream) {
        List<Employee> employees = employeeRespository.findByFilters(department, active);

        Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Title
            Paragraph title = new Paragraph("Employee Report", TITLE_FONT);
            title.setSpacingAfter(12f);
            document.add(title);

            // Table: 10 columns matching the Excel export
            PdfPTable table = new PdfPTable(EXPORT_HEADERS.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3f, 3f, 5f, 3.5f, 2.5f, 3f, 1.5f, 4f, 4f});

            // Header row
            for (String header : EXPORT_HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(5f);
                table.addCell(cell);
            }

            // Data rows
            int rowNum = 0;
            for (Employee e : employees) {
                Color bg = (rowNum % 2 == 0) ? ROW_ODD_BG : ROW_EVEN_BG;
                addDataCell(table, e.getId() != null ? String.valueOf(e.getId()) : "", bg);
                addDataCell(table, e.getFirstName(),    bg);
                addDataCell(table, e.getLastName(),     bg);
                addDataCell(table, e.getEmail(),        bg);
                addDataCell(table, e.getDepartment(),   bg);
                addDataCell(table, e.getSalary() != null ? String.format("%.2f", e.getSalary()) : "", bg);
                addDataCell(table, e.getDateOfJoining() != null ? e.getDateOfJoining().toString() : "", bg);
                addDataCell(table, e.getActive() != null ? String.valueOf(e.getActive()) : "", bg);
                addDataCell(table, e.getCreatedAt() != null ? e.getCreatedAt().toString() : "", bg);
                addDataCell(table, e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : "", bg);
                rowNum++;
            }

            document.add(table);
        } catch (DocumentException ex) {
            throw new PdfProcessingException("Failed to generate PDF report", ex);
        } finally {
            document.close();
        }
    }

    private void addDataCell(PdfPTable table, String text, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", DATA_FONT));
        cell.setBackgroundColor(background);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    @Override
    @Transactional
    public void hardDelete(Long id) {
        if (!employeeRespository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRespository.deleteById(id);
    }
}

