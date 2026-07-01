package com.learnhub.catalog.course.repository;

import com.learnhub.catalog.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findByIdAndInstructorId(UUID id, UUID instructorId);
    List<Course> findByInstructorIdOrderByUpdatedAtDesc(UUID instructorId);
    List<Course> findByStatusOrderByCreatedAtDesc(Course.CourseStatus status);
    @Query("SELECT c FROM Course c WHERE c.status = :status")
    List<Course> findAllByStatus(@Param("status") Course.CourseStatus status);
}
