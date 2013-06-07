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
import java.util.Set;

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
public class ID<Column extends Enum<Column> & Table.Columns> {
  // My Columns.
  // Not really needed but forces the subclass to initialise its columns correctly by doing a EnumSet.allOf(Column.class).

  protected final Set<Column> columns;
  // My fields. Holds the current fields.
  protected final Fields<Column> fields;

  protected ID(Set<Column> columns) {
    this.columns = columns;
    fields = new Fields<>(columns);
  }

  public void setFields(Fields<Column> from) {
    this.fields.copyCorresponding(from);
  }

  public void setIDField(Column column, String value) {
    if (value == null) {
      throw new NullPointerException("Null ID field");
    }
    if (value.length() == 0) {
      throw new IllegalArgumentException("Zero length ID field");
    }
    fields.setField(column, value);
  }

  public void setField(Column column, String s) {
    fields.setField(column, s);
  }

  public void setField(Column column, ResultSet rs) throws SQLException {
    fields.setField(column, rs);
  }

  public String getString(Column column) {
    return fields.getString(column);
  }

  public String toDb(Column column) {
    return fields.getForDb(column);
  }

  public Fields<Column> getFields() {
    return fields;
  }

  public boolean equals(ID it) {
    return toID().equals(it.toID());
  }

  @Override
  public String toString() {
    // Override this always.
    assert (false);
    return "ID";
  }

  public String toID() {
    // Override this always.
    assert (false);
    return "ID";
  }
}
