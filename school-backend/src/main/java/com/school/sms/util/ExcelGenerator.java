package com.school.sms.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Small shared helper around Apache POI so every Excel export (students,
 * teachers, fee-collection report) uses the same bold/shaded header row
 * styling and column auto-sizing instead of re-implementing it per export.
 */
public final class ExcelGenerator {

    private ExcelGenerator() {
    }

    public static CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    public static void writeHeaderRow(Sheet sheet, CellStyle headerStyle, String... columns) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    public static void setCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value != null ? value : "");
    }

    public static void setCell(Row row, int column, Number value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        } else {
            row.createCell(column).setCellValue("");
        }
    }

    public static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // autoSizeColumn under-estimates for bold header text; pad a little so headers don't clip.
            int current = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(current + 1024, 255 * 256));
        }
    }

    public static byte[] toBytes(XSSFWorkbook workbook) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write Excel workbook", ex);
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
                // best-effort close of the in-memory workbook
            }
        }
    }
}
