package com.example.baitapsession05.repository;

import com.example.baitapsession05.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
