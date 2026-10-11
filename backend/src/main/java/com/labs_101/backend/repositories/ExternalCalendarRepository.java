package com.labs_101.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.ExternalCalendar;

public interface ExternalCalendarRepository extends JpaRepository<ExternalCalendar, Long> {
    List<ExternalCalendar> findByUser_IdOrderByCreatedAt(String userId);

    Optional<ExternalCalendar> findByIdAndUser_Id(Long id, String userId);
}
