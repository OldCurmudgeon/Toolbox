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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
public class Db {
    // A logger.
    private static final Logger log = LoggerFactory.getLogger(Db.class);
    // Acces tool params.
    Params params = Params.getParams();
    // I'm the only one. (Singleton).
    private static Db me = null;

    // The connection.
    private Connection connection = null;
    // The statement (reused throughout).
    //private boolean statementInUse = false;
    private final Semaphore statementInUse = new Semaphore(1);
    private Statement statement = null;
    // My tables.
    //public DeviceTable deviceTable = new DeviceTable(this);

    // Type of db attached to.
    public enum DbType {
        Sql6("6"),
        Sql7("7"),
        Sql2000("8"),
        Sql2005("9"),
        Sql2008("10");

        String version;

        DbType(String version) {
            this.version = version;
        }
    }

    public DbType type = null;

    private Db() {
    }

    public static Db getDb() throws SQLException, ClassNotFoundException {
        if (me == null) {
            // It's ok, we can call this constructor
            me = new Db();
            // Start everything up.
            me.init();
        }
        return me;
    }

    protected void init() throws SQLException, ClassNotFoundException {
        // Connect me to the database.
        connection = openConnection();
        // What type of connection?
        type = getDbType(connection);
        // Build my statement.
        statement = connection.createStatement();
        statement.setCursorName("Cursor");
        // Check everything is there.
        //devicePedTable.ensureTableExists();
    }

    private DbType getDbType(Connection connection) throws SQLException {
        DbType dbType = null;
        String version = connection.getMetaData().getDatabaseProductVersion();
        // Which one is it.
        for (DbType t : DbType.values()) {
            if (version.startsWith(t.version)) {
                dbType = t;
            }
        }
        return dbType;
    }


    public void close() throws SQLException {
        if (statement != null) {
            if (statementInUse.availablePermits() != 1) {
                log.warn("You didn't release the statement!!");
            }
            statement.close();
            statement = null;
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
        log.trace("Db connection closed.");
    }

    private Connection openConnection() throws ClassNotFoundException, SQLException {
        // Grab all my details.
        String driver = params.get("DbDriver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = params.get("DbURL", "jdbc:sqlserver://localhost:1433;SelectMethod=cursor;DatabaseName=PEDS");
        String user = params.get("DbUser", "sa");
        String password = params.get("DbPassword", "instore");
        // Connect to the database.
        log.trace("Db Select driver:" + driver);
        //Class c = Class.forName(driver);
        log.trace("Db Connect:" + url);
        Class.forName(driver);
        Connection c = DriverManager.getConnection(url, user, password);
        log.trace("Db Connected: " + c.getMetaData().getDatabaseProductName());
        return c;
    }

    /**
     * lock
     *
     * @return boolean
     */
    private static final Lock lock = new ReentrantLock();

    public boolean lock() {
        lock.lock();
        /** @todo Find a way to lock the database.
         * Probably lock the DevicePEDs table. */
        return true;
    }

    /**
     * unlock
     */
    public void unlock() {
        lock.unlock();
    }

    /**
     * beginTransaction
     *
     * @return boolean
     */
    public boolean beginTransaction() throws SQLException {
        // NB: *** It is your responsibility to ALWAYS call endTransaction or rollbackTransaction unless aborting the whole application.
        // Stop all autocommit so we can rollback.
        connection.setAutoCommit(false);
        // Begin the transaction. Unnecessary ... jdbc seems to do this itself.
        //execute ( "BEGIN TRANSACTION" );
        return true;
    }

    /**
     * endTransaction
     */
    public void endTransaction() throws SQLException {
        // End the transaction.
        //execute( "COMMIT TRANSACTION" );
        // Commit.
        connection.commit();
        // Back to autocommit.
        connection.setAutoCommit(true);
    }

    /**
     * rollBackTransaction
     */
    public void rollbackTransaction() throws SQLException {
        // Roll back the transaction.
        //execute( "ROLLBACK TRANSACTION" );
        // Roll everything back.
        connection.rollback();
        // Back to autocommit.
        connection.setAutoCommit(true);
    }

    private void checkStatement(int count) throws IllegalStateException {
        if (statementInUse.availablePermits() != count) {
            throw new IllegalStateException("Permits = " + statementInUse.availablePermits() + " should be " + count + " ... will probably block. " + Thread.currentThread().getName());
        }
    }

    /**
     * createStatement
     *
     * @return Statement
     */
    private final Object lockGet = new Object();

    public Statement getStatement() {
        Statement s = null;
        synchronized (lockGet) {
            try {
                if (false) {
                    // Useful for debugging race conditions.
                    try {
                        //log.trace("Acquire: " + log.getStack());
                        checkStatement(1);
                    } catch (IllegalStateException ex) {
                        log.debug(ex.toString(), ex);
                    }
                }
                statementInUse.acquire();
                log.trace("Statement locked !! " + Thread.currentThread().getName());
                s = statement;
            } catch (InterruptedException ex) {
            }
        }
        return s;
    }

    /**
     * releaseStatement
     *
     * @param statement Statement
     */
    private final Object lockRelease = new Object();

    public void releaseStatement(Statement statement) {
        synchronized (lockRelease) {
            try {
                if (false) {
                    // Useful for debugging race conditions.
                    try {
                        //log.trace("Release: " + log.getStack());
                        checkStatement(0);
                    } catch (IllegalStateException ex) {
                        log.debug(ex.toString(), ex);
                    }
                }
                log.trace("Statement unlocked !! " + Thread.currentThread().getName());
                statementInUse.release();
            } catch (IllegalStateException ex) {
                log.debug(ex.toString(), ex);
            }
        }
    }

    public boolean tableExists(String tableName) throws SQLException {
        boolean exists = false;
        String query = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' AND TABLE_NAME='" + tableName + "'";
        Statement s = getStatement();
        log.trace("SQL: " + query);
        try (ResultSet rs = s.executeQuery(query)) {
            if (rs.next()) {
                exists = true;
            }
        }
        releaseStatement(s);
        return exists;
    }

    public void truncateAllTables() throws SQLException {
        // Truncate all tables.
        log.warn("Truncating all tables.");
        //devicePedTable.truncate();
    }

    public static void main(String[] args) {
        try {
            Db db = new Db();
            db.init();
        } catch (SQLException | ClassNotFoundException ex) {
            log.debug(ex.toString(), ex);
        }
    }
}
