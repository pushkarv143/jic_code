package com.school.sms.service.impl;

import com.school.sms.dto.response.CategoryAmountDto;
import com.school.sms.dto.response.ExcelImportResultDto;
import com.school.sms.dto.response.FeeCollectionReportDto;
import com.school.sms.dto.response.ImportSkippedRowDto;
import com.school.sms.dto.response.MonthCollectedDto;
import com.school.sms.entity.AcademicYear;
import com.school.sms.entity.Guardian;
import com.school.sms.entity.Role;
import com.school.sms.entity.SchoolClass;
import com.school.sms.entity.Section;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentStatus;
import com.school.sms.entity.Teacher;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.AcademicYearRepository;
import com.school.sms.repository.GuardianRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.SchoolClassRepository;
import com.school.sms.repository.SectionRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.ExcelService;
import com.school.sms.service.ReportService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.ExcelGenerator;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelServiceImpl implements ExcelService {

    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReportService reportService;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportStudents(Long classId, Long sectionId, String status) {
        Specification<Student> spec = new SpecificationBuilder<Student>()
                .with("deleted", SearchOperation.EQUALS, false)
                .with(classId != null, "schoolClass.id", SearchOperation.EQUALS, classId)
                .with(sectionId != null, "section.id", SearchOperation.EQUALS, sectionId)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? StudentStatus.valueOf(status.toUpperCase()) : null)
                .build();
        List<Student> students = studentRepository.findAll(spec);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");
        CellStyle headerStyle = ExcelGenerator.headerStyle(workbook);
        String[] columns = {"Admission Number", "Name", "Class", "Section", "Roll Number", "Status", "Guardian Name", "Guardian Phone"};
        ExcelGenerator.writeHeaderRow(sheet, headerStyle, columns);

        int rowIdx = 1;
        for (Student student : students) {
            Row row = sheet.createRow(rowIdx++);
            Guardian primary = resolvePrimaryGuardian(student.getId());
            String name = student.getUser() != null
                    ? NameUtil.fullName(student.getUser().getFirstName(), student.getUser().getLastName())
                    : (primary != null ? primary.getName() : "");

            ExcelGenerator.setCell(row, 0, student.getAdmissionNumber());
            ExcelGenerator.setCell(row, 1, name);
            ExcelGenerator.setCell(row, 2, student.getSchoolClass().getClassName());
            ExcelGenerator.setCell(row, 3, student.getSection().getSectionName());
            ExcelGenerator.setCell(row, 4, student.getRollNumber());
            ExcelGenerator.setCell(row, 5, student.getStatus().name());
            ExcelGenerator.setCell(row, 6, primary != null ? primary.getName() : "");
            ExcelGenerator.setCell(row, 7, primary != null ? primary.getPhone() : "");
        }

        ExcelGenerator.autoSizeColumns(sheet, columns.length);
        return ExcelGenerator.toBytes(workbook);
    }

    @Override
    @Transactional
    public ExcelImportResultDto importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("An .xlsx file is required");
        }

        AcademicYear currentYear = academicYearRepository.findByCurrentTrue()
                .orElseThrow(() -> new BadRequestException(
                        "No current academic year is configured; set one (Settings > Academic Years) before importing students"));
        Role studentRole = roleRepository.findByName(AppConstants.ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", AppConstants.ROLE_STUDENT));

        List<ImportSkippedRowDto> skipped = new ArrayList<>();
        int imported = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                int displayRowNumber = rowIdx + 1;
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                try {
                    imported += importRow(row, formatter, currentYear, studentRole) ? 1 : 0;
                } catch (BadRequestException ex) {
                    skipped.add(ImportSkippedRowDto.builder().rowNumber(displayRowNumber).reason(ex.getMessage()).build());
                } catch (Exception ex) {
                    skipped.add(ImportSkippedRowDto.builder().rowNumber(displayRowNumber)
                            .reason("Unexpected error: " + ex.getMessage()).build());
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read the uploaded Excel file: " + ex.getMessage());
        }

        return ExcelImportResultDto.builder().importedCount(imported).skippedRows(skipped).build();
    }

    /**
     * Note: bulk-imported students always get a login account. Students carry no name
     * column of their own per SCHEMA_CONTRACT.md (only an optional linked User does), so
     * without one the spreadsheet's "Name" column would have nowhere to be persisted. A
     * username/email are derived from the generated admission number and a random,
     * never-communicated password is set — this account exists purely to hold identity
     * fields, not for the student to actually log in with (unlike StudentServiceImpl.create(),
     * where a login is only created when the caller explicitly supplies credentials).
     */
    private boolean importRow(Row row, DataFormatter formatter, AcademicYear currentYear, Role studentRole) {
        String name = cellText(row, 0, formatter);
        String className = cellText(row, 1, formatter);
        String sectionName = cellText(row, 2, formatter);
        String rollNumberText = cellText(row, 3, formatter);
        String statusText = cellText(row, 4, formatter);
        String guardianName = cellText(row, 5, formatter);
        String guardianPhone = cellText(row, 6, formatter);

        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("Name is required");
        }
        if (!StringUtils.hasText(className)) {
            throw new BadRequestException("Class is required");
        }
        if (!StringUtils.hasText(sectionName)) {
            throw new BadRequestException("Section is required");
        }

        SchoolClass schoolClass = schoolClassRepository
                .findFirstByClassNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(className, currentYear.getId())
                .orElseThrow(() -> new BadRequestException("Class not found in the current academic year: " + className));
        Section section = sectionRepository
                .findFirstBySchoolClassIdAndSectionNameIgnoreCase(schoolClass.getId(), sectionName)
                .orElseThrow(() -> new BadRequestException("Section not found: " + sectionName + " in class " + className));

        Integer rollNumber = parseRollNumber(rollNumberText);
        StudentStatus status = parseStatus(statusText);

        String admissionNumber = generateAdmissionNumber();
        String[] nameParts = splitName(name);

        User user = User.builder()
                .username(admissionNumber.toLowerCase())
                .email(admissionNumber.toLowerCase() + "@students.school.local")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .role(studentRole)
                .active(true)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .admissionNumber(admissionNumber)
                .schoolClass(schoolClass)
                .section(section)
                .rollNumber(rollNumber)
                .academicYear(currentYear)
                .status(status)
                .deleted(false)
                .build();

        if (StringUtils.hasText(guardianName)) {
            Guardian guardian = Guardian.builder()
                    .student(student)
                    .name(guardianName)
                    .phone(StringUtils.hasText(guardianPhone) ? guardianPhone : null)
                    .primary(true)
                    .build();
            student.getGuardians().add(guardian);
        }

        studentRepository.save(student);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTeachers() {
        List<Teacher> teachers = teacherRepository.findAll().stream().filter(t -> !t.isDeleted()).toList();

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Teachers");
        CellStyle headerStyle = ExcelGenerator.headerStyle(workbook);
        String[] columns = {"Employee ID", "Name", "Department", "Designation", "Status", "Phone", "Email"};
        ExcelGenerator.writeHeaderRow(sheet, headerStyle, columns);

        int rowIdx = 1;
        for (Teacher teacher : teachers) {
            Row row = sheet.createRow(rowIdx++);
            User user = teacher.getUser();
            ExcelGenerator.setCell(row, 0, teacher.getEmployeeId());
            ExcelGenerator.setCell(row, 1, user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : "");
            ExcelGenerator.setCell(row, 2, teacher.getDepartment().getName());
            ExcelGenerator.setCell(row, 3, teacher.getDesignation().getName());
            ExcelGenerator.setCell(row, 4, teacher.getStatus().name());
            ExcelGenerator.setCell(row, 5, user != null ? user.getPhone() : "");
            ExcelGenerator.setCell(row, 6, user != null ? user.getEmail() : "");
        }

        ExcelGenerator.autoSizeColumns(sheet, columns.length);
        return ExcelGenerator.toBytes(workbook);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportFeeCollectionReport(Long academicYearId) {
        FeeCollectionReportDto report = reportService.getFeeCollectionReport(academicYearId);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Fee Collection");
        CellStyle headerStyle = ExcelGenerator.headerStyle(workbook);

        int rowIdx = 0;
        Row totalsHeader = sheet.createRow(rowIdx++);
        writeStyledRow(totalsHeader, headerStyle, "Total Due", "Total Collected", "Total Outstanding");
        Row totalsRow = sheet.createRow(rowIdx++);
        ExcelGenerator.setCell(totalsRow, 0, report.getTotalDue());
        ExcelGenerator.setCell(totalsRow, 1, report.getTotalCollected());
        ExcelGenerator.setCell(totalsRow, 2, report.getTotalOutstanding());

        rowIdx++;
        Row categoryHeader = sheet.createRow(rowIdx++);
        writeStyledRow(categoryHeader, headerStyle, "Category", "Collected");
        List<CategoryAmountDto> byCategory = report.getByCategory() != null ? report.getByCategory() : List.of();
        for (CategoryAmountDto category : byCategory) {
            Row row = sheet.createRow(rowIdx++);
            ExcelGenerator.setCell(row, 0, category.getCategoryName());
            ExcelGenerator.setCell(row, 1, category.getCollected());
        }

        rowIdx++;
        Row monthHeader = sheet.createRow(rowIdx++);
        writeStyledRow(monthHeader, headerStyle, "Month", "Collected");
        List<MonthCollectedDto> byMonth = report.getByMonth() != null ? report.getByMonth() : List.of();
        for (MonthCollectedDto month : byMonth) {
            Row row = sheet.createRow(rowIdx++);
            ExcelGenerator.setCell(row, 0, monthName(month.getMonth()));
            ExcelGenerator.setCell(row, 1, month.getCollected());
        }

        ExcelGenerator.autoSizeColumns(sheet, 3);
        return ExcelGenerator.toBytes(workbook);
    }

    private void writeStyledRow(Row row, CellStyle style, String... values) {
        for (int i = 0; i < values.length; i++) {
            ExcelGenerator.setCell(row, i, values[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private Guardian resolvePrimaryGuardian(Long studentId) {
        List<Guardian> guardians = guardianRepository.findAllByStudentIdOrderByIdAsc(studentId);
        return guardians.stream().filter(Guardian::isPrimary).findFirst()
                .or(() -> guardians.stream().findFirst())
                .orElse(null);
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int col = 0; col <= 6; col++) {
            if (StringUtils.hasText(cellText(row, col, formatter))) {
                return false;
            }
        }
        return true;
    }

    private String cellText(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private Integer parseRollNumber(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return (int) Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid roll number: " + text);
        }
    }

    private StudentStatus parseStatus(String text) {
        if (!StringUtils.hasText(text)) {
            return StudentStatus.ACTIVE;
        }
        try {
            return StudentStatus.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + text);
        }
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx < 0) {
            return new String[]{trimmed, null};
        }
        return new String[]{trimmed.substring(0, spaceIdx), trimmed.substring(spaceIdx + 1).trim()};
    }

    private String generateAdmissionNumber() {
        int year = Year.now().getValue();
        String prefix = "ADM" + year;
        long sequence = studentRepository.countByAdmissionNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private String monthName(Integer month) {
        if (month == null || month < 1 || month > 12) {
            return "-";
        }
        return MONTH_NAMES[month - 1];
    }
}
