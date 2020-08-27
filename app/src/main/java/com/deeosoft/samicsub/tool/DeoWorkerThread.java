package com.deeosoft.samicsub.tool;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeoWorkerThread extends Thread {
    private static final String TAG = "DeoWorkerThread";
    AtomicBoolean isRunning = new AtomicBoolean(true);
    ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    public DeoWorkerThread(){
        super(TAG);
        start();
    }

    @Override
    public void run() {
        while(isRunning.get()){
            Runnable task = queue.poll();
            if (task != null) {
                task.run();
            }
        }
    }

    public DeoWorkerThread execute(Runnable task){
        queue.add(task);
        return this;
    }

    public void killThread(){
        isRunning.set(false);
    }

    public void refreshTaskQueue(){
        queue.clear();
    }
}
