package com.example.baitapsession05.controller;

import com.example.baitapsession05.dto.StudentCreateRequest;
import com.example.baitapsession05.model.Student;
import com.example.baitapsession05.response.ApiResponse;
import com.example.baitapsession05.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createStudent(@Valid @RequestBody StudentCreateRequest req) {
        Student student = studentService.createStudent(req);
        return ResponseEntity.created(URI.create("/students/" + student.getId()))
                .body(ApiResponse.success("Tạo sinh viên thành công"));
    }
}
