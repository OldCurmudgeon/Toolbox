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
package com.oldcurmudgeon.toolbox.walkers;

/**
 * Filters stuff.
 *
 * @author OldCurmudgeon
 */
public abstract class Filter<T> {
    // Return true if this one should be accepted.
    public boolean accept(T it) {
        // Default to accept.
        return true;
    }

    // Return the filtered value.
    public T filter(T it) {
        // Default to unfiltered.
        return it;
    }

}
