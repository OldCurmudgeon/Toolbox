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

/**
 * See http://www.artima.com/weblogs/viewpost.jsp?thread=109017 for original.
 */
import java.lang.reflect.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DynamicObjectAdapterFactory {
  // Use methods in adaptee unless they exist in target in which case use adapter.
  // Implement target in passing.
  public static <T> T adapt(final Object adaptee,
                            final Class<T> target,
                            final Object adapter) {

    return (T) Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class[]{target},
            new InvocationHandler() {
      private final String name = adaptee != null
              ? adaptee.getClass().getSimpleName() + "(" + adaptee.toString() + ")"
              + "+" + adapter.getClass().getSimpleName() + "(" + adapter.toString() + ")"
              : adapter.getClass().getSimpleName() + "(" + adapter.toString() + ")";
      // The methods I wish to adapt.
      private Map<MethodIdentifier, Method> adaptedMethods = new HashMap<>();

      {
        // initializer block - find all methods in adapter object
        Method[] methods = adapter.getClass().getDeclaredMethods();
        for (Method m : methods) {
          // Keep a map of them.
          adaptedMethods.put(new MethodIdentifier(m), m);
        }
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
          // Has it been adapted?
          Method otherMethod = adaptedMethods.get(new MethodIdentifier(method));
          if (otherMethod != null) {
            return otherMethod.invoke(adapter, args);
          } else {
            return method.invoke(adaptee, args);
          }
        } catch (InvocationTargetException e) {
          throw e.getTargetException();
        }
      }

      @Override
      public String toString() {
        StringBuilder s = new StringBuilder();
        // Really simple. May get more flexible later.
        s.append("Adapted: ").append(name);
        return s.toString();
      }
    });
  }

  private static class MethodIdentifier {
    private final String name;
    private final Class[] parameters;

    public MethodIdentifier(Method m) {
      name = m.getName();
      parameters = m.getParameterTypes();
    }

    @Override
    public boolean equals(Object o) {
      // I am always equal to me.
      if (this == o) {
        return true;
      }
      // I cannot be equal to something of a different type.
      if (!(o instanceof MethodIdentifier)) {
        return false;
      }
      MethodIdentifier mid = (MethodIdentifier) o;
      return name.equals(mid.name) && Arrays.equals(parameters, mid.parameters);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }
}

// Example of usage.
class ExtendedConnection {
  // Extra stuff I am adding to a java.sql.Connection.
  public interface Connection extends java.sql.Connection {
    // Additional methods.
    public String getAlias();

    public String getUsage();

    public boolean isValid();
  }

  // The actual extension class.
  // Don't implement Connection or, again, it will change whenever java.sql.Connection changes.
  public static class ConnectionExtension {
    // Public so the proxy can get to them.
    private final String alias;
    private final String usage;
    private final java.sql.Connection original;
    private final Type type;

    public ConnectionExtension(String alias, String usage, java.sql.Connection original) {
      this.alias = alias;
      this.usage = usage;
      this.original = original;
      this.type = Type.getType(original);
    }

    // The extensions.
    public String getAlias() {
      return alias;
    }

    public String getUsage() {
      return usage;
    }

    public boolean isValid() {
      String sql = type.getCheckQuery();
      try (Statement statement = original.createStatement()) {
        statement.execute(sql);
      } catch (SQLException exception) {
        // All errors result in false.
        //log.warn("Validity check failed.", exception);
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return alias + "(" + usage + ")";
    }

    // What type of database we are connecting to.
    enum Type {
      MSSQL("SELECT 1"),
      Oracle("SELECT 1 FROM DUAL"),
      Unknown("SELECT 1");
      /*
       * A simple query to check that the connection is valid all the way to the DB.
       * 
       * See http://stackoverflow.com/q/14320231/823393 for discussion.
       */
      final String checkQuery;

      Type(String checkQuery) {
        this.checkQuery = checkQuery;
      }

      public String getCheckQuery() {
        return checkQuery;
      }

      static Type getType(java.sql.Connection c) {
        try {
          // See http://stackoverflow.com/q/254213/823393 for reference.
          String name = c.getMetaData().getDatabaseProductName();
          if (name.contains("Oracle")) {
            return Oracle;
          } else if (name.contains("Microsoft SQL Server")) {
            return MSSQL;
          } else {
            return Unknown;
          }
        } catch (Exception e) {
          return Unknown;
        }
      }
    }

    public void close() throws SQLException {
      // For demo.
      original.close();
    }
  }

  public static Connection newInstance(java.sql.Connection c, String alias, String usage) {
    // Make a ConnectionExtension look just like the Connection.
    return DynamicObjectAdapterFactory.adapt(c, Connection.class, new ConnectionExtension(alias, usage, c));
  }
}
