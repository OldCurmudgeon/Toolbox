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
import java.util.*;

/**
 * <p>Title: PEDTracker</p>
 *
 * <p>Description: Fields holds values for each column.</p>
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: Sanderson RBS</p>
 *
 * @author OldCurmudgeon, Richard Perrott
 * @version 1.0
 */
public class Fields<Column extends Enum<Column> & Table.Columns> {
   // The columns each field relates to.

   protected final Set<Column> columns;
   // EnumMap of the fields, one for each column.
   protected final EnumMap<Column, Field<Column>> fields;
   // EnumSet of what has changed.
   protected final EnumSet<Column> changed;
   // Map from column name to column enum.
   private final Map<String, Column> columnNames;

   /**
    * Build a set of fields, one for each column.
    *
    * @param columns Set
    */
   public Fields(Set<Column> columns) {
      // Those are my columns.
      this.columns = columns;

      // A sample column used to find the universe of the enum of Columns.
      Column sampleCol = columns.iterator().next();

      // Build the column names map.
      columnNames = new HashMap<>();
      Set<Column> allColumns = EnumSet.allOf(sampleCol.getDeclaringClass());
      // Fill it.
      for (Column c : allColumns) {
         columnNames.put(c.name(), c);
      }

      // I want to make fields an EnumMap for efficiency.
      // This is a wierd way of doing it - if you can think
      // of a better way please replace this code.

      // Make a template HashMap for the EnumMap
      HashMap<Column, Field> template = new HashMap<>();
      // Add one item to it. Assumes there is at least one column, which makes sense.
      // Doesnt matter what field type I put in it.
      template.put(sampleCol, new Field<Column>(0));
      // Build the EnumMap from the template HashMap.
      fields = new EnumMap(template);
      // Clear out my sample entry.
      fields.clear();

      // Start the changed set empty.
      changed = EnumSet.noneOf(sampleCol.getDeclaringClass());
   }

   /**
    * Take a clone of an existing set of fields.
    *
    * @param columns Set
    * @param from Fields
    */
   public Fields(Set<Column> columns, Fields<Column> from) {
      this(columns);
      copy(from, Copy.Exact, null);
   }

   /**
    * Take a copy of all fields in "from" that also appear in "which" (unless
    * "which" is null, in which case all of them) using the specified copy
    * method.
    *
    * @param from Fields
    * @param type Copy
    * @param which Set
    * @return Fields
    */
   public final Fields copy(Fields<Column> from, Copy type, Set<Column> which) {
      // Look at every column.
      for (Column column : columns) {
         // Only the ones specified. null means all.
         if (which == null || which.contains(column)) {
            copy(from.getField(column), column, type);
         }
      }
      return this;
   }

   /**
    * Utility method of the above. Defaults to exact copy of all columns.
    *
    * @param from Fields
    * @return Fields
    */
   public Fields copy(Fields<Column> from) {
      return copy(from, Copy.Exact);
   }

   /**
    * Utility method of the above. Defaults to all columns.
    *
    * @param from Fields
    * @param type Copy
    * @return Fields
    */
   public Fields copy(Fields<Column> from, Copy type) {
      return copy(from, type, null);
   }

   /**
    * Utility method of the above. Defaults to exact copy.
    *
    * @param from Fields
    * @param which Set
    * @return Fields
    */
   public Fields copy(Fields<Column> from, Set<Column> which) {
      return copy(from, Copy.Exact, which);
   }

   /**
    * Copy a single field using the specified copy method, keeping track of any
    * changes in the "changed" set.
    *
    * @param f Field
    * @param column Column
    * @param type Copy
    */
   private void copy(Field<Column> f, Column column, Copy type) {
      Field<Column> t = fields.get(column);
      if (type.shouldCopy(f == null, t == null)) {
         if (f != null) {
            // Clone it.
            changeField(column, new Field<>(f));
         } else {
            // It's null! Bin it.
            if (fields.containsKey(column)) {
               changed.add(column);
               fields.remove(column);
            }
         }
      }
   }

   /**
    * Copy fields which have the same name.
    *
    * Generally used to copy between tables.
    *
    * @param fromFields Fields
    * @param type Copy
    */
   public void copyCorresponding(Fields<Column> fromFields, Copy type) {
      // Walk its field names.
      for (Column from : fromFields.columns) {
         // Is it in my enum?
         Column column = findColumn(from.name());
         if (column != null) {
            // Same name! Copy it.
            copy(fromFields.getField(from), column, type);
         }
      }
   }

   /**
    * Utility method of the above. Default to "Exact" copy.
    *
    * @param fromFields Fields
    */
   public void copyCorresponding(Fields<Column> fromFields) {
      copyCorresponding(fromFields, Copy.Exact);
   }

   /**
    * Find a column with the specified name. Returns null if no column with that
    * name exists.
    *
    * @param name String
    * @return Column
    */
   private Column findColumn(String name) {
      // Will return null if no column of that name.
      return columnNames.get(name);
   }

   /**
    * Keeps track of which columns have changed.
    *
    * @param c Column
    * @param f Field
    */
   private void trackChanges(Column c, Field f) {
      if (f != null) {
         // What was it?
         Field old = fields.get(c);
         if (old != null) {
            // Compare them.
            // Cant use old.equals because we may be doing a copyCorresponding.
            if (!old.forDb().equals(f.forDb())) {
               // The DB form of the field has changed!
               changed.add(c);
            }
         } else {
            // Brand new! It HAS changed.
            changed.add(c);
         }
      } else {
         // Probably should do something here.
         // Replacing something with null should probably be recorded.
         // @toDo
      }
   }

   /**
    * Clear down all fields to null and reset changed statii.
    */
   public void clear() {
      // Remove all fields.
      fields.clear();
      clearChanges();
   }

   /**
    * Reset "changed" statii.
    */
   public void clearChanges() {
      changed.clear();
   }

   /**
    * What has changed since the last reset.
    *
    * @return Set
    */
   public Set<Column> getChanges() {
      return changed;
   }

   /**
    * Clear to null all the specified columns.
    *
    * @param which Set
    */
   public void clear(Set<Column> which) {
      // Look at every column.
      for (Column column : columns) {
         // Only the ones specified.
         if (which.contains(column)) {
            // Bin it.
            fields.remove(column);
            changed.add(column);
         }
      }
   }

   /**
    * Are all of the fields empty.
    *
    * @return boolean
    */
   public boolean isEmpty() {
      return fields.isEmpty();
   }

   /**
    * Get a specific field.
    *
    * @param column Column
    * @return Field
    */
   public Field<Column> getField(Column column) {
      return fields.get(column);
   }

   /**
    * Get the value of a specific field. Returns null if the field does not
    * contain a value or it contains the value null.
    *
    * @param column Column
    * @return Object
    */
   private Object getValue(Column column) {
      Field<Column> f = getField(column);
      return f != null ? f.value() : null;
   }

   /**
    * Get a String value of the field.
    *
    * If the field is not a String then a ClassCast exception may be thrown.
    *
    * @param column Column
    * @return String
    */
   public String getString(Column column) {
      return (String) getValue(column);
   }

   /**
    * Get a Boolean value of the field.
    *
    * If the field is not a Boolean then a ClassCast exception may be thrown.
    *
    * @param column Column
    * @return Boolean
    */
   public Boolean getBoolean(Column column) {
      return (Boolean) getValue(column);
   }

   /**
    * Get an Enum value of the field.
    *
    * If the field is not an Enum then a ClassCast exception may be thrown.
    *
    * The SampleEnum must be provided as a lookup for the field. If the field
    * does not contain an enum of the type passed then a IllegalArgumentException
    * may be thrown.
    *
    * @param column Column
    * @return Enum
    */
   public Enum getEnum(Column column, Enum sampleEnum) {
      Object e = getValue(column);
      // In case the field has been read from the db and is therefore a String.
      if (e instanceof String) {
         e = Enum.valueOf(sampleEnum.getClass(), (String) e);
      }
      return (Enum) e;
   }

   /**
    * Get an Integer value of the field.
    *
    * If the field is not an Integer then a ClassCast exception may be thrown.
    *
    * @param column Column
    * @return Integer
    */
   public Integer getInteger(Column column) {
      return (Integer) getValue(column);
   }

   /**
    * Get a Long value of the field.
    *
    * If the field is not a Long then a ClassCast exception may be thrown.
    *
    * @param column Column
    * @return Long
    */
   public Long getLong(Column column) {
      return (Long) getValue(column);
   }

   /**
    * Get a Timestamp value of the field.
    *
    * If the field is not a Timestamp then a ClassCast exception may be thrown.
    *
    * @param column Column
    * @return Timestamp
    */
   public Timestamp getTimestamp(Column column) {
      return (Timestamp) getValue(column);
   }

   /**
    * Get the value of the field as it should appear when presented to the
    * database.
    * E.g. 0, 'String' or {ts ...}.
    *
    * @param column Column
    * @return String
    */
   public String getForDb(Column column) {
      return getField(column).forDb();
   }

   /**
    * Set a field's value from another field.
    * Takes a copy of the offered field.
    *
    * @param column Column
    * @param f Field
    */
   public void setField(Column column, Field<Column> f) {
      changeField(column, new Field<>(f));
   }

   /**
    * Set a field's value from a String.
    * Takes a copy of the offered field.
    *
    * @param column Column
    * @param s String
    */
   public void setField(Column column, String s) {
      changeField(column, new Field<Column>(s));
   }

   /**
    * Set a field's value from a Boolean.
    * Takes a copy of the offered field.
    *
    * @param column Column
    * @param b Boolean
    */
   public void setField(Column column, Boolean b) {
      changeField(column, new Field<Column>(b));
   }

   /**
    * Set a field's value from an Enum.
    * Takes a copy of the offered field.
    *
    * @param column Column
    * @param b Boolean
    */
   public void setField(Column column, Enum e) {
      changeField(column, new Field<Column>(e));
   }

   /**
    * Set a field's value from a Timestamp.
    * Takes a copy of the offered field.
    *
    * @param column Column
    * @param b Boolean
    */
   public void setField(Column column, Timestamp ts) {
      changeField(column, new Field<Column>(ts));
   }

   /**
    * Set a field's value from a long.
    * Takes a copy of the offered field.
    *
    * @param column Column
    * @param b Boolean
    */
   public void setField(Column column, Long l) {
      changeField(column, new Field<Column>(l));
   }

   /**
    * Set a field's value from a ResultSet.
    * Takes a copy of the offered field.
    *
    * @param column Column
    * @param b Boolean
    */
   public void setField(Column column, ResultSet rs) throws SQLException {
      // Dont collect nulls.
      if (rs.getObject(column.name()) != null) {
         // Use the ResultSet getter built into the type.
         Field<Column> newField = column.getType().get(rs, column.name());
         changeField(column, newField);
      } else {
         // Remove it if it was null in db.
         if (fields.containsKey(column)) {
            fields.remove(column);
            changed.add(column);
         }
      }
   }

   /**
    * Replace a field with a new value.
    *
    * @param column Column
    * @param newField Field
    */
   private void changeField(Column column, Field<Column> newField) {
      trackChanges(column, newField);
      fields.put(column, newField);
   }

   /**
    * Lists all fields and their values and summarises the changed set.
    *
    * @return String
    */
   @Override
   public String toString() {
      return fields + "\tChanged: " + changed;
   }

   /**
    * Sorts the array of fields on the given column.
    *
    * @param records ArrayList
    * @param sortColumn Column
    */
   public void sort(ArrayList<Fields<Column>> records, final Column sortColumn) {
      Collections.sort(records, new Comparator() {
         // Sort records by a particular column.

         @Override
         public int compare(Object oa, Object ob) {
            Fields<Column> a = (Fields<Column>) oa;
            Fields<Column> b = (Fields<Column>) ob;
            return (a.compareTo(b, sortColumn));
         }
      });

   }

   /**
    * Compare two "Fields" objects by comparing the specified column.
    *
    * @param fb Fields
    * @param column Column
    * @return int
    */
   private int compareTo(Fields<Column> fb, Column column) {
      Field<Column> a = getField(column);
      Field<Column> b = fb.getField(column);
      return a.compareTo(b);
   }

   /**
    * Allowed Copy methods.
    */
   public enum Copy {

      Exact {

         @Override
         boolean shouldCopy(boolean fromNull, boolean toNull) {
            return true;
         }
      },
      WhereTargetFieldsArePresent {

         @Override
         boolean shouldCopy(boolean fromNull, boolean toNull) {
            return !toNull;
         }
      },
      WhereSourceFieldsArePresent {

         @Override
         boolean shouldCopy(boolean fromNull, boolean toNull) {
            return !fromNull;
         }
      },
      WhereBothFieldsArePresent {

         @Override
         boolean shouldCopy(boolean fromNull, boolean toNull) {
            return !fromNull && !toNull;
         }
      },
      ReplaceNulls {

         @Override
         boolean shouldCopy(boolean fromNull, boolean toNull) {
            return toNull;
         }
      },
      NullsOnly {

         @Override
         boolean shouldCopy(boolean fromNull, boolean toNull) {
            return fromNull;
         }
      };

      // Each must decide whether to copy or not.
      abstract boolean shouldCopy(boolean fromNull, boolean toNull);
   }
}
