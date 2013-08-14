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

package com.oldcurmudgeon.toolbox.twiddlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author OldCurmudgeon
 */
public class Args {
  //
  //
  // Parameter lists.
  public final Set<String> ons = new HashSet<>(); // '+'
  public final Set<String> offs = new HashSet<>(); // '-'
  public final Map<String, String> vals = new HashMap<>(); // '/x=y'
  public final List<String> slashes = new ArrayList<>(); // /x
  public final List<String> files = new ArrayList<>(); // Everything else

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (String s : ons) {
      sb.append(" +").append(s);
    }
    for (String s : offs) {
      sb.append(" -").append(s);
    }
    for (String s : slashes) {
      sb.append(" /").append(s);
    }
    for (String s : vals.keySet()) {
      sb.append(" /").append(s).append('=').append(vals.get(s));
    }
    for (String s : files) {
      sb.append(' ').append(s);
    }
    return sb.toString();
  }

  /**
   * on Have we had a +x for this param.
   *
   * @param key String
   * @return boolean
   */
  public boolean on(String key) {
    return (ons.contains(key.toUpperCase()));
  }

  /**
   * off Have we had a -x for this param.
   *
   * @param key String
   * @return boolean
   */
  public boolean off(String key) {
    return (offs.contains(key.toUpperCase()));
  }

  /**
   * onOff
   *
   * Will return true, if defaultValue is true and off(name) is false;
   * Will return true, if defaultValue is false and on(name) is true;
   *
   * @param name
   * @param defaultValue
   * @return boolean
   */
  public boolean onOff(final String key, final boolean defaultValue) {
    return (defaultValue) ? !off(key) : on(key);
  }

  /**
   * val Have we had a /Key=Value for this param.
   *
   * @param key String
   * @return String
   */
  public String val(String key) {
    return (val(key, ""));
  }

  /**
   * val Have we had a /Key=Value for this param.
   *
   * @param key String
   * @return String
   */
  public String val(String key, String defaultVal) {
    String val = getVal(key);
    return ((val != null ? val : defaultVal));
  }

  private String getVal(String key) {
    boolean found = vals.containsKey(key.toUpperCase());
    if (!found) {
      // Did they supply a / at start or = at end?
      if (key.length() >= 1 && key.charAt(0) == '/') {
        key = key.substring(1);
      }
      if (key.length() >= 1 && key.charAt(key.length() - 1) == '=') {
        key = key.substring(0, key.length() - 1);
      }
      found = vals.containsKey(key.toUpperCase());
    }
    return ((found
        ? (String) vals.get(key.toUpperCase()) : null));
  }

  // Get multiple entries.
  public String[] mget(String name) {
     // NB: Used to deliver each key extension e.g. "1", "2" for Key1= and Key2=
     // Now delivers the parameters instead.
    Map<String,String> got = new HashMap<>();
    // Walk all keys.
    for (String key : vals.keySet()) {
      if (key.toLowerCase().startsWith(name.toLowerCase())) {
        // Save the key/value pair.
        got.put(key,vals.get(key));
      }
    }
    // That's how many we have.
    String[] a = new String[got.size()];
    // Roll them out in key order.
    int i = 0;
    for ( String key : new TreeSet<> (got.keySet()) ) {
       a[i++] = got.get(key);
    }
    return a;
  }

  /**
   * Pulls all optional parameters from the args list.
   *
   * @param args The original args.
   */
  public final void read(String[] args) {
    if (args != null) {
      // Read them all.
      for (String arg : args) {
        String argUc = arg.toUpperCase();
        if (arg.length() > 0) {
          //System.err.println("Arg " + i + "=" + args[i]);
          switch (arg.charAt(0)) {
            case '/':
              int split = arg.indexOf('=');
              if (split > 0) {
                // Got an '=' in it.
                vals.put(argUc.substring(1, split),
                    arg.substring(split + 1));
              } else {
                // No '=', just record it.
                slashes.add(arg);
              }
              break;

            case '+':
              ons.add(argUc.substring(1));
              break;

            case '-':
              offs.add(argUc.substring(1));
              break;

            default:
              files.add(arg);
              break;

          }
        }
      }
    }
  }

  /**
   * Args
   *
   * @param args String[]
   */
  public Args(String[] args) {
    read(args);
  }

  /**
   * Args
   */
  public Args() {
  }
}
