/*
 * Copyright 2013 OldCurmudgeon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oldcurmudgeon.toolbox.containers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Use one of these to block on a spin-lock.
 *
 * @author OldCurmudgeon
 */
public class Blocked {
    // Normal lock.
    private final ReentrantLock lock = new ReentrantLock();
    // Let someone through.
    private final Condition go = lock.newCondition();

    public final void await() throws InterruptedException {
        lock.lock();
        try {
            // Wait for someone to release.
            go.await();
        } finally {
            lock.unlock();
        }
    }

    public final void release() {
        lock.lock();
        try {
            // Let someone through.
            go.signal();
        } finally {
            lock.unlock();
        }
    }

    private static class Test implements Runnable {
        private final Blocked blocked;
        private final AtomicBoolean wait;
        private final AtomicInteger count;

        public Test(Blocked blocked, AtomicBoolean wait, AtomicInteger count) {
            this.blocked = blocked;
            this.wait = wait;
            this.count = count;
        }

        @Override
        public void run() {
            int c = count.incrementAndGet();
            try {
                System.out.println("Waiting " + c);
                while (wait.get()) {
                    blocked.await();
                }
            } catch (InterruptedException ie) {
                // Ignore - just get out of the loop.
            } finally {
                count.getAndDecrement();
            }
            System.out.println("Finished " + c);
        }

    }

    public static void main(String args[]) {
        try {
            Blocked blocked = new Blocked();
            AtomicBoolean wait = new AtomicBoolean(true);
            AtomicInteger count = new AtomicInteger(0);
            // Make a bunch.
            for (int i = 0; i < 100; i++) {
                Test test = new Test(blocked, wait, count);
                new Thread(test).start();
            }
            Thread.sleep(1000);
            // Stop everyone.
            wait.set(false);
            int wakeups = 0;
            while (count.get() > 0) {
                blocked.release();
                wakeups += 1;
                Thread.sleep(10);
            }
            System.out.println("Finished! Wakeups=" + wakeups);

        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

}
