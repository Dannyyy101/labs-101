package com.labs_101.backend.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.labs_101.backend.entities.Cheer;

@RepositoryRestResource(exported = false)
public interface CheerRepository extends JpaRepository<Cheer, Long> {

    boolean existsBySender_IdAndSession_Id(String senderId, Long sessionId);

    /** Those of the sessions the sender already cheered on. */
    @Query("select c.session.id from Cheer c where c.sender.id = :senderId and c.session.id in :sessionIds")
    List<Long> cheeredSessionIds(@Param("senderId") String senderId, @Param("sessionIds") Collection<Long> sessionIds);
}
