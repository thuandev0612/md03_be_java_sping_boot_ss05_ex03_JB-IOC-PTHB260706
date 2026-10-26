package com.example.baitapsession05.dto;

import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseResponse {

    private final Long id;
    private final String title;
    private final CourseStatus status;
    private final Long instructorId;

    public static CourseResponse fromEntity(Course course) {
        return new CourseResponse(course.getId(), course.getTitle(), course.getStatus(),
                course.getInstructor().getId());
    }
}
