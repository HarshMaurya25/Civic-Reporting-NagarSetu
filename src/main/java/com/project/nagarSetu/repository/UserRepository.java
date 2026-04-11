package com.project.nagarSetu.repository;

import com.project.nagarSetu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
        User findByEmail(String email);

        @Query("SELECT u FROM User u WHERE u.id = :id AND u.enable = true ")
        User findByUserId(@Param("id") UUID id);
}
