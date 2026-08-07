package com.school.sms.util;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Small shared helper around OpenPDF (package {@code com.lowagie.text}, the
 * fork actually on the classpath — not iText) so every printable document
 * (fee receipt, salary slip, report card, ID cards) shares the same fonts,
 * table-cell styling and open/close handling instead of re-implementing it
 * four times. Kept deliberately low-level/presentation-only: callers build
 * page content, this class only renders it and guarantees the
 * {@link Document}/{@link PdfWriter} pair is always closed.
 */
public final class PdfGenerator {

    private PdfGenerator() {
    }

    public static final Color BRAND_COLOR = new Color(30, 64, 122);
    public static final Color LIGHT_GRAY = new Color(240, 242, 245);
    public static final Color BORDER_GRAY = new Color(200, 200, 200);

    public static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_COLOR);
    public static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    public static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    public static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);
    public static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
    public static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    public static final Font TABLE_CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    public static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    @FunctionalInterface
    public interface Body {
        void build(Document document) throws DocumentException, IOException;
    }

    /**
     * Opens a {@link Document} of the given page size over an in-memory
     * buffer, invokes {@code body} to populate it, and always closes the
     * document (and therefore flushes the {@link PdfWriter}) before
     * returning the finished bytes — a single choke point so no caller can
     * forget to close the writer or leak it on an exception.
     */
    public static byte[] render(Rectangle pageSize, float margin, Body body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(pageSize, margin, margin, margin, margin);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            body.build(document);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Failed to generate PDF document", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    /** A school-name title + optional subtitle line, centered, with a brand-colored rule underneath. */
    public static PdfPTable schoolHeader(String schoolName, String subtitle) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell(new Paragraph(schoolName != null ? schoolName : "School Management System", TITLE_FONT));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(2f);
        table.addCell(titleCell);

        if (subtitle != null && !subtitle.isBlank()) {
            PdfPCell subtitleCell = new PdfPCell(new Paragraph(subtitle, SUBTITLE_FONT));
            subtitleCell.setBorder(Rectangle.NO_BORDER);
            subtitleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            subtitleCell.setPaddingBottom(6f);
            table.addCell(subtitleCell);
        }

        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setFixedHeight(2f);
        ruleCell.setBackgroundColor(BRAND_COLOR);
        ruleCell.setBorder(Rectangle.NO_BORDER);
        ruleCell.setPaddingBottom(10f);
        table.addCell(ruleCell);

        return table;
    }

    public static Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, SECTION_FONT);
        p.setSpacingBefore(10f);
        p.setSpacingAfter(6f);
        return p;
    }

    /** A borderless label/value pair, used to build a two-column "key: value" grid via a PdfPTable. */
    public static PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, LABEL_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4f);
        return cell;
    }

    public static PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "-", VALUE_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4f);
        return cell;
    }

    public static PdfPCell tableHeaderCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        return cell;
    }

    public static PdfPCell tableCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "-", TABLE_CELL_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        cell.setBorderColor(BORDER_GRAY);
        return cell;
    }

    public static PdfPCell tableCell(String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "-", font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        cell.setBorderColor(BORDER_GRAY);
        return cell;
    }

    /** Loads a raster image (PNG/JPEG bytes) for embedding, or {@code null} if the bytes are empty/invalid. */
    public static Image imageFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return Image.getInstance(bytes);
        } catch (Exception ex) {
            return null;
        }
    }

    public static Chunk footerNote(String text) {
        return new Chunk(text, SMALL_FONT);
    }
}
