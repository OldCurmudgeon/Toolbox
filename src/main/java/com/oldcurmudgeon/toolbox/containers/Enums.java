/*
 * Copyright 2013 OldCurmudgeon
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author OldCurmudgeon
 */
public class Enums {

  public interface HasName {

    public String name();

  }

  public interface PoliteEnum extends HasName {

    default String politeName() {
      return name().replace("_", " ");
    }

  }

  public interface Lookup<P, Q> {

    public Q lookup(P p) throws Exception;

  }

    public interface ReverseLookup<E extends Enum<E>> extends Lookup<String, E> {

      // Map of all classes that have lookups.
      Map<Class, Map<String, Enum>> lookups = new ConcurrentHashMap<>();

      // What I need from the Enum.
      Class<E> getDeclaringClass();

      @Override
      default E lookup(String name) throws InterruptedException, ExecutionException {
        // What class.
        Class<E> c = getDeclaringClass();
        // Get the map - make a new one of not present.
        final Map<String, Enum> lookup = lookups.computeIfAbsent(c, k -> 
                  Stream.of(c.getEnumConstants())
                  // Roll each enum into the lookup.
                  .collect(Collectors.toMap(Enum::name, Function.identity())));
        // Look it up.
        return c.cast(lookup.get(name));
      }

    }

  // Use the above interfaces to add to the enum.
  public enum X implements PoliteEnum, ReverseLookup<X> {

    A_For_Ism,
    B_For_Mutton,
    C_Forth_Highlanders;
  }

  public void test() {
    System.out.println("Hello");
  }

  public static void main(String args[]) {
    try {
      new Enums().test();
    } catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }

}
