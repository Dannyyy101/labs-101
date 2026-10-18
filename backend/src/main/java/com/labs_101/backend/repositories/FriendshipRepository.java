package com.labs_101.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.labs_101.backend.entities.Friendship;

// not exported by Spring Data REST, everyone could list all friendships otherwise
@RepositoryRestResource(exported = false)
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Requests and friendships of the user in both directions. */
    @Query("""
            select f from Friendship f join fetch f.requester join fetch f.addressee
            where f.requester.id = :userId or f.addressee.id = :userId
            """)
    List<Friendship> findAllOf(@Param("userId") String userId);

    /** The request or friendship of the two, whoever sent it. */
    @Query("""
            select f from Friendship f join fetch f.requester join fetch f.addressee
            where (f.requester.id = :a and f.addressee.id = :b) or (f.requester.id = :b and f.addressee.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") String a, @Param("b") String b);
}
