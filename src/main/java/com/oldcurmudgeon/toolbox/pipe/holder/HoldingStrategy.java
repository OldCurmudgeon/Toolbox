/*
 * Copyright 2016 pcaswell.
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
package com.oldcurmudgeon.toolbox.pipe.holder;

import com.oldcurmudgeon.toolbox.pipe.Pipe;

import java.util.function.Function;

/**
 * Pick a holding strategy to use.
 * <p>
 * Each is a Holder factory
 */
public enum HoldingStrategy {
    /**
     * Hold nothing.
     */
    None(new Function<Pipe, Holder>() {
        // We are stateless so we only need one supplier.
        final Holder NOTHING = new Holder() {
            /**
             * Always give null.
             */
            @Override
            public Object get() {
                return null;
            }

            /**
             * Black hole.
             */
            @Override
            public void put(Object it) {
            }

        };

        /**
         * Give them a Holder that uses this strategy.
         */
        @Override
        public Holder apply(Pipe t) {
            // Always give them the same one.
            return NOTHING;
        }
    }),
    /**
     * Writing overwrites the previous.
     * <p>
     * Reading reads the current value.
     */
    PassThrough(HoldAtomic::make),
    /**
     * Writing blocks until space available.
     * <p>
     * Reading blocks until something is written.
     */
    Block(HoldBlockingQueue::make),
    /**
     * Forward all requests up the chain.
     */
    Delegate(HoldDelegate::make);

    /**
     * Keeps track of the maker that makes the Holders.
     */
    final Function<Pipe, Holder> maker;

    public <T> Holder<T> holder() {
        return maker.<T>apply(Pipe.NOPIPE);
    }

    public <T> Holder<T> holder(Pipe<T> from) {
        return maker.<T>apply(from);
    }

    HoldingStrategy(Function<Pipe, Holder> maker) {
        this.maker = maker;
    }
}
