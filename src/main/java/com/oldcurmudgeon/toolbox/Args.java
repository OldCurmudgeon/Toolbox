/*
 * Copyright 2015 Paul Caswell.
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
package com.oldcurmudgeon.toolbox;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @param <E>
 * @author Paul Caswell
 */
public class Args<E extends Enum<E> & Enums.ReverseLookup<E>> {
    // The ons and offs.
    private final Set<E> ons;
    private final Set<E> offs;
    private final Map<E, String> values;

    /**
     * An arg can be on (+x), off(-x), contain data (/x=data) or not present.
     */
    public enum State {
        On, Off, Data, Absent
    }

    /**
     * All arg enums must reverse lookup.
     *
     * @param <E>
     */
    public interface Arg<E extends Enum<E>> extends Enums.ReverseLookup<E> {

    }

    /**
     * Capture the command-line parameters.
     *
     * @param args
     * @param of
     */
    public Args(String args[], Class<E> of) {
        ons = EnumSet.<E>noneOf(of);
        offs = EnumSet.<E>noneOf(of);
        values = new EnumMap<>(of);
        // Populate them.
        for (String s : args) {
            if (s != null && s.length() > 0) {
                // Split on "=" if present.
                String[] parts = s.split("=");
                // What's the first character.
                char first = s.charAt(0);
                // What's the rest?
                E arg = Enums.ReverseLookup.lookup(of, parts[0]);
                if (arg == null) {
                    arg = Enums.ReverseLookup.lookup(of, parts[0].substring(1));
                }
                if (arg != null) {
                    switch (first) {
                        case '+':
                            ons.add(arg);
                            break;
                        case '-':
                            offs.add(arg);
                            break;
                        case '/':
                            values.put(arg, parts[1] != null ? parts[1] : "");
                            break;
                        default:
                            // No decoration - if theres an equals then it's a value, otherwise it's an on.
                            if (parts.length > 1) {
                                values.put(arg, parts[1]);
                            } else {
                                ons.add(arg);
                            }
                    }
                } else {
                    // Just ignore unexpected args.
                    //System.err.println("Arg '" + s + "' invalid or unrecognised.");
                }

            }
        }
    }

    /**
     * Is this arg on/off/not present.
     *
     * @param a
     * @return
     */
    public Set<State> is(E a) {
        Set<State> state = EnumSet.noneOf(State.class);
        if (ons.contains(a)) {
            state.add(State.On);
        }
        if (offs.contains(a)) {
            state.add(State.Off);
        }
        return state;
    }

    /**
     * Get all ons.
     */
    public Set<E> ons() {
        return ons;
    }

    /**
     * Get all offs.
     */
    public Set<E> offs() {
        return offs;
    }

    /**
     * Get the value behind this arg.
     */
    public String value(E e, String dflt) {
        String v = values.get(e);
        return v != null ? v : dflt;
    }

    /**
     * Is this arg on?
     */
    public boolean on(E e) {
        return ons.contains(e);
    }

    /**
     * Is this arg off?
     */
    public boolean off(E e) {
        return offs.contains(e);
    }

    public enum TestArgs implements Arg<TestArgs> {
        Arg1, Arg2, Arg3, Arg4, Arg5
    }

    public static void main(String args[]) {
        try {
            Args a = new Args(new String[]{"Arg1", "Arg2", "/Arg3=10", "+Arg4", "-Arg5"}, TestArgs.class);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

}
