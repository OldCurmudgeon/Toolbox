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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * <p>Title: </p>
 * <p>
 * <p>Description: </p>
 * <p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public final class VersionUtils {
    // The log file.
    private static final Logger log = LoggerFactory.getLogger(VersionUtils.class);

    private VersionUtils() {
        super();
    }

    public static String getVersion(Object obj) {
        return getVersion(obj.getClass());
    }

    public static String getVersion(Class cls) {
        URL url = getLocation(cls);
        File file = new File(url.getFile().replaceAll("%20", " "));
        long lastModified = -1;
        String name = file.getName();
        String test = name.toLowerCase();
        //LOGGER.debugr("File: {}", name);
        if (test.endsWith(".jar") || test.endsWith(".zip")) {
            // Chop off extension
            name = name.substring(0, test.lastIndexOf('.'));
            lastModified = file.lastModified();
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
                ZipEntry ze = jarFile.getEntry(JarFile.MANIFEST_NAME);
                lastModified = ze.getTime();
            } catch (IOException ex) {
                log.warn(ex.toString(), ex);
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException ex) {
                        log.warn(ex.toString(), ex);
                    }
                }
            }
        } else {
            String className = cls.getName().replace('.', '/') + ".class";
            //LOGGER.debug( "Class: {}", name );
            file = new File(file, className);
            lastModified = file.lastModified();
            name = file.getName();
            name = name.substring(0, name.lastIndexOf('.'));
            if (lastModified == 0) {
                // Try with class name too.
            }
        }
        StringBuilder sb = new StringBuilder(name.length() + 20);
        sb.append(name);
        sb.append(" (");
        // TODO
        //LogFile.dateLogFormat.format(new Date(lastModified), sb, new FieldPosition(0));
        sb.append(')');
        return sb.toString();
    }

    public static URL getLocation(final Object obj) {
        return getLocation(obj.getClass());
    }

    public static URL getLocation(final Class cls) {
        final ProtectionDomain pd = cls.getProtectionDomain();
        final CodeSource cs = pd.getCodeSource();
        return cs.getLocation();
    }

    public static String getAppName() {
        // Try to collect it from the jar by preference.
        // Assume we are in the same jar as the main class.
        // We are likely to be as these utils should be packaged in the jar.
        URL url = getLocation(VersionUtils.class);
        //System.out.println("getAppName: url="+url);
        //System.out.println("URL: "+url.toString());
        File file = new File(url.getFile());
        String name = file.getName();
        //System.out.println("getAppName: name="+name);
        //System.out.println("Name: "+name);
        if (name.toLowerCase().endsWith(".jar")) {
            name = name.substring(0, name.length() - ".jar".length());
        } else {
            // Its source.
            // Can only use this in jvm 1.5+
            if (jvmVersionGE(1, 5)) {
                // Grab the thread management bean.
                ThreadMXBean temp = ManagementFactory.getThreadMXBean();
                // Grab its current thread stack.
                ThreadInfo t = temp.getThreadInfo(1, Integer.MAX_VALUE);
                StackTraceElement st[] = t.getStackTrace();
                if (st.length > 0) {
                    // Grab the oldest entry in the stack.
                    name = st[st.length - 1].getClassName();
                    // Polishing. Remove any package names.
                    int packageNamePos = name.lastIndexOf('.');
                    if (packageNamePos >= 0) {
                        name = name.substring(packageNamePos + 1);
                    }
                } else {
                    name = getPackageName();
                }
            } else {
                // The package name is the nearest thing we can find.
                name = getPackageName();
            }
        }
        return name;
    }

    public static boolean jvmVersionGE(int major, int minor) {
        String v = System.getProperty("java.version");
        String[] parts = v.split("\\.");
        boolean ge = true;
        // Must have got an array.
        ge &= parts != null;
        // At least two entries.
        ge &= parts.length >= 2;
        // Major >=
        int jvmMajor = Integer.parseInt(parts[0]);
        int jvmMinor = Integer.parseInt(parts[1]);
        ge &= jvmMajor >= major;
        // Check minor if major is same.
        if (major == jvmMajor) {
            ge &= jvmMinor >= minor;
        }
        return ge;
    }

    /**
     * getToolName
     *
     * @return String
     */
    public static String getPackageName() {
        Throwable stack = new Exception();
        StackTraceElement[] calls = stack.getStackTrace();
        // Find the first entry on the stack that isnt in my 'utils.' package.
        String packageName = null;
        for (int i = 0; i < calls.length && packageName == null; i++) {
            String name = calls[i].getClassName();
            // The first stack element that us not a utils one.
            if (!name.startsWith("utils.")) {
                packageName = name.split("\\.")[1];
            }
        }
        if (packageName == null) {
            packageName = "Unknown";
        }
        return packageName;
    }
}
