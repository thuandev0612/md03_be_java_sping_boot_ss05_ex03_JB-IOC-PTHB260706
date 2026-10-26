package com.example.baitapsession05.controller;

import com.example.baitapsession05.dto.InstructorCreateRequest;
import com.example.baitapsession05.model.Instructor;
import com.example.baitapsession05.response.ApiResponse;
import com.example.baitapsession05.service.InstructorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/instructors")
public class InstructorController {

    private final InstructorService instructorService;

    @Autowired
    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createInstructor(@Valid @RequestBody InstructorCreateRequest req) {
        Instructor instructor = instructorService.createInstructor(req);
        return ResponseEntity.created(URI.create("/instructors/" + instructor.getId()))
                .body(ApiResponse.success("Tạo giảng viên thành công"));
    }
}
