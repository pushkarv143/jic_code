package com.school.sms.controller;

import com.school.sms.dto.request.ClassOfficialRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.service.ClassOfficialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes/{classId}/officials")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Class Officials", description = "Head boy, head girl and other posts held in a class")
public class ClassOfficialController {

    private final ClassOfficialService classOfficialService;

    // Same read set as the rest of the class module: who holds a post is roster
    // information, not something only management needs.
    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL','TEACHER','RECEPTIONIST','ACCOUNTANT')";
    // Appointing is a management act; a subject teacher does not get to name a head boy.
    private static final String WRITE_ROLES = "hasAnyRole('SUPER_ADMIN','PRINCIPAL','VICE_PRINCIPAL')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Current holders of every post in this class")
    public ResponseEntity<ApiResponse<List<ClassOfficialDto>>> getCurrent(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Class officials retrieved successfully",
                classOfficialService.getCurrent(classId)));
    }

    @GetMapping("/history")
    @PreAuthorize(READ_ROLES)
    @Operation(summary = "Every holder this class has had, newest appointment first")
    public ResponseEntity<ApiResponse<List<ClassOfficialDto>>> getHistory(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Class official history retrieved successfully",
                classOfficialService.getHistory(classId)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "Appoint a student to a post, ending the sitting holder's tenure if there is one")
    public ResponseEntity<ApiResponse<ClassOfficialDto>> appoint(@PathVariable Long classId,
                                                                 @Valid @RequestBody ClassOfficialRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class official appointed successfully",
                classOfficialService.appoint(classId, request)));
    }

    @DeleteMapping("/{officialId}")
    @PreAuthorize(WRITE_ROLES)
    @Operation(summary = "End an appointment, leaving the post vacant (the record is kept, not deleted)")
    public ResponseEntity<Void> end(@PathVariable Long classId, @PathVariable Long officialId) {
        classOfficialService.end(classId, officialId);
        return ResponseEntity.noContent().build();
    }
}
