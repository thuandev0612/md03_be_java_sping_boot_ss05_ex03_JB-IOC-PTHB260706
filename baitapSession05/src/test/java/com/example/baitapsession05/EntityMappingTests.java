package com.example.baitapsession05;

import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import com.example.baitapsession05.model.Instructor;
import com.example.baitapsession05.model.Student;
import com.example.baitapsession05.model.StudentEnrollment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// These integration tests use PostgreSQL and roll back their inserted records.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@ActiveProfiles("test")
class EntityMappingTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsEntitiesWithGeneratedIdsAndStringStatus() {
        Instructor instructor = new Instructor(null, "Nguyen Van An", "an@example.com");
        entityManager.persist(instructor);

        Course course = new Course(null, "Spring Boot", CourseStatus.ACTIVE, instructor);
        entityManager.persist(course);
        entityManager.flush();
        entityManager.clear();

        assertThat(instructor.getId()).isPositive();
        assertThat(course.getId()).isPositive();
        assertThat(entityManager.find(Instructor.class, instructor.getId()).getEmail())
                .isEqualTo("an@example.com");
        Course savedCourse = entityManager.find(Course.class, course.getId());
        assertThat(savedCourse.getTitle()).isEqualTo("Spring Boot");
        assertThat(savedCourse.getStatus()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(savedCourse.getInstructor().getId()).isEqualTo(instructor.getId());
        assertThat(jdbcTemplate.queryForObject(
                "select status from courses where id = ?", String.class, course.getId()))
                .isEqualTo("ACTIVE");
    }

    @Test
    void databaseHasFourIdentityColumnsAndNameLengthLimit() {
        assertThat(jdbcTemplate.queryForList("""
                select table_name from information_schema.columns
                where table_schema = 'public' and column_name = 'id'
                  and table_name in ('courses', 'instructors', 'students', 'student_enrollments')
                  and data_type = 'bigint' and is_identity = 'YES'
                """, String.class)).containsExactlyInAnyOrder(
                        "courses", "instructors", "students", "student_enrollments");

        assertThat(jdbcTemplate.queryForObject("""
                select character_maximum_length from information_schema.columns
                where table_schema = 'public' and table_name = 'instructors' and column_name = 'name'
                """, Integer.class)).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "insert into instructors (name, email) values (null, 'test@example.com')",
            "insert into instructors (name, email) values ('Test', null)",
            "insert into courses (title, status, instructor_id) values (null, 'ACTIVE', 1)",
            "insert into courses (title, status, instructor_id) values ('Test', null, 1)",
            "insert into courses (title, status, instructor_id) values ('Test', 'ACTIVE', null)"
    })
    void databaseRejectsMissingRequiredValues(String sql) {
        assertThatThrownBy(() -> jdbcTemplate.update(sql))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOf(SQLException.class)
                .extracting(error -> ((SQLException) error).getSQLState()).isEqualTo("23502");
    }

    @Test
    void databaseRejectsInstructorNameLongerThan100Characters() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into instructors (name, email) values (?, ?)",
                "A".repeat(101), "test@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOf(SQLException.class)
                .extracting(error -> ((SQLException) error).getSQLState()).isEqualTo("22001");
    }

    @Test
    void databaseRejectsStatusOutsideTheEnum() {
        Instructor instructor = new Instructor(null, "Test", "test@example.com");
        entityManager.persist(instructor);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into courses (title, status, instructor_id) values (?, ?, ?)",
                "Test", "UNKNOWN", instructor.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOf(SQLException.class)
                .extracting(error -> ((SQLException) error).getSQLState()).isEqualTo("23514");
    }

    @Test
    void loadsInstructorCoursesAndStudentEnrollmentsInBothDirections() {
        Instructor instructor = new Instructor(null, "Nguyen Van An", "an@example.com");
        Student firstStudent = new Student(null, "Tran Thi Binh", "binh@example.com");
        Student secondStudent = new Student(null, "Le Van Cuong", "cuong@example.com");
        entityManager.persist(instructor);
        entityManager.persist(firstStudent);
        entityManager.persist(secondStudent);

        Course javaCourse = new Course(null, "Java", CourseStatus.ACTIVE, instructor);
        Course springCourse = new Course(null, "Spring", CourseStatus.ACTIVE, instructor);
        entityManager.persist(javaCourse);
        entityManager.persist(springCourse);

        StudentEnrollment firstEnrollment = new StudentEnrollment(null, firstStudent, javaCourse);
        entityManager.persist(firstEnrollment);
        entityManager.persist(new StudentEnrollment(null, firstStudent, springCourse));
        entityManager.persist(new StudentEnrollment(null, secondStudent, javaCourse));
        entityManager.flush();
        entityManager.clear();

        assertThat(firstStudent.getId()).isPositive();
        assertThat(firstEnrollment.getId()).isPositive();
        assertThat(entityManager.find(Instructor.class, instructor.getId()).getCourses())
                .extracting(Course::getId)
                .containsExactlyInAnyOrder(javaCourse.getId(), springCourse.getId());

        Student savedStudent = entityManager.find(Student.class, firstStudent.getId());
        assertThat(savedStudent.getEmail()).isEqualTo("binh@example.com");
        assertThat(savedStudent.getEnrollments())
                .extracting(enrollment -> enrollment.getCourse().getId())
                .containsExactlyInAnyOrder(javaCourse.getId(), springCourse.getId());
        assertThat(entityManager.find(Course.class, javaCourse.getId()).getEnrollments())
                .extracting(enrollment -> enrollment.getStudent().getId())
                .containsExactlyInAnyOrder(firstStudent.getId(), secondStudent.getId());

        StudentEnrollment savedEnrollment = entityManager.find(StudentEnrollment.class, firstEnrollment.getId());
        assertThat(savedEnrollment.getStudent().getId()).isEqualTo(firstStudent.getId());
        assertThat(savedEnrollment.getCourse().getId()).isEqualTo(javaCourse.getId());
    }

    @Test
    void databaseHasExactlyTheThreeForeignKeysWithSnakeCaseNames() {
        assertThat(jdbcTemplate.queryForList("""
                select source.relname || '.' || col.attname || ' -> ' || target.relname
                from pg_constraint fk
                join pg_class source on source.oid = fk.conrelid
                join pg_namespace ns on ns.oid = source.relnamespace
                join pg_class target on target.oid = fk.confrelid
                join pg_attribute col on col.attrelid = source.oid and col.attnum = any(fk.conkey)
                where fk.contype = 'f' and ns.nspname = 'public'
                  and source.relname in ('courses', 'student_enrollments')
                """, String.class)).containsExactlyInAnyOrder(
                        "courses.instructor_id -> instructors",
                        "student_enrollments.student_id -> students",
                        "student_enrollments.course_id -> courses");
    }

    @ParameterizedTest
    @ValueSource(strings = {"instructor", "student", "course"})
    void databaseRejectsReferencesToMissingEntities(String reference) {
        Instructor instructor = new Instructor(null, "Test", "instructor@example.com");
        Student student = new Student(null, "Test", "student@example.com");
        entityManager.persist(instructor);
        entityManager.persist(student);
        Course course = new Course(null, "Test", CourseStatus.ACTIVE, instructor);
        entityManager.persist(course);
        entityManager.flush();

        // Identity-generated records use positive IDs; this negative ID does not exist.
        assertThatThrownBy(() -> {
            switch (reference) {
                case "instructor" -> jdbcTemplate.update(
                        "insert into courses (title, status, instructor_id) values ('Test', 'ACTIVE', ?)",
                        Long.MIN_VALUE);
                case "student" -> jdbcTemplate.update(
                        "insert into student_enrollments (student_id, course_id) values (?, ?)",
                        Long.MIN_VALUE, course.getId());
                case "course" -> jdbcTemplate.update(
                        "insert into student_enrollments (student_id, course_id) values (?, ?)",
                        student.getId(), Long.MIN_VALUE);
                default -> throw new IllegalArgumentException(reference);
            }
        }).isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOf(SQLException.class)
                .extracting(error -> ((SQLException) error).getSQLState()).isEqualTo("23503");
    }
}
