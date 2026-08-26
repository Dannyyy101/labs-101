package com.labs_101.backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.labs_101.backend.entities.CalendarEvent;

public interface CalendarRepository extends MongoRepository<CalendarEvent, String> {
}
