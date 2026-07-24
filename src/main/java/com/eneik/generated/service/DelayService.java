package com.eneik.generated.service;

import org.springframework.stereotype.Service;

@Service
public class DelayService {
    public void sleep(long milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
}
