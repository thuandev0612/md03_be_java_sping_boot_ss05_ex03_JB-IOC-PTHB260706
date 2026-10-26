package com.example.baitapsession05.dto;

import com.example.baitapsession05.model.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseUpdateRequest {

    @NotBlank(message = "Tên khóa học không được để trống")
    @Size(max = 255, message = "Tên khóa học không được vượt quá 255 ký tự")
    private String title;

    @NotNull(message = "Trạng thái khóa học không được để trống")
    private CourseStatus status;

    @NotNull(message = "ID giảng viên không được để trống")
    @Positive(message = "ID giảng viên phải lớn hơn 0")
    private Long instructorId;
}
