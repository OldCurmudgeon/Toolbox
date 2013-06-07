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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Implementation of a Trie structure.
 *
 * A Trie is a compact form of tree that takes advantage of common prefixes to the keys.
 *
 * A normal HashSet will take the key and compute a hash from it, this hash will be used to locate the value through
 * various methods but usually some kind of bucket system is used. The memory footprint resulting becomes something like
 * O(k*n) where k is the average width of the key.
 *
 * A Trie structure essentially combines all common prefixes into a single key. For example, holding the strings A, AB,
 * ABC and ABCD will only take enough space to record the presence of ABCD. The presence of the others will be recorded
 * as flags within the record of ABCD structure at zero cost. Smilarly, ABCDEF and ABCDEG
 * will share the ABCDE root.
 *
 * This structure is useful for holding similar strings such as product IDs or credit card numbers.
 *
 * This implementation does NOT allow null as key or value.
 *
 * NB: The keys to the EntrySet are actually Strings disguised as K type. If this causes issues, use the 
 * keyFactory constructor.
 *
 * @author OldCurmudgeon
 */
public class TrieMap<K extends CharSequence, V> extends AbstractMap<K, V> implements Map<K, V> {

  /**
   * Map each character to a sub-trie.
   *
   * Could replace this with a 256 entry array of Tries but this will handle multi-byte character sets and I can discard
   * empty maps.
   *
   * Maintained at null until needed (for better memory footprint).
   *
   */
  private Map<Character, TrieMap<K, V>> children = null;
  // Here we store the map contents at this node of the tree. A null value means there is no value here.
  private V value = null;
  // My key factory for the EntrySet keys. Defaults to a String key.
  private final KeyFactory<K> keyFactory;
  
  // Message for null.
  private static final String NoNullsPlease = "A Trie cannot hold nulls!";
  // Message for type failure.
  private static final String KeyIsCharSequencePlease = "Key must be a CharSequence!";
  
  // Without keyFactory constructor - defaults to String keys.
  public TrieMap() {
    this.keyFactory = StringKeyFactory;
  }
  
  // With keyFactory constructor.
  public TrieMap(KeyFactory keyFactory) {
    this.keyFactory = keyFactory;
  }

  /**
   * Set the value to a new setting and return the old one.
   *
   * @param newValue
   * @return old value.
   */
  private V setValue(V newValue) {
    // Protect agains nulls.
    ensureNotNull(newValue);
    V old = value;
    value = newValue;
    return old;
  }

  /**
   * Create a new set of children.
   *
   * I've always wanted to name a method something like this.
   */
  private void makeChildren() {
    if (children == null) {
      // Use a TreeMap to ensure sorted iteration.
      children = new TreeMap<>();
    }
  }

  /**
   * Finds the TrieMap that "should" contain the key.
   *
   * @param key
   *
   * The key to find.
   *
   * @param grow
   *
   * Set to true to grow the Trie to fit the key if necessary.
   *
   * @return
   *
   * The sub Trie that "should" contain the key or null if key was not found and grow was false.
   */
  private TrieMap<K, V> find(CharSequence key, boolean grow) {
    if (key.length() == 0) {
      // Found it!
      return this;
    } else {
      // Not at end of string.
      if (grow) {
        // Grow the tree.
        makeChildren();
      }
      // Allow children == null to stop the search.
      if (children != null) {
        // Ask the kids.
        char ch = key.charAt(0);
        TrieMap<K, V> child = children.get(ch);
        if (child == null && grow) {
          // Make the child.
          child = new TrieMap<>(keyFactory);
          // Store the child.
          children.put(ch, child);
        }
        // Allow child == null to stop the search.
        if (child != null) {
          // Find it in the child.
          return child.find(key.subSequence(1, key.length()), grow);
        }
      }
    }
    return null;

  }

  /**
   *
   * Add a new value to the map.
   *
   * Time footprint = O(s.length).
   *
   * @param s
   *
   * The key defining the place to add.
   *
   * @param value
   *
   * The value to add there.
   *
   * @return
   *
   * The value that was there, or null if it wasn't.
   *
   */
  @Override
  public V put(K key, V value) {
    // Protect agains nulls.
    ensureNotNull(key, value);
    // Find it and set its value -- passing grow = true ensures we return something.
    return find(key, true).setValue(value);
  }

  /**
   * Gets the value at the specified key position.
   *
   * @param o
   *
   * The key to the location.
   *
   * @return
   *
   * The value at that location, or null if there is no value at that location.
   */
  @Override
  public V get(Object k) {
    // Protect agains nulls.
    ensureNotNull(k);
    V got = null;
    // Must be a CharSequence.
    if (k instanceof CharSequence) {
      CharSequence key = (CharSequence) k;
      // Find its place but don't grow.
      TrieMap<K, V> it = find(key, false);
      if (it != null) {
        // Found it.
        got = it.value;
      }
    } else {
      throw new IllegalArgumentException(KeyIsCharSequencePlease);
    }
    return got;
  }

  /**
   * Remove the value at the specified location.
   *
   * @param o
   *
   * The key to the location.
   *
   * @return
   *
   * The value that was removed, or null if there was no value at that location.
   */
  @Override
  public V remove(Object k) {
    // Protect agains nulls.
    ensureNotNull(k);
    V old = null;
    if (k instanceof CharSequence) {
      CharSequence key = (CharSequence) k;
      // Find it.
      TrieMap<K, V> it = find(key, false);
      if (it != null) {
        // Record it.
        old = it.value;
        // Remove it.
        it.value = null;
      }
    } else {
      throw new IllegalArgumentException(KeyIsCharSequencePlease);
    }
    return old;
  }

  /**
   * Count the number of values in the structure.
   *
   * @return
   *
   * The number of values in the structure.
   */
  @Override
  public int size() {
    // If I am a leaf then size increases by 1.
    int size = value != null ? 1 : 0;
    if (children != null) {
      // Add sizes of all my children.
      for (Map.Entry<Character, TrieMap<K,V>> e : children.entrySet()) {
        size += e.getValue().size();
      }
    }
    return size;
  }

  /**
   * Is the tree empty?
   *
   * @return
   *
   * true if the tree is empty. false if there is still at least one value in the tree.
   */
  @Override
  public boolean isEmpty() {
    // I am empty if I am not a leaf and I have no children
    // (slightly quicker than the AbstractCollection implementation).
    return value == null && (children == null || children.isEmpty());
  }

  // From here on we are really only implementimg Map
  /**
   * Returns all keys as a Set.
   *
   * @return
   *
   * A TrieSet of this.
   *
   */
  @Override
  public Set<K> keySet() {
    // Wrap me in a TrieSet.
    return new TrieSet<>((TrieMap<K, Object>) this);
  }

  /**
   * Does the map contain the specified key.
   *
   * @param key
   *
   * The key to look for.
   *
   * @return
   *
   * true if the key is in the Map. false if not.
   */
  @Override
  public boolean containsKey(Object o) {
    // Protect agains nulls.
    ensureNotNull(o);
    if (o instanceof CharSequence) {
      // Find wihout growing.
      TrieMap<K, V> it = find((CharSequence) o, false);
      if (it != null) {
        return it.value != null;
      }
    } else {
      throw new IllegalArgumentException(KeyIsCharSequencePlease);
    }
    return false;
  }

  /**
   * Clear down completely.
   */
  @Override
  public void clear() {
    children = null;
    value = null;
  }

  /**
   * Return a list of key/value pairs.
   *
   * @return
   *
   * The entry set.
   */
  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet<>(this);
  }

  /**
   * Check v must not be null.
   *
   * Throws exception or returns.
   *
   * @param v
   */
  private static void ensureNotNull(Object... v) {
    // Protect against adding nulls.
    for (Object o : v) {
      if (o == null) {
        throw new IllegalArgumentException(NoNullsPlease);
      }
    }
  }

  // Package private because TrieSet uses this class.
  static class EntrySet<K extends CharSequence, V> 
  extends AbstractSet<Map.Entry<K, V>> 
  implements Set<Map.Entry<K, V>> {
    // The map.
    final TrieMap<K, V> map;

    EntrySet(TrieMap<K, V> map) {
      this.map = map;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return new EnrySetIterator<>(map);
    }

    @Override
    public int size() {
      return map.size();
    }

    // The core iterator - all others use this.
    private static class EnrySetIterator<K extends CharSequence, V> implements Iterator<Map.Entry<K, V>> {
      // Current state of the search.
      private State<K, V> state;
      // Next Entry to deliver.
      private Entry<K, V> next = null;
      // The key factory.
      final KeyFactory<K> keyFactory;
      
      // The current state of the iteration.
      private static class State<K extends CharSequence, V> {
        // The key to the current head TrieMap.
        final CharSequence head;
        // The iterator across all sub-characters of this head.
        final Iterator<Character> ci;
        // The map I am working on.
        final TrieMap<K, V> map;
        // The parent state. Allows me to stack the states.
        final State<K, V> parent;

        private State(State<K, V> parent, CharSequence newHead, TrieMap<K, V> map) {
          // Make a sub-state.
          this.head = newHead;
          Map<Character, TrieMap<K, V>> children = map.children;
          // children can be empty.
          if (children != null) {
            ci = children.keySet().iterator();
          } else {
            // Make an empty one.
            ci = Collections.<Character>emptyList().iterator();
          }
          // Keep track of map and parent.
          this.map = map;
          this.parent = parent;
        }
      }

      private EnrySetIterator(State<K, V> parent, CharSequence head, TrieMap<K, V> map) {
        this.keyFactory = map.keyFactory;
        state = new State<>(parent, head, map);
      }

      public EnrySetIterator(TrieMap<K, V> map) {
        // Start at the root with an empty key.
        this(null, "", map);
        // Special case when "" is in the set.
        if (map.value != null) {
          // Prime the next immediately.
          next = new Entry<>(keyFactory.toK(""), map);
        }
      }

      // Become my parent.
      private boolean stepOut() {
        boolean finished = true;
        if (state != null) {
          // Up one.
          state = state.parent;
          // Not finished.
          finished = false;
        }
        return finished;
      }

      // Become my inner child. :)
      private void stepIn(Character ch, TrieMap<K, V> child) {
        state = new State<>(state, new StringBuilder(state.head).append(ch), child);
      }

      @Override
      public boolean hasNext() {
        boolean finished = false;
        // Carry on 'till we've found a next or we've finished.
        while (next == null && state != null && !finished) {
          // Step forward through the child list.
          Character ch = state.ci.hasNext() ? state.ci.next() : null;
          if (ch != null) {
            TrieMap<K, V> it = state.map.children.get(ch);
            if (it.value != null) {
              next = new Entry<>(keyFactory.toK(new StringBuilder(state.head).append(ch)), it);
            }
            // Step in to that one.
            stepIn(ch, it);
          } else {
            // Child iterator exhausted. Step up to parent.
            finished = stepOut();
          }
        }
        return next != null;
      }

      @Override
      public Map.Entry<K, V> next() {
        Entry<K, V> it = null;
        if (hasNext()) {
          it = next;
          next = null;
        }
        return it;
      }

      @Override
      public void remove() {
        // Remove the empty string entry from the current map 'cause that's us.
        state.map.remove(keyFactory.toK(""));
      }
    }
  }

  /**
   * An entry.
   *
   * @param <V>
   *
   * The type of the value.
   */
  private static class Entry<K extends CharSequence, V> 
  implements Map.Entry<K, V> {
    private final K key;
    private final TrieMap<K, V> map;

    Entry(K key, TrieMap<K, V> map) {
      this.key = key;
      this.map = map;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return map.value;
    }

    @Override
    public V setValue(V newValue) {
      // Protect agains nulls.
      ensureNotNull(newValue);
      V oldValue = map.value;
      map.value = newValue;
      return oldValue;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry e = (Entry) o;
      return key.equals(e.getKey()) && map.value.equals(e.getValue());
    }

    @Override
    public int hashCode() {
      return key.hashCode() ^ map.value.hashCode();
    }

    @Override
    public String toString() {
      return key + "=" + map.value;
    }
  }

  // Key factories.
  public interface KeyFactory<K extends CharSequence> {
    // Make a key from this sequence.
    public K toK(CharSequence c);
  }

  public static class StringKeyFactory implements KeyFactory<String> {
    @Override
    public String toK(CharSequence c) {
      return c.toString();
    }
  }
  // They are all idempotent so we can pre-build them.
  public static final KeyFactory StringKeyFactory = new StringKeyFactory();

  public static class StringBuilderKeyFactory implements KeyFactory<StringBuilder> {
    @Override
    public StringBuilder toK(CharSequence c) {
      return new StringBuilder(c);
    }
  }
  // They are all idempotent so we can pre-build them.
  public static final KeyFactory StringBuilderKeyFactory = new StringBuilderKeyFactory();

  public static class StringBufferKeyFactory implements KeyFactory<StringBuffer> {
    @Override
    public StringBuffer toK(CharSequence c) {
      return new StringBuffer(c);
    }
  }
  // They are all idempotent so we can pre-build them.
  public static final KeyFactory StringBufferKeyFactory = new StringBufferKeyFactory();

  public static <V> Map<String, V> newStringMap() {
    return new TrieMap<>(StringKeyFactory);
  }
  public static <V> Map<StringBuilder, V> newStringBuilderMap() {
    return new TrieMap<>(StringBuilderKeyFactory);
  }
  public static <V> Map<StringBuffer, V> newStringBufferMap() {
    return new TrieMap<>(StringBufferKeyFactory);
  }

  // Testing.
  public static void main(String[] args) {
    try {
      TrieMap<String, String> t = new TrieMap<>();
      String[] tests = {
        "",
        "A",
        "AB",
        "A",
        "AB",
        "ABCDEFG",
        "ZYX",
        "a",
        "zyx",
        "0123456789",
        "0123456789A"
      };
      for (String s : tests) {
        String added = t.put(s, s);
        System.out.println("Added '" + s + 
                "'\tResult: " + added + 
                "\tSize: " + t.size() + 
                "\tContains: " + t.containsKey(s));
      }
      System.out.println("Trie: " + t);
      System.out.println("Removing: " + "0123456789");
      t.remove("0123456789");
      System.out.println("Trie: " + t);
      Iterator<Map.Entry<String, String>> i = t.entrySet().iterator();
      while (i.hasNext()) {
        Map.Entry<String, String> e = i.next();
        if (e.getKey().equals("A")) {
          System.out.println("Removing: " + "A");
          i.remove();
        }
      }
      System.out.println("Trie: " + t);

      String[] tests2 = {
        "",
        "A",
        "ABCD",
        "ZYXWVU"
      };
      for (String s : tests2) {
        System.out.println("'" + s + "'\tContains: " + t.containsKey(s));
      }
      System.out.println("Trie: " + t);

      Set<String> keys = t.keySet();
      System.out.println("Keys: " + keys);

      Set<Map.Entry<String, String>> entries = t.entrySet();
      System.out.println("Entries: " + entries);

      t.clear();
      System.out.println("Clear: " + t);

      Map<StringBuilder, StringBuilder> m = TrieMap.<StringBuilder>newStringBuilderMap();
      m.put(new StringBuilder(""), new StringBuilder(""));
      m.put(new StringBuilder("0"), new StringBuilder("0"));
      m.put(new StringBuilder("01"), new StringBuilder("01"));
      m.put(new StringBuilder("012"), new StringBuilder("012"));
      System.out.println("Entries: " + m.entrySet());
      for ( Map.Entry<StringBuilder,StringBuilder> e : m.entrySet() ) {
        System.out.println(e.getKey().toString()+" key is a "+e.getKey().getClass().getSimpleName());
        System.out.println(e.getValue().toString()+" value is a "+e.getKey().getClass().getSimpleName());
      }
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
  }
}
