package com.occupify.identity.repository;

import com.occupify.identity.entity.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    @Modifying
    @Query("UPDATE users SET password = :password, updated_at = CURRENT_TIMESTAMP WHERE email = :email")
    Mono<Integer> updatePasswordByEmail(String email, String password);

    @Modifying
    @Query("UPDATE users SET status = :status, updated_at = CURRENT_TIMESTAMP WHERE email = :email")
    Mono<Integer> updateStatusByEmail(String email, String status);
}
