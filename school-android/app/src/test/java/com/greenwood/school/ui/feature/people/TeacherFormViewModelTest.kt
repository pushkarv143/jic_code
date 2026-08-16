package com.greenwood.school.ui.feature.people

import androidx.lifecycle.SavedStateHandle
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.DepartmentDto
import com.greenwood.school.data.remote.dto.DesignationDto
import com.greenwood.school.data.remote.dto.TeacherDto
import com.greenwood.school.domain.repository.AcademicRepository
import com.greenwood.school.domain.repository.PeopleRepository
import com.greenwood.school.navigation.Routes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * `Validators.email` leans on `android.util.Patterns`, which is not available to the
 * JVM suite, so every case here keeps the email field blank — `email()` returns its
 * "required" message before touching Patterns. The populated-email path belongs to
 * the instrumented suite, matching the split ValidatorsTest already documents.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TeacherFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var people: PeopleRepository
    private lateinit var academic: AcademicRepository

    private val departments = listOf(DepartmentDto(id = 3, name = "Science"))
    private val designations = listOf(DesignationDto(id = 5, name = "Senior Teacher"))

    private val existing = TeacherDto(
        id = 7,
        departmentId = 3,
        designationId = 5,
        username = "teacher.demo",
        firstName = "Asha",
        lastName = "Rao",
        gender = "FEMALE",
        employmentType = "FULL_TIME",
        salary = 45000.0,
        experienceYears = 6,
        joiningDate = "2021-06-01",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        people = mockk(relaxed = true)
        academic = mockk(relaxed = true)
        coEvery { academic.getDepartments(any()) } returns ApiResult.Success(departments)
        coEvery { academic.getDesignations(any()) } returns ApiResult.Success(designations)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(teacherId: Long) = TeacherFormViewModel(
        peopleRepository = people,
        academicRepository = academic,
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_TEACHER_ID to teacherId)),
    )

    @Test
    fun `creating with an empty form is rejected before any request goes out`() = runTest(dispatcher) {
        val vm = viewModel(NEW)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Username is required", state.usernameError)
        assertEquals("Password is required", state.passwordError)
        assertEquals("First name is required", state.firstNameError)
        assertEquals("Email is required", state.emailError)
        assertEquals("Select a department", state.departmentError)
        assertEquals("Select a designation", state.designationError)

        coVerify(exactly = 0) { people.createTeacher(any()) }
    }

    @Test
    fun `editing prefills the form from the loaded record`() = runTest(dispatcher) {
        coEvery { people.getTeacher(7) } returns ApiResult.Success(existing)

        val vm = viewModel(7)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(true, state.isEditing)
        assertEquals(false, state.isLoading)
        assertEquals("teacher.demo", state.username)
        assertEquals("Asha", state.firstName)
        assertEquals("Rao", state.lastName)
        assertEquals("FEMALE", state.gender)
        assertEquals("FULL_TIME", state.employmentType)
        assertEquals("6", state.experienceYears)
        // A whole-number salary must come back as plain digits, not "45000.0",
        // or re-saving would round-trip a value the user never typed.
        assertEquals("45000", state.salary)
        assertEquals(departments.first(), state.selectedDepartment)
        assertEquals(designations.first(), state.selectedDesignation)
    }

    @Test
    fun `editing does not demand the account fields that only exist on create`() = runTest(dispatcher) {
        coEvery { people.getTeacher(7) } returns ApiResult.Success(existing)

        val vm = viewModel(7)
        advanceUntilIdle()
        // Blank them out: PUT /teachers/{id} carries no username or password, so the
        // form must not block on either.
        vm.update { copy(username = "", password = "", email = "") }

        vm.save()
        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.usernameError)
        assertNull(state.passwordError)
        // Email is still genuinely required, so the save is stopped for that reason.
        assertNotNull(state.emailError)
        coVerify(exactly = 0) { people.updateTeacher(any(), any()) }
    }

    private companion object {
        /** What Routes.teacherForm() encodes for "new". */
        const val NEW = -1L
    }
}
