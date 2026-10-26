package com.example.baitapsession05.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentRequest {

    @NotNull(message = "ID sinh viên không được để trống")
    @Positive(message = "ID sinh viên phải lớn hơn 0")
    private Long studentId;

    @NotNull(message = "ID khóa học không được để trống")
    @Positive(message = "ID khóa học phải lớn hơn 0")
    private Long courseId;
}
