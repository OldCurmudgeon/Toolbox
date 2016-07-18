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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>
 * Title: Separator</p>
 *
 * <p>
 * Description: Handles separators.</p>
 *
 * If no separators are generated then the first and last strings are also not generated.
 *
 * @author OldCurmudgeon
 * @version 1.0
 */
public class Separator<T> {
  // ToDo: Not sure we need firstString. Does anyone use it?
  private final String firstString;
  private final String sepString;
  private final String lastString;
  private final Decorator<T> decorator;
  boolean first = true;

  // Use for url params ("?","&").
  public Separator(final String first, final String sep, String last, final Decorator<T> decorator) {
    this.firstString = first;
    this.sepString = sep;
    this.lastString = last;
    this.decorator = decorator;
  }

  public Separator(final String first, final String sep, final Decorator<T> decorator) {
    this(first, sep, "", decorator);
  }

  public Separator(final String sep, final Decorator<T> decorator) {
    this("", sep, decorator);
  }

  @SuppressWarnings("unchecked")
  public Separator(final String first, final String sep) {
    this(first, sep, "", (Decorator<T>) Trivial);
  }

  @SuppressWarnings("unchecked")
  public Separator(final String first, final String sep, final String last) {
    this(first, sep, last, (Decorator<T>) Trivial);
  }

  // Use for commas etc.
  public Separator(final String sep) {
    this("", sep);
  }

  public String sep() {
    // Return empty string first and then the separator on every subsequent invocation.
    if (first) {
      first = false;
      return firstString;
    }
    return sepString;
  }

  public String fin() {
    // Return the last string if we used the first.
    return first ? "" : lastString;
  }

  public void reset() {
    first = true;
  }

  @Override
  public String toString() {
    return first ? "(" + firstString + sepString + lastString + ")" : firstString + sepString + lastString;
  }

  // They should all come through here.
  private StringBuilder addOne(final StringBuilder s, final Object v) {
    // PAC - We should not discard nulls - it should be left up to the decorator to strip null fields.
    //if (v != null) {
    @SuppressWarnings("unchecked")
    final String decorated = decorator.decorate((T) v);
    if (decorated != null) {
      s.append(sep()).append(decorated);
    }
    //}
    return s;
  }

  // They should all come through here.
  public StringBuilder add(final StringBuilder s, final T v) {
    return addOne(s, v);
  }

  public StringBuilder add(final StringBuilder s, final T... values) {
    return add(s, values == null ? null : Arrays.asList(Rebox.rebox(values)));
  }

  public StringBuilder add(final StringBuilder s, final Iterable<T> i) {
    if (null != i) {
      for (final T v : i) {
        addOne(s, v);
      }
    }
    return s.append(fin());
  }

  public StringBuilder add(final StringBuilder s, final Iterator<T> i) {
    if (null != i) {
      while (i.hasNext()) {
        addOne(s, i.next());
      }
    }
    return s.append(fin());
  }

  public String separate(final T... values) {
    return add(new StringBuilder(values.length << 2), values).toString();
  }

  public String separate(final Iterator<T> i) {
    return add(new StringBuilder(1024), i).toString();
  }

  public String separate(final Iterable<T> i) {
    return separate(i.iterator());
  }

  // Static Utilities.
  public static <T> String separate(final String separator, final T... values) {
    return new Separator<T>(separator).separate(values);
  }

  public static <T> String separate(final String separator, final Iterator<T> i) {
    return new Separator<T>(separator).separate(i);
  }

  public static <T> String separate(final String separator, final Iterable<T> i) {
    return separate(separator, i.iterator());
  }

  public static <T> String separate(final String separator, final Decorator<T> decorator, final T... objs) {
    return new Separator<T>(separator, decorator).separate(objs);
  }

  public static <T> String separate(final String separator, final Decorator<T> decorator, final Iterator<T> i) {
    return new Separator<T>(separator, decorator).separate(i);
  }

  public static <T> String separate(final String separator, final Decorator<T> decorator, final Iterable<T> i) {
    return separate(separator, decorator, i.iterator());
  }

  public static <K, V> String separate(final String separator, final Map<K, V> m) {
    return new Separator<K>(separator, new MapDecorator<K, V>(m)).separate(m.keySet());
  }

  public static <T> String separate(final String first, final String separator, final T... values) {
    return new Separator<T>(first, separator).separate(values);
  }

  public static <T> String separate(final String first, final String separator, final Iterable<T> i) {
    return separate(first, separator, i.iterator());
  }

  public static <T> String separate(final String first, final String separator, final Iterator<T> i) {
    return new Separator<T>(first, separator).separate(i);
  }

  public static <K, V> String separate(final String first, final String separator, Map<K, V> m) {
    return new Separator<K>(first, separator, new MapDecorator<K, V>(m)).separate(m.keySet());
  }

  public static <T> String separate(final String first, final String separator, final Decorator<T> decorator, final T... objs) {
    return new Separator<T>(first, separator, decorator).separate(objs);
  }

  public static <T> String separate(final String first, final String separator, final Decorator<T> decorator, final Iterable<T> i) {
    return separate(first, separator, decorator, i.iterator());
  }

  public static <T> String separate(final String first, final String separator, final Decorator<T> decorator, final Iterator<T> i) {
    return new Separator<T>(first, separator, decorator).separate(i);
  }

  public static <T> String separate(final String first, final String separator, String last, final T... values) {
    return new Separator<T>(first, separator, last).separate(values);
  }

  public static <T> String separate(final String first, final String separator, String last, final Iterable<T> i) {
    return separate(first, separator, last, i.iterator());
  }

  public static <T> String separate(final String first, final String separator, String last, final Iterator<T> i) {
    return new Separator<T>(first, separator, last).separate(i);
  }

  public static <K, V> String separate(final String first, final String separator, String last, Map<K, V> m) {
    return new Separator<K>(first, separator, last, new MapDecorator<K, V>(m)).separate(m.keySet());
  }

  public static <T> String separate(final String first, final String separator, String last, final Decorator<T> decorator, final T... objs) {
    return new Separator<T>(first, separator, last, decorator).separate(objs);
  }

  public static <T> String separate(final String first, final String separator, String last, final Decorator<T> decorator, final Iterable<T> i) {
    return separate(first, separator, last, decorator, i.iterator());
  }

  public static <T> String separate(final String first, final String separator, String last, final Decorator<T> decorator, final Iterator<T> i) {
    return new Separator<T>(first, separator, last, decorator).separate(i);
  }

  // A decorator enhances the values being separated.
  public interface Decorator<T> {
    // Decorate any object.
    public String decorate(T o);

  }

  // Default Simple Decorator - Just converts it to a string.
  private static final Decorator<?> Trivial = new Decorator<Object>() {
    // Thread safe because it is stateless.
    public final String decorate(final Object s) {
      // Just returns toString.
      return s == null ? "null" : s.toString();
    }

  };

  // Drops nulls and converts everything else to a string.
  public static final Decorator<?> DropNulls = new Decorator<Object>() {
    // Thread safe because it is stateless.
    public final String decorate(final Object s) {
      // Just returns toString or null if not present.
      return s == null ? null : s.toString();
    }

  };

  // Blank nulls and converts everything else to a string.
  public static final Decorator<?> BlankNulls = new Decorator<Object>() {
    // Thread safe because it is stateless.
    public final String decorate(final Object s) {
      // Just returns toString or blank if not present.
      return s == null ? "" : s.toString();
    }

  };

  // Ready-to-use Map decorator.
  public static class MapDecorator<K, V> implements Decorator<K> {
    // Keep track of the map.
    private final Map<K, V> map;

    public MapDecorator(Map<K, V> map) {
      this.map = map;
    }

    // key=value
    public final String decorate(K k) {
      final V v = map.get(k);
      return k.toString() + "=" + (v == null ? "null" : v.toString());
    }

  }

  // Replaces all entries with a specific string.
  public static class Replace<T> implements Decorator<T> {
    // The string to replace with.
    private final String with;

    public Replace(String with) {
      this.with = with;
    }

    public final String decorate(T o) {
      // Replace every field.
      return with;
    }

  }

  public static void main(String[] args) {
    // Some tiny tests.
    try {
      final Map<String, String> m = new HashMap<String, String>();
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
      System.out.println(separate(",", new int[]{1, 1}));
      System.out.println(separate(",", new long[]{1, 1}));
      System.out.println(separate(",", new float[]{1, 1}));
      System.out.println(separate(",", new double[]{1, 1}));
      Separator<Object> ssep = new Separator<Object>(",");
      StringBuilder b = new StringBuilder();
      ssep.add(b, 1, 2);
      ssep.add(b, "3", "4");
      System.out.println(b);

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
