package com.school.sms.service;

public interface PdfService {

    /** Renders {@link com.school.sms.dto.response.FeeReceiptDto} (via FeePaymentService.getReceipt) as a printable PDF receipt. */
    byte[] generateFeeReceiptPdf(Long paymentId);

    /** Renders {@link com.school.sms.dto.response.SalarySlipDto} (via PayrollService.getSalarySlip) as a printable PDF salary slip. */
    byte[] generateSalarySlipPdf(Long payrollId);

    /** Renders {@link com.school.sms.dto.response.ReportCardDto} (via MarkService.getReportCard) as a printable PDF report card. */
    byte[] generateReportCardPdf(Long studentId, Long examId);

    /** A printable, credit-card-shaped ID card PDF for a student, with an embedded QR code encoding their admission number. */
    byte[] generateStudentIdCardPdf(Long studentId);

    /** A printable, credit-card-shaped ID card PDF for a teacher, with an embedded QR code encoding their employee id. */
    byte[] generateTeacherIdCardPdf(Long teacherId);
}
