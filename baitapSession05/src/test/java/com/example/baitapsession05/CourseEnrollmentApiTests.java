package com.example.baitapsession05;

import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import com.example.baitapsession05.model.Instructor;
import com.example.baitapsession05.model.Student;
import com.example.baitapsession05.model.StudentEnrollment;
import com.example.baitapsession05.repository.CourseRepository;
import com.example.baitapsession05.repository.InstructorRepository;
import com.example.baitapsession05.repository.StudentEnrollmentRepository;
import com.example.baitapsession05.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CourseEnrollmentApiTests {

    private static final long MISSING_ID = Long.MAX_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void completesTheFiveRequestEnrollmentFlowWithoutSerializingEntities() throws Exception {
        long instructorId = createViaApi("/instructors", """
                {"name":"Nguyen Van An","email":"an@example.com"}
                """);
        long courseId = createViaApi("/courses", courseJson("Java", "INACTIVE", instructorId));

        expectResponse(performJson(put("/courses/{id}", courseId),
                courseJson("Java Spring Boot", "ACTIVE", instructorId)), 200, true);

        long studentId = createViaApi("/students", """
                {"name":"Tran Thi Binh","email":"binh@example.com"}
                """);
        long enrollmentId = createViaApi("/students-enrollments", enrollmentJson(studentId, courseId));
        entityManager.flush();
        entityManager.clear();

        Instructor instructor = instructorRepository.findById(instructorId).orElseThrow();
        Course course = courseRepository.findById(courseId).orElseThrow();
        Student student = studentRepository.findById(studentId).orElseThrow();
        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();
        assertThat(instructor.getName()).isEqualTo("Nguyen Van An");
        assertThat(instructor.getEmail()).isEqualTo("an@example.com");
        assertThat(course.getTitle()).isEqualTo("Java Spring Boot");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(course.getInstructor().getId()).isEqualTo(instructorId);
        assertThat(student.getName()).isEqualTo("Tran Thi Binh");
        assertThat(student.getEmail()).isEqualTo("binh@example.com");
        assertThat(enrollment.getStudent().getId()).isEqualTo(studentId);
        assertThat(enrollment.getCourse().getId()).isEqualTo(courseId);
        assertThat(instructor.getCourses()).extracting(Course::getId).contains(courseId);
        assertThat(course.getEnrollments()).extracting(StudentEnrollment::getId).contains(enrollmentId);
        assertThat(student.getEnrollments()).extracting(StudentEnrollment::getId).contains(enrollmentId);
    }

    @Test
    void updatesCourseAndReassignsItsInstructorInPostgres() throws Exception {
        Course course = saveCourse();
        long oldInstructorId = course.getInstructor().getId();
        Instructor newInstructor = saveInstructor("New instructor");

        expectResponse(performJson(put("/courses/{id}", course.getId()),
                courseJson("Updated course", "ACTIVE", newInstructor.getId())), 200, true);
        entityManager.flush();
        entityManager.clear();

        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated course");
        assertThat(updated.getStatus()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(updated.getInstructor().getId()).isEqualTo(newInstructor.getId());
        assertThat(instructorRepository.findById(oldInstructorId).orElseThrow().getCourses())
                .extracting(Course::getId).doesNotContain(course.getId());
    }

    @Test
    void rejectsCourseCreationWhenInstructorDoesNotExist() throws Exception {
        long count = courseRepository.count();

        expectResponse(performJson(post("/courses"),
                courseJson("Java", "INACTIVE", MISSING_ID)), 404, false);

        assertThat(courseRepository.count()).isEqualTo(count);
    }

    @Test
    void rejectsUpdatingMissingCourseWithoutCreatingOne() throws Exception {
        Instructor instructor = saveInstructor("Instructor");
        long count = courseRepository.count();

        expectResponse(performJson(put("/courses/{id}", MISSING_ID),
                courseJson("Java", "ACTIVE", instructor.getId())), 404, false);

        assertThat(courseRepository.count()).isEqualTo(count);
    }

    @Test
    void rejectsUnknownInstructorDuringUpdateWithoutMutatingCourse() throws Exception {
        Course course = saveCourse();
        long originalInstructorId = course.getInstructor().getId();

        expectResponse(performJson(put("/courses/{id}", course.getId()),
                courseJson("Should not be saved", "ACTIVE", MISSING_ID)), 404, false);
        entityManager.flush();
        entityManager.clear();

        Course unchanged = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Original course");
        assertThat(unchanged.getStatus()).isEqualTo(CourseStatus.INACTIVE);
        assertThat(unchanged.getInstructor().getId()).isEqualTo(originalInstructorId);
    }

    @Test
    void rejectsEnrollmentForMissingStudentWithoutWritingARow() throws Exception {
        Course course = saveCourse();
        long count = enrollmentRepository.count();

        expectResponse(performJson(post("/students-enrollments"),
                enrollmentJson(MISSING_ID, course.getId())), 404, false);

        assertThat(enrollmentRepository.count()).isEqualTo(count);
    }

    @Test
    void rejectsEnrollmentForMissingCourseWithoutWritingARow() throws Exception {
        Student student = studentRepository.save(new Student(null, "Student", "student@example.com"));
        long count = enrollmentRepository.count();

        expectResponse(performJson(post("/students-enrollments"),
                enrollmentJson(student.getId(), MISSING_ID)), 404, false);

        assertThat(enrollmentRepository.count()).isEqualTo(count);
    }

    @Test
    void validatesRequiredFieldsOnEveryEndpoint() throws Exception {
        for (String path : new String[]{"/instructors", "/courses", "/students", "/students-enrollments"}) {
            expectResponse(performJson(post(path), "{}"), 400, false);
        }
        expectResponse(performJson(put("/courses/1"), "{}"), 400, false);
    }

    @Test
    void rejectsBlankNamesAndTitles() throws Exception {
        for (String path : new String[]{"/instructors", "/students"}) {
            expectResponse(performJson(post(path),
                    "{\"name\":\"   \",\"email\":\"person@example.com\"}"), 400, false);
        }
        expectResponse(performJson(post("/courses"), courseJson("   ", "ACTIVE", 1L)), 400, false);
    }

    @Test
    void rejectsMalformedEmailsBeforePersistingPeople() throws Exception {
        long instructorCount = instructorRepository.count();
        long studentCount = studentRepository.count();

        for (String path : new String[]{"/instructors", "/students"}) {
            expectResponse(performJson(post(path),
                    "{\"name\":\"Test person\",\"email\":\"not-an-email\"}"), 400, false);
        }

        assertThat(instructorRepository.count()).isEqualTo(instructorCount);
        assertThat(studentRepository.count()).isEqualTo(studentCount);
    }

    @Test
    void rejectsNullAndNonpositiveForeignKeyIds() throws Exception {
        for (String invalidId : new String[]{"null", "0", "-1"}) {
            expectResponse(performJson(post("/courses"), """
                    {"title":"Java","status":"ACTIVE","instructorId":%s}
                    """.formatted(invalidId)), 400, false);
            expectResponse(performJson(post("/students-enrollments"), """
                    {"studentId":%s,"courseId":1}
                    """.formatted(invalidId)), 400, false);
            expectResponse(performJson(post("/students-enrollments"), """
                    {"studentId":1,"courseId":%s}
                    """.formatted(invalidId)), 400, false);
        }
    }

    @Test
    void wrapsInvalidEnumErrorsForCourseCreationAndUpdate() throws Exception {
        String invalidCourse = courseJson("Java", "NOT_A_STATUS", 1L);

        expectResponse(performJson(post("/courses"), invalidCourse), 400, false);
        expectResponse(performJson(put("/courses/1"), invalidCourse), 400, false);
    }

    @Test
    void wrapsMalformedJsonAndMissingRequestBodyErrors() throws Exception {
        expectResponse(performJson(post("/instructors"), "{\"name\":"), 400, false);
        expectResponse(mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON)), 400, false);
    }

    @Test
    void wrapsInvalidCoursePathIdsAsBadRequests() throws Exception {
        for (String invalidId : new String[]{"0", "-1", "abc"}) {
            expectResponse(performJson(put("/courses/{id}", invalidId),
                    courseJson("Java", "ACTIVE", 1L)), 400, false);
        }
    }

    private Instructor saveInstructor(String name) {
        return instructorRepository.save(new Instructor(null, name, "instructor@example.com"));
    }

    private Course saveCourse() {
        return courseRepository.save(new Course(null, "Original course", CourseStatus.INACTIVE,
                saveInstructor("Original instructor")));
    }

    private String courseJson(String title, String courseStatus, long instructorId) {
        return """
                {"title":"%s","status":"%s","instructorId":%d}
                """.formatted(title, courseStatus, instructorId);
    }

    private String enrollmentJson(long studentId, long courseId) {
        return """
                {"studentId":%d,"courseId":%d}
                """.formatted(studentId, courseId);
    }

    private ResultActions performJson(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createViaApi(String path, String json) throws Exception {
        String location = expectResponse(performJson(post(path), json), 201, true)
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith(path + "/");
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        assertThat(id).isPositive();
        return id;
    }

    private ResultActions expectResponse(ResultActions result, int expectedStatus, boolean success) throws Exception {
        return result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(success))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$").value(hasKey("data")))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
