package com.novastudy.repository;

import com.novastudy.entity.TokenBlackList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlackList, Long> {
    Optional<TokenBlackList> findByToken(String token);

    @Modifying
    @Query("DELETE FROM TokenBlackList t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(LocalDateTime now);
}
