package com.example.baitapsession05;

import com.example.baitapsession05.dto.CourseResponse;
import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import com.example.baitapsession05.model.Instructor;
import com.example.baitapsession05.repository.CourseRepository;
import com.example.baitapsession05.repository.InstructorRepository;
import com.example.baitapsession05.service.CourseService;
import com.example.baitapsession05.response.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CoursePaginationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InstructorRepository instructorRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseService courseService;
    @PersistenceContext
    private EntityManager entityManager;

    private Instructor instructor;
    private final List<Course> courses = new ArrayList<>();

    @BeforeEach
    void prepareCoursesInTheDedicatedTestDatabase() {
        instructor = instructorRepository.save(new Instructor(null, "Pagination Instructor", "pages@example.com"));
        for (String title : List.of("Kotlin", "Alpha", "Spring", "Beta", "Zeta", "Delta",
                "Java", "Lambda", "Python", "Omega", "Gamma", "Epsilon")) {
            courses.add(courseRepository.save(new Course(null, title, CourseStatus.ACTIVE, instructor)));
        }
        for (String title : List.of("Inactive Gamma", "Inactive Alpha", "Inactive Beta")) {
            courseRepository.save(new Course(null, title, CourseStatus.INACTIVE, instructor));
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void usesPageZeroSizeTenAndIdDescendingByDefault() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalItems").value(12))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items", hasSize(10)))
                .andExpect(jsonPath("$.data.items[0].id").value(courses.get(11).getId()));
    }

    @Test
    void sortsByTitleAndMapsOnlyResponseFields() throws Exception {
        titlePage(0, "ASC")
                .andExpect(jsonPath("$.data", aMapWithSize(6)))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.pageable").doesNotExist())
                .andExpect(jsonPath("$.data.sort").doesNotExist())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].title").value("Alpha"))
                .andExpect(jsonPath("$.data.items[1].title").value("Beta"))
                .andExpect(jsonPath("$.data.items[0].instructorId").value(instructor.getId()))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].instructor").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].enrollments").doesNotExist())
                .andExpect(jsonPath("$.data.totalPages").value(6));
    }

    @Test
    void retrievesTheNextPageAndDescendingTitles() throws Exception {
        titlePage(1, "ASC")
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Delta"))
                .andExpect(jsonPath("$.data.items[1].title").value("Epsilon"));
        titlePage(0, "DESC")
                .andExpect(jsonPath("$.data.items[0].title").value("Zeta"))
                .andExpect(jsonPath("$.data.items[1].title").value("Spring"));
    }

    @Test
    void adjustsNegativePagesToZeroWithoutThrowing() throws Exception {
        titlePage(-5, "ASC")
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.items[0].title").value("Alpha"));
    }

    @Test
    void treatsBlankSortByAsIdAndHonorsDirection() throws Exception {
        mockMvc.perform(get("/courses").param("size", "2").param("sortBy", "  ").param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(courses.get(0).getId()))
                .andExpect(jsonPath("$.data.items[1].id").value(courses.get(1).getId()));
    }

    @Test
    void returnsAnEmptyPageBeyondTheLastPage() throws Exception {
        mockMvc.perform(get("/courses").param("page", "99").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(99))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(12));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonpositivePageSizesWithWrapped400(int size) throws Exception {
        expectBadRequest(mockMvc.perform(get("/courses").param("size", String.valueOf(size))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "instructor", "enrollments", "instructor.email"})
    void rejectsUnsupportedSortPropertiesWithWrapped400(String sortBy) throws Exception {
        expectBadRequest(mockMvc.perform(get("/courses").param("sortBy", sortBy)));
    }

    @ParameterizedTest
    @CsvSource({"page,abc", "size,abc", "direction,SIDEWAYS", "page,2147483648", "size,2147483648"})
    void handlesParameterConversionErrors(String parameter, String value) throws Exception {
        expectBadRequest(mockMvc.perform(get("/courses").param(parameter, value)));
    }

    @Test
    void rejectsOffsetsThatJpaCannotRepresent() throws Exception {
        expectBadRequest(mockMvc.perform(get("/courses").param("page", "2147483647").param("size", "2")));
    }

    @Test
    void sortsInstructorIdThroughTheObjectRelationship() throws Exception {
        Instructor second = instructorRepository.save(new Instructor(null, "Second", "second@example.com"));
        Course secondCourse = courseRepository.save(new Course(null, "Second course", CourseStatus.INACTIVE, second));
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/courses").param("sortBy", "instructorId").param("size", "1")
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(secondCourse.getId()))
                .andExpect(jsonPath("$.data.items[0].instructorId").value(second.getId()));
    }

    @Test
    void breaksTitleTiesByIdToKeepPageBoundariesStable() {
        Instructor reference = entityManager.getReference(Instructor.class, instructor.getId());
        Course duplicate = courseRepository.save(new Course(null, "Alpha", CourseStatus.ACTIVE, reference));
        Course another = courseRepository.save(new Course(null, "Alpha", CourseStatus.ACTIVE, reference));
        entityManager.flush();
        entityManager.clear();

        PageResponse<CourseResponse> first = courseService.getPagedCourses(0, 2, "title", Sort.Direction.ASC);
        PageResponse<CourseResponse> next = courseService.getPagedCourses(1, 2, "title", Sort.Direction.ASC);
        assertThat(first.getItems()).extracting(CourseResponse::getId)
                .containsExactly(courses.get(1).getId(), duplicate.getId());
        assertThat(next.getItems().get(0).getId()).isEqualTo(another.getId());
    }

    @Test
    void filtersInactiveCoursesBeforePagingAndCountsOnlyMatchedCourses() throws Exception {
        mockMvc.perform(get("/courses").param("status", "INACTIVE").param("size", "2")
                        .param("sortBy", "title").param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].title").value("Inactive Alpha"))
                .andExpect(jsonPath("$.data.items[1].title").value("Inactive Beta"))
                .andExpect(jsonPath("$.data.items[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.items[1].status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(false));

        mockMvc.perform(get("/courses").param("status", "INACTIVE").param("page", "1").param("size", "2")
                        .param("sortBy", "title").param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].title").value("Inactive Gamma"))
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void returnsAnEmptyWrapperWhenTheStatusHasNoMatches() throws Exception {
        mockMvc.perform(get("/courses").param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void normalizesNegativePagesAndSortsInactiveCoursesDescending() throws Exception {
        mockMvc.perform(get("/courses").param("status", "INACTIVE").param("page", "-5").param("size", "2")
                        .param("sortBy", "title").param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.items[0].title").value("Inactive Gamma"))
                .andExpect(jsonPath("$.data.items[1].title").value("Inactive Beta"));
    }

    @Test
    void rejectsInvalidStatusWithWrapped400() throws Exception {
        expectBadRequest(mockMvc.perform(get("/courses").param("status", "UNKNOWN")));
    }

    private ResultActions titlePage(int page, String direction) throws Exception {
        return mockMvc.perform(get("/courses").param("page", String.valueOf(page)).param("size", "2")
                        .param("sortBy", "title").param("direction", direction))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    private void expectBadRequest(ResultActions response) throws Exception {
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
