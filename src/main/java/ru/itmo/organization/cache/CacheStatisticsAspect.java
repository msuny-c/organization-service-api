package ru.itmo.organization.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;
import ru.itmo.organization.config.CacheSettingsProperties;

import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CacheStatisticsAspect {

    private final CacheSettingsProperties properties;
    private final Statistics statistics;
    private final AtomicLong lastHitCount = new AtomicLong(0);
    private final AtomicLong lastMissCount = new AtomicLong(0);

    @Pointcut("execution(* ru.itmo.organization.repository..*(..)) || execution(* ru.itmo.organization.service..*(..))")
    public void monitoredOperations() {}

    @AfterReturning("monitoredOperations()")
    public void logCacheStatistics() {
        if (!properties.isLoggingEnabled()) {
            return;
        }

        long hits = statistics.getSecondLevelCacheHitCount();
        long misses = statistics.getSecondLevelCacheMissCount();

        long hitDelta = hits - lastHitCount.getAndSet(hits);
        long missDelta = misses - lastMissCount.getAndSet(misses);

        if (hitDelta == 0 && missDelta == 0) {
            return;
        }

        log.info("L2 Cache stats: hits={}, misses={}, hitDelta={}, missDelta={}", hits, misses, hitDelta, missDelta);
    }
}
