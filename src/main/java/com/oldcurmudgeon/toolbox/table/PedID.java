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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * <p>Title: PEDTracker</p>
 * <p>
 * <p>Description: Detect PED movements and raise alert if suspect.</p>
 * <p>
 * <p>Copyright: Copyright (c) 2009</p>
 * <p>
 * <p>Company: Sanderson RBS</p>
 *
 * @author OldCurmudgeon, Richard Perrott
 * @version 1.0
 */
public class PedID extends ID<PedID.Column> {
    // Must be at least one key and it must be static or the Column Enum won't initialise properly.

    static final Map<Column, Type> columnTypes = new EnumMap<>(Column.class);

    public enum Column implements Table.Columns {

        SerialNumber(Type.String),
        Version(Type.String);
        Type type;
        // Accessable from other tables.
        public static final String SerialNumberDbType = "[nchar](20)";
        public static final String VersionDbType = "[nvarchar](300)";
        public static final String AliasDbType = "[nvarchar](50)";

        // Keep track of the column types etc.
        Column(Type type) {
            this.type = type;
        }

        // I hate having to do this!! A perfect example for Enum subclassing.
        // They are final in the hope that they will be inlined.
        @Override
        public final Type getType() {
            return type;
        }

        @Override
        public final String getDbType() {
            return "";
        }

        @Override
        public final boolean getAllowNulls() {
            return false;
        }

    }
    //public static EnumSet<DevicePedTable.Column> dpKey = EnumSet.of(DevicePedTable.Column.SerialNumber);
    //public static EnumSet<PedTable.Column> dKey = EnumSet.of(PedTable.Column.SerialNumber);
    // Use these to copy the right columns from a Fields object.
    //public static Map<Column, DevicePedTable.Column> fromDevicePed = new EnumMap<Column, DevicePedTable.Column>(Column.class);
    //public static Map<Column, PedTable.Column> fromPed = new EnumMap<Column, PedTable.Column>(Column.class);

    private PedID() {
        super(EnumSet.allOf(Column.class));
        // Now safe to fill in the maps because Column is now fully initialised.
        for (Column c : Column.values()) {
            columnTypes.put(c, c.type);
        }
        // Link me in.
        //fromDevicePed.put(Column.SerialNumber, DevicePedTable.Column.SerialNumber);
        //fromPed.put(Column.SerialNumber, PedTable.Column.SerialNumber);
    }

    public PedID(String serialNumber) {
        this();
        setSerialNumber(serialNumber);
    }

    public PedID(String serialNumber, String version) {
        this(serialNumber);
        setVersion(version);
    }

    public PedID(ResultSet resultSet) throws SQLException {
        this();
        setField(Column.SerialNumber, resultSet);
        setField(Column.Version, resultSet);
    }

    public PedID(Fields fields) throws SQLException {
        this();
        /** @todo Make this type-safe by passing a table instead and using the above froms. */
        setFields(fields);
    }

    private void setSerialNumber(String serialNumber) {
        setIDField(Column.SerialNumber, serialNumber);
    }

    private void setVersion(String version) {
        if (version != null) {
            setField(Column.Version, version);
        }
    }

    public String getSerialNumber() {
        return getString(Column.SerialNumber);
    }

    public String getVersion() {
        return getString(Column.Version);
    }

    @Override
    public String toString() {
        String version = getVersion();
        //return this != Unknown ? getSerialNumber() + (version != null && version.length() > 0 ? "," + version : "") : "Unknown";
        return getSerialNumber() + (version != null && version.length() > 0 ? "," + version : "");
    }

    @Override
    public String toID() {
        return toString();
    }

    public String toDb() {
        return toDb(Column.SerialNumber) + "," + toDb(Column.Version);
    }

    public String toCsv() {
        return getSerialNumber() + "," + getVersion();

    }

    public boolean equalsWithoutVersion(PedID eventPedID) {
        String mySerial = getFields().getString(Column.SerialNumber);
        String itsSerial = eventPedID.getFields().getString(Column.SerialNumber);
        return mySerial.equals(itsSerial);
    }

    // A null device ID.
    //public static final PedID Unknown = new PedID("000000000");
}
