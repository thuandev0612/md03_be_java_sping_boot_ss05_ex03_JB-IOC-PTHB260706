package com.example.baitapsession05;

import com.example.baitapsession05.dto.InstructorCreateRequest;
import com.example.baitapsession05.exception.ResourceNotFoundException;
import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import com.example.baitapsession05.model.Instructor;
import com.example.baitapsession05.model.Student;
import com.example.baitapsession05.model.StudentEnrollment;
import com.example.baitapsession05.repository.CourseRepository;
import com.example.baitapsession05.repository.InstructorRepository;
import com.example.baitapsession05.repository.StudentEnrollmentRepository;
import com.example.baitapsession05.repository.StudentRepository;
import com.example.baitapsession05.service.InstructorService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@ActiveProfiles("test")
class RepositoryServiceTests {

    @Autowired
    private InstructorService instructorService;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void createsInstructorFromRequestAndReadsItBackFromPostgres() {
        InstructorCreateRequest request = new InstructorCreateRequest("Nguyen Van An", "an@example.com");

        Instructor created = instructorService.createInstructor(request);
        entityManager.flush();
        entityManager.clear();

        assertThat(created.getId()).isPositive();
        Instructor found = instructorService.findInstructorById(created.getId());
        assertThat(found.getName()).isEqualTo(request.getName());
        assertThat(found.getEmail()).isEqualTo(request.getEmail());
        assertThat(found.getCourses()).isEmpty();
    }

    @Test
    void listsInstructorsStoredInTheDatabase() {
        Instructor first = instructorRepository.save(new Instructor(null, "An", "an@example.com"));
        Instructor second = instructorRepository.save(new Instructor(null, "Binh", "binh@example.com"));
        entityManager.flush();
        entityManager.clear();

        assertThat(instructorService.findAllInstructors())
                .extracting(Instructor::getId)
                .contains(first.getId(), second.getId());
    }

    @Test
    void handlesMissingInstructorWithOptionalAndADomainException() {
        long missingId = Long.MIN_VALUE;
        assertThat(instructorRepository.findById(missingId)).isEmpty();

        assertThatThrownBy(() -> instructorService.findInstructorById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy giảng viên có id = " + missingId);
    }

    @Test
    void inheritedRepositoryMethodsPersistUpdateAndDeleteRelatedEntities() {
        Instructor instructor = instructorRepository.save(
                new Instructor(null, "An", "an@example.com"));
        Student student = studentRepository.save(
                new Student(null, "Binh", "binh@example.com"));
        Course course = courseRepository.save(
                new Course(null, "Java", CourseStatus.ACTIVE, instructor));
        StudentEnrollment enrollment = studentEnrollmentRepository.save(
                new StudentEnrollment(null, student, course));
        entityManager.flush();
        entityManager.clear();

        assertThat(studentRepository.findById(student.getId()).orElseThrow().getEmail())
                .isEqualTo("binh@example.com");
        StudentEnrollment savedEnrollment = studentEnrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(savedEnrollment.getStudent().getId()).isEqualTo(student.getId());
        assertThat(savedEnrollment.getCourse().getInstructor().getId()).isEqualTo(instructor.getId());

        // Update a detached entity through the inherited save() method.
        course.setTitle("Java Spring Boot");
        courseRepository.save(course);
        entityManager.flush();
        entityManager.clear();
        assertThat(courseRepository.findById(course.getId()).orElseThrow().getTitle())
                .isEqualTo("Java Spring Boot");

        // Remove only this test's records, starting with the foreign-key owners.
        studentEnrollmentRepository.deleteById(enrollment.getId());
        studentEnrollmentRepository.flush();
        courseRepository.deleteById(course.getId());
        courseRepository.flush();
        studentRepository.deleteById(student.getId());
        instructorRepository.deleteById(instructor.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(studentEnrollmentRepository.findById(enrollment.getId())).isEmpty();
        assertThat(courseRepository.findById(course.getId())).isEmpty();
        assertThat(studentRepository.findById(student.getId())).isEmpty();
        assertThat(instructorRepository.findById(instructor.getId())).isEmpty();
    }
}
