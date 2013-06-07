package com.oldcurmudgeon.toolbox.io;

import com.oldcurmudgeon.toolbox.containers.CloseableBlockingQueue;
import com.oldcurmudgeon.toolbox.twiddlers.ProcessTimer;
import com.oldcurmudgeon.toolbox.twiddlers.RegexFilenameFilter;
import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 *
 * Function: ---------
 *
 * Presents an Iterable interface to a file list.
 *
 * Implementation: ---------------
 *
 * A collector thread is created in which the specified folder is listed using
 * the File.list method. The file filter provided will always return false (thus
 * NOT adding this file to the list) but will add each file it is "shown" that
 * is accepted by the filter to a blocking queue before doing so.
 *
 * The queue is then consumed by the iterator (running in your thread) to walk
 * the files.
 *
 * Subfolder recursion is handled by spawning a new collector in its own thread
 * for each sub directory found. All collectors feed the same blocking queue.
 * This has the benefit of making full use of all cores but of course file
 * ordering is even less predictable.
 *
 * Benefits: ---------
 *
 * Fixed memory footprint. The footprint of a FileList depends solely on the
 * depth of the directory structure (up to a fixed limit of MAX_THREADS).
 *
 * No long pause while waiting to list all files in a large folder. Files can be
 * consumed as fast as they can be listed by the underlying OS.
 *
 * Many known issues attributed to File.list are hidden such as returning null,
 * consuming InterruptedExceptions etc.
 *
 * Potentially faster, especially if processing each file as it arrives because
 * File.list is running in parallel with your thread.
 *
 * The full path to the file is always listed.
 *
 * Can list recursively.
 *
 * Contract: ---------
 *
 * You MUST either iterate to the very end OR call close. Failing to do at least
 * one of these WILL leak a running thread.
 *
 * Limitations -----------
 *
 * There is a limit to the number of threads that are allowed to run at any one
 * time. This limit is provided for sanity purposes. If we are asked to
 * recursively list a folder structure that was twice the depth of the thread
 * limit it would be possible for all threads to be blocked trying to recurse.
 *
 * As the thread limit is currently 50 this implies a folder depth limit of 100
 * which seems acceptable.
 *
 * Notes: ------
 *
 * A FileList is both an iterator and iterable. The iterableness is achieved in
 * the usual way by returning a reference to itself. Note that this means you
 * should not use the same object both ways at once. I.E. The following would do
 * unexpected things.
 *
 * FileList f = new FileList (...); // Use as an Iterator. File file = f.next
 * (); // Use as an Iterable for ( File file : f ) { // You will not see the
 * first file here. }
 *
 * ToDo: -----
 *
 * ToDo: Fix the "it = fileQueue.poll(1, TimeUnit.MILLISECONDS);" because it
 * looks horrible.
 *
 * ToDo: Are the QUEUE_LENGTH and MAX_THREADS right? Is there a right value for
 * either?
 *
 * ToDo: Can we somehow enforce the contract of close? Can we detect that we
 * have gone out of scope and automagically close?
 *
 * ToDo: Implement "remove" as delete of the file ... or not ... probably not.
 *
 * ToDo: Is there a better collection type to use than a ConcurrentLinkedQueue
 * to keep track of the collectors? An alternative could be a synchronised Set
 * as all I really need to do is add Futures to it in one thread and remove from
 * it in another. If I could safely remove expired ones from it while iterating
 * that would be nice.
 *
 * ToDo: Do we really need to wait for all threads to terminate when closed?
 * Surely they will terminate in time anyway.
 *
 * @author OldCurmudgeon
 */
public class FileList implements Iterable<File>, Iterator<File>, Closeable {
  // The queue length.
  private static final int QUEUE_LENGTH = 50;
  // Max collector threads.
  private static final int MAX_THREADS = 50;
  // The queue. Marked closed to close the collector(s).
  private final CloseableBlockingQueue<File> fileQueue = new CloseableBlockingQueue<>(QUEUE_LENGTH);
  // The next file name to deliver.
  private File next = null;
  // Thread pool for the collectors.
  ExecutorService threads = Executors.newFixedThreadPool(MAX_THREADS);
  // Futures of all collectors running in the pool.
  ConcurrentLinkedQueue<Future> collectors = new ConcurrentLinkedQueue<>();

  // All files version.
  public FileList(String path) throws IOException {
    this(path, false, null);
  }

  // Just filtered.
  public FileList(String path, FilenameFilter filter) throws IOException {
    this(path, false, filter);
  }

  // Just recursive.
  public FileList(String path, boolean recurse) throws IOException {
    this(path, recurse, null);
  }

  // Both filtered and recursive.
  public FileList(String path, boolean recurse, FilenameFilter filter) throws IOException {
    // Start the main collector immediately.
    startCollector(path, recurse, filter);
  }

  // Start a new collector.
  private void startCollector(String path, boolean recurse, FilenameFilter filter) throws IOException {
    // Make my Callable.
    Callable<Void> c = new FileListCollector(path, recurse, filter);
    // Start it up and keep track of it so we can find out when it has finished.
    collectors.add(threads.submit(c));
  }

  // Hand back an iterator over the files.
  @Override
  public Iterator<File> iterator() {
    // I am the iterator
    return this;
  }

  // Give them the next file from the queue.
  @Override
  public File next() {
    File it = null;
    if (hasNext()) {
      it = next;
      next = null;
    }
    return it;
  }

  // Prime the pump.
  @Override
  public boolean hasNext() {
    if (next == null) {
      next = waitForNextOrClose();
    }
    return next != null;
  }

  // How many in the queue.
  public int size () {
    return fileQueue.size();
  }
  
  // Spin on the queue waiting for a new file or the listing to have closed.
  private File waitForNextOrClose() {
    File it = null;
    // Wait for either closed or there's a file.
    while (it == null && !fileQueue.isClosed()) {
      try {
        // Poll but wait a really short time in case we are closed.
        it = fileQueue.poll(100, TimeUnit.MILLISECONDS);
      } catch (InterruptedException ex) {
        log.log("! waitForNextOrClose Interrupted!");
        // Get out now!
        Thread.currentThread().interrupt();
      }
      if (it == null) {
        // There was nothing there for a whole tick!
        // Have they all finished?
        checkForFinished();
      }
    }
    return it;
  }

  // Called when nothing in queue.
  private void checkForFinished() {
    // Count the running threads.
    int runningThreads = 0;
    try {
      // Track dead ones to remove.
      List<Future> deadThreads = new LinkedList<>();
      // Walk all collectors.
      for (Future f : collectors) {
        // I've seen f being null once. No idea how.
        if (f != null) {
          // If it's not done then it's running.
          if (!f.isDone()) {
            // Count it.
            runningThreads += 1;
          } else {
            // Mark for deletion.
            deadThreads.add(f);
          }
        }
      }
      // Clear dead threads - just to be tidy.
      collectors.removeAll(deadThreads);
    } catch (ConcurrentModificationException cme) {
      // Probably a new thread has been started while I was checking!
      // Therefore almost certainly NOT all finished.
      runningThreads += 1;
    }
    // If no threads are left, we're done.
    if (runningThreads == 0) {
      // Finished! Close everything down.
      close();
    }
  }

  // Perhaps later we will delete the file ... or perhaps not.
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Cannot remove from a FileList.");
  }

  // Close down the whole system.
  @Override
  public void close() {
    // Use the fileQueue state to indicate closed.
    if (!fileQueue.isClosed()) {
      // Close the queue ... unblocking all collectors (I hope).
      fileQueue.close();
      // Shut them down agressively as this may be called by user prematurely as well as by me.
      threads.shutdownNow();
      // Wait until all is done.
      boolean terminated = false;
      do {
        try {
          // Wait up to 1 second for termination.
          terminated = threads.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
          // Ignore the interrupt! If I exit here we will leak (I think).
        }
      } while (!terminated);
      log.log("! All done");
    }
  }

  // The inner collector class that feeds the queue from File.list()
  private class FileListCollector implements Callable<Void>, FilenameFilter {
    // Filter for the files.
    private final FilenameFilter filter;
    // Path to look in.
    private final String path;
    // Should we recurse when we see a directory.
    private boolean recurse = false;
    // Has the File.list finished/closed?
    private volatile boolean finished = false;

    /**
     * *
     * Constructor
     *
     * @param path
     *
     * Folder to search.
     *
     * @param recurse
     *
     * Should we recurse when we see a directory?
     *
     * @param filter
     *
     * Matcher for the file name.
     *
     * @throws IOException If path does not exist.
     */
    private FileListCollector(String path, boolean recurse, FilenameFilter filter) throws IOException {
      this.path = new File(path).getCanonicalPath();
      this.filter = filter;
      this.recurse = recurse;
    }

    // Just like run but returns null.
    @Override
    public Void call() throws Exception {
      try {
        // Name me
        Thread.currentThread().setName("FileList " + path);
        log.log("* Starting thread " + path);
        // Don't start a list if we are closing.
        if (!finished && !fileQueue.isClosed()) {
          // Start the list process .. I am also a FileNameFilter.
          (new File(path)).list(this);
        }
      } catch (StopException e) {
        /*
         * StopException is deliberately thrown by the collector to abort the
         * File.list process above. It IS therefore safe to ignore this
         * exception as it is specifically designed to be caught here and
         * discarded.
         */
        log.log("! Stopped " + path);
      } finally {
        // All finished!
        finished = true;
        log.log("- Finished thread " + path);
      }
      return null;
    }

    // Always return false but feed the queue.
    @Override
    public boolean accept(File dir, String name) {
      // Called by the File.list method.
      if (!finished && !fileQueue.isClosed()) {
        // Always check the file.
        acceptFile(dir, name);
        // Also, if recursing, start a new collector.
        if (recurse) {
          // Is it directory?
          File f = new File(dir, name);
          if (f.isDirectory()) {
            // Yes!
            acceptDir(f);
          }
        }
      }
      // Were we closed or interrupted?
      if (fileQueue.isClosed() || Thread.interrupted()) {
        // They want me to close.
        finished = true;
      }
      // Close as fast as I can.
      if (finished) {
        // Throw a run-time exception through the File.list() method and get out into my Run method?
        try {
          // I've seen this interrupted (under stress) but I can't catch InterruptedException
          throw new StopException();
        } catch (Exception ex) {
          // Try again.
          throw new StopException();
        }
      }
      // Never return true. I have always already got that file.
      return false;
    }

    // Seen a file ... should we accept it?
    private void acceptFile(File dir, String name) {
      // Do I like this file?
      if (filter == null || filter.accept(dir, name)) {
        // Yes! Add it to my queue.
        try {
          // Block here if the queue is full.
          //log.log("? Posting " + dir + File.separator + name);
          fileQueue.put(new File(dir, name));
        } catch (InterruptedException ex) {
          log.log("! Accept Interrupted! " + path);
          // They want me to close.
          finished = true;
        }
      }
    }

    // Seen a directory ... recurse.
    private void acceptDir(File dir) {
      // Check fileQueue again here because isFile could take a while.
      if (!fileQueue.isClosed()) {
        try {
          // Start a new collector
          startCollector(dir.getCanonicalPath(), recurse, filter);
        } catch (IOException ex) {
          /*
           * Not sure what happened. f is not a file so should be a folder and
           * it was given to me by File.list so how could getCanonicalPath fail.
           */
          //ex.printStackTrace();
        }
      }
    }
  }
  // ***** Following only needed for testing. *****
  private static TinyLogger log = new TinyLogger("FileList");
  private static final String testPath = "C:\\Public";

  public static void main(String[] args) {
    try {
      log.setDebug(true).reset();

      FileList l;

      // Thousands.
      log.log(true, "----\r\nThousands.\r\n----");
      l = new FileList("C:\\Junk\\ThousandsOfFiles\\", new RegexFilenameFilter("*.txt"));
      // Iterate across all of them.
      ProcessTimer allTimer = new ProcessTimer();
      ProcessTimer firstTimer = new ProcessTimer();
      int nFiles = 0;
      for (File f : l) {
        // First file
        firstTimer.stop();
        nFiles += 1;
      }
      log.log("" + nFiles + " took " + allTimer + " first file at " + firstTimer);

      log.log(true, "----\r\nThousands cut short.\r\n----");
      for (int i = 0; i < 10; i++) {
        l = new FileList("C:\\Junk\\ThousandsOfFiles\\", new RegexFilenameFilter("*.txt"));
        // Iterate across some of them.
        allTimer = new ProcessTimer();
        firstTimer = new ProcessTimer();
        nFiles = 0;
        for (File f : l) {
          nFiles += 1;
          firstTimer.stop();
          if (nFiles >= 1000) {
            l.close();
          }
        }
        log.log("" + nFiles + " took " + allTimer + " first file at " + firstTimer);
      }

      // Full flat list.
      log.log(true, "----\r\nFlat list.\r\n----");
      l = new FileList(testPath);
      for (File f : l) {
        log.log(f.getCanonicalPath());
      }

      // Premature close.
      log.log(true, "----\r\nFirst 10.\r\n----");
      l = new FileList(testPath);
      for (int i = 0; i < 10; i++) {
        log.log("" + i + " = " + l.next());
      }
      l.close();

      // 1000 times.
      log.log(true, "----\r\nPremature close (after 10) 1000 times.\r\n----");
      for (int j = 0; j < 1000; j++) {
        // Premature close.
        l = new FileList(testPath);
        for (int i = 0; i < 10 && l.hasNext(); i++) {
          l.next();
        }
        l.close();
      }

      // Recursive closed lots of times.
      log.log(true, "----\r\nRecursive premature close (after 100) 10 times.\r\n----");
      for (int j = 1; j <= 10; j++) {
        l = new FileList(testPath, true);
        for (int i = 0; i < 100; i++) {
          File f = l.next();
          //System.out.println(i + "-" + f);
          log.log("= Pulled " + f);
        }
        // Get most of the threads blocked.
        Thread.sleep(1000);
        try {
          l.close();
          log.log(true, "Finished iteration " + j);
        } catch (Exception e) {
          log.log(true, "Exception at iteration " + j);
          throw (e);
        }
      }

      // Filtered.
      log.log(true, "----\r\nFiltered.\r\n----");
      l = new FileList(testPath, new RegexFilenameFilter("Junk.*"));
      for (File f : l) {
        log.log(f.getCanonicalPath());
      }

      // Non-existent.
      log.log(true, "----\r\nNothing.\r\n----");
      l = new FileList(testPath, new RegexFilenameFilter("NoFileOfThisName.*"));
      for (File f : l) {
        log.log(f.getCanonicalPath());
      }

      // Invalid folder.
      log.log(true, "----\r\nInvalid folder.\r\n----");
      l = new FileList(testPath + "\\NoFolderOfThisName");
      for (File f : l) {
        log.log(f.getCanonicalPath());
      }

      // Security exception (or not)?
      log.log(true, "----\r\nRemote.\r\n----");
      l = new FileList("\\\\PAULSPC\\Public\\ATest", true);
      for (File f : l) {
        log.log(f.getCanonicalPath());
      }

      // Recursive.
      log.log(true, "----\r\nFull Recursive list.\r\n----");
      l = new FileList(testPath, true);
      int found = 0;
      for (File f : l) {
        log.log(f.getCanonicalPath());
        found += 1;
      }
      log.log(true, "Found " + found);

      log.log(true, "----\r\nFinished ... There should be no wild threads. If there were, we wouldnt finish.\r\n----");
    } catch (Exception ex) {
      log.log(ex);
    }
  }
}

class StopException extends RuntimeException {
  // Used to stop the list mechanism before it reaches the end of the directory.
  // List certainly consumes and discards (sometimes) InterruptedExceptions so
  // this is used instead.
}

