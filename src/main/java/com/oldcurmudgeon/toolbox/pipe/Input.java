package com.oldcurmudgeon.toolbox.pipe;

/**
 * Interface to be passed to pipe readers.
 *
 * @author OldCurmudgeon
 */
public interface Input<T> {

  public T get();
}
