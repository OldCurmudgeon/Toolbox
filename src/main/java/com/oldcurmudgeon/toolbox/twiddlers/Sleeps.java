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
package com.oldcurmudgeon.toolbox.twiddlers;

import com.oldcurmudgeon.toolbox.containers.Params;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: Sleeps</p>
 * <p>
 * <p>Description: Provides various sleeps.</p>
 * <p>
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * @param <S>
 * @author OldCurmudgeon
 */
public class Sleeps<S extends Enum<S> & Sleeps.Sleeper> {
    // My sleepers.
    final EnumSet<S> sleepers;
    // The surrogates that actually do the sleeping.
    final Map<S, Surrogate<S>> surrogates;

    private void sleep(S sleeper) {
        // Forward it to the surrogate.
        surrogates.get(sleeper).sleep();
    }

    private void doze(S sleeper) throws InterruptedException {
        // Forward it to the surrogate.
        surrogates.get(sleeper).doze();
    }

    // What a sleeper enum looks like.
    public interface Sleeper {
        // Get the sleep time.
        double getSeconds();
    }

    // Not allowed a zero-param constructor.
    private Sleeps() {
        sleepers = null;
        surrogates = null;
    }

    // Constructor grabs the enum of sleepers.
    public Sleeps(EnumSet<S> sleepers) {
        // Grab my sleepers.
        this.sleepers = sleepers;

        // Creates my surrogate map.

        // A sample sleeper used to find the universe of the enum.
        Sleeper sample = sleepers.iterator().next();

        // I want to make surrogates an EnumMap for efficiency.
        // This is a wierd way of doing it - if you can think
        // of a better way please replace this code.

        // Make a template HashMap for the EnumMap
        HashMap<Sleeper, Surrogate> template = new HashMap<>();
        // Add one item to it. Assumes there is at least one, which makes sense.
        // Doesnt matter what field type I put in it.
        template.put(sample, null);
        // Build the EnumMap from the template HashMap.
        surrogates = new EnumMap(template);
        // Clear out my sample entry.
        surrogates.clear();

        // Create all the surrogates.
        for (S s : sleepers) {
            surrogates.put(s, new Surrogate<>(s));
        }
    }

    // Wait for one tick.
    public static void oneTick() {
        new Surrogate(-1).sleep();
    }

    // Convert seconds to milliseconds.
    public static long milliseconds(double seconds) {
        return Math.round(seconds * 1000.0d);
    }

    // Surrogate sleeper - actually does the sleeping.
    private static class Surrogate<S extends Enum<S> & Sleeps.Sleeper> {
        // Default value for a reset.
        private final long originalMilliseconds;
        // Current value in case we want to escalate.
        private long milliseconds = 0;
        // Time we last woke up.
        protected long lastAwakening = 0;

        Surrogate(Sleeper sleeper) {
            this(sleeper.getSeconds());
        }

        Surrogate(double time) {
            // Record the time from the sleeper.
            setTime(time);
            // Keep track of original for reset.
            originalMilliseconds = milliseconds;
        }

        /**
         * setTime
         * <p>
         * Allow us to change the sleep times dynamically.
         *
         * @param seconds double
         */
        public final void setTime(double seconds) {
            milliseconds = seconds >= 0 ? milliseconds(seconds) : -1;
        }

        // Sleep the specified time - wake up early if interrupted.
        public long sleep() {
            long time;
            try {
                time = doze();
            } catch (InterruptedException ex) {
                time = (lastAwakening = System.currentTimeMillis());
            }
            return time;
        }

        // Sleep the specified time - throw interrupt if interrupted.
        public long doze() throws InterruptedException {
            if (milliseconds >= 0) {
                // Sleep at least long enough so we dont wake up more often than our alotted time.
                long sleepUntil = lastAwakening + milliseconds;
                do {
                    // Never sleep a negative time.
                    long sleepTime = Math.max(0, sleepUntil - System.currentTimeMillis());
                    // Always sleep, even if it's zero.
                    Thread.sleep(sleepTime);
                } while (System.currentTimeMillis() < sleepUntil);
            } else {
                // Wait until tick has changed.
                do {
                    Thread.sleep(0);
                } while (System.currentTimeMillis() <= lastAwakening);
            }
            // Keep track of when we woke up.
            return (lastAwakening = System.currentTimeMillis());
        }

        // If I had gone straight back to sleep last time, would it be time to wake up now?
        public boolean wakeup() {
            long now = System.currentTimeMillis();
            boolean awake = now > lastAwakening + milliseconds;
            if (awake) {
                lastAwakening = now;
            }
            return awake;
        }
    }

    // Collect times from params.
    public void setTimesFromParams(Params params) throws NumberFormatException {
        for (S s : sleepers) {
            String paramName = "Sleep" + s;
            String param = params.get(paramName, null);
            if (param != null) {
                Surrogate<S> surrogate = surrogates.get(s);
                Double newTime = Double.parseDouble(param);
                surrogate.setTime(newTime);
            }
        }
    }

    // Back to default settings.
    public void reset() {
        // Reset all to original.
        for (Surrogate<S> s : surrogates.values()) {
            s.milliseconds = s.originalMilliseconds;
        }
    }

    /* My test enum.
     * Collection of sleep times.
     * Times are in seconds.
     * 0 Means don't wait at all.
     * -1 means wait the smallest amount of time but at least one "tick".
     * Override these from properties file with "Sleep...=n".
     * Resolution is miliseconds - note that on Windows the true res is nearer 15ms.
     */
    public enum Tests implements Sleeps.Sleeper {
        // Ten second test.
        TenSeconds(10),
        // Generic one-tick wait.
        OneTick(-1),
        // Don't wait.
        DontWait(0);
        // How many seconds.
        double seconds;

        // Store the seconds value.
        Tests(double seconds) {
            this.seconds = seconds;
        }

        // Implement the interface.
        @Override
        public double getSeconds() {
            return seconds;
        }
    }

    public static void main(String[] args) {
        try {
            Sleeps<Tests> sleeps = new Sleeps(EnumSet.allOf(Tests.class));
            sleeps.sleep(Tests.TenSeconds);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }
}
