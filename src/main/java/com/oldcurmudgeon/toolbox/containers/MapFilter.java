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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Allows the filtering of maps by key prefix.
 *
 * E.G.
 *
 * # The CardTypes table. # Truncate table first (or leave it be) Table.CardTypes.Truncate=false # Update records (or only add) Table.CardTypes.Update=false # Fields in table
 * Table.CardTypes.Columns=Name,Type,PAN_Length,LUHN_Check,Range Table.CardTypes.KeyColumn=Name # NB: These are MSSql forms. Table.CardTypes.Column.Name=varchar(40) NOT NULL
 * Table.CardTypes.Column.Type=varchar(10) NOT NULL Table.CardTypes.Column.PAN_Length=bigint NOT NULL Table.CardTypes.Column.LUHN_Check=bit NOT NULL Table.CardTypes.Column.Range=varchar (20) NOT NULL
 * # Data. Table.CardTypes.Record.01='Please Select ...','0',0,0,'-' Table.CardTypes.Record.02='Wilko Gift Card','3',19,1,'98261711' Table.CardTypes.Record.03='Wilko Saver Card','5',19,1,'9826171190'
 * Table.CardTypes.Record.04='Selfridges Gift Card','2000',16,1,'98261'
 *
 * after filtering by "Table.CardTypes." becomes
 *
 * # The CardTypes table. # Truncate table first (or leave it be) Truncate=false # Update records (or only add) Update=false # Fields in table Columns=Name,Type,PAN_Length,LUHN_Check,Range
 * KeyColumn=Name # NB: These are MSSql forms. Column.Name=varchar(40) NOT NULL Column.Type=varchar(10) NOT NULL Column.PAN_Length=bigint NOT NULL Column.LUHN_Check=bit NOT NULL Column.Range=varchar
 * (20) NOT NULL # Data. Record.01='Please Select ...','0',0,0,'-' Record.02='Wilko Gift Card','3',19,1,'98261711' Record.03='Wilko Saver Card','5',19,1,'9826171190' Record.04='Selfridges Gift
 * Card','2000',16,1,'98261'
 *
 * Note that all access through the filter reference the underlying Map so adding to a MapFilder results in additions to the Map.
 *
 * @author OldCurmudgeon
 * @param <T>
 */
public class MapFilter<T> implements Map<String, T> {

  // The enclosed map -- could also be a MapFilter.

  final private Map<String, T> map;
      // Use a TreeMap for predictable iteration order.
  // Store Map.Entry to reflect changes down into the underlying map.
  // The Key is the shortened string. The entry.key is the full string.
  final private Map<String, Map.Entry<String, T>> entries = new TreeMap<>();
  // The prefix they are looking for in this map.
  final private String prefix;

  public MapFilter(Map<String, T> map, String prefix) {
    // Store my backing map.
    this.map = map;
    // Record my prefix.
    this.prefix = prefix;
    // Build my entries.
    rebuildEntries();
  }

  public MapFilter(Map<String, T> map) {
    this(map, "");
  }

  private synchronized void rebuildEntries() {
    // Start empty.
    entries.clear();
    // Build my entry set.
    for (Map.Entry<String, T> e : map.entrySet()) {
      String key = e.getKey();
      // Retain each one that starts with the specified prefix.
      if (key.startsWith(prefix)) {
        // Key it on the remainder.
        String k = key.substring(prefix.length());
        // Entries k always contains the LAST occurrence if there are multiples.
        entries.put(k, e);
      }
    }

  }

  @Override
  public String toString() {
    return "MapFilter(" + prefix + ") of " + map + " containing " + entrySet();
  }

  // Constructor from a properties file.
  public MapFilter(Properties p, String prefix) {
        // Properties extends HashTable<Object,Object> so it implements Map.
    // I need Map<String,T> so I wrap it in a HashMap for simplicity.
    // Java-8 breaks if we use diamond inference.
    this(new HashMap<>((Map) p), prefix);
  }

  // Helper to fast filter the map.
  public MapFilter<T> filter(String prefix) {
    // Wrap me in a new filter.
    return new MapFilter<>(this, prefix);
  }

  // Count my entries.
  @Override
  public int size() {
    return entries.size();
  }

  // Are we empty.
  @Override
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  // Is this key in me?
  @Override
  public boolean containsKey(Object key) {
    return entries.containsKey(key);
  }

  // Is this value in me.
  @Override
  public boolean containsValue(Object value) {
    // Walk the values.
    for (Map.Entry<String, T> e : entries.values()) {
      if (value.equals(e.getValue())) {
        // Its there!
        return true;
      }
    }
    return false;
  }

  // Get the referenced value - if present.
  @Override
  public T get(Object key) {
    return get(key, null);
  }

  // Get the referenced value - if present.
  public T get(Object key, T dflt) {
    Map.Entry<String, T> e = entries.get((String) key);
    return e != null ? e.getValue() : dflt;
  }

  // Add to the underlying map.
  @Override
  public T put(String key, T value) {
    T old = null;
    // Do I have an entry for it already?
    Map.Entry<String, T> entry = entries.get(key);
    // Was it already there?
    if (entry != null) {
      // Yes. Just update it.
      old = entry.setValue(value);
    } else {
      // Add it to the map.
      map.put(prefix + key, value);
      // Rebuild.
      rebuildEntries();
    }
    return old;
  }

  // Get rid of that one.
  @Override
  public T remove(Object key) {
    // Do I have an entry for it?
    Map.Entry<String, T> entry = entries.get((String) key);
    if (entry != null) {
      entries.remove(key);
      // Change the underlying map.
      return map.remove(prefix + key);
    }
    return null;
  }

  // Add all of them.
  @Override
  public void putAll(Map<? extends String, ? extends T> m) {
    for (Map.Entry<? extends String, ? extends T> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  // Clear everything out.
  @Override
  public void clear() {
        // Just remove mine.
    // This does not clear the underlying map - perhaps it should remove the filtered entries.
    for (String key : entries.keySet()) {
      map.remove(prefix + key);
    }
    entries.clear();
  }

  @Override
  public Set<String> keySet() {
    return entries.keySet();
  }

  @Override
  public Collection<T> values() {
    // Roll them all out into a new ArrayList.
    List<T> values = new ArrayList<>();
    for (Map.Entry<String, T> v : entries.values()) {
      values.add(v.getValue());
    }
    return values;
  }

  @Override
  public Set<Map.Entry<String, T>> entrySet() {
    // Roll them all out into a new TreeSet.
    Set<Map.Entry<String, T>> entrySet = new TreeSet<>();
    for (Map.Entry<String, Map.Entry<String, T>> v : entries.entrySet()) {
      entrySet.add(new Entry<>(v));
    }
    return entrySet;
  }

  /**
   * An entry.
   *
   * @param <T>
   *
   * The type of the value.
   */
  private static class Entry<T> implements Map.Entry<String, T>, Comparable<Entry<T>> {

    // Note that entry in the entry is an entry in the underlying map.

    private final Map.Entry<String, Map.Entry<String, T>> entry;

    Entry(Map.Entry<String, Map.Entry<String, T>> entry) {
      this.entry = entry;
    }

    @Override
    public String getKey() {
      return entry.getKey();
    }

    @Override
    public T getValue() {
      // Remember that the value is the entry in the underlying map.
      return entry.getValue().getValue();
    }

    @Override
    public T setValue(T newValue) {
      // Remember that the value is the entry in the underlying map.
      return entry.getValue().setValue(newValue);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry e = (Entry) o;
      return getKey().equals(e.getKey()) && getValue().equals(e.getValue());
    }

    @Override
    public int hashCode() {
      return getKey().hashCode() ^ getValue().hashCode();
    }

    @Override
    public String toString() {
      return getKey() + "=" + getValue();
    }

    @Override
    public int compareTo(Entry<T> o) {
      return getKey().compareTo(o.getKey());
    }

  }

  // Simple tests.
  public static void main(String[] args) {
    String[] samples = {
      "Some.For.Me",
      "Some.For.You",
      "Some.More",
      "Yet.More"};
    Map map = new HashMap();
    for (String s : samples) {
      map.put(s, s);
    }
    Map all = new MapFilter(map);
    Map some = new MapFilter(map, "Some.");
    Map someFor = new MapFilter(some, "For.");
    System.out.println("All: " + all);
    System.out.println("Some: " + some);
    System.out.println("Some.For: " + someFor);

    Properties props = new Properties();
    props.setProperty("namespace.prop1", "value1");
    props.setProperty("namespace.prop2", "value2");
    props.setProperty("namespace.iDontKnowThisNameAtCompileTime", "anothervalue");
    props.setProperty("someStuff.morestuff", "stuff");
    Map<String, String> filtered = new MapFilter(props, "namespace.");
    System.out.println("namespace props " + filtered);
  }

}
