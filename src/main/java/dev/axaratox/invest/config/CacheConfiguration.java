package dev.axaratox.invest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfiguration {

    public static final String INSTRUMENTS = "instruments";
    public static final String LAST_PRICES = "lastPrices";
    public static final String CURRENCY_FIGI = "currencyFigi";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(INSTRUMENTS, LAST_PRICES, CURRENCY_FIGI);
    }

    @CacheEvict(value = INSTRUMENTS, allEntries = true)
    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.DAYS)
    public void clearInstrumentsCache() {
        log.info("Cache \"{}\" was flushed", INSTRUMENTS);
    }

    @CacheEvict(value = LAST_PRICES, allEntries = true)
    @Scheduled(initialDelay = 5, fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void clearLastPricesCache() {
        log.info("Cache \"{}\" was flushed", LAST_PRICES);
    }

    @CacheEvict(value = CURRENCY_FIGI, allEntries = true)
    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.DAYS)
    public void clearCurrencyCache() {
        log.info("Cache \"{}\" was flushed", CURRENCY_FIGI);
    }
}
