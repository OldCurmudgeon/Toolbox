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
package com.oldcurmudgeon.toolbox.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>Title: PEDTracker</p>
 *
 * <p>Description: Detect PED movements and raise alert if suspect.</p>
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: Sanderson RBS</p>
 *
 * @author OldCurmudgeon, Richard Perrott
 * @version 1.0
 */
public class Field<Column extends Enum<Column>> implements Comparable<Field<?>> {
  // The type of this field.

  private Type type;
  // The value of this field.
  private final Object value;
  // Its representation in the db
  private final String forDb;
  // ThreadLocal version for date formatting.
  private static final DbTimeFormat dbTimeFormat = new DbTimeFormat(new SimpleDateFormat("'{ts' ''yyyy-MM-dd HH:mm:ss'''}'"));

  /**
   * Field types I can handle.
   *
   * Each has knowledge of how to gather it's value from a ResultSet.
   */
  public enum Type {

    // String type.
    String() {

      // ResultSet getter.
      @Override
      public Field get(ResultSet rs, String colName) throws SQLException {
        return new Field(rs.getString(colName).trim());
      }
    },
    // Enum type.
    Enum() {

      // ResultSet getter.
      @Override
      public Field get(ResultSet rs, String colName) throws SQLException {
        // Enums are stored in db as Strings and therefore CAN appear in Fields that way.
        // The Fields object handles the conversion depending on how it is accessed.
        return new Field(rs.getString(colName).trim());
      }

      // Comparer.
      @Override
      public int compare(Object a, Object b) {
        int diff;
        // b might be a string but a WILL be an enum.
        //System.out.println("Enum compare a="+a.getClass()+ " b="+b.getClass());
        Field<?> ea = (Field<?>) a;
        if (b instanceof String) {
          // Compare as string.
          diff = ea.toString().compareTo((String) b);
        } else {
          // Compare as enum.
          diff = ea.compareTo((Field<?>) b);
        }
        return diff;
      }
    },
    // Boolean
    Boolean() {

      // ResultSet getter.
      @Override
      public Field get(ResultSet rs, String colName) throws SQLException {
        return new Field(rs.getBoolean(colName));
      }
    },
    // Integer
    Integer() {

      // ResultSet getter.
      @Override
      public Field get(ResultSet rs, String colName) throws SQLException {
        return new Field(rs.getInt(colName));
      }
    },
    // Long
    Long() {

      // ResultSet getter.
      @Override
      public Field get(ResultSet rs, String colName) throws SQLException {
        return new Field(rs.getLong(colName));
      }
    },
    // Timestamp
    Timestamp() {

      // ResultSet getter.
      @Override
      public Field get(ResultSet rs, String colName) throws SQLException {
        return new Field(rs.getTimestamp(colName));
      }

      @Override
      public Object copy(Object value) {
        // Timestamps are mutable so a copy must make a new one.
        Timestamp t = (Timestamp) value;
        return (new Timestamp(t.getTime()));
      }
    };

    // Immutables can just be the original.
    // Override this for any mutables we handle such as Timestamp.
    public Object copy(Object value) {
      return value;
    }

    public boolean equals(Field b) {
      return compare(this, b) == 0;
    }

    // Compare method.
    public int compare(Object va, Object vb) {
      // neither is enum, use the value's compareTo.
      Comparable a = (Comparable) va;
      Comparable b = (Comparable) vb;
      return a.compareTo(b);
    }

    // Getters.
    // From ResultSet. All must implement.
    public abstract Field get(ResultSet rs, String colName) throws SQLException;
  }

  /**
   * Get the value stored in the field.
   *
   * @return Object
   */
  public Object value() {
    return value;
  }

  /**
   * Returns the field in the form it should appear when used in a database
   * query.
   *
   * E.G. 0, 'String', {ts ...}
   *
   * @return String
   */
  public String forDb() {
    return forDb;
  }

  /**
   * Make a new field as a clone of an existing one.
   *
   * @param f Field
   */
  public Field(Field<Column> f) {
    type = f.type;
    // Use the type-specific copy method.
    value = type.copy(f.value);
    forDb = f.forDb;
  }

  /**
   * New field containing the Timestamp.
   *
   * @param timestamp Timestamp
   */
  public Field(Timestamp timestamp) {
    type = Type.Timestamp;
    value = timestamp;
    forDb = dbTimeFormat.format(timestamp);
  }

  /**
   * New field containing the String.
   *
   * @param s String
   */
  public Field(String s) {
    type = Type.String;
    value = s;
    forDb = "'" + s.trim() + "'";
  }

  /**
   * New field containing the Integer.
   *
   * @param i Integer
   */
  public Field(Integer i) {
    type = Type.Integer;
    value = new Integer(i);
    forDb = "" + i;
  }

  /**
   * New field containing the Long.
   *
   * @param l Long
   */
  public Field(Long l) {
    type = Type.Long;
    value = new Long(l);
    forDb = "" + l;
  }

  /**
   * New field containing the Boolean.
   *
   * @param timestamp Timestamp
   */
  public Field(Boolean b) {
    type = Type.Boolean;
    value = b;
    forDb = (b.booleanValue() ? "1" : "0");
  }

  /**
   * New field containing the Enum.
   *
   * @param timestamp Timestamp
   */
  public Field(Enum e) {
    type = Type.Enum;
    value = e;
    forDb = "'" + e + "'";
  }

  /**
   * Equality tester.
   *
   * @param timestamp Timestamp
   */
  public boolean equals(Field<Column> it) {
    return compareTo(it) == 0;
  }

  /**
   * Comparator.
   *
   * @param it Field<?>
   */
  @Override
  public int compareTo(Field<?> it) {
    int diff;
    Field<?> b = (Field<?>) it;
    // If one is an Enum, use it's compare.
    if (type != Type.Enum && b.type == Type.Enum) {
      // I am not an Enum but it is so tell it to compare with me.
      diff = -b.compareTo(this);
    } else {
      diff = type.compare(this.value, b.value);
    }
    // Use it's type-specific comparator.
    return diff;
  }

  /**
   * Thread-safe formatter for a date.
   */
  public static class DbTimeFormat extends ThreadLocal<DateFormat> {

    private final DateFormat f;

    DbTimeFormat(DateFormat f) {
      this.f = f;
    }

    public String format(Date date) {
      return get().format(date);
    }

    public String format(Timestamp date) {
      return get().format(date);
    }

    public Date parse(String date) throws ParseException {
      return get().parse(date);
    }

    @Override
    public DateFormat initialValue() {
      return (DateFormat) f.clone();
    }
  }

  /**
   * Returns the 'natural' form of the value which is the form it should appear
   * in database queries.
   *
   * @return String
   */
  @Override
  public String toString() {
    return forDb;
  }
}
