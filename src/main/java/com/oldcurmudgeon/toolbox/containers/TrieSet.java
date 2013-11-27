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

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of a Trie Set of a CharSequence.
 *
 * @author OldCurmudgeon
 * @param <K>
 */
public class TrieSet<K extends CharSequence> extends AbstractSet<K> implements Set<K> {
  // Hold a TrieMap<Object> and always put v in it.
  private final Object v = new Object();
  // My backing map.
  private final TrieMap<K, Object> map;

  // Start empty.
  public TrieSet() {
    this(new TrieMap<K, Object>());
  }

  // Start empty.
  public TrieSet(TrieMap.KeyFactory keyFactory) {
    this(new TrieMap<K, Object>(keyFactory));
  }

  // Wrap an existing map.
  // Package private because I expect only TrieMap will want a Set wrapper.
  // Used by TrieMap to return its keyset.
  TrieSet(TrieMap<K, Object> map) {
    this.map = map;
  }

  @Override
  public Iterator<K> iterator() {
    return new TrieSetIterator<>(map);
  }

  // My iterator feeds off an EntryIterator from the TrieMap.
  private static class TrieSetIterator<K extends CharSequence> implements Iterator<K> {
    // Keep an Entry iterator.
    private final Iterator<Map.Entry<K, Object>> i;

    public TrieSetIterator(TrieMap<K, Object> map) {
      // Make an EntrySet and grab its iterator.
      i = new TrieMap.EntrySet<>(map).iterator();
    }

    @Override
    public K next() {
      K it = null;
      if (i.hasNext()) {
        // Return just the key from the Entry.
        it = i.next().getKey();
      }
      return it;
    }

    @Override
    public boolean hasNext() {
      // Same as the entry iterator.
      return i.hasNext();
    }

    @Override
    public void remove() {
      // Ripple up to the entry iterator.
      i.remove();
    }
  }

  @Override
  public boolean add(K k) {
    return map.put(k, v) == null;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(Object o) {
    return o instanceof CharSequence && map.containsKey((K) o);
  }

  /**
   *
   * @param args
   */
  public static void main(String[] args) {
    int nStrings = 0;
    int nChars = 0;
    try {
      TrieSet t = new TrieSet();
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
        boolean added = t.add(s);
        System.out.println("Added '" + s + "'\tResult: " + added + "\tSize: " + t.size() + "\tContains: " + t.contains(s));
        //System.out.println("Trie: " + t);
      }

      System.out.println("Trie: " + t);
      System.out.println("Removing: 0123456789");
      t.remove("0123456789");
      System.out.println("Trie: " + t);

      String[] tests2 = {
        "",
        "A",
        "ABCD",
        "ZYXWVU"
      };
      for (String s : tests2) {
        System.out.println("'" + s + "'\tContains: " + t.contains(s));
      }
      System.out.println("Trie: " + t);
      // Remove
      TrieSet t2 = new TrieSet();
      t2.addAll(Arrays.<String>asList(tests2));
      t.removeAll(t2);
      System.out.println("Removed them: " + t);

      // Remove while iterating.
      Iterator<String> i = t.iterator();
      while (i.hasNext()) {
        String n = i.next();
        if (n.equals("AB")) {
          i.remove();
          System.out.println("Removed: " + n);
        }
      }
      System.out.println("Finished: " + t);
      // Test to destrunction.
      t.clear();
      System.out.println("Testing to destruction: ");
      for (long n = 0;; n++) {
        String s = Long.toString(n);
        nStrings += 1;
        nChars += s.length();
        t.add(s);
        if (nStrings % 100 == 0) {
          System.out.println("Added: " + nStrings);
        }
      }
    } catch (Throwable ex) {
      System.out.println("Strings: " + nStrings);
      System.out.println("Chars: " + nChars);
      ex.printStackTrace();
    }
  }
}