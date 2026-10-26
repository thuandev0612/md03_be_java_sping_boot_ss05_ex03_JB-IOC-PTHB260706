package com.example.baitapsession05.service;

import com.example.baitapsession05.dto.CourseCreateRequest;
import com.example.baitapsession05.dto.CourseUpdateRequest;
import com.example.baitapsession05.dto.CourseResponse;
import com.example.baitapsession05.exception.BadRequestException;
import com.example.baitapsession05.exception.ResourceNotFoundException;
import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import com.example.baitapsession05.model.Instructor;
import com.example.baitapsession05.repository.CourseRepository;
import com.example.baitapsession05.repository.InstructorRepository;
import com.example.baitapsession05.response.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CourseService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "title", "title",
            "status", "status",
            "instructorId", "instructor.id");

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;

    @Autowired
    public CourseService(CourseRepository courseRepository,
                         InstructorRepository instructorRepository) {
        this.courseRepository = courseRepository;
        this.instructorRepository = instructorRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getPagedCourses(int page, int size, String sortBy, Sort.Direction direction) {
        Page<Course> courses = courseRepository.findAll(createPageable(page, size, sortBy, direction));
        return toPageResponse(courses);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getPagedCoursesByStatus(
            int page, int size, String sortBy, Sort.Direction direction, CourseStatus status) {
        CourseStatus safeStatus = status == null ? CourseStatus.ACTIVE : status;
        Page<Course> courses = courseRepository.findAllByStatus(
                safeStatus, createPageable(page, size, sortBy, direction));
        return toPageResponse(courses);
    }

    private Pageable createPageable(int page, int size, String sortBy, Sort.Direction direction) {
        int safePage = Math.max(page, 0);
        if (size <= 0) {
            throw new BadRequestException("size phải lớn hơn 0");
        }
        if ((long) safePage * size > Integer.MAX_VALUE) {
            throw new BadRequestException("page và size tạo ra vị trí bắt đầu quá lớn");
        }

        String requestedField = sortBy == null || sortBy.isBlank() ? "id" : sortBy.trim();
        String sortField = SORT_FIELDS.get(requestedField);
        if (sortField == null) {
            throw new BadRequestException("sortBy hợp lệ: id, title, status, instructorId");
        }
        Sort.Direction safeDirection = direction == null ? Sort.Direction.DESC : direction;
        Sort sort = Sort.by(safeDirection, sortField);
        // Keep page boundaries deterministic when several courses have the same sort value.
        if (!sortField.equals("id")) {
            sort = sort.and(Sort.by(safeDirection, "id"));
        }
        return PageRequest.of(safePage, size, sort);
    }

    private PageResponse<CourseResponse> toPageResponse(Page<Course> courses) {
        Page<CourseResponse> mapped = courses.map(CourseResponse::fromEntity);
        return new PageResponse<>(mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                Math.toIntExact(mapped.getTotalElements()), mapped.getTotalPages(), mapped.isLast());
    }

    @Transactional
    public Course createCourse(CourseCreateRequest req) {
        Instructor instructor = instructorRepository.findById(req.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy giảng viên có id = " + req.getInstructorId()));

        Course course = new Course();
        course.setTitle(req.getTitle());
        course.setStatus(req.getStatus());
        course.setInstructor(instructor);
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, CourseUpdateRequest req) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khóa học có id = " + id));
        Instructor instructor = instructorRepository.findById(req.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy giảng viên có id = " + req.getInstructorId()));

        course.setTitle(req.getTitle());
        course.setStatus(req.getStatus());
        course.setInstructor(instructor);
        return courseRepository.save(course);
    }
}
