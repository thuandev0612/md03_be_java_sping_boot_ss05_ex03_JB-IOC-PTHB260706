package com.example.baitapsession05.service;

import com.example.baitapsession05.exception.ResourceNotFoundException;
import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.Student;
import com.example.baitapsession05.model.StudentEnrollment;
import com.example.baitapsession05.repository.CourseRepository;
import com.example.baitapsession05.repository.StudentEnrollmentRepository;
import com.example.baitapsession05.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentEnrollmentService {

    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public StudentEnrollmentService(StudentEnrollmentRepository studentEnrollmentRepository,
                                    StudentRepository studentRepository,
                                    CourseRepository courseRepository) {
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public StudentEnrollment enrollStudent(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sinh viên có id = " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khóa học có id = " + courseId));

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        return studentEnrollmentRepository.save(enrollment);
    }
}
