package com.bt.accounts.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TimeProvider {
    private final AtomicLong offsetSeconds = new AtomicLong(0);

    public Instant now() {
        long offset = offsetSeconds.get();
        return Instant.now().plusSeconds(offset);
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
