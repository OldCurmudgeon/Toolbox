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

import com.oldcurmudgeon.toolbox.twiddlers.Rebox;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>Title: Separator</p>
 *
 * <p>Description: Handles separators.</p>
 *
 * If no separators are generated then the first and last strings are also not generated.
 *
 * @author OldCurmudgeon
 * @version 1.0
 */
public class Separator<T> {
  private final String firstString;
  private final String sepString;
  private final String lastString;
  private final Decorator decorator;
  // First time around?
  boolean first = true;

  // The full thing.
  public Separator(String first, String sep, String last, Decorator decorator) {
    this.firstString = first;
    this.sepString = sep;
    this.lastString = last;
    this.decorator = decorator;
  }

  public Separator(String first, String sep, Decorator decorator) {
    // Default to no last.
    this(first, sep, "", decorator);
  }

  public Separator(String sep, Decorator decorator) {
    // Default to no first.
    this("", sep, decorator);
  }

  public Separator(String first, String sep, String last) {
    // Default to trivial decorator.
    this(first, sep, last, TrivialDecorator);
  }

  public Separator(String first, String sep) {
    // Default to empty last.
    this(first, sep, "");
  }

  // Use for commas etc.
  public Separator(String sep) {
    // Default first to empty.
    this("", sep);
  }

  // The core functionality.
  public String sep() {
    // Return first string first and then the separator on every subsequent invocation.
    if (first) {
      first = false;
      return firstString;
    }
    return sepString;
  }

  public String fin() {
    // Return the last string if we used the first - otherwise nothing.
    return first ? "" : lastString;
  }

  public void reset() {
    // Back to beginning - whatever that means.
    first = true;
  }

  @Override
  public String toString() {
    // Bracketed if not started yet.
    return first ? "(" + firstString + sepString + lastString + ")" : firstString + sepString + lastString;
  }

  // They should all come through here.
  public StringBuilder add(StringBuilder s, Object v) {
    if (v != null) {
      String decorated = decorator.decorate(v);
      if (decorated != null) {
        s.append(sep()).append(decorated);
      }
    }
    return s;
  }

  public StringBuilder add(StringBuilder s, T... values) {
    // Remember to rebox it in case it's a primitive array.
    for (T v : Rebox.rebox(values)) {
      add(s, v);
    }
    return s.append(fin());
  }

  public StringBuilder add(StringBuilder s, Iterable<T> i) {
    if (i != null) {
      for (T v : i) {
        add(s, v);
      }
    }
    return s.append(fin());
  }

  public StringBuilder add(StringBuilder s, Iterator<T> i) {
    if (i != null) {
      while (i.hasNext()) {
        add(s, i.next());
      }
    }
    return s.append(fin());
  }

  public String separate(T... values) {
    return add(new StringBuilder(2 * values.length), values).toString();
  }

  public String separate(Iterator<T> i) {
    return add(new StringBuilder(), i).toString();
  }

  public String separate(Iterable<T> i) {
    return separate(i.iterator());
  }

  // Static Utilities.
  public static <T> String separate(String separator, T... values) {
    return new Separator<T>(separator).separate(values);
  }

  public static <T> String separate(String separator, Iterator<T> i) {
    return new Separator<T>(separator).separate(i);
  }

  public static <T> String separate(String separator, Iterable<T> i) {
    return separate(separator, i.iterator());
  }

  public static <T> String separate(String separator, Decorator decorator, T... objs) {
    return new Separator<T>(separator, decorator).separate(objs);
  }

  public static <T> String separate(String separator, Decorator decorator, Iterator<T> i) {
    return new Separator<T>(separator, decorator).separate(i);
  }

  public static <T> String separate(String separator, Decorator decorator, Iterable<T> i) {
    return separate(separator, decorator, i.iterator());
  }

  public static <K, V> String separate(String separator, Map<K, V> m) {
    return new Separator<K>(separator, new MapDecorator<>(m)).separate(m.keySet());
  }

  public static <T> String separate(String first, String separator, T... values) {
    return new Separator<T>(first, separator).separate(values);
  }

  public static <T> String separate(String first, String separator, Iterable<T> i) {
    return separate(first, separator, i.iterator());
  }

  public static <T> String separate(String first, String separator, Iterator<T> i) {
    return new Separator<T>(first, separator).separate(i);
  }

  public static <K, V> String separate(String first, String separator, Map<K, V> m) {
    return new Separator<K>(first, separator, new MapDecorator<>(m)).separate(m.keySet());
  }

  public static <T> String separate(String first, String separator, Decorator<T> decorator, T... objs) {
    return new Separator<T>(first, separator, decorator).separate(objs);
  }

  public static <T> String separate(String first, String separator, Decorator<T> decorator, Iterable<T> i) {
    return separate(first, separator, decorator, i.iterator());
  }

  public static <T> String separate(String first, String separator, Decorator<T> decorator, Iterator<T> i) {
    return new Separator<T>(first, separator, decorator).separate(i);
  }

  public static <T> String separate(String first, String separator, String last, T... values) {
    return new Separator<T>(first, separator, last).separate(values);
  }

  public static <T> String separate(String first, String separator, String last, Iterable<T> i) {
    return separate(first, separator, last, i.iterator());
  }

  public static <T> String separate(String first, String separator, String last, Iterator<T> i) {
    return new Separator<T>(first, separator, last).separate(i);
  }

  public static <K, V> String separate(String first, String separator, String last, Map<K, V> m) {
    return new Separator<K>(first, separator, last, new MapDecorator<>(m)).separate(m.keySet());
  }

  public static <T> String separate(String first, String separator, String last, Decorator<T> decorator, T... objs) {
    return new Separator<T>(first, separator, last, decorator).separate(objs);
  }

  public static <T> String separate(String first, String separator, String last, Decorator<T> decorator, Iterable<T> i) {
    return separate(first, separator, last, decorator, i.iterator());
  }

  public static <T> String separate(String first, String separator, String last, Decorator<T> decorator, Iterator<T> i) {
    return new Separator<T>(first, separator, last, decorator).separate(i);
  }

  // A decorator enhances the values being separated.
  public interface Decorator<T> {
    // Decorate any object.
    public String decorate(T o);

  }
  // Default Simple Decorator - Just converts it to a string.
  private static Decorator<Object> TrivialDecorator = new Decorator<Object>() {
    // Thread safe because it is stateless.
    @Override
    public String decorate(Object s) {
      // Just returns toString.
      return s != null ? s.toString() : "null";
    }

  };

  // Ready-to-use Map decorator.
  public static class MapDecorator<K, V> implements Decorator<K> {
    // Keep track of the map.
    private Map<K, V> map;

    public MapDecorator(Map<K, V> map) {
      this.map = map;
    }

    // key=value
    @Override
    public String decorate(K k) {
      V v = map.get(k);
      return k.toString() + "=" + (v != null ? v.toString() : "null");
    }

  }

  public static void main(String[] args) {
    // Some tiny tests.
    try {
      Map<String, String> m = new HashMap<>();
      m.put("Key1", "Value1");
      m.put("Key2", "Value2");
      System.out.println(separate(",", m));
      System.out.println(separate(",", m.keySet()));
      System.out.println(separate(",", m.keySet().iterator()));
      System.out.println(separate(",", m.entrySet()));
      // Silly AND invalid.
      //System.out.println(separate(",", "Hello"));
      System.out.println(separate("[", ",", "]", "Hello"));
      System.out.println(separate("[", ",", "]", "Hello", "Hello"));
      System.out.println(separate("[", ",", "]", new String[]{"Hello", "Hello"}));
      System.out.println(separate("[", ",", "]", 1, 2, 3, 4));
      System.out.println(separate(",", 1, 2, 3, 4));
      System.out.println(separate(",", new int[]{1, 1}));
      System.out.println(separate(",", new long[]{1, 1}));
      System.out.println(separate(",", new float[]{1, 1}));
      System.out.println(separate(",", new double[]{1, 1}));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
