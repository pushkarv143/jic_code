package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.ClassOfficialDto
import com.greenwood.school.data.remote.dto.HomeroomDto
import com.greenwood.school.data.remote.dto.MyClassOfficialRequestDto
import com.greenwood.school.data.remote.dto.MyClassStudentUpdateRequestDto
import com.greenwood.school.core.network.Paged
import com.greenwood.school.data.remote.dto.StudentDto

/**
 * The class teacher's own section.
 *
 * <p>Nothing here takes a class or section id: the server resolves the section from
 * `sections.class_teacher_id` for the caller, so there is no parameter the app could
 * get wrong and nothing to tamper with.
 *
 * <p>No add-student method, deliberately. Admitting a pupil is the office's job and
 * the API has no route for a class teacher to do it — see [MyClassApi].
 */
interface MyClassRepository {

    /** The caller's homeroom section. Fails with 403 if they hold none. */
    suspend fun myClass(): ApiResult<HomeroomDto>

    suspend fun students(search: String? = null, page: Int = 0, size: Int = 50): ApiResult<Paged<StudentDto>>

    suspend fun updateStudent(studentId: Long, request: MyClassStudentUpdateRequestDto): ApiResult<StudentDto>

    suspend fun officials(): ApiResult<List<ClassOfficialDto>>

    suspend fun appointOfficial(request: MyClassOfficialRequestDto): ApiResult<ClassOfficialDto>

    suspend fun endOfficial(officialId: Long): ApiResult<Unit>
}
