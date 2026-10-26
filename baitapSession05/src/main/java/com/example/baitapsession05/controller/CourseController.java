package com.example.baitapsession05.controller;

import com.example.baitapsession05.dto.CourseCreateRequest;
import com.example.baitapsession05.dto.CourseUpdateRequest;
import com.example.baitapsession05.dto.CourseResponse;
import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import com.example.baitapsession05.response.ApiResponse;
import com.example.baitapsession05.response.PageResponse;
import com.example.baitapsession05.service.CourseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    @Autowired
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(defaultValue = "ACTIVE") CourseStatus status) {
        PageResponse<CourseResponse> courses = courseService.getPagedCoursesByStatus(page, size, sortBy, direction, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách khóa học thành công", courses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCourse(@Valid @RequestBody CourseCreateRequest req) {
        Course course = courseService.createCourse(req);
        return ResponseEntity.created(URI.create("/courses/" + course.getId()))
                .body(ApiResponse.success("Tạo khóa học thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCourse(
            @PathVariable @Positive Long id, @Valid @RequestBody CourseUpdateRequest req) {
        courseService.updateCourse(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khóa học thành công"));
    }
}
