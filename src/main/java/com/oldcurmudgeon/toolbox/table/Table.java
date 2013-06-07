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

import com.oldcurmudgeon.toolbox.table.Field.Type;
import com.oldcurmudgeon.toolbox.table.Fields.Copy;
import com.oldcurmudgeon.toolbox.walkers.Separator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: Table</p>
 *
 * <p>Description: Do most of the boring work with database tables.</p>
 *
 * A table generally has a number of columns.
 *
 * This is a Generic Table which is designed to be subclassed for specific
 * tables. Much of the normal database drudge is done for you here.
 *
 * I wanted to implement these columns using enums because
 * the functionality of a column fits so well with the character
 * of enums. However, there were some hoops I had to jump through
 * to get there.
 *
 * Each table has a current set of fields, one field for each column. There
 * are utility routines to gather lists of these from a query if needed. If
 * a method updates the fields this will be documented.
 *
 * I am not sure what would happen if you create two Table objects attached
 * to the same table in the same database.
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: Sanderson RBS</p>
 *
 * @param <Column>
 * @author OldCurmudgeon, Richard Perrott
 * @version 1.0
 */
public class Table<Column extends Enum<Column> & Table.Columns> {
  // Name of the table.
  protected final String tableName;
  /**
   * All of the columns in the table. This is actually an EnumSet.
   */
  protected final Set<Column> columns;
  /**
   * Holds a single "Fields" object for various uses.
   *
   * Generally it will hold a "current" set containing the results of the most
   * recent read. Note that some reads will not change the fields. Those that
   * return several "Fields" objects will generally NOT change the "fields",
   * those that return just one will generally fill the "fields" with those
   * read.
   *
   * All the set/get methods of the "Fields" object are proxied in the Table
   * object. It is therefore simple to treat a table as a set of fields.
   */
  protected final Fields<Column> fields;
  /**
   * The database this table is attached to.
   */
  protected Db db;
  // Log
  private static final Logger log = LoggerFactory.getLogger(Table.class);
  // Prettifying stuff.
  static final String CR = "\r\n\t";
  // Emitted between fields.
  static final String COMMA = "," + CR;
  // Natural joins of this table.
  ArrayList<Join> joins = new ArrayList<>();
  // Alias name for the table.
  String alias;

  /**
   * The base interface for all Column enums.
   *
   * Three properties of each column are exposed.
   *
   * These are:
   *
   * Type :- This is the internal type of the column such as Long, String,
   * Timestamp etc. See Field.Type for the currently implemented types.
   *
   * DbType :- How this column is defined when the table is created such as
   * [nchar](10), [datetime] etc.
   *
   * AllowNulls :- Should the table allow nulls in the database for this field.
   */
  public interface Columns {

    /**
     * Interrogates the field type of the column (String/Long/Timestamp etc)
     *
     * @return The field type of the column.
     */
    public Type getType();

    /**
     * Provides the type of this column in the db. ([nchar](10), [datetime] etc.)
     *
     * @return The text used in the database to define the column.
     */
    public String getDbType();

    /**
     * Nulls allowed in db?
     *
     * @return Whether this column should allow nulls in the database.
     */
    public boolean getAllowNulls();
  }

  /**
   * Constructor.
   *
   * Please be sure to call this in your table constructor.
   *
   * @param db The database to connect to.
   * @param tableName The name of the table to use in the database.
   * @param columns The columns in the database table.
   */
  public Table(Db db,
          String tableName,
          String alias,
          Set<Column> columns) {
    this.db = db;
    this.tableName = tableName;
    this.columns = columns;
    this.alias = alias;
    // One set of fields.
    fields = new Fields<>(columns);
  }

  /**
   * Returns all columns.
   *
   * @return
   */
  public final Set<Column> allColumns() {
    return columns;
  }

  /**
   * List the values of all fields as a (kind of) comma-separated list in their
   * db format.
   *
   * Kind-of :- There WILL be a comma but there may also be various white-space
   * characters such as carriage returns and tabs to layout queries readably.
   *
   * @param skipNulls will ignore fields that have null values.
   *
   * @return
   */
  protected String getFields(boolean skipNulls) {
    StringBuilder flds = new StringBuilder();
    Separator comma = new Separator(COMMA);
    for (Column col : columns) {
      Field<Column> field = fields.getField(col);
      if (!skipNulls) {
        // Always emit something.
        flds = flds.append(comma.sep()).append(field != null ? field.forDb() : "<null>");
      } else {
        // Only emit non-null fields.
        if (field != null) {
          flds = flds.append(comma.sep()).append(field.forDb());
        }
      }
    }
    return flds.toString();
  }

  /**
   * Gives the alias if appropriate.
   * Returns nothing if table name/alias unnecessary.
   *
   * @return String
   */
  private String tableName() {
    return alias.equals(tableName) ? "" : alias + ".";
  }

  /**
   * List the names of all of all fields as a comma-separated list.
   *
   * @param columns
   * @param skipNulls will ignore fields that have null values.
   * @return the query
   */
  protected final String getColumns(Set<Column> columns, boolean skipNulls) {
    return getColumns(columns, skipNulls, true);
  }

  /**
   * List the names of all of all fields as a comma-separated list.
   *
   * @param columns
   * @param skipNulls will ignore fields that have null values.
   * @param addJoins add any joins too (probably incompatible with skipNulls = true).
   * @return the query
   */
  protected final String getColumns(Set<Column> columns, boolean skipNulls, boolean addJoins) {
    StringBuilder cols = new StringBuilder();
    Separator comma = new Separator(COMMA);
    for (Column col : columns) {
      if (!skipNulls) {
        // Always emit something.
        cols = cols.append(comma.sep()).append(tableName()).append(col.name());
      } else {
        // Only emit column name for non-null fields.
        if (fields.getField(col) != null) {
          cols = cols.append(comma.sep()).append(tableName()).append(col.name());
        }
      }
    }

    if (!skipNulls && addJoins) {
      // And for each joined table.
      for (Join j : joins) {
        cols.append(comma.sep()).append(j.with.getColumns(skipNulls));
      }
    }
    return cols.toString();
  }

  /**
   * Utility version of the above.
   *
   * Defaults to this table's columns and NOT skipping nulls.
   *
   * @return
   */
  protected final String getColumns() {
    return getColumns(false);
  }

  /**
   * Utility version of the above.
   *
   * Defaults to this table's columns.
   *
   * @param skipNulls
   * @return
   */
  protected final String getColumns(boolean skipNulls) {
    return getColumns(columns, skipNulls);
  }

  /**
   * Clears all changes so far recorded on the fields.
   */
  public void clearChanges() {
    fields.clearChanges();
  }

  /**
   * Returns which fields have been changed.
   *
   * @return java.util.Set
   */
  public Set<Column> getChanges() {
    return fields.getChanges();
  }

  /**
   * Builds an array of "Fields" objects returned by the query.
   *
   * Defaults to this table's columns.
   *
   * @param skipNulls
   * @return
   */
  private void getQueryResults(String query, ArrayList<Fields<Column>> records, Set<Column> avoid) throws SQLException {
    logSql(query);
    Statement s = db.getStatement();
    try (ResultSet rs = s.executeQuery(query)) {
      while (rs.next()) {
        Fields<Column> newFields = new Fields<>(columns, fields);
        readFields(rs, newFields, avoid);
        records.add(newFields);
      }
    }
    db.releaseStatement(s);
  }

  /**
   * Build a query to get all records matching the key.
   *
   * The values in the current set of fields will be used for selection.
   *
   * @return the query.
   */
  private String readQuery(Set<Column> columns, Set<Column> key) {
    String query = "SELECT " + getColumns(columns, false)
            + CR + "FROM " + tableName + " " + alias + getJoins();
    if (key != null) {
      query += CR + "WHERE " + keyValues(key);
    }
    return query;
  }

  /**
   * As above but with two alternate keys.
   *
   * @return the query.
   */
  private String readQuery(Set<Column> columns, Set<Column> key, Set<Column> orKey) {
    String query = "SELECT " + getColumns(columns, false)
            + CR + "FROM " + tableName + " " + alias
            + getJoins()
            + CR + "WHERE (" + keyValues(key) + ") "
            + CR + "OR ( " + keyValues(orKey) + ")";
    return query;
  }

  /**
   * Adds a LEFT JOIN ... for each joined table.
   * 
   * @return String
   */
  private String getJoins() {
    String join = "";
    if (joins != null) {
      for (Join j : joins) {
        join += CR + j.join(alias);
      }
    }
    return join;
  }

  /**
   * Read fields from the passed ResultSet into the "fields" object, ignoring
   * any columns appearing in the "avoid" field. The avoid mechanism is designed
   * to pass a set of key columns which should not be overwritten.
   *
   * @param rs ResultSet
   * @param fields Fields
   * @param avoid Set
   * @throws SQLException
   */
  private void readFields(ResultSet rs, Fields<Column> fields, Set<Column> avoid) throws SQLException {
    for (Column col : columns) {
      // Not key column.
      if (avoid == null || !avoid.contains(col)) {
        fields.setField(col, rs);
      }
    }
  }

  /**
   * Build a query that will delete the record with values from "fields" that
   * also appear in the key.
   *
   * @param key Set
   * @return String
   */
  private String deleteQuery(Set<Column> key) {
    String query = "DELETE FROM " + tableName + CR + "WHERE ";
    query += keyValues(key, false);
    return query;
  }

  /**
   * Truncates the table.
   *
   * @throws SQLException
   */
  public void truncate() throws SQLException {
    //log.warn("Truncating table " + tableName);
    update("TRUNCATE TABLE " + tableName, false);
  }

  private String keyValues(Set<Column> key) {
    return keyValues(key, true);
  }

  /**
   * Builds the part of the query that ensures the final query will only match
   * the fields in the key.
   *
   * @param key Set
   * @return String
   */
  private String keyValues(Set<Column> key, boolean addTableName) {
    StringBuilder vals = new StringBuilder();
    Separator and = new Separator(" AND ");
    String table = addTableName ? tableName() : "";
    for (Column e : key) {
      Field<Column> f = fields.getField(e);
      try {
        // ... AND Col = 'Value'
        vals = vals.append(and.sep()).append(table).append(e.name()).append(" = ").append(f.forDb());
      } catch (NullPointerException npe) {
        throw new NullPointerException("Field " + e + " missing");
      }
    }
    return vals.toString();
  }

  /**
   * Execute the query as an update.
   *
   * Logs a warning if the update did not update anything as this is likely to
   * indicate a bug.
   *
   * @param query String
   * @param warnIfNone boolean
   * @return int - how many records were updated.
   * @throws SQLException
   */
  protected int update(String query, boolean warnIfNone) throws SQLException {
    int updated = 0;
    logSql(query);
    Statement s = db.getStatement();
    try {
      updated = s.executeUpdate(query);
    } catch (SQLException e) {
      log.warn("Query failed: " + query);
      throw e;
    } finally {
      db.releaseStatement(s);
    }
    // Did something happen?
    if (updated != -1 && updated != 1) {
      if (warnIfNone) {
        log.warn("Updated: " + updated);
      } else {
        log.trace("Updated: " + updated);
      }
    }
    return updated;
  }

  /**
   * Build an update query based on the key.
   *
   * Key values will be matched exactly.
   *
   * Any non-null values in the fields that are not in the key will be updated.
   *
   * @param key Set
   * @param columns Set
   * @return String
   */
  private String updateQuery(Set<Column> key, Set<Column> columns) {
    /** @todo Do something else if nothing to update. */
    String query = "UPDATE " + tableName + " SET ";
    // SET - All update columns not in key.
    Separator comma = new Separator(COMMA);
    for (Column col : columns) {
      // Not key column.
      if (!key.contains(col)) {
        Field<Column> field = fields.getField(col);
        // Skip nulls.
        if (field != null) {
          query += comma.sep() + col.name() + " = " + field.forDb();
        }
      }
    }
    // WHERE All columns in key.
    query += CR + "WHERE " + keyValues(key, false);
    return query;
  }

  /**
   * Utility version of the above.
   *
   * Defaults to the table's columns.
   *
   * @param key Set
   * @return String
   */
  private String updateQuery(Set<Column> key) {
    return (updateQuery(key, columns));
  }

  /**
   * Does whatever is necessary to ensure the table exists.
   *
   * Adds indexes from the makeIndexQueries method which can be over-ridden.
   *
   * @throws SQLException
   */
  public void ensureTableExists() throws SQLException {
    if (!db.tableExists(tableName)) {
      // Make the table.
      String query = makeTableQuery();
      update(query, false);
      // Are there any indexes?
      String[] makeIndexQueries = makeIndexQueries();
      if (makeIndexQueries != null) {
        for (int i = 0; i < makeIndexQueries.length; i++) {
          update(makeIndexQueries[i], false);
        }
      }
    }
  }

  /**
   * Return an array of queries that build all indexes for the table.
   *
   * Please override this function if you need indexes on your fields.
   *
   * e.g.
   * <code>
   * @Override
   * protected String[] makeIndexQueries() {
   *  String[] queries = new String[]{
   *    "CREATE UNIQUE INDEX [ID] ON " + tableName + "([SerialNumber], [Version])",
   *    "CREATE UNIQUE INDEX [TimeStamp] ON " + tableName + "([LastUpdated])"
   *  };
   *  return queries;
   * }
   * </code>
   *
   * @return java.lang.String[]
   */
  protected String[] makeIndexQueries() {
    // Override.
    return null;
  }

  /**
   * Inserts a new record in the table as defined by the current contents of
   * "fields".
   *
   * @throws SQLException
   */
  public void insertRecord() throws SQLException {
    String query = insertQuery(true);
    update(query, true);
  }

  /**
   * Builds a query to insert a new record in the table. Passing a "true" value
   * for skipNulls will skip any null fields, thus leaving them at their default
   * value.
   *
   * @param skipNulls boolean
   * @return String
   */
  private String insertQuery(boolean skipNulls) {
    String query = "INSERT INTO " + tableName + CR + "(" + getColumns(skipNulls) + ")";
    query += CR + "VALUES (" + getFields(skipNulls) + ");";
    return query;
  }

  /**
   * Builds a query that will make the table.
   *
   * @return String
   */
  private String makeTableQuery() {
    StringBuilder qry = new StringBuilder().append("CREATE TABLE ").append(tableName).append(CR + "(");
    Separator comma = new Separator(",");
    for (Column col : columns) {
      qry = qry.append(comma.sep()).append(col.name()).append(" ").append(col.getDbType()).append(" ").append(col.getAllowNulls() ? "" : "NOT ").append("NULL");
    }
    qry = qry.append(") ON [PRIMARY]");
    return qry.toString();
  }

  /**
   * Updates the record defined by the key to contain values from the "fields".
   * Returns the number of records updated.
   *
   * @param key Set
   * @return int
   * @throws SQLException
   */
  public int updateRecord(Set<Column> key) throws SQLException {
    String query = updateQuery(key);
    return update(query, true);
  }

  /**
   * Updates the record defined by the key to contain values from the "fields".
   * Returns the number of records updated.
   *
   * Only updates fields found in the "columns" parameter.
   *
   * @param key
   * @param columns
   * @return
   * @throws SQLException
   */
  public int updateRecord(Set<Column> key, Set<Column> columns) throws SQLException {
    String query = updateQuery(key, columns);
    return update(query, true);
  }

  /**
   * Deletes the record specified by the key.
   *
   * A warning will be logged if no record was found to update.
   *
   * Returns the number of records deleted.
   *
   * @param key Set
   * @return int
   * @throws SQLException
   */
  public int deleteRecord(Set<Column> key) throws SQLException {
    return deleteRecord(key, true);
  }

  /**
   * Deletes the record specified by the key.
   *
   * If the "warnifNone" parameter is true, a warning will be logged if no
   * record was found to update.
   *
   * Returns the number of records deleted.
   *
   * @param key Set
   * @param warnIfNone boolean
   * @return int
   * @throws SQLException
   */
  public int deleteRecord(Set<Column> key, boolean warnIfNone) throws SQLException {
    String query = deleteQuery(key);
    return update(query, warnIfNone);
  }

  /**
   * Reads a single record from the table as defined by the key.
   *
   * Updates "fields" with the content of the record if one is read and returns
   * it.
   *
   * If more than one record matches or no records match then no action will be
   * taken and null will be returned.
   *
   * @param key Set
   * @return Fields
   * @throws SQLException
   */
  public Fields<Column> readRecord(Set<Column> key) throws SQLException {
    Fields<Column> red = null;
    ArrayList<Fields<Column>> records = readRecords(key);
    if (records != null && records.size() == 1) {
      red = records.get(0);
      // Keep those.
      copy(red);
    }
    // Return the last one.
    return red;
  }

  /**
   * Reads a single record from the table as defined by the id and the key.
   *
   * Updates "fields" with the content of the record if one is read and returns
   * it.
   *
   * If more than one record matches or no records match then no action will be
   * taken and null will be returned.
   *
   * @param key Set
   * @return Fields
   * @throws SQLException
   */
  public Fields<Column> readRecord(ID id, Set<Column> key) throws SQLException {
    // Set up the key.
    copyCorresponding(id);
    // Remember any changes.
    clearChanges();
    // Do the read.
    return readRecord(key);
  }

  /**
   * Reads all records matching the given key.
   *
   * @param columns Set
   * @param key Set
   * @return java.util.ArrayList
   * @throws SQLException
   */
  public ArrayList<Fields<Column>> readRecords(Set<Column> columns, Set<Column> key) throws SQLException {
    ArrayList<Fields<Column>> records = new ArrayList<>();
    String query = readQuery(columns, key);
    getQueryResults(query, records, null);
    return records;
  }

  /**
   * Utility version of the above.
   *
   * Defaults the columns to the table's columns.
   *
   * @param key Set
   * @return java.util.ArrayList
   * @throws SQLException
   */
  public ArrayList<Fields<Column>> readRecords(Set<Column> key) throws SQLException {
    return readRecords(columns, key);
  }

  /**
   * Reads ALL of the records from the table.
   *
   * @return
   * @throws SQLException
   */
  public ArrayList<Fields<Column>> readRecords() throws SQLException {
    return readRecords(columns, null);
  }

  /**
   * Enhanced version of above using two keys.
   *
   * @param columns Set
   * @param key1 Set
   * @param orKey2 Set
   * @return java.util.ArrayList
   * @throws SQLException
   */
  public ArrayList<Fields<Column>> readRecords(Set<Column> columns, Set<Column> key1, Set<Column> orKey2) throws SQLException {
    ArrayList<Fields<Column>> records = new ArrayList<>();
    String query = readQuery(columns, key1, orKey2);
    getQueryResults(query, records, null);
    return records;
  }

  /**
   * Count the records in the table matching the specified "WHERE" clause.
   *
   * @param where
   * @return
   * @throws SQLException
   */
  public int count(String where) throws SQLException {
    int count = 0;
    String query = countQuery(where);
    // Probably don't want it logged.
    //logQuery = false;
    //logSql(query);
    Statement s = null;
    ResultSet rs = null;
    try {
      s = db.getStatement();
      rs = s.executeQuery(query);
      // Scan each record.
      while (rs.next()) {
        count += rs.getInt(1);
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (s != null) {
        db.releaseStatement(s);
      }
    }
    return count;
  }

  /**
   * Builds a query to count the records in the table that match the specified
   * "WHERE" clause.
   *
   * @param where String
   * @return String
   */
  private String countQuery(String where) {
    String query = "SELECT COUNT(*) FROM " + tableName;
    if (where != null) {
      query += " WHERE " + where;
    }
    return query;
  }

  /**
   * Sort the records on a specific column.
   *
   * @param records ArrayList
   * @param sortColumn Column
   */
  public void sortRecords(ArrayList<Fields<Column>> records, Column sortColumn) {
    if (records.size() > 1) {
      fields.sort(records, sortColumn);
    }
  }

  /**
   * Inserts an identity record, returning its new, unique ID.
   *
   * @param idColumn Column
   * @return long
   * @throws SQLException
   */
  public long insertIdentityRecord(Column idColumn) throws SQLException {
    long identity = -1;
    String query = insertQuery(true);
    logSql(query);
    Statement s = db.getStatement();
    s.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
    ResultSet rs = s.getGeneratedKeys();
    // Scan the record.
    if (rs.next()) {
      identity = rs.getLong(1);
    }
    db.releaseStatement(s);
    return identity;
  }

  /**
   * Copy all fields from the supplied table into this table. Various Copy
   * methods are available.
   *
   * Note that only column names are matched, not the enums.
   *
   * Use the "corresponding" copy methods to copy between tables.
   *
   * @param from Table
   * @param copy Copy
   */
  public void copyCorresponding(Table from, Copy copy) {
    copyCorresponding(from.getFields(), copy);
  }

  /**
   * As above but defaults to copy all.
   *
   * @param from
   */
  public void copyCorresponding(Table from) {
    copyCorresponding(from, Copy.Exact);
  }

  /**
   * As above but from an ID.
   *
   * @param id ID
   */
  public void copyCorresponding(ID id) {
    fields.copyCorresponding(id.getFields());
  }

  /**
   * As above but copies from a Fields object rather than a table.
   *
   * @param fromFields
   * @param copy
   */
  public void copyCorresponding(Fields fromFields, Copy copy) {
    fields.copyCorresponding(fromFields, copy);
  }

  /**
   * As above but copies Exact by default.
   *
   * @param fromFields
   */
  public void copyCorresponding(Fields fromFields) {
    fields.copyCorresponding(fromFields, Copy.Exact);
  }

  /**
   * Copy from a fields object.
   *
   * @param from
   * @param type
   */
  public void copy(Fields<Column> from, Copy type) {
    // Copy the fields into mine.
    fields.copy(from, type);
  }

  /**
   * As above but copies Exact by default.
   *
   * @param from
   */
  public void copy(Fields<Column> from) {
    fields.copy(from);
  }

  /**
   * Override this method to add further defaults.
   *
   * Called after every "reset" call.
   *
   */
  public void setDefaults() {
    // Override.
  }

  /**
   * Clears down all fields.
   */
  public void reset() {
    fields.clear();
    setDefaults();
  }

  /**
   * Get all fields.
   *
   * @return The current fields.
   */
  public final Fields<Column> getFields() {
    return fields;
  }

  /**
   * Get a specific field.
   *
   * The returned field can be null when (ToDo)
   *
   * @param column The column to get the field from.
   * @return The requested field.
   */
  public final Field<Column> getField(Column column) {
    return fields.getField(column);
  }

  /**
   * Get the value of a field.
   *
   * Not used but shows the technique.
   *
   * @param column
   * @return the value
   */
  @Deprecated
  private Object getValue(Column column) {
    Field<Column> f = getField(column);
    return f != null ? f.value() : null;
  }

  /**
   * Gets the field as a String.
   *
   * @param column
   * @return the field
   */
  public final String getString(Column column) {
    return fields.getString(column);
  }

  /**
   * Gets the field as a Boolean
   *
   * @param column
   * @return the field
   */
  public final Boolean getBoolean(Column column) {
    return fields.getBoolean(column);
  }

  /**
   * Gets the field as an Enum of the offered type.
   *
   * @param column
   * @param sampleEnum
   * @return the field
   */
  public final Enum getEnum(Column column, Enum sampleEnum) {
    return fields.getEnum(column, sampleEnum);
  }

  /**
   * Gets the field as an Integer.
   *
   * @param column
   * @return
   */
  public final Integer getInteger(Column column) {
    return fields.getInteger(column);
  }

  /**
   * Gets the field as a Long.
   *
   * @param column
   * @return
   */
  public final Long getLong(Column column) {
    return fields.getLong(column);
  }

  /**
   * Gets the field as a Timestamp
   *
   * @param column
   * @return
   */
  public final Timestamp getTimestamp(Column column) {
    return fields.getTimestamp(column);
  }

  /**
   * Set a field.
   *
   * @param column Column
   * @param f Field
   */
  public void setField(Column column, Field<Column> f) {
    fields.setField(column, f);
  }

  /**
   * Set a field from a String.
   *
   * @param column
   * @param s
   */
  public void setField(Column column, String s) {
    fields.setField(column, s);
  }

  /**
   * Set a field from a Boolean.
   *
   * @param column
   * @param b
   */
  public void setField(Column column, Boolean b) {
    fields.setField(column, b);
  }

  /**
   * Set a field from a boolean.
   *
   * @param column
   * @param b
   */
  public void setField(Column column, boolean b) {
    fields.setField(column, b);
  }

  /**
   * Set a field from a Long.
   *
   * @param column
   * @param l
   */
  public void setField(Column column, Long l) {
    fields.setField(column, l);
  }

  /**
   * Set a field from a long.
   *
   * @param column
   * @param l
   */
  public void setField(Column column, long l) {
    fields.setField(column, l);
  }

  /**
   * Set a field from an Enum.
   *
   * @param column
   * @param e
   */
  public void setField(Column column, Enum e) {
    fields.setField(column, e);
  }

  /**
   * Set a field from a Timestamp.
   *
   * @param column
   * @param ts
   */
  public void setField(Column column, Timestamp ts) {
    fields.setField(column, ts);
  }

  /**
   * Clear a field.
   *
   * @param which
   */
  public void clear(Set<Column> which) {
    fields.clear(which);
  }
  /**
   * A one-shot request not to log the following query.
   *
   * Used primarily for polling queries.
   *
   * Will automatically revert to "true" once the next query
   * has been done.
   */
  public boolean logQuery = true;
  /**
   * The query that was not logged because the logQuery flag was set.
   */
  public String notLogged = "";

  /**
   * Log an sql query.
   *
   * @param query String
   */
  public void logSql(String query) {
    if (logQuery) {
      log.trace("SQL[" + tableName + "]: " + query);
    } else {
      // Postpone for later.
      notLogged = query;
      // One-shot only.
      logQuery = true;
    }
  }

  /**
   * Simple narrative on the state of the table.
   *
   * @return java.lang.String
   */
  @Override
  public String toString() {
    return "Table: \"" + tableName + "\" Fields: " + fields;
  }

  /**
   * Uses the UNION ALL trick (or similar) to speed up updates.
   *
   * You must supply the columns you plan to update to the constructor.
   *
   * They must be in the same order as they are in the table.
   *
   * You must ONLY fill these fields at each call of add.
   */
  protected class QuickInserter {
    // Grabs that many before forcing an emit.

    protected static final int MAX = 500;
    // The main part of the query is grown here.
    protected final StringBuilder s = new StringBuilder();
    // The header bit INSERT INTO ...
    protected final String insert;
    // The trailer ...
    protected final String end;
    // How many we have added so far.
    protected int count = 0;
    // Separator for each entry.
    protected final Separator sep;
    // The fields.
    protected EnumSet<Column> fields;

    /** 
     * e.g. (2005)
      INSERT INTO MyTable (FirstCol, SecondCol)
      SELECT 'First' ,1
      UNION ALL SELECT 'Second' ,2
      UNION ALL SELECT 'Third' ,3
      UNION ALL SELECT 'Fourth' ,4
      UNION ALL SELECT 'Fifth' ,5

     * e.g. (2008)
      INSERT INTO MyTable (FirstCol, SecondCol)
      VALUES ('First',1),
      ('Second',2),
      ('Third',3),
      ('Fourth',4),
      ('Fifth',5)
     */


    public QuickInserter(EnumSet<Column> fields) {
      this.fields = fields;
      switch ( db.type ) {
        case Sql2005:
          insert = "INSERT INTO " + tableName + "(" + getColumns(fields, false, false) + ")\r\nSELECT ";
          sep = new Separator("\r\nUNION ALL SELECT ");
          end = "";
          break;
          
        case Sql2008:
          insert = "INSERT INTO " + tableName + "(" + getColumns(fields, false, false) + ")\r\nVALUES (";
          sep = new Separator("),\r\n(");
          end = ")";
          break;
          
        default:
          throw new IllegalStateException ("Unrecognised db type.");
      }
    }
    
    public void add() throws SQLException {
      s.append(sep.sep()).append(getFields(true));
      // Count them.
      count += 1;
      // After every 100 or so ...
      if (count >= MAX) {
        // Make it so.
        update();
        // Start again.
        reset();
      }
    }

    public void close() throws SQLException {
      if (count > 0) {
        // Force an update if anything left to update.
        update();
      }
    }

    private void update() throws SQLException {
      logQuery = false;
      // Call owner's update.
      s.append(end);
      Table.this.update(s.insert(0, insert).toString(), false);
    }

    private void reset() {
      // Reset.
      count = 0;
      sep.reset();
      s.setLength(0);
    }
  }

}
