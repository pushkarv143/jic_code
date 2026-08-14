package com.greenwood.school.di

import com.greenwood.school.data.repository.AcademicRepositoryImpl
import com.greenwood.school.data.repository.AttendanceRepositoryImpl
import com.greenwood.school.data.repository.AuthRepositoryImpl
import com.greenwood.school.data.repository.ClassroomRepositoryImpl
import com.greenwood.school.data.repository.CommunicationRepositoryImpl
import com.greenwood.school.data.repository.ExamRepositoryImpl
import com.greenwood.school.data.repository.FeeRepositoryImpl
import com.greenwood.school.data.repository.HostelRepositoryImpl
import com.greenwood.school.data.repository.LibraryRepositoryImpl
import com.greenwood.school.data.repository.PayrollRepositoryImpl
import com.greenwood.school.data.repository.PeopleRepositoryImpl
import com.greenwood.school.data.repository.ReportRepositoryImpl
import com.greenwood.school.data.repository.SettingsRepositoryImpl
import com.greenwood.school.data.repository.StudentRepositoryImpl
import com.greenwood.school.data.repository.TransportRepositoryImpl
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.AttendanceRepository
import com.greenwood.school.domain.repository.AuthRepository
import com.greenwood.school.domain.repository.ClassroomRepository
import com.greenwood.school.domain.repository.CommunicationRepository
import com.greenwood.school.domain.repository.ExamRepository
import com.greenwood.school.domain.repository.FeeRepository
import com.greenwood.school.domain.repository.HostelRepository
import com.greenwood.school.domain.repository.LibraryRepository
import com.greenwood.school.domain.repository.PayrollRepository
import com.greenwood.school.domain.repository.PeopleRepository
import com.greenwood.school.domain.repository.ReportRepository
import com.greenwood.school.domain.repository.SettingsRepository
import com.greenwood.school.domain.repository.StudentRepository
import com.greenwood.school.domain.repository.TransportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every domain repository contract to its data-layer implementation.
 *
 * ViewModels depend on the interfaces only, which is what lets the ViewModel tests
 * substitute fakes without Hilt or a network stack.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton abstract fun bindAcademicRepository(impl: AcademicRepositoryImpl): AcademicRepository

    @Binds @Singleton abstract fun bindStudentRepository(impl: StudentRepositoryImpl): StudentRepository

    @Binds @Singleton abstract fun bindPeopleRepository(impl: PeopleRepositoryImpl): PeopleRepository

    @Binds @Singleton abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds @Singleton abstract fun bindExamRepository(impl: ExamRepositoryImpl): ExamRepository

    @Binds @Singleton abstract fun bindClassroomRepository(impl: ClassroomRepositoryImpl): ClassroomRepository

    @Binds @Singleton abstract fun bindFeeRepository(impl: FeeRepositoryImpl): FeeRepository

    @Binds @Singleton abstract fun bindPayrollRepository(impl: PayrollRepositoryImpl): PayrollRepository

    @Binds @Singleton abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds @Singleton abstract fun bindTransportRepository(impl: TransportRepositoryImpl): TransportRepository

    @Binds @Singleton abstract fun bindHostelRepository(impl: HostelRepositoryImpl): HostelRepository

    @Binds
    @Singleton
    abstract fun bindCommunicationRepository(impl: CommunicationRepositoryImpl): CommunicationRepository

    @Binds @Singleton abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
