package com.school.sms.service.impl;

import com.school.sms.dto.response.AttendanceSummaryReportDto;
import com.school.sms.dto.response.CategoryAmountDto;
import com.school.sms.dto.response.CategoryCountDto;
import com.school.sms.dto.response.ClassCountDto;
import com.school.sms.dto.response.ClassPercentageDto;
import com.school.sms.dto.response.DepartmentCountDto;
import com.school.sms.dto.response.FeeCollectionReportDto;
import com.school.sms.dto.response.LibrarySummaryReportDto;
import com.school.sms.dto.response.MonthCollectedDto;
import com.school.sms.dto.response.MonthPaidDto;
import com.school.sms.dto.response.PayrollSummaryReportDto;
import com.school.sms.dto.response.StatusCountDto;
import com.school.sms.dto.response.StudentsSummaryDto;
import com.school.sms.dto.response.TeachersSummaryDto;
import com.school.sms.dto.response.TransportSummaryReportDto;
import com.school.sms.entity.IssueStatus;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.TeacherStatus;
import com.school.sms.repository.BookIssueRepository;
import com.school.sms.repository.BookRepository;
import com.school.sms.repository.BusRepository;
import com.school.sms.repository.FeePaymentRepository;
import com.school.sms.repository.PayrollRepository;
import com.school.sms.repository.RouteRepository;
import com.school.sms.repository.StudentAttendanceRepository;
import com.school.sms.repository.StudentFeeRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.StudentTransportRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final PayrollRepository payrollRepository;
    private final BookRepository bookRepository;
    private final BookIssueRepository bookIssueRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final StudentTransportRepository studentTransportRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentsSummaryDto getStudentsSummary() {
        long totalActive = studentRepository.countByDeletedFalseAndStatus(StudentStatus.ACTIVE);

        List<ClassCountDto> byClass = studentRepository.countActiveGroupByClass().stream()
                .map(row -> ClassCountDto.builder()
                        .className((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        List<StatusCountDto> byStatus = studentRepository.countGroupByStatus().stream()
                .map(row -> StatusCountDto.builder()
                        .status(((StudentStatus) row[0]).name())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        return StudentsSummaryDto.builder().totalActive(totalActive).byClass(byClass).byStatus(byStatus).build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeachersSummaryDto getTeachersSummary() {
        long totalActive = teacherRepository.countByDeletedFalseAndStatus(TeacherStatus.ACTIVE);

        List<DepartmentCountDto> byDepartment = teacherRepository.countActiveGroupByDepartment().stream()
                .map(row -> DepartmentCountDto.builder()
                        .departmentName((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        return TeachersSummaryDto.builder().totalActive(totalActive).byDepartment(byDepartment).build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryReportDto getAttendanceSummary(LocalDate startDate, LocalDate endDate, Long classId) {
        List<Object[]> rows = studentAttendanceRepository.aggregateAttendanceByClass(startDate, endDate, classId);

        double totalPresentEquivalent = 0;
        long totalMarked = 0;
        List<ClassPercentageDto> byClass = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String className = (String) row[0];
            double presentEquivalent = ((Number) row[1]).doubleValue();
            long marked = ((Number) row[2]).longValue();
            double percentage = marked == 0 ? 0.0 : round2(presentEquivalent / marked * 100);
            byClass.add(ClassPercentageDto.builder().className(className).percentage(percentage).build());
            totalPresentEquivalent += presentEquivalent;
            totalMarked += marked;
        }

        double averagePercentage = totalMarked == 0 ? 0.0 : round2(totalPresentEquivalent / totalMarked * 100);

        return AttendanceSummaryReportDto.builder().averagePercentage(averagePercentage).byClass(byClass).build();
    }

    @Override
    @Transactional(readOnly = true)
    public FeeCollectionReportDto getFeeCollectionReport(Long academicYearId) {
        List<Object[]> rows = studentFeeRepository.aggregateDuesSummary(null, academicYearId);
        Object[] row = rows.isEmpty() ? new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L} : rows.get(0);
        BigDecimal totalDue = (BigDecimal) row[0];
        BigDecimal totalCollected = (BigDecimal) row[1];

        List<CategoryAmountDto> byCategory = feePaymentRepository.aggregateCollectedByCategory(academicYearId).stream()
                .map(r -> CategoryAmountDto.builder().categoryName((String) r[0]).collected((BigDecimal) r[1]).build())
                .toList();

        List<MonthCollectedDto> byMonth = feePaymentRepository.aggregateCollectedByMonth(academicYearId).stream()
                .map(r -> MonthCollectedDto.builder().month(((Number) r[0]).intValue()).collected((BigDecimal) r[1]).build())
                .toList();

        return FeeCollectionReportDto.builder()
                .totalDue(totalDue)
                .totalCollected(totalCollected)
                .totalOutstanding(totalDue.subtract(totalCollected))
                .byCategory(byCategory)
                .byMonth(byMonth)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollSummaryReportDto getPayrollSummary(Integer year) {
        List<Object[]> rows = payrollRepository.aggregateYearTotals(year);
        Object[] row = rows.isEmpty() ? new Object[]{BigDecimal.ZERO, BigDecimal.ZERO} : rows.get(0);

        List<MonthPaidDto> byMonth = payrollRepository.aggregatePaidByMonth(year).stream()
                .map(r -> MonthPaidDto.builder().month(((Number) r[0]).intValue()).paidAmount((BigDecimal) r[1]).build())
                .toList();

        return PayrollSummaryReportDto.builder()
                .totalPaidAmount((BigDecimal) row[0])
                .totalPendingAmount((BigDecimal) row[1])
                .byMonth(byMonth)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LibrarySummaryReportDto getLibrarySummary() {
        List<CategoryCountDto> byCategory = bookRepository.countGroupByCategory().stream()
                .map(row -> CategoryCountDto.builder()
                        .categoryName((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        return LibrarySummaryReportDto.builder()
                .totalBooks(bookRepository.countByDeletedFalse())
                .totalIssued(bookIssueRepository.countByStatus(IssueStatus.ISSUED))
                .totalOverdue(bookIssueRepository.countByStatusAndDueDateBefore(IssueStatus.ISSUED, LocalDate.now()))
                .byCategory(byCategory)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TransportSummaryReportDto getTransportSummary() {
        return TransportSummaryReportDto.builder()
                .totalBuses(busRepository.countByDeletedFalse())
                .totalRoutes(routeRepository.count())
                .studentsUsingTransport(studentTransportRepository.countDistinctStudents())
                .build();
    }

    private double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
