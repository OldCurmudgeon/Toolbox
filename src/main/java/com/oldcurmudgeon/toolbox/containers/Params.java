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
package com.oldcurmudgeon.toolbox.containers;

import com.oldcurmudgeon.toolbox.twiddlers.RegexFilenameFilter;
import com.oldcurmudgeon.toolbox.twiddlers.VersionUtils;
import com.oldcurmudgeon.toolbox.walkers.Separator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>Title: Parameter Manager</p>
 * <p>
 * <p>Description: </p>
 * <p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>
 * <p>Company: </p>
 * <p>
 * Enhancements.
 * <p>
 * 1. No longer able to write properties.
 * <p>
 * This also makes a number of the rather complex features much simpler.
 * <p>
 * 2. Load from a number of possible locations.
 * <p>
 * All matching properties files are loaded from all locations.
 * <p>
 * 3. Dated property files.
 * <p>
 * If multiple files match the mask file*.properties in each location then
 * only the lexically last one is loaded. I.e. if iOrdering.properties and
 * iOrdering.20120330.properties are present then only the second one is
 * loaded.
 * <p>
 * 4. Timed reload.
 * <p>
 * If it is detected that we are being used from inside a webapp then the
 * properties file locations are scanned again every minute.
 * <p>
 * 5. Listeners.
 * <p>
 * You can register listeners to be informed of reload.
 * <p>
 * 6. Only reload if one of the files have changed.
 * <p>
 * Don't bother to reload from a file if it hasn't changed.
 *
 * @author OldCurmudgeon
 * @version 1.0
 */
public class Params {
    public static final String ME = "Me";
    // I'm the only one.
    private static Params me = null;
    // Lock for the properties file.
    private static final Object lock = new Object();
    // Those who are interested in being told of changes.
    private static final List<ModifiedListener> listeners = new ArrayList<>();
    // The files I loaded.
    private static final List<WatchedFile> loaded = new LinkedList<>();
    // My name.
    private String myName = "";
    // Alternate name.
    private String secondName = null;
    // The ext to look for.
    private static final String propertiesExtension = ".properties";
    // My main set of properties.
    private Properties properties = new Properties();
    // Extra set, completely under the users control (apart from searching).
    private Properties extraProperties = new Properties();
    // What to look for.
    static final String[] ons = new String[]{"1", "true", "yes", "on", "y", "+"};
    static final String[] offs = new String[]{"0", "false", "no", "off", "n", "-"};
    // Where to look.
    static final List<String> searchPaths = new LinkedList<>();
    // Are we a webApp
    private static boolean webApp = false;
    // Extras to add.
    private static String extras = null;

    private void startReloadTimer() {
        // Start a new timer task to repeat every minute.
        int rate = 60 * 1000;
        // Make me a listener.
        addListener(new ModifiedListener() {
            @Override
            public void modified() {
                System.out.println("Modified! We now have " + got() + " parameters: " + properties);
            }
        });

        // Make a daemon scheduled thread to re-read properties.
        new Timer("Read properties timer", true).schedule(new TimerTask() {
            @Override
            public void run() {
                // Gain exclusive access.
                synchronized (lock) {
                    // And reload.
                    loadProperties();
                    loadExtras(extras);
                }
            }
        }, rate, rate);
    }

    /**
     * Params
     */
    // Force singletonism.
    private Params() {
        getSearchPath();
    }

    // Add further search paths.
    public Params addSearchPath(String... searchPath) {
        searchPaths.addAll(Arrays.asList(searchPath));
        // Reload
        loadProperties();
        loadExtras(extras);
        return this;
    }

    // What files were loaded?
    public List<String> getLoaded() {
        List<String> loadedFiles = new LinkedList<>();
        for (WatchedFile f : loaded) {
            try {
                loadedFiles.add(f.file.getCanonicalPath());
            } catch (IOException ex) {
                loadedFiles.add("?" + f.file.getAbsolutePath());
            }
        }
        return loadedFiles;
    }

    /**
     * What search path should I use?
     * <p>
     * The search path depends on what we are.
     * <p>
     * A POJO will probably look in '.'
     * but a webApp will normally look in CATALINA_HOME.
     */
    private static void getSearchPath() {
        // Start empty.
        searchPaths.clear();
        String tomcatHome = System.getProperty("catalina.home");
        if (tomcatHome != null) {
            searchPaths.add(tomcatHome);
            webApp = true;
        } else {
            // POJOs look in "."
            searchPaths.add(".");
        }
    }

    /**
     * load
     *
     * @param name String
     */
    private void load(final String name) {
        // Keep a note of my name.
        myName = name;
        loadProperties();
        loadExtras(extras);
    }

    public Params loadMore(final String anotherName) {
        secondName = anotherName;
        if (!secondName.equalsIgnoreCase(myName)) {
            loadProperties();
            loadExtras(extras);
        } else {
            // Silly!!
            secondName = null;
        }
        return this;
    }

    private void loadFromFile(WatchedFile file) {
        final Properties newProperties = readPropertiesFrom(file.getFile());
        // Add them all.
        properties.putAll(newProperties);
        // Remember the file.
        loaded.add(new WatchedFile(file.getFile()));
    }

    private Properties readPropertiesFrom(File f) {
        final Properties newProperties = new Properties();
        try {
            try (FileInputStream in = new FileInputStream(f)) {
                //System.err.println("Loading properties from " + f.getCanonicalPath());
                // Read them in.
                newProperties.load(in);
            }
        } catch (IOException ex) {
            // Ignore no-properties case.
            //Throwables.ignore(ex);
        }
        return newProperties;
    }

    private void buildFileList(List<WatchedFile> list, String name) {
        // Make my filter.
        RegexFilenameFilter filter = new RegexFilenameFilter(name + "*" + ".properties");
        // Load from every known search path.
        for (String path : searchPaths) {
            // No idea how one could be null but I've seen it.
            if (path != null) {
                // List all matching files.
                String[] files = new File(path).list(filter);
                if (files != null && files.length > 0) {
                    // Sort them and take the last one.
                    Arrays.sort(files, propertiesFileComparator);
                    // Grab the last one.
                    File f = new File(path, files[files.length - 1]);
                    if (f.exists()) {
                        //System.err.println("Candidate: " + f.getAbsolutePath());
                        // Remember the file.
                        list.add(new WatchedFile(f));
                        // Does it refer to any files internally?
                        final Properties newProperties = readPropertiesFrom(f);
                        // Are there any @FileName props in there?
                        for (final Enumeration e = newProperties.propertyNames(); e.hasMoreElements(); ) {
                            final String itsName = ((String) e.nextElement()).toLowerCase();
                            // An '@' at the start makes it a file name.
                            if (itsName.length() > 0 && itsName.charAt(0) == '@') {
                                // Bring them in as extras.
                                File extra = new File(itsName.substring(1));
                                if (extra.exists()) {
                                    //System.err.println("Extra Canditate: " + f.getAbsolutePath());
                                    list.add(new WatchedFile(extra));
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    static class PropertiesFileComparator implements Comparator<String> {
        @Override
        public int compare(String t0, String t1) {
            // Makes sure xxx.20120330.properties comes after xxx.properties
            String a = t0.replaceAll(Pattern.quote(propertiesExtension), "");
            String b = t1.replaceAll(Pattern.quote(propertiesExtension), "");
            return a.compareTo(b);
        }
    }

    static final PropertiesFileComparator propertiesFileComparator = new PropertiesFileComparator();

    /**
     * load
     *
     * @param name String
     */
    private void loadProperties() {
        // Build my file list.
        List<WatchedFile> files = buildFileList();
        //System.out.println("File list: " + files);
        if (shouldReload(files)) {
            reload(files);
        }
    }

    private boolean shouldReload(List<WatchedFile> files) {
        // Have any changed?
        for (WatchedFile file : files) {
            // Is it in the old list?
            if (loaded.contains(file)) {
                // In old list! Check the old one for change.
                if (loaded.get(loaded.indexOf(file)).changed()) {
                    return true;
                }
            } else {
                // Not in old list.
                return true;
            }
        }
        // Any in the old list not in this one.
        for (WatchedFile file : loaded) {
            // Is it in the new list?
            if (!files.contains(file)) {
                // File gone.
                return true;
            }
        }
        return false;
    }

    private void reload(List<WatchedFile> files) {
        // Start empty.
        loaded.clear();
        properties.clear();
        for (WatchedFile file : files) {
            loadFromFile(file);
        }
        loadExtras(extras);
        // Let them know we changed.
        tellListeners();
    }

    private List<WatchedFile> buildFileList() {
        List<WatchedFile> list = new LinkedList<>();
        buildFileList(list, myName);
        buildFileList(list, secondName);
        return list;
    }

    // Listen for property file changes.
    public Params addListener(final ModifiedListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
            listeners.add(listener);
            tellListeners();
        }
        return this;
    }

    // Load some extra properties from xxx=yyy;ppp=qqq list.
    private void loadExtras(final String extras) {
        Params.extras = extras;
        if (extras != null) {
            // Extras is a ; delimited list.
            final String[] newEntries = extras.split(";");
            for (int i = 0; i < newEntries.length; i++) {
                final String[] nameAndValue = newEntries[i].split("=");
                properties.setProperty(nameAndValue[0], nameAndValue[1]);
            }
        }
    }

    /**
     * getParams
     *
     * @return Params
     */
    public static Params getParams() {
        synchronized (lock) {
            if (me == null) {
                // it's ok, we can call this constructor
                me = new Params();
            }
            return me;
        }
    }

    /**
     * getParams
     *
     * @param name String
     * @return Params
     */
    public static Params getParams(final String name, final String extras) {
        synchronized (lock) {
            Params p = getParams();
            p.load(name);
            // Start the reload
            if (webApp) {
                // Start a timed reload if we are a webapp.
                p.startReloadTimer();
            }
            return p;
        }
    }

    // Count of entries.
    public int got() {
        return properties.size();
    }

    private static void tellListeners() {
        synchronized (listeners) {
            for (final ModifiedListener l : listeners) {
                l.modified();
            }
        }
    }

    /**
     * getParams
     *
     * @param name String
     * @return Params
     */
    public static Params getParams(final String name) {
        // Get without extras.
        return getParams(name, null);
    }

    /**
     * get
     *
     * @param name         String
     * @param defaultValue String
     * @return String
     */
    public String get(final String name, final String defaultValue) {
        return properties.getProperty(name, extraProperties.getProperty(name, defaultValue));
    }

    public Properties getExtras() {
        return extraProperties;
    }

    /**
     * get
     *
     * @param name String
     * @return String
     */
    public String get(final String name) {
        String s = get(name, null);
        if (s == null) {
            // Special for 'Me'
            if ("Me".equals(name)) {
                // Collect the name of the program.
                s = VersionUtils.getAppName();
            }
        }
        return s;
    }

    public Properties get() {
        return properties;
    }

    /**
     * mget
     * <p>
     * Returns all existing suffixes of name existing in params.
     *
     * @param name String
     */
    public String[] mget(final String name) {
        // NB: Used to deliver each key extension e.g. "1", "2" for Key1= and Key2=
        // Now delivers the parameters instead.
        final Map<String, String> got = new HashMap<>();
        getStartsWith(properties, name, got, true);
        // That's how many we have.
        final String[] a = new String[got.size()];
        // Roll them out in key order.
        int i = 0;
        for (final String key : new TreeSet<>(got.keySet())) {
            a[i++] = got.get(key);
        }
        return a;
    }

    public String[] mgetKeys(final String startsWith) {
        // Deliver each key that starts with the name
        final Map<String, String> got = new HashMap<>();
        getStartsWith(properties, startsWith, got, true);
        // That's how many we have.
        final String[] a = new String[got.size()];
        // Roll them out in key order.
        int i = 0;
        for (final String key : new TreeSet<>(got.keySet())) {
            a[i++] = key;
        }
        return a;
    }

    private static void getStartsWith(
            final Properties properties,
            final String name,
            final Map<String, String> got,
            final boolean caseInsensitive) {
        // Walk all keys.
        final String testName = caseInsensitive ? name.toLowerCase() : name;
        for (final Map.Entry<Object, Object> e : properties.entrySet()) {
            String k = (String) e.getKey();
            String s = k;
            if (caseInsensitive) {
                s = s.toLowerCase();
            }
            if (s.startsWith(testName)) {
                // Save the key/value pair.
                got.put(k, (String) e.getValue());
            }
        }
    }

    public Map<String, String> getStartsWith(final String name) {
        final TreeMap<String, String> got = new TreeMap<>();
        getStartsWith(properties, name, got, false);
        return got;
    }

    /**
     * set
     *
     * @param name  String
     * @param value String
     */
    public Params set(final String name, final String value) {
        properties.setProperty(name, value);
        return this;
    }

    /**
     * Allow any default value to be overridden, for when a value was not set.
     *
     * @param name
     * @param value
     */
    public Params setIfNull(final String name, final String value) {
        if (!properties.containsKey(name)) {
            set(name, value);
        }
        return this;
    }

    /**
     * setDefault
     *
     * @param name  String
     * @param value String
     */
    public Params setDefault(final String name, final String value) {
        // Set it if we havent got one like that yet.
        setIfNull(name, value);
        return this;
    }

    /**
     * anyOf
     * <p>
     * Returns true if a parameter exists of the specified name with one of the values in the list.
     *
     * @param name  String
     * @param anyOf String[]
     * @return boolean
     */
    public boolean anyOf(final String name, final String[] anyOf) {
        // Initially none found.
        boolean found = false;
        // Is it there in the file?
        if (properties.containsKey(name)) {
            // Yup! Does it have any of the options?
            final String v = properties.getProperty(name, "").toLowerCase();
            found = in(v, anyOf);
        }
        return found;
    }

    public static boolean in(final String v, final String[] anyOf) {
        if (v == null) {
            return false;
        }
        boolean found = false;
        for (int i = 0; !found && i < anyOf.length; i++) {
            // Found one?
            found |= v.equalsIgnoreCase(anyOf[i]);
        }
        return found;
    }

    public static boolean isOff(final String v) {
        return in(v, offs);
    }

    public static boolean isOn(final String v) {
        return in(v, ons);
    }

    /**
     * on
     * <p>
     * Will return true if the parameter has been specified AND it has value 1/yes/true
     *
     * @param name String
     * @return boolean
     */
    public boolean on(final String name) {
        return anyOf(name, ons);
    }

    /**
     * off
     * <p>
     * Will return true if the parameter has been specified AND it has value 0/no/false
     *
     * @param name String
     * @return boolean
     */
    public boolean off(final String name) {
        return anyOf(name, offs);
    }

    /**
     * onOff
     * <p>
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
     * test
     * <p>
     * Will return true if system is on test.
     *
     * @return boolean
     */
    public boolean onTest() {
        final String t = get("Test");
        // If the value is yyyymmdd then we are on test.
        return t != null;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        Separator cr = new Separator("\r\n");
        // Do it sorted.
        for (Map.Entry e : new TreeMap<>(properties).entrySet()) {
            s.append(cr.sep()).append(e.getKey()).append("=").append(e.getValue());
        }
        return s.toString();
    }

    // Attach listeners like this.
    public interface ModifiedListener {
        void modified();
    }

    private static class WatchedFile implements Comparable<WatchedFile> {
        private final File file;
        private long lastTime;

        public WatchedFile(File file) {
            this.file = file;
            lastTime = file.lastModified();
        }

        public boolean changed() {
            return getFile().lastModified() != lastTime;
        }

        @Override
        public int compareTo(WatchedFile it) {
            return getFile().compareTo(it.getFile());
        }

        @Override
        public boolean equals(Object it) {
            if (it == null) {
                return false;
            }
            if (!(it instanceof WatchedFile)) {
                return false;
            }
            return file.equals(((WatchedFile) it).file);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + (this.file != null ? this.file.hashCode() : 0);
            hash = 23 * hash + (int) (this.lastTime ^ (this.lastTime >>> 32));
            return hash;
        }

        /**
         * @return the file
         */
        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return file.toString();
        }
    }
}
