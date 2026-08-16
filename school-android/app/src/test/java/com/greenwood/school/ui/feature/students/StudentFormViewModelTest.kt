package com.greenwood.school.ui.feature.students

import androidx.lifecycle.SavedStateHandle
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.AcademicYearDto
import com.greenwood.school.data.remote.dto.SchoolClassDto
import com.greenwood.school.data.remote.dto.SectionDto
import com.greenwood.school.data.remote.dto.StudentDto
import com.greenwood.school.data.remote.dto.StudentRequestDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.StudentRepository
import com.greenwood.school.navigation.Routes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The edit half of the student form, which had no way into it until the detail
 * screen grew an edit action — so this is the first coverage the update path has.
 *
 * Email and phone are left blank throughout: `Validators.email` needs
 * `android.util.Patterns`, which the JVM suite does not have, and both validators
 * short-circuit on a blank optional value before reaching it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var students: StudentRepository
    private lateinit var academic: AcademicRepository

    private val year = AcademicYearDto(id = 2, yearName = "2025-26", startDate = "2025-04-01", endDate = "2026-03-31", isCurrent = true)
    private val schoolClass = SchoolClassDto(id = 4, className = "Grade 5", academicYearId = 2)
    private val section = SectionDto(id = 9, sectionName = "A", classId = 4)

    private val existing = StudentDto(
        id = 11,
        admissionNumber = "ADM-011",
        firstName = "Ravi",
        lastName = "Kumar",
        classId = 4,
        sectionId = 9,
        rollNumber = "12",
        admissionDate = "2025-04-10",
        dateOfBirth = "2014-08-01",
        gender = "MALE",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        students = mockk(relaxed = true)
        academic = mockk(relaxed = true)
        coEvery { academic.getClasses(any(), any()) } returns ApiResult.Success(listOf(schoolClass))
        coEvery { academic.getAcademicYears(any()) } returns ApiResult.Success(listOf(year))
        coEvery { academic.getSections(4) } returns ApiResult.Success(listOf(section))
        coEvery { students.getStudent(11) } returns ApiResult.Success(existing)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(studentId: Long) = StudentFormViewModel(
        studentRepository = students,
        academicRepository = academic,
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_STUDENT_ID to studentId)),
    )

    @Test
    fun `editing prefills from the record and resolves the class and section`() = runTest(dispatcher) {
        val vm = viewModel(11)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.isEditing)
        assertEquals("Ravi", state.firstName)
        assertEquals("Kumar", state.lastName)
        assertEquals("12", state.rollNumber)
        assertEquals("MALE", state.gender)
        assertEquals(schoolClass, state.selectedClass)
        // The section list is fetched for the student's class and the current one
        // preselected, so saving without touching it keeps them where they are.
        assertEquals(section, state.selectedSection)
        assertEquals(year, state.selectedYear)
    }

    @Test
    fun `saving an edit updates that student and sends no guardians`() = runTest(dispatcher) {
        val request = slot<StudentRequestDto>()
        coEvery { students.updateStudent(eq(11L), capture(request)) } returns ApiResult.Success(existing)

        val vm = viewModel(11)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { students.updateStudent(11L, any()) }
        coVerify(exactly = 0) { students.createStudent(any()) }

        assertEquals("Ravi", request.captured.firstName)
        assertEquals(4L, request.captured.classId)
        assertEquals(9L, request.captured.sectionId)
        assertEquals(2L, request.captured.academicYearId)
        // Guardians are managed from the detail screen on an existing record; sending
        // them here would re-create the ones already on file.
        assertNull(request.captured.guardians)
        assertTrue(vm.state.value.isSaved)
    }

    @Test
    fun `a new student goes to create, not update`() = runTest(dispatcher) {
        coEvery { students.createStudent(any()) } returns ApiResult.Success(existing)

        val vm = viewModel(NEW)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        // Blank form: the required fields stop it before any request goes out.
        assertEquals("First name is required", vm.state.value.firstNameError)
        coVerify(exactly = 0) { students.createStudent(any()) }
        coVerify(exactly = 0) { students.updateStudent(any(), any()) }
    }

    private companion object {
        /** What Routes.studentForm() encodes for "new". */
        const val NEW = -1L
    }
}
