package com.eneik.generated.util;

import org.springframework.stereotype.Component;

@Component
public class ThreadSleeper implements Sleeper {
    @Override
    public void sleep(long milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
}
