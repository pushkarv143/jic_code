package com.greenwood.school.data.repository

import com.greenwood.school.core.common.DocumentOpener
import com.greenwood.school.core.common.IoDispatcher
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.ErrorMapper
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.api.CommunicationApi
import com.greenwood.school.data.remote.api.FeeApi
import com.greenwood.school.data.remote.api.HostelApi
import com.greenwood.school.data.remote.api.LibraryApi
import com.greenwood.school.data.remote.api.PayrollApi
import com.greenwood.school.data.remote.api.ReportApi
import com.greenwood.school.data.remote.api.SettingsApi
import com.greenwood.school.data.remote.api.TransportApi
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
import com.greenwood.school.data.remote.dto.GenerateDuesRequestDto
import com.greenwood.school.data.remote.dto.GenerateDuesResultDto
import com.greenwood.school.data.remote.dto.GeneratePayrollRequestDto
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
import com.greenwood.school.data.remote.dto.IssueBookRequestDto
import com.greenwood.school.data.remote.dto.LibraryDashboardDto
import com.greenwood.school.data.remote.dto.LibrarySummaryReportDto
import com.greenwood.school.data.remote.dto.MarkPaidRequestDto
import com.greenwood.school.data.remote.dto.NamedRequestDto
import com.greenwood.school.data.remote.dto.NoticeDto
import com.greenwood.school.data.remote.dto.NotificationDto
import com.greenwood.school.data.remote.dto.NotificationSendResultDto
import com.greenwood.school.data.remote.dto.PayrollDashboardDto
import com.greenwood.school.data.remote.dto.PayrollRunDto
import com.greenwood.school.data.remote.dto.PayrollSummaryReportDto
import com.greenwood.school.data.remote.dto.PickupPointDto
import com.greenwood.school.data.remote.dto.PickupPointRequestDto
import com.greenwood.school.data.remote.dto.ReturnBookRequestDto
import com.greenwood.school.data.remote.dto.RouteDto
import com.greenwood.school.data.remote.dto.RouteRequestDto
import com.greenwood.school.data.remote.dto.SalarySlipDto
import com.greenwood.school.data.remote.dto.SalaryStructureDto
import com.greenwood.school.data.remote.dto.SalaryStructureRequestDto
import com.greenwood.school.data.remote.dto.ScholarshipDto
import com.greenwood.school.data.remote.dto.ScholarshipRequestDto
import com.greenwood.school.data.remote.dto.SchoolInfoDto
import com.greenwood.school.data.remote.dto.SendNotificationRequestDto
import com.greenwood.school.data.remote.dto.StatusRequestDto
import com.greenwood.school.data.remote.dto.StudentFeeDto
import com.greenwood.school.data.remote.dto.StudentTransportDto
import com.greenwood.school.data.remote.dto.StudentTransportRequestDto
import com.greenwood.school.data.remote.dto.StudentsSummaryReportDto
import com.greenwood.school.data.remote.dto.SystemSettingDto
import com.greenwood.school.data.remote.dto.TeachersSummaryReportDto
import com.greenwood.school.data.remote.dto.TransportSummaryReportDto
import com.greenwood.school.data.remote.dto.VacateRequestDto
import com.greenwood.school.domain.repository.CommunicationRepository
import com.greenwood.school.domain.repository.FeeRepository
import com.greenwood.school.domain.repository.HostelRepository
import com.greenwood.school.domain.repository.LibraryRepository
import com.greenwood.school.domain.repository.PayrollRepository
import com.greenwood.school.domain.repository.ReportRepository
import com.greenwood.school.domain.repository.SettingsRepository
import com.greenwood.school.domain.repository.TransportRepository
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeeRepositoryImpl @Inject constructor(
    private val api: FeeApi,
    private val documentOpener: DocumentOpener,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), FeeRepository {

    override suspend fun getCategories(): ApiResult<List<FeeCategoryDto>> = call { api.getFeeCategories() }

    override suspend fun createCategory(name: String, description: String?): ApiResult<FeeCategoryDto> =
        call { api.createFeeCategory(NamedRequestDto(name, description)) }

    override suspend fun updateCategory(id: Long, name: String, description: String?): ApiResult<FeeCategoryDto> =
        call { api.updateFeeCategory(id, NamedRequestDto(name, description)) }

    override suspend fun deleteCategory(id: Long): ApiResult<Unit> = ack { api.deleteFeeCategory(id) }

    override suspend fun getStructures(
        classId: Long?,
        academicYearId: Long?,
        feeCategoryId: Long?,
    ): ApiResult<List<FeeStructureDto>> = call { api.getFeeStructures(classId, academicYearId, feeCategoryId) }

    override suspend fun createStructure(request: FeeStructureRequestDto): ApiResult<FeeStructureDto> =
        call { api.createFeeStructure(request) }

    override suspend fun updateStructure(id: Long, request: FeeStructureRequestDto): ApiResult<FeeStructureDto> =
        call { api.updateFeeStructure(id, request) }

    override suspend fun deleteStructure(id: Long): ApiResult<Unit> = ack { api.deleteFeeStructure(id) }

    override suspend fun getStudentFees(
        studentId: Long?,
        classId: Long?,
        sectionId: Long?,
        status: String?,
        academicYearId: Long?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<StudentFeeDto>> = paged {
        api.getStudentFees(studentId, classId, sectionId, status, academicYearId, page, size)
    }

    override suspend fun getStudentFee(id: Long): ApiResult<StudentFeeDto> = call { api.getStudentFee(id) }

    override suspend fun generateDues(
        classId: Long,
        academicYearId: Long,
        feeStructureIds: List<Long>,
    ): ApiResult<GenerateDuesResultDto> =
        call { api.generateDues(GenerateDuesRequestDto(classId, academicYearId, feeStructureIds)) }

    override suspend fun collectPayment(request: FeePaymentRequestDto): ApiResult<FeePaymentResultDto> =
        call { api.collectPayment(request) }

    override suspend fun getPayments(
        studentId: Long?,
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<FeePaymentDto>> = paged { api.getFeePayments(studentId, startDate, endDate, page, size) }

    override suspend fun getReceipt(paymentId: Long): ApiResult<FeeReceiptDto> = call { api.getReceipt(paymentId) }

    override suspend fun downloadReceipt(paymentId: Long): ApiResult<File> = execute {
        documentOpener.save(api.downloadReceiptPdf(paymentId), "fee-receipt-$paymentId.pdf") ?: failDownload()
    }

    override suspend fun getDuesSummary(classId: Long?, academicYearId: Long?): ApiResult<DuesSummaryDto> =
        call { api.getDuesSummary(classId, academicYearId) }

    override suspend fun getScholarships(
        studentId: Long?,
        academicYearId: Long?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<ScholarshipDto>> = paged { api.getScholarships(studentId, academicYearId, page, size) }

    override suspend fun createScholarship(request: ScholarshipRequestDto): ApiResult<ScholarshipDto> =
        call { api.createScholarship(request) }

    override suspend fun updateScholarship(id: Long, request: ScholarshipRequestDto): ApiResult<ScholarshipDto> =
        call { api.updateScholarship(id, request) }

    override suspend fun deleteScholarship(id: Long): ApiResult<Unit> = ack { api.deleteScholarship(id) }
}

@Singleton
class PayrollRepositoryImpl @Inject constructor(
    private val api: PayrollApi,
    private val documentOpener: DocumentOpener,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), PayrollRepository {

    override suspend fun getSalaryStructures(
        employeeId: Long?,
        employeeType: String?,
    ): ApiResult<List<SalaryStructureDto>> = call { api.getSalaryStructures(employeeId, employeeType) }

    override suspend fun createSalaryStructure(
        request: SalaryStructureRequestDto,
    ): ApiResult<SalaryStructureDto> = call { api.createSalaryStructure(request) }

    override suspend fun updateSalaryStructure(
        id: Long,
        request: SalaryStructureRequestDto,
    ): ApiResult<SalaryStructureDto> = call { api.updateSalaryStructure(id, request) }

    override suspend fun deleteSalaryStructure(id: Long): ApiResult<Unit> = ack { api.deleteSalaryStructure(id) }

    override suspend fun getRuns(
        employeeId: Long?,
        employeeType: String?,
        month: Int?,
        year: Int?,
        status: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<PayrollRunDto>> = paged {
        api.getPayrollRuns(employeeId, employeeType, month, year, status, page, size)
    }

    override suspend fun generate(
        employeeType: String,
        month: Int,
        year: Int,
    ): ApiResult<GeneratePayrollResultDto> =
        call { api.generatePayroll(GeneratePayrollRequestDto(employeeType, month, year)) }

    override suspend fun markPaid(id: Long, paymentDate: String): ApiResult<PayrollRunDto> =
        call { api.markPaid(id, MarkPaidRequestDto(paymentDate)) }

    override suspend fun getSalarySlip(id: Long): ApiResult<SalarySlipDto> = call { api.getSalarySlip(id) }

    override suspend fun downloadSalarySlip(id: Long): ApiResult<File> = execute {
        documentOpener.save(api.downloadSalarySlip(id), "salary-slip-$id.pdf") ?: failDownload()
    }

    override suspend fun getDashboard(month: Int?, year: Int?): ApiResult<PayrollDashboardDto> =
        call { api.getDashboard(month, year) }
}

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val api: LibraryApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), LibraryRepository {

    override suspend fun getCategories(): ApiResult<List<BookCategoryDto>> = call { api.getCategories() }

    override suspend fun createCategory(name: String): ApiResult<BookCategoryDto> =
        call { api.createCategory(NamedRequestDto(name)) }

    override suspend fun updateCategory(id: Long, name: String): ApiResult<BookCategoryDto> =
        call { api.updateCategory(id, NamedRequestDto(name)) }

    override suspend fun deleteCategory(id: Long): ApiResult<Unit> = ack { api.deleteCategory(id) }

    override suspend fun getBooks(
        search: String?,
        categoryId: Long?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<BookDto>> = paged {
        api.getBooks(search?.takeIf { it.isNotBlank() }, categoryId, page, size)
    }

    override suspend fun getBook(id: Long): ApiResult<BookDto> = call { api.getBook(id) }

    override suspend fun createBook(request: BookRequestDto): ApiResult<BookDto> = call { api.createBook(request) }

    override suspend fun updateBook(id: Long, request: BookRequestDto): ApiResult<BookDto> =
        call { api.updateBook(id, request) }

    override suspend fun deleteBook(id: Long): ApiResult<Unit> = ack { api.deleteBook(id) }

    override suspend fun getIssues(
        bookId: Long?,
        studentId: Long?,
        teacherId: Long?,
        status: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<BookIssueDto>> = paged {
        api.getIssues(bookId, studentId, teacherId, status, page, size)
    }

    override suspend fun issueBook(
        bookId: Long,
        studentId: Long?,
        teacherId: Long?,
        dueDate: String,
    ): ApiResult<BookIssueDto> = call { api.issueBook(IssueBookRequestDto(bookId, studentId, teacherId, dueDate)) }

    override suspend fun returnBook(issueId: Long, returnDate: String): ApiResult<BookIssueDto> =
        call { api.returnBook(issueId, ReturnBookRequestDto(returnDate)) }

    override suspend fun getOverdue(page: Int, size: Int): ApiResult<Paged<BookIssueDto>> =
        paged { api.getOverdue(page, size) }

    override suspend fun getDashboard(): ApiResult<LibraryDashboardDto> = call { api.getDashboard() }
}

@Singleton
class TransportRepositoryImpl @Inject constructor(
    private val api: TransportApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), TransportRepository {

    override suspend fun getDrivers(): ApiResult<List<DriverDto>> = call { api.getDrivers() }
    override suspend fun createDriver(request: DriverRequestDto): ApiResult<DriverDto> =
        call { api.createDriver(request) }

    override suspend fun updateDriver(id: Long, request: DriverRequestDto): ApiResult<DriverDto> =
        call { api.updateDriver(id, request) }

    override suspend fun deleteDriver(id: Long): ApiResult<Unit> = ack { api.deleteDriver(id) }

    override suspend fun getBuses(): ApiResult<List<BusDto>> = call { api.getBuses() }
    override suspend fun createBus(request: BusRequestDto): ApiResult<BusDto> = call { api.createBus(request) }
    override suspend fun updateBus(id: Long, request: BusRequestDto): ApiResult<BusDto> =
        call { api.updateBus(id, request) }

    override suspend fun deleteBus(id: Long): ApiResult<Unit> = ack { api.deleteBus(id) }

    override suspend fun getRoutes(busId: Long?): ApiResult<List<RouteDto>> = call { api.getRoutes(busId) }
    override suspend fun createRoute(request: RouteRequestDto): ApiResult<RouteDto> = call { api.createRoute(request) }
    override suspend fun updateRoute(id: Long, request: RouteRequestDto): ApiResult<RouteDto> =
        call { api.updateRoute(id, request) }

    override suspend fun deleteRoute(id: Long): ApiResult<Unit> = ack { api.deleteRoute(id) }

    override suspend fun getPickupPoints(routeId: Long): ApiResult<List<PickupPointDto>> =
        call { api.getPickupPoints(routeId) }

    override suspend fun createPickupPoint(
        routeId: Long,
        request: PickupPointRequestDto,
    ): ApiResult<PickupPointDto> = call { api.createPickupPoint(routeId, request) }

    override suspend fun updatePickupPoint(id: Long, request: PickupPointRequestDto): ApiResult<PickupPointDto> =
        call { api.updatePickupPoint(id, request) }

    override suspend fun deletePickupPoint(id: Long): ApiResult<Unit> = ack { api.deletePickupPoint(id) }

    override suspend fun getStudentTransport(
        studentId: Long?,
        routeId: Long?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<StudentTransportDto>> = paged { api.getStudentTransport(studentId, routeId, page, size) }

    override suspend fun assignTransport(request: StudentTransportRequestDto): ApiResult<StudentTransportDto> =
        call { api.assignTransport(request) }

    override suspend fun updateTransport(
        id: Long,
        request: StudentTransportRequestDto,
    ): ApiResult<StudentTransportDto> = call { api.updateTransportAssignment(id, request) }

    override suspend fun removeTransport(id: Long): ApiResult<Unit> = ack { api.removeTransportAssignment(id) }
}

@Singleton
class HostelRepositoryImpl @Inject constructor(
    private val api: HostelApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), HostelRepository {

    override suspend fun getHostels(): ApiResult<List<HostelDto>> = call { api.getHostels() }
    override suspend fun createHostel(request: HostelRequestDto): ApiResult<HostelDto> =
        call { api.createHostel(request) }

    override suspend fun updateHostel(id: Long, request: HostelRequestDto): ApiResult<HostelDto> =
        call { api.updateHostel(id, request) }

    override suspend fun deleteHostel(id: Long): ApiResult<Unit> = ack { api.deleteHostel(id) }

    override suspend fun getRooms(hostelId: Long): ApiResult<List<HostelRoomDto>> = call { api.getRooms(hostelId) }

    override suspend fun createRoom(hostelId: Long, request: HostelRoomRequestDto): ApiResult<HostelRoomDto> =
        call { api.createRoom(hostelId, request) }

    override suspend fun updateRoom(id: Long, request: HostelRoomRequestDto): ApiResult<HostelRoomDto> =
        call { api.updateRoom(id, request) }

    override suspend fun deleteRoom(id: Long): ApiResult<Unit> = ack { api.deleteRoom(id) }

    override suspend fun getResidents(
        studentId: Long?,
        roomId: Long?,
        status: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<HostelStudentDto>> = paged { api.getResidents(studentId, roomId, status, page, size) }

    override suspend fun allocateRoom(request: HostelStudentRequestDto): ApiResult<HostelStudentDto> =
        call { api.allocateRoom(request) }

    override suspend fun vacateRoom(id: Long, vacateDate: String): ApiResult<HostelStudentDto> =
        call { api.vacateRoom(id, VacateRequestDto(vacateDate)) }

    override suspend fun getVisitors(
        studentId: Long?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<HostelVisitorDto>> = paged { api.getVisitors(studentId, page, size) }

    override suspend fun logVisitor(request: HostelVisitorRequestDto): ApiResult<HostelVisitorDto> =
        call { api.logVisitor(request) }

    override suspend fun checkoutVisitor(id: Long): ApiResult<HostelVisitorDto> = call { api.checkoutVisitor(id) }

    override suspend fun getHostelFees(
        studentId: Long?,
        month: Int?,
        year: Int?,
        paidStatus: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<HostelFeeDto>> = paged { api.getHostelFees(studentId, month, year, paidStatus, page, size) }

    override suspend fun createHostelFee(request: HostelFeeRequestDto): ApiResult<HostelFeeDto> =
        call { api.createHostelFee(request) }

    override suspend fun markHostelFeePaid(id: Long): ApiResult<HostelFeeDto> = call { api.markHostelFeePaid(id) }
}

@Singleton
class CommunicationRepositoryImpl @Inject constructor(
    private val api: CommunicationApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), CommunicationRepository {

    override suspend fun getNotices(
        targetRole: String?,
        includeExpired: Boolean?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<NoticeDto>> = paged { api.getNotices(targetRole, includeExpired, page, size) }

    override suspend fun getNotice(id: Long): ApiResult<NoticeDto> = call { api.getNotice(id) }

    override suspend fun saveNotice(
        id: Long?,
        title: String,
        description: String,
        targetRole: String?,
        expiryDate: String?,
        attachment: File?,
    ): ApiResult<NoticeDto> = call {
        val filePart = attachment?.asFilePart()
        if (id == null) {
            api.createNotice(
                title = title.asTextPart(),
                description = description.asTextPart(),
                targetRole = targetRole?.asTextPart(),
                expiryDate = expiryDate?.asTextPart(),
                file = filePart,
            )
        } else {
            api.updateNotice(
                id = id,
                title = title.asTextPart(),
                description = description.asTextPart(),
                targetRole = targetRole?.asTextPart(),
                expiryDate = expiryDate?.asTextPart(),
                file = filePart,
            )
        }
    }

    override suspend fun deleteNotice(id: Long): ApiResult<Unit> = ack { api.deleteNotice(id) }

    override suspend fun getEvents(
        startDate: String?,
        endDate: String?,
        eventType: String?,
    ): ApiResult<List<CalendarEventDto>> = call { api.getEvents(startDate, endDate, eventType) }

    override suspend fun createEvent(request: CalendarEventRequestDto): ApiResult<CalendarEventDto> =
        call { api.createEvent(request) }

    override suspend fun updateEvent(id: Long, request: CalendarEventRequestDto): ApiResult<CalendarEventDto> =
        call { api.updateEvent(id, request) }

    override suspend fun deleteEvent(id: Long): ApiResult<Unit> = ack { api.deleteEvent(id) }

    override suspend fun getBirthdays(month: Int): ApiResult<BirthdaysResponseDto> = call { api.getBirthdays(month) }

    override suspend fun getMyNotifications(page: Int, size: Int): ApiResult<Paged<NotificationDto>> =
        paged { api.getMyNotifications(page, size) }

    override suspend fun sendNotification(
        request: SendNotificationRequestDto,
    ): ApiResult<NotificationSendResultDto> = call { api.sendNotification(request) }

    override suspend fun submitPublicEnquiry(
        request: AdmissionEnquiryRequestDto,
    ): ApiResult<AdmissionEnquiryDto> = call { api.submitPublicEnquiry(request) }

    override suspend fun getEnquiries(
        status: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<AdmissionEnquiryDto>> = paged { api.getEnquiries(status, page, size) }

    override suspend fun updateEnquiryStatus(id: Long, status: String): ApiResult<AdmissionEnquiryDto> =
        call { api.updateEnquiryStatus(id, StatusRequestDto(status)) }
}

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val api: ReportApi,
    private val documentOpener: DocumentOpener,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), ReportRepository {

    override suspend fun getAnalyticsDashboard(): ApiResult<AnalyticsDashboardDto> =
        call { api.getAnalyticsDashboard() }

    override suspend fun getStudentsSummary(): ApiResult<StudentsSummaryReportDto> =
        call { api.getStudentsSummary() }

    override suspend fun getTeachersSummary(): ApiResult<TeachersSummaryReportDto> =
        call { api.getTeachersSummary() }

    override suspend fun getAttendanceSummary(
        startDate: String?,
        endDate: String?,
        classId: Long?,
    ): ApiResult<AttendanceSummaryReportDto> = call { api.getAttendanceSummary(startDate, endDate, classId) }

    override suspend fun getFeeCollection(academicYearId: Long?): ApiResult<FeeCollectionReportDto> =
        call { api.getFeeCollection(academicYearId) }

    override suspend fun exportFeeCollection(academicYearId: Long?): ApiResult<File> = execute {
        documentOpener.save(api.exportFeeCollectionExcel(academicYearId), "fee-collection.xlsx") ?: failDownload()
    }

    override suspend fun getPayrollSummary(year: Int?): ApiResult<PayrollSummaryReportDto> =
        call { api.getPayrollSummary(year) }

    override suspend fun getLibrarySummary(): ApiResult<LibrarySummaryReportDto> = call { api.getLibrarySummary() }

    override suspend fun getTransportSummary(): ApiResult<TransportSummaryReportDto> =
        call { api.getTransportSummary() }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val api: SettingsApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), SettingsRepository {

    override suspend fun getSchoolInfo(): ApiResult<SchoolInfoDto> = call { api.getSchoolInfo() }

    override suspend fun updateSchoolInfo(request: SchoolInfoDto): ApiResult<SchoolInfoDto> =
        call { api.updateSchoolInfo(request) }

    override suspend fun getSystemSettings(): ApiResult<List<SystemSettingDto>> = call { api.getSystemSettings() }

    override suspend fun updateSystemSettings(
        settings: List<SystemSettingDto>,
    ): ApiResult<List<SystemSettingDto>> = call { api.updateSystemSettings(settings) }

    override suspend fun getAuditLogs(
        userId: Long?,
        entityName: String?,
        action: String?,
        page: Int,
        size: Int,
    ): ApiResult<Paged<AuditLogDto>> = paged {
        api.getAuditLogs(userId, entityName, action, page = page, size = size)
    }

    override suspend fun globalSearch(query: String): ApiResult<GlobalSearchResponseDto> =
        call { api.globalSearch(query) }
}
