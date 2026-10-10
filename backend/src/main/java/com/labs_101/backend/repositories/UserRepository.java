package com.labs_101.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.labs_101.backend.entities.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, String> {

    /** Name or email containing the query, ignoring case. */
    @Query("""
            select u from User u
            where lower(u.name) like lower(concat('%', :query, '%')) or lower(u.email) like lower(concat('%', :query, '%'))
            order by u.name
            """)
    List<User> search(@Param("query") String query, Pageable pageable);

    /** Locks the row until the transaction ends, e.g. to check a daily limit before inserting. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") String id);
}
