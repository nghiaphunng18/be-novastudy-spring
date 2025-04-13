package com.novastudy.service;

import com.novastudy.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TokenCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired blacklisted tokens");
        int deletedCount = tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        logger.info("Deleted {} expired blacklisted tokens", deletedCount);
    }
}
