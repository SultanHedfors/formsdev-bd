package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Slf4j
public class ScheduleAssignmentJobQueue {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    public ScheduleAssignmentJobQueue() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    queue.take().run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrupted, shutting down queue processing.", e);
                    break;
                } catch (Exception e) {
                    log.error("EXCEPTION EXECUTING QUEUED OPERATION!", e);
                }
            }
        });
    }

    public void submitJob(Runnable job) {
        queue.add(job);
    }
}
