package com.school.sms.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.school.sms.dto.response.FeeReceiptDto;
import com.school.sms.dto.response.ReportCardDto;
import com.school.sms.dto.response.ReportCardSubjectRowDto;
import com.school.sms.dto.response.SalarySlipDto;
import com.school.sms.entity.Student;
import com.school.sms.entity.Teacher;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.SchoolInfoRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.FeePaymentService;
import com.school.sms.service.FileStorageService;
import com.school.sms.service.MarkService;
import com.school.sms.service.PayrollService;
import com.school.sms.service.PdfService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.PdfGenerator;
import com.school.sms.util.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private static final String DEFAULT_SCHOOL_NAME = "School Management System";
    // DecimalFormat is not thread-safe; a fresh instance is created per format() call
    // below rather than shared as a static field (this service is a Spring singleton).
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    // Roughly CR80 credit-card proportions, scaled up slightly for legibility when printed.
    private static final Rectangle ID_CARD_SIZE = new Rectangle(320f, 200f);

    private final FeePaymentService feePaymentService;
    private final PayrollService payrollService;
    private final MarkService markService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolInfoRepository schoolInfoRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateFeeReceiptPdf(Long paymentId) {
        FeeReceiptDto receipt = feePaymentService.getReceipt(paymentId);

        return PdfGenerator.render(PageSize.A5, 28f, document -> {
            document.add(PdfGenerator.schoolHeader(receipt.getSchoolName(), "Fee Payment Receipt"));

            PdfPTable info = twoColumnTable();
            addRow(info, "Receipt No.", receipt.getReceiptNumber());
            addRow(info, "Payment Date", receipt.getPaymentDate() != null ? receipt.getPaymentDate().format(DATE_FORMAT) : "-");
            addRow(info, "Student Name", receipt.getStudentName());
            addRow(info, "Admission No.", receipt.getAdmissionNumber());
            addRow(info, "Class / Section", joinNonNull(receipt.getClassName(), receipt.getSectionName()));
            addRow(info, "Academic Year", receipt.getAcademicYearName());
            document.add(info);

            document.add(PdfGenerator.sectionTitle("Payment Details"));
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1f});
            table.addCell(PdfGenerator.tableHeaderCell("Description", Element.ALIGN_LEFT));
            table.addCell(PdfGenerator.tableHeaderCell("Amount", Element.ALIGN_RIGHT));
            table.addCell(PdfGenerator.tableCell(receipt.getFeeCategoryName(), Element.ALIGN_LEFT));
            table.addCell(PdfGenerator.tableCell(formatAmount(receipt.getAmount()), Element.ALIGN_RIGHT));
            document.add(table);

            Paragraph total = new Paragraph("Total Paid: " + formatAmount(receipt.getAmount()), PdfGenerator.SECTION_FONT);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(10f);
            document.add(total);

            PdfPTable meta = twoColumnTable();
            addRow(meta, "Payment Mode", receipt.getPaymentMode());
            addRow(meta, "Transaction ID", receipt.getTransactionId());
            addRow(meta, "Collected By", receipt.getCollectedByName());
            document.add(meta);

            Paragraph footer = new Paragraph("This is a system-generated receipt and does not require a signature.", PdfGenerator.SMALL_FONT);
            footer.setSpacingBefore(30f);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateSalarySlipPdf(Long payrollId) {
        SalarySlipDto slip = payrollService.getSalarySlip(payrollId);
        String period = monthName(slip.getMonth()) + " " + slip.getYear();

        return PdfGenerator.render(PageSize.A4, 40f, document -> {
            document.add(PdfGenerator.schoolHeader(slip.getSchoolName(), "Salary Slip - " + period));

            PdfPTable info = twoColumnTable();
            addRow(info, "Employee Name", slip.getEmployeeName());
            addRow(info, "Employee ID", slip.getEmployeeId());
            addRow(info, "Department", slip.getDepartmentName());
            addRow(info, "Designation", slip.getDesignationName());
            addRow(info, "Pay Period", period);
            document.add(info);

            document.add(PdfGenerator.sectionTitle("Earnings & Deductions"));

            BigDecimal gross = nz(slip.getBasicSalary()).add(nz(slip.getHra())).add(nz(slip.getDa())).add(nz(slip.getOtherAllowances()));
            BigDecimal totalDeductions = nz(slip.getPf()).add(nz(slip.getEsi()));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1f});
            table.addCell(PdfGenerator.tableHeaderCell("Component", Element.ALIGN_LEFT));
            table.addCell(PdfGenerator.tableHeaderCell("Amount", Element.ALIGN_RIGHT));
            addAmountRow(table, "Basic Salary", slip.getBasicSalary());
            addAmountRow(table, "HRA", slip.getHra());
            addAmountRow(table, "DA", slip.getDa());
            addAmountRow(table, "Other Allowances", slip.getOtherAllowances());
            table.addCell(PdfGenerator.tableCell("Gross Earnings", Element.ALIGN_LEFT, PdfGenerator.SECTION_FONT));
            table.addCell(PdfGenerator.tableCell(formatAmount(gross), Element.ALIGN_RIGHT, PdfGenerator.SECTION_FONT));
            addAmountRow(table, "Provident Fund (PF)", slip.getPf() != null ? slip.getPf().negate() : null);
            addAmountRow(table, "ESI", slip.getEsi() != null ? slip.getEsi().negate() : null);
            table.addCell(PdfGenerator.tableCell("Total Deductions", Element.ALIGN_LEFT, PdfGenerator.SECTION_FONT));
            table.addCell(PdfGenerator.tableCell(formatAmount(totalDeductions), Element.ALIGN_RIGHT, PdfGenerator.SECTION_FONT));
            document.add(table);

            Paragraph net = new Paragraph("Net Salary Payable: " + formatAmount(slip.getNetSalary()),
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14, PdfGenerator.BRAND_COLOR));
            net.setAlignment(Element.ALIGN_RIGHT);
            net.setSpacingBefore(14f);
            document.add(net);

            Paragraph footer = new Paragraph("This is a system-generated salary slip and does not require a signature.", PdfGenerator.SMALL_FONT);
            footer.setSpacingBefore(30f);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReportCardPdf(Long studentId, Long examId) {
        ReportCardDto card = markService.getReportCard(studentId, examId);

        return PdfGenerator.render(PageSize.A4, 40f, document -> {
            document.add(PdfGenerator.schoolHeader(resolveSchoolName(), "Student Report Card"));

            PdfPTable info = twoColumnTable();
            addRow(info, "Student Name", card.getStudentName());
            addRow(info, "Admission No.", card.getAdmissionNumber());
            addRow(info, "Class / Section", joinNonNull(card.getClassName(), card.getSectionName()));
            addRow(info, "Examination", card.getExamName());
            document.add(info);

            document.add(PdfGenerator.sectionTitle("Subject-wise Performance"));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1.3f, 1.3f, 1f});
            table.addCell(PdfGenerator.tableHeaderCell("Subject", Element.ALIGN_LEFT));
            table.addCell(PdfGenerator.tableHeaderCell("Marks Obtained", Element.ALIGN_CENTER));
            table.addCell(PdfGenerator.tableHeaderCell("Max Marks", Element.ALIGN_CENTER));
            table.addCell(PdfGenerator.tableHeaderCell("Grade", Element.ALIGN_CENTER));

            List<ReportCardSubjectRowDto> rows = card.getSubjects() != null ? card.getSubjects() : List.of();
            for (ReportCardSubjectRowDto row : rows) {
                table.addCell(PdfGenerator.tableCell(row.getSubjectName(), Element.ALIGN_LEFT));
                table.addCell(PdfGenerator.tableCell(
                        row.getMarksObtained() != null ? row.getMarksObtained().toString() : "-", Element.ALIGN_CENTER));
                table.addCell(PdfGenerator.tableCell(
                        row.getMaxMarks() != null ? row.getMaxMarks().toString() : "-", Element.ALIGN_CENTER));
                table.addCell(PdfGenerator.tableCell(row.getGradeName(), Element.ALIGN_CENTER));
            }
            document.add(table);

            document.add(PdfGenerator.sectionTitle("Result Summary"));
            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.addCell(PdfGenerator.tableHeaderCell("Total Obtained", Element.ALIGN_CENTER));
            summary.addCell(PdfGenerator.tableHeaderCell("Total Max", Element.ALIGN_CENTER));
            summary.addCell(PdfGenerator.tableHeaderCell("Percentage", Element.ALIGN_CENTER));
            summary.addCell(PdfGenerator.tableHeaderCell("Overall Grade", Element.ALIGN_CENTER));
            summary.addCell(PdfGenerator.tableCell(
                    card.getTotalObtained() != null ? card.getTotalObtained().toString() : "-", Element.ALIGN_CENTER, PdfGenerator.SECTION_FONT));
            summary.addCell(PdfGenerator.tableCell(
                    card.getTotalMax() != null ? card.getTotalMax().toString() : "-", Element.ALIGN_CENTER, PdfGenerator.SECTION_FONT));
            summary.addCell(PdfGenerator.tableCell(
                    card.getOverallPercentage() != null ? card.getOverallPercentage() + " %" : "-", Element.ALIGN_CENTER, PdfGenerator.SECTION_FONT));
            summary.addCell(PdfGenerator.tableCell(card.getOverallGrade(), Element.ALIGN_CENTER, PdfGenerator.SECTION_FONT));
            document.add(summary);

            Paragraph footer = new Paragraph("This is a system-generated report card.", PdfGenerator.SMALL_FONT);
            footer.setSpacingBefore(30f);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStudentIdCardPdf(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        String name = student.getUser() != null
                ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                : student.getAdmissionNumber();
        byte[] photoBytes = fileStorageService.readFile(student.getPhotoUrl());
        byte[] qrBytes = QrCodeGenerator.generatePng("STUDENT:" + student.getAdmissionNumber(), 200);
        String validUntil = student.getAcademicYear() != null && student.getAcademicYear().getEndDate() != null
                ? student.getAcademicYear().getEndDate().format(DATE_FORMAT)
                : "-";

        return PdfGenerator.render(ID_CARD_SIZE, 10f, document -> buildIdCard(
                document,
                "STUDENT IDENTITY CARD",
                photoBytes,
                name,
                "Class / Section", joinNonNull(student.getSchoolClass().getClassName(), student.getSection().getSectionName()),
                "Admission No.", student.getAdmissionNumber(),
                student.getBloodGroup(),
                qrBytes,
                validUntil
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTeacherIdCardPdf(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        String name = teacher.getUser() != null
                ? NameUtil.fullName(teacher.getUser().getFirstName(), teacher.getUser().getLastName())
                : teacher.getEmployeeId();
        byte[] photoBytes = fileStorageService.readFile(teacher.getUser() != null ? teacher.getUser().getProfileImage() : null);
        byte[] qrBytes = QrCodeGenerator.generatePng("TEACHER:" + teacher.getEmployeeId(), 200);
        String validUntil = academicYearRepository.findByCurrentTrue()
                .map(year -> year.getEndDate() != null ? year.getEndDate().format(DATE_FORMAT) : "-")
                .orElse("-");

        return PdfGenerator.render(ID_CARD_SIZE, 10f, document -> buildIdCard(
                document,
                "STAFF IDENTITY CARD",
                photoBytes,
                name,
                "Department", teacher.getDepartment().getName() + " / " + teacher.getDesignation().getName(),
                "Employee ID", teacher.getEmployeeId(),
                teacher.getBloodGroup(),
                qrBytes,
                validUntil
        ));
    }

    private void buildIdCard(Document document, String cardTitle, byte[] photoBytes, String name,
                              String line1Label, String line1Value, String line2Label, String line2Value,
                              String bloodGroup, byte[] qrBytes, String validUntil) throws com.lowagie.text.DocumentException {
        String schoolName = resolveSchoolName();

        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell headerCell = new PdfPCell(new Paragraph(schoolName,
                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 11, java.awt.Color.WHITE)));
        headerCell.setBackgroundColor(PdfGenerator.BRAND_COLOR);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(5f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        header.addCell(headerCell);

        PdfPCell subCell = new PdfPCell(new Paragraph(cardTitle,
                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 7, java.awt.Color.WHITE)));
        subCell.setBackgroundColor(PdfGenerator.BRAND_COLOR);
        subCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        subCell.setPaddingBottom(4f);
        subCell.setBorder(Rectangle.NO_BORDER);
        header.addCell(subCell);
        document.add(header);

        PdfPTable body = new PdfPTable(2);
        body.setWidthPercentage(100);
        body.setWidths(new float[]{1f, 1.6f});
        body.setSpacingBefore(6f);

        Image photo = PdfGenerator.imageFromBytes(photoBytes);
        PdfPCell photoCell;
        if (photo != null) {
            photoCell = new PdfPCell(photo, true);
        } else {
            photoCell = new PdfPCell(new Paragraph("NO\nPHOTO", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8, java.awt.Color.GRAY)));
            photoCell.setBackgroundColor(PdfGenerator.LIGHT_GRAY);
            photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        }
        photoCell.setFixedHeight(70f);
        photoCell.setBorderColor(PdfGenerator.BORDER_GRAY);
        photoCell.setPadding(3f);
        body.addCell(photoCell);

        PdfPTable details = new PdfPTable(1);
        details.setWidthPercentage(100);
        details.addCell(borderlessCell(name, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 11)));
        details.addCell(borderlessCell(line1Label + ": " + (line1Value != null ? line1Value : "-"), PdfGenerator.SMALL_FONT));
        details.addCell(borderlessCell(line2Label + ": " + (line2Value != null ? line2Value : "-"), PdfGenerator.SMALL_FONT));
        details.addCell(borderlessCell("Blood Group: " + (bloodGroup != null ? bloodGroup : "-"), PdfGenerator.SMALL_FONT));
        PdfPCell detailsCell = new PdfPCell(details);
        detailsCell.setFixedHeight(70f);
        detailsCell.setBorderColor(PdfGenerator.BORDER_GRAY);
        detailsCell.setPadding(3f);
        body.addCell(detailsCell);
        document.add(body);

        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);
        footer.setWidths(new float[]{1.6f, 1f});
        footer.setSpacingBefore(6f);

        PdfPCell validCell = new PdfPCell(new Paragraph("Valid Until: " + validUntil, PdfGenerator.SMALL_FONT));
        validCell.setBorder(Rectangle.NO_BORDER);
        validCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        footer.addCell(validCell);

        Image qr = PdfGenerator.imageFromBytes(qrBytes);
        PdfPCell qrCell;
        if (qr != null) {
            qrCell = new PdfPCell(qr, true);
        } else {
            qrCell = new PdfPCell(new Paragraph(""));
        }
        qrCell.setFixedHeight(50f);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footer.addCell(qrCell);
        document.add(footer);
    }

    private PdfPCell borderlessCell(String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(1.5f);
        return cell;
    }

    private PdfPTable twoColumnTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1.4f});
        return table;
    }

    private void addRow(PdfPTable table, String label, String value) {
        table.addCell(PdfGenerator.labelCell(label));
        table.addCell(PdfGenerator.valueCell(value));
    }

    private void addAmountRow(PdfPTable table, String label, BigDecimal amount) {
        table.addCell(PdfGenerator.tableCell(label, Element.ALIGN_LEFT));
        table.addCell(PdfGenerator.tableCell(formatAmount(amount), Element.ALIGN_RIGHT));
    }

    private String formatAmount(BigDecimal amount) {
        return "Rs. " + new DecimalFormat("#,##0.00").format(nz(amount));
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String joinNonNull(String a, String b) {
        if (!StringUtils.hasText(a)) {
            return b;
        }
        if (!StringUtils.hasText(b)) {
            return a;
        }
        return a + " - " + b;
    }

    private String monthName(Integer month) {
        if (month == null || month < 1 || month > 12) {
            return "-";
        }
        return MONTH_NAMES[month - 1];
    }

    private String resolveSchoolName() {
        return schoolInfoRepository.findById(1L)
                .map(info -> StringUtils.hasText(info.getName()) ? info.getName() : DEFAULT_SCHOOL_NAME)
                .orElse(DEFAULT_SCHOOL_NAME);
    }
}
