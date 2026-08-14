package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.dto.AdmissionEnquiryDto
import com.greenwood.school.data.remote.dto.AdmissionEnquiryRequestDto
import com.greenwood.school.data.remote.dto.AnalyticsDashboardDto
import com.greenwood.school.data.remote.dto.AttendanceSummaryReportDto
import com.greenwood.school.data.remote.dto.AuditLogDto
import com.greenwood.school.data.remote.dto.BirthdaysResponseDto
import com.greenwood.school.data.remote.dto.BookCategoryDto
import com.greenwood.school.data.remote.dto.BookDto
import com.greenwood.school.data.remote.dto.BookIssueDto
import com.greenwood.school.data.remote.dto.BookRequestDto
import com.greenwood.school.data.remote.dto.BusDto
import com.greenwood.school.data.remote.dto.BusRequestDto
import com.greenwood.school.data.remote.dto.CalendarEventDto
import com.greenwood.school.data.remote.dto.CalendarEventRequestDto
import com.greenwood.school.data.remote.dto.DriverDto
import com.greenwood.school.data.remote.dto.DriverRequestDto
import com.greenwood.school.data.remote.dto.DuesSummaryDto
import com.greenwood.school.data.remote.dto.FeeCategoryDto
import com.greenwood.school.data.remote.dto.FeeCollectionReportDto
import com.greenwood.school.data.remote.dto.FeePaymentDto
import com.greenwood.school.data.remote.dto.FeePaymentRequestDto
import com.greenwood.school.data.remote.dto.FeePaymentResultDto
import com.greenwood.school.data.remote.dto.FeeReceiptDto
import com.greenwood.school.data.remote.dto.FeeStructureDto
import com.greenwood.school.data.remote.dto.FeeStructureRequestDto
import com.greenwood.school.data.remote.dto.GenerateDuesResultDto
import com.greenwood.school.data.remote.dto.GeneratePayrollResultDto
import com.greenwood.school.data.remote.dto.GlobalSearchResponseDto
import com.greenwood.school.data.remote.dto.HostelDto
import com.greenwood.school.data.remote.dto.HostelFeeDto
import com.greenwood.school.data.remote.dto.HostelFeeRequestDto
import com.greenwood.school.data.remote.dto.HostelRequestDto
import com.greenwood.school.data.remote.dto.HostelRoomDto
import com.greenwood.school.data.remote.dto.HostelRoomRequestDto
import com.greenwood.school.data.remote.dto.HostelStudentDto
import com.greenwood.school.data.remote.dto.HostelStudentRequestDto
import com.greenwood.school.data.remote.dto.HostelVisitorDto
import com.greenwood.school.data.remote.dto.HostelVisitorRequestDto
import com.greenwood.school.data.remote.dto.LibraryDashboardDto
import com.greenwood.school.data.remote.dto.LibrarySummaryReportDto
import com.greenwood.school.data.remote.dto.NoticeDto
import com.greenwood.school.data.remote.dto.NotificationDto
import com.greenwood.school.data.remote.dto.NotificationSendResultDto
import com.greenwood.school.data.remote.dto.PayrollDashboardDto
import com.greenwood.school.data.remote.dto.PayrollRunDto
import com.greenwood.school.data.remote.dto.PayrollSummaryReportDto
import com.greenwood.school.data.remote.dto.PickupPointDto
import com.greenwood.school.data.remote.dto.PickupPointRequestDto
import com.greenwood.school.data.remote.dto.RouteDto
import com.greenwood.school.data.remote.dto.RouteRequestDto
import com.greenwood.school.data.remote.dto.SalarySlipDto
import com.greenwood.school.data.remote.dto.SalaryStructureDto
import com.greenwood.school.data.remote.dto.SalaryStructureRequestDto
import com.greenwood.school.data.remote.dto.ScholarshipDto
import com.greenwood.school.data.remote.dto.ScholarshipRequestDto
import com.greenwood.school.data.remote.dto.SchoolInfoDto
import com.greenwood.school.data.remote.dto.SendNotificationRequestDto
import com.greenwood.school.data.remote.dto.StudentFeeDto
import com.greenwood.school.data.remote.dto.StudentTransportDto
import com.greenwood.school.data.remote.dto.StudentTransportRequestDto
import com.greenwood.school.data.remote.dto.StudentsSummaryReportDto
import com.greenwood.school.data.remote.dto.SystemSettingDto
import com.greenwood.school.data.remote.dto.TeachersSummaryReportDto
import com.greenwood.school.data.remote.dto.TransportSummaryReportDto
import java.io.File

/** Fees, payroll, library, transport, hostel, communication, reporting, settings. */

interface FeeRepository {
    suspend fun getCategories(): ApiResult<List<FeeCategoryDto>>
    suspend fun createCategory(name: String, description: String?): ApiResult<FeeCategoryDto>
    suspend fun updateCategory(id: Long, name: String, description: String?): ApiResult<FeeCategoryDto>
    suspend fun deleteCategory(id: Long): ApiResult<Unit>

    suspend fun getStructures(
        classId: Long? = null,
        academicYearId: Long? = null,
        feeCategoryId: Long? = null,
    ): ApiResult<List<FeeStructureDto>>

    suspend fun createStructure(request: FeeStructureRequestDto): ApiResult<FeeStructureDto>
    suspend fun updateStructure(id: Long, request: FeeStructureRequestDto): ApiResult<FeeStructureDto>
    suspend fun deleteStructure(id: Long): ApiResult<Unit>

    suspend fun getStudentFees(
        studentId: Long? = null,
        classId: Long? = null,
        sectionId: Long? = null,
        status: String? = null,
        academicYearId: Long? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<StudentFeeDto>>

    suspend fun getStudentFee(id: Long): ApiResult<StudentFeeDto>

    suspend fun generateDues(
        classId: Long,
        academicYearId: Long,
        feeStructureIds: List<Long>,
    ): ApiResult<GenerateDuesResultDto>

    suspend fun collectPayment(request: FeePaymentRequestDto): ApiResult<FeePaymentResultDto>

    suspend fun getPayments(
        studentId: Long? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<FeePaymentDto>>

    suspend fun getReceipt(paymentId: Long): ApiResult<FeeReceiptDto>
    suspend fun downloadReceipt(paymentId: Long): ApiResult<File>
    suspend fun getDuesSummary(classId: Long? = null, academicYearId: Long? = null): ApiResult<DuesSummaryDto>

    suspend fun getScholarships(
        studentId: Long? = null,
        academicYearId: Long? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<ScholarshipDto>>

    suspend fun createScholarship(request: ScholarshipRequestDto): ApiResult<ScholarshipDto>
    suspend fun updateScholarship(id: Long, request: ScholarshipRequestDto): ApiResult<ScholarshipDto>
    suspend fun deleteScholarship(id: Long): ApiResult<Unit>
}

interface PayrollRepository {
    suspend fun getSalaryStructures(
        employeeId: Long? = null,
        employeeType: String? = null,
    ): ApiResult<List<SalaryStructureDto>>

    suspend fun createSalaryStructure(request: SalaryStructureRequestDto): ApiResult<SalaryStructureDto>
    suspend fun updateSalaryStructure(id: Long, request: SalaryStructureRequestDto): ApiResult<SalaryStructureDto>
    suspend fun deleteSalaryStructure(id: Long): ApiResult<Unit>

    suspend fun getRuns(
        employeeId: Long? = null,
        employeeType: String? = null,
        month: Int? = null,
        year: Int? = null,
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<PayrollRunDto>>

    suspend fun generate(employeeType: String, month: Int, year: Int): ApiResult<GeneratePayrollResultDto>
    suspend fun markPaid(id: Long, paymentDate: String): ApiResult<PayrollRunDto>
    suspend fun getSalarySlip(id: Long): ApiResult<SalarySlipDto>
    suspend fun downloadSalarySlip(id: Long): ApiResult<File>
    suspend fun getDashboard(month: Int? = null, year: Int? = null): ApiResult<PayrollDashboardDto>
}

interface LibraryRepository {
    suspend fun getCategories(): ApiResult<List<BookCategoryDto>>
    suspend fun createCategory(name: String): ApiResult<BookCategoryDto>
    suspend fun updateCategory(id: Long, name: String): ApiResult<BookCategoryDto>
    suspend fun deleteCategory(id: Long): ApiResult<Unit>

    suspend fun getBooks(
        search: String? = null,
        categoryId: Long? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<BookDto>>

    suspend fun getBook(id: Long): ApiResult<BookDto>
    suspend fun createBook(request: BookRequestDto): ApiResult<BookDto>
    suspend fun updateBook(id: Long, request: BookRequestDto): ApiResult<BookDto>
    suspend fun deleteBook(id: Long): ApiResult<Unit>

    suspend fun getIssues(
        bookId: Long? = null,
        studentId: Long? = null,
        teacherId: Long? = null,
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<BookIssueDto>>

    suspend fun issueBook(bookId: Long, studentId: Long?, teacherId: Long?, dueDate: String): ApiResult<BookIssueDto>
    suspend fun returnBook(issueId: Long, returnDate: String): ApiResult<BookIssueDto>
    suspend fun getOverdue(page: Int = 0, size: Int = 20): ApiResult<Paged<BookIssueDto>>
    suspend fun getDashboard(): ApiResult<LibraryDashboardDto>
}

interface TransportRepository {
    suspend fun getDrivers(): ApiResult<List<DriverDto>>
    suspend fun createDriver(request: DriverRequestDto): ApiResult<DriverDto>
    suspend fun updateDriver(id: Long, request: DriverRequestDto): ApiResult<DriverDto>
    suspend fun deleteDriver(id: Long): ApiResult<Unit>

    suspend fun getBuses(): ApiResult<List<BusDto>>
    suspend fun createBus(request: BusRequestDto): ApiResult<BusDto>
    suspend fun updateBus(id: Long, request: BusRequestDto): ApiResult<BusDto>
    suspend fun deleteBus(id: Long): ApiResult<Unit>

    suspend fun getRoutes(busId: Long? = null): ApiResult<List<RouteDto>>
    suspend fun createRoute(request: RouteRequestDto): ApiResult<RouteDto>
    suspend fun updateRoute(id: Long, request: RouteRequestDto): ApiResult<RouteDto>
    suspend fun deleteRoute(id: Long): ApiResult<Unit>

    suspend fun getPickupPoints(routeId: Long): ApiResult<List<PickupPointDto>>
    suspend fun createPickupPoint(routeId: Long, request: PickupPointRequestDto): ApiResult<PickupPointDto>
    suspend fun updatePickupPoint(id: Long, request: PickupPointRequestDto): ApiResult<PickupPointDto>
    suspend fun deletePickupPoint(id: Long): ApiResult<Unit>

    suspend fun getStudentTransport(
        studentId: Long? = null,
        routeId: Long? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<StudentTransportDto>>

    suspend fun assignTransport(request: StudentTransportRequestDto): ApiResult<StudentTransportDto>
    suspend fun updateTransport(id: Long, request: StudentTransportRequestDto): ApiResult<StudentTransportDto>
    suspend fun removeTransport(id: Long): ApiResult<Unit>
}

interface HostelRepository {
    suspend fun getHostels(): ApiResult<List<HostelDto>>
    suspend fun createHostel(request: HostelRequestDto): ApiResult<HostelDto>
    suspend fun updateHostel(id: Long, request: HostelRequestDto): ApiResult<HostelDto>
    suspend fun deleteHostel(id: Long): ApiResult<Unit>

    suspend fun getRooms(hostelId: Long): ApiResult<List<HostelRoomDto>>
    suspend fun createRoom(hostelId: Long, request: HostelRoomRequestDto): ApiResult<HostelRoomDto>
    suspend fun updateRoom(id: Long, request: HostelRoomRequestDto): ApiResult<HostelRoomDto>
    suspend fun deleteRoom(id: Long): ApiResult<Unit>

    suspend fun getResidents(
        studentId: Long? = null,
        roomId: Long? = null,
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<HostelStudentDto>>

    suspend fun allocateRoom(request: HostelStudentRequestDto): ApiResult<HostelStudentDto>
    suspend fun vacateRoom(id: Long, vacateDate: String): ApiResult<HostelStudentDto>

    suspend fun getVisitors(studentId: Long? = null, page: Int = 0, size: Int = 20): ApiResult<Paged<HostelVisitorDto>>
    suspend fun logVisitor(request: HostelVisitorRequestDto): ApiResult<HostelVisitorDto>
    suspend fun checkoutVisitor(id: Long): ApiResult<HostelVisitorDto>

    suspend fun getHostelFees(
        studentId: Long? = null,
        month: Int? = null,
        year: Int? = null,
        paidStatus: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<HostelFeeDto>>

    suspend fun createHostelFee(request: HostelFeeRequestDto): ApiResult<HostelFeeDto>
    suspend fun markHostelFeePaid(id: Long): ApiResult<HostelFeeDto>
}

interface CommunicationRepository {
    suspend fun getNotices(
        targetRole: String? = null,
        includeExpired: Boolean? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<NoticeDto>>

    suspend fun getNotice(id: Long): ApiResult<NoticeDto>

    suspend fun saveNotice(
        id: Long?,
        title: String,
        description: String,
        targetRole: String?,
        expiryDate: String?,
        attachment: File?,
    ): ApiResult<NoticeDto>

    suspend fun deleteNotice(id: Long): ApiResult<Unit>

    suspend fun getEvents(
        startDate: String? = null,
        endDate: String? = null,
        eventType: String? = null,
    ): ApiResult<List<CalendarEventDto>>

    suspend fun createEvent(request: CalendarEventRequestDto): ApiResult<CalendarEventDto>
    suspend fun updateEvent(id: Long, request: CalendarEventRequestDto): ApiResult<CalendarEventDto>
    suspend fun deleteEvent(id: Long): ApiResult<Unit>
    suspend fun getBirthdays(month: Int): ApiResult<BirthdaysResponseDto>

    suspend fun getMyNotifications(page: Int = 0, size: Int = 20): ApiResult<Paged<NotificationDto>>
    suspend fun sendNotification(request: SendNotificationRequestDto): ApiResult<NotificationSendResultDto>

    suspend fun submitPublicEnquiry(request: AdmissionEnquiryRequestDto): ApiResult<AdmissionEnquiryDto>

    suspend fun getEnquiries(
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<AdmissionEnquiryDto>>

    suspend fun updateEnquiryStatus(id: Long, status: String): ApiResult<AdmissionEnquiryDto>
}

interface ReportRepository {
    suspend fun getAnalyticsDashboard(): ApiResult<AnalyticsDashboardDto>
    suspend fun getStudentsSummary(): ApiResult<StudentsSummaryReportDto>
    suspend fun getTeachersSummary(): ApiResult<TeachersSummaryReportDto>

    suspend fun getAttendanceSummary(
        startDate: String? = null,
        endDate: String? = null,
        classId: Long? = null,
    ): ApiResult<AttendanceSummaryReportDto>

    suspend fun getFeeCollection(academicYearId: Long? = null): ApiResult<FeeCollectionReportDto>
    suspend fun exportFeeCollection(academicYearId: Long? = null): ApiResult<File>
    suspend fun getPayrollSummary(year: Int? = null): ApiResult<PayrollSummaryReportDto>
    suspend fun getLibrarySummary(): ApiResult<LibrarySummaryReportDto>
    suspend fun getTransportSummary(): ApiResult<TransportSummaryReportDto>
}

interface SettingsRepository {
    suspend fun getSchoolInfo(): ApiResult<SchoolInfoDto>
    suspend fun updateSchoolInfo(request: SchoolInfoDto): ApiResult<SchoolInfoDto>
    suspend fun getSystemSettings(): ApiResult<List<SystemSettingDto>>
    suspend fun updateSystemSettings(settings: List<SystemSettingDto>): ApiResult<List<SystemSettingDto>>

    suspend fun getAuditLogs(
        userId: Long? = null,
        entityName: String? = null,
        action: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<Paged<AuditLogDto>>

    suspend fun globalSearch(query: String): ApiResult<GlobalSearchResponseDto>
}
