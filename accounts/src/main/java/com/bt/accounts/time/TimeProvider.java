package com.bt.accounts.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TimeProvider {
    private static volatile TimeProvider instance;
    private final AtomicLong offsetSeconds = new AtomicLong(0);

    public TimeProvider() {
        instance = this;
    }

    public Instant now() {
        long offset = offsetSeconds.get();
        return Instant.now().plusSeconds(offset);
    }

    public ZonedDateTime zonedNow() {
        return ZonedDateTime.ofInstant(now(), ZoneId.systemDefault());
    }

    public static TimeProvider current() {
        TimeProvider ref = instance;
        if (ref == null) {
            throw new IllegalStateException("TimeProvider is not initialized");
        }
        return ref;
    }

    public static LocalDateTime currentDateTime() {
        return current().zonedNow().toLocalDateTime();
    }

    public long getOffsetSeconds() {
        return offsetSeconds.get();
    }

    public void setAbsolute(Instant target) {
        long diff = target.getEpochSecond() - Instant.now().getEpochSecond();
        offsetSeconds.set(diff);
    }

    public void advanceSeconds(long seconds) {
        offsetSeconds.addAndGet(seconds);
    }

    public void reset() {
        offsetSeconds.set(0);
    }
}
