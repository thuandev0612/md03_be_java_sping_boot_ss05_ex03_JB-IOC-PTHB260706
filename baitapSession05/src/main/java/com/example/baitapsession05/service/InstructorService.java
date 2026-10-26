package com.example.baitapsession05.service;

import com.example.baitapsession05.dto.InstructorCreateRequest;
import com.example.baitapsession05.exception.ResourceNotFoundException;
import com.example.baitapsession05.model.Instructor;
import com.example.baitapsession05.repository.InstructorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InstructorService {

    private final InstructorRepository instructorRepository;

    @Autowired
    public InstructorService(InstructorRepository instructorRepository) {
        this.instructorRepository = instructorRepository;
    }

    public Instructor findInstructorById(long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy giảng viên có id = " + id));
    }

    public List<Instructor> findAllInstructors() {
        return instructorRepository.findAll();
    }

    @Transactional
    public Instructor createInstructor(InstructorCreateRequest req) {
        Instructor instructor = new Instructor();
        instructor.setName(req.getName());
        instructor.setEmail(req.getEmail());
        return instructorRepository.save(instructor);
    }
}
