package com.school.sms.service.impl;

import com.school.sms.dto.request.PayrollGenerateRequest;
import com.school.sms.dto.request.PayrollMarkPaidRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.PayrollDashboardDto;
import com.school.sms.dto.response.PayrollDto;
import com.school.sms.dto.response.PayrollGenerateResultDto;
import com.school.sms.dto.response.SalarySlipDto;
import com.school.sms.entity.Payroll;
import com.school.sms.entity.PayrollEmployeeType;
import com.school.sms.entity.PayrollStatus;
import com.school.sms.entity.SalaryStructure;
import com.school.sms.entity.Staff;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.TeacherStatus;
import com.school.sms.entity.User;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.PayrollRepository;
import com.school.sms.repository.SalaryStructureRepository;
import com.school.sms.repository.SchoolInfoRepository;
import com.school.sms.repository.StaffRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.PayrollService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final String DEFAULT_SCHOOL_NAME = "School Management System";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PayrollRepository payrollRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final SchoolInfoRepository schoolInfoRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PayrollDto> getAll(Long employeeId, PayrollEmployeeType employeeType, Integer month,
                                            Integer year, String status, Pageable pageable) {
        Specification<Payroll> spec = new SpecificationBuilder<Payroll>()
                .with(employeeId != null, "employeeId", SearchOperation.EQUALS, employeeId)
                .with(employeeType != null, "employeeType", SearchOperation.EQUALS, employeeType)
                .with(month != null, "month", SearchOperation.EQUALS, month)
                .with(year != null, "year", SearchOperation.EQUALS, year)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? PayrollStatus.valueOf(status.toUpperCase()) : null)
                .build();

        Page<Payroll> page = payrollRepository.findAll(spec, pageable);

        Map<Long, User> usersById = userRepository.findAllById(
                page.getContent().stream().map(Payroll::getEmployeeId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return PageResponse.from(page, page.getContent().stream().map(p -> toDto(p, usersById)).toList());
    }

    @Override
    @Transactional
    public PayrollGenerateResultDto generate(PayrollGenerateRequest request) {
        List<Long> employeeIds = request.getEmployeeType() == PayrollEmployeeType.TEACHER
                ? teacherRepository.findAllByDeletedFalseAndStatusOrderByIdAsc(TeacherStatus.ACTIVE).stream()
                        .map(Teacher::getUser).map(User::getId).toList()
                : staffRepository.findAllByDeletedFalseAndStatus(TeacherStatus.ACTIVE).stream()
                        .map(Staff::getUser).map(User::getId).toList();

        int generated = 0;
        int skipped = 0;

        for (Long employeeId : employeeIds) {
            SalaryStructure structure = salaryStructureRepository.findByEmployeeId(employeeId).orElse(null);
            if (structure == null || structure.getEmployeeType() != request.getEmployeeType()) {
                // No salary structure (of the requested type) for this employee — not a candidate at all.
                continue;
            }

            if (payrollRepository.existsByEmployeeIdAndMonthAndYear(employeeId, request.getMonth(), request.getYear())) {
                skipped++;
                continue;
            }

            BigDecimal basic = structure.getBasicSalary();
            BigDecimal allowances = structure.getHra().add(structure.getDa()).add(structure.getOtherAllowances());
            BigDecimal pf = percentOf(basic, structure.getPfPercentage());
            BigDecimal esi = percentOf(basic, structure.getEsiPercentage());
            BigDecimal deductions = pf.add(esi);
            BigDecimal netSalary = basic.add(allowances).subtract(deductions);

            Payroll payroll = Payroll.builder()
                    .employeeId(employeeId)
                    .employeeType(request.getEmployeeType())
                    .month(request.getMonth())
                    .year(request.getYear())
                    .basicSalary(basic)
                    .allowances(allowances)
                    .deductions(deductions)
                    .pf(pf)
                    .esi(esi)
                    .netSalary(netSalary)
                    .status(PayrollStatus.PENDING)
                    .build();
            payrollRepository.save(payroll);
            generated++;
        }

        return PayrollGenerateResultDto.builder().generatedCount(generated).skippedCount(skipped).build();
    }

    @Override
    @Transactional
    public PayrollDto markPaid(Long id, PayrollMarkPaidRequest request) {
        Payroll payroll = findEntity(id);
        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaymentDate(request.getPaymentDate());
        Payroll saved = payrollRepository.save(payroll);
        auditLogService.record("PAYROLL_PAID", "Payroll", saved.getId(), null, null);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SalarySlipDto getSalarySlip(Long id) {
        Payroll payroll = findEntity(id);
        User user = userRepository.findById(payroll.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", payroll.getEmployeeId()));

        String employeeCode = null;
        String departmentName = null;
        String designationName = null;

        // The payroll row itself only persists a combined "allowances" figure
        // (per the payroll table's actual columns — no separate hra/da/other
        // split survives generation). To still show the hra/da/otherAllowances
        // breakdown the slip format calls for, we read it off the employee's
        // CURRENT salary structure; if it has since changed, the breakdown may
        // no longer sum exactly to the historical payroll.allowances figure —
        // acceptable for a "printable slip" (no PDF yet) MVP, noted for the frontend.
        SalaryStructure currentStructure = salaryStructureRepository.findByEmployeeId(payroll.getEmployeeId()).orElse(null);
        BigDecimal hra = currentStructure != null ? currentStructure.getHra() : BigDecimal.ZERO;
        BigDecimal da = currentStructure != null ? currentStructure.getDa() : BigDecimal.ZERO;
        BigDecimal otherAllowances = currentStructure != null ? currentStructure.getOtherAllowances() : payroll.getAllowances();

        if (payroll.getEmployeeType() == PayrollEmployeeType.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(payroll.getEmployeeId()).orElse(null);
            if (teacher != null) {
                employeeCode = teacher.getEmployeeId();
                departmentName = teacher.getDepartment().getName();
                designationName = teacher.getDesignation().getName();
            }
        } else {
            Staff staff = staffRepository.findByUserId(payroll.getEmployeeId()).orElse(null);
            if (staff != null) {
                employeeCode = staff.getEmployeeId();
                departmentName = staff.getDepartment().getName();
                designationName = staff.getDesignation().getName();
            }
        }

        String schoolName = schoolInfoRepository.findById(1L)
                .map(info -> StringUtils.hasText(info.getName()) ? info.getName() : DEFAULT_SCHOOL_NAME)
                .orElse(DEFAULT_SCHOOL_NAME);

        return SalarySlipDto.builder()
                .employeeName(NameUtil.fullName(user.getFirstName(), user.getLastName()))
                .employeeId(employeeCode)
                .departmentName(departmentName)
                .designationName(designationName)
                .month(payroll.getMonth())
                .year(payroll.getYear())
                .basicSalary(payroll.getBasicSalary())
                .hra(hra)
                .da(da)
                .otherAllowances(otherAllowances)
                .pf(payroll.getPf())
                .esi(payroll.getEsi())
                .netSalary(payroll.getNetSalary())
                .schoolName(schoolName)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDashboardDto getDashboard(Integer month, Integer year) {
        List<Object[]> rows = payrollRepository.aggregateDashboard(month, year);
        Object[] row = rows.isEmpty() ? new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L} : rows.get(0);

        return PayrollDashboardDto.builder()
                .totalPaid((BigDecimal) row[0])
                .totalPending((BigDecimal) row[1])
                .employeeCount(((Number) row[2]).longValue())
                .build();
    }

    private BigDecimal percentOf(BigDecimal base, BigDecimal percentage) {
        return base.multiply(percentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private Payroll findEntity(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", id));
    }

    private PayrollDto toDto(Payroll payroll) {
        User user = userRepository.findById(payroll.getEmployeeId()).orElse(null);
        return toDto(payroll, user != null ? Map.of(user.getId(), user) : Map.of());
    }

    private PayrollDto toDto(Payroll payroll, Map<Long, User> usersById) {
        User user = usersById.get(payroll.getEmployeeId());
        return PayrollDto.builder()
                .id(payroll.getId())
                .employeeId(payroll.getEmployeeId())
                .employeeName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .employeeType(payroll.getEmployeeType().name())
                .month(payroll.getMonth())
                .year(payroll.getYear())
                .basicSalary(payroll.getBasicSalary())
                .allowances(payroll.getAllowances())
                .deductions(payroll.getDeductions())
                .pf(payroll.getPf())
                .esi(payroll.getEsi())
                .netSalary(payroll.getNetSalary())
                .paymentDate(payroll.getPaymentDate())
                .status(payroll.getStatus().name())
                .build();
    }
}
