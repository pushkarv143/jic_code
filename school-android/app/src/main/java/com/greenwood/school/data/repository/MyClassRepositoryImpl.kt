package com.greenwood.school.data.repository

import com.greenwood.school.core.common.IoDispatcher
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.ErrorMapper
import com.greenwood.school.data.remote.api.MyClassApi
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.HomeroomDto
import com.greenwood.school.data.remote.dto.MyClassOfficialRequestDto
import com.greenwood.school.data.remote.dto.MyClassStudentUpdateRequestDto
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.domain.repository.MyClassRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Straight pass-through to `/api/v1/my-class` — nothing is cached.
 *
 * <p>Unlike [AcademicRepositoryImpl], which memoises reference data that changes a
 * few times a year, a roster changes when the office admits or moves a pupil and a
 * post changes when the class teacher appoints one. A stale roster on a register is
 * worse than one extra request.
 */
@Singleton
class MyClassRepositoryImpl @Inject constructor(
    private val api: MyClassApi,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), MyClassRepository {

    override suspend fun myClass(): ApiResult<HomeroomDto> = call { api.myClass() }

    override suspend fun students(search: String?, page: Int, size: Int): ApiResult<Paged<StudentDto>> =
        paged { api.students(search = search?.takeIf { it.isNotBlank() }, page = page, size = size) }

    override suspend fun updateStudent(
        studentId: Long,
        request: MyClassStudentUpdateRequestDto,
    ): ApiResult<StudentDto> = call { api.updateStudent(studentId, request) }

    override suspend fun officials(): ApiResult<List<ClassOfficialDto>> = call { api.officials() }

    override suspend fun appointOfficial(request: MyClassOfficialRequestDto): ApiResult<ClassOfficialDto> =
        call { api.appointOfficial(request) }

    override suspend fun endOfficial(officialId: Long): ApiResult<Unit> = ack { api.endOfficial(officialId) }
}
