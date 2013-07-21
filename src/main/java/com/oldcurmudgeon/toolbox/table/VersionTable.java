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

import com.oldcurmudgeon.toolbox.containers.Params;
import com.oldcurmudgeon.toolbox.table.Field.Type;
import com.oldcurmudgeon.toolbox.twiddlers.Sleeps;
import java.sql.SQLException;
import java.util.EnumSet;

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
public class VersionTable extends Table<VersionTable.Column> {
  // Access tool params.
  Params params = Params.getParams();

  public enum Column implements Table.Columns {

    Version(Type.String, PedID.Column.VersionDbType, false),
    Alias(Type.String, PedID.Column.AliasDbType, false);

    final Type type;
    final String dbType;
    final boolean allowNulls;

    // Keep track of the column types etc.
    Column(Type type, String dbType, boolean allowNulls) {
      this.type = type;
      this.dbType = dbType;
      this.allowNulls = allowNulls;
    }

    // I hate having to do this!! A perfect example for Enum subclassing.
    // They are final in the hope that they will be inlined.
    @Override
    public final Type getType () {
      return type;
    }

    @Override
    public final String getDbType () {
      return dbType;
    }

    @Override
    public final boolean getAllowNulls () {
      return allowNulls;
    }

  }

  public String getAlias(String version) throws SQLException {
    String a = lookupAlias(version);
    // Null means not seen yet.
    if (a == null) {
      a = newAlias(version);
    }
    return a;
  }
  private final String UNKNOWN = "Unknown-";

  private String newAlias(String version) {
    String newAlias = defaultAlias(version);
    if (newAlias == null) {
      newAlias = UNKNOWN + System.currentTimeMillis();
    }
    reset();
    setField(Column.Version, version);
    boolean success = false;
    do {
      try {
        setField(Column.Alias, newAlias);
        insertRecord();
        success = true;
      } catch (SQLException e) {
        // Try again.
        Sleeps.oneTick();
        newAlias = UNKNOWN + System.currentTimeMillis();
      }
    } while (!success);
    return newAlias;
  }

  private String defaultAlias(String version) {
    String param = "Alias" + version.replaceAll("=", "#");
    String dflt = params.get(param, null);
    return dflt;
  }

  private String lookupAlias(String version) throws SQLException {
    String a = null;
    reset();
    setField(Column.Version, version);
    Fields<Column> f = readRecord(versionKey);
    if (f != null) {
      Field<Column> aliasField = getField(Column.Alias);
      if (aliasField != null) {
        a = aliasField.value().toString();
      }
    }
    return a;
  }
  // Keys.
  public static final EnumSet<Column> versionKey = EnumSet.of(
          Column.Version);
  public static final EnumSet<Column> aliasKey = EnumSet.of(
          Column.Alias);

  public VersionTable(Db db) {
    super(db, "Versions", "V", EnumSet.allOf(Column.class));
  }

  @Override
  protected String[] makeIndexQueries() {
    String[] queries = new String[]{
      "CREATE UNIQUE INDEX [Version] ON " + tableName + "([Version] ASC)",
      "CREATE UNIQUE INDEX [Alias] ON " + tableName + "([Alias] ASC)"
    };
    return queries;
  }

  @Override
  public void setDefaults() {
    // Defaults.
  }
}
