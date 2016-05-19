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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HoldAtomic<T> implements Holder<T> {

  // Holding area for the things.
  private AtomicReference<T> r = new AtomicReference<>(null);

  /**
   * Stick it into the Atomic.
   */
  private Consumer<T> consumer = r::set;

  /**
   * Pull it out.
   */
  private Supplier<T> producer = r::get;

  /**
   * Transform Holder functions to BlockingQueue functions.
   */
  @Override
  public T get() {
    return producer.get();
  }

  @Override
  public void put(T it) {
    consumer.accept(it);
  }

  public static <T> Holder<T> make(Pipe p) {
    // Ignore the pipe.
    return new HoldAtomic<>();
  }
}
