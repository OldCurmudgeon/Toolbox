package com.oldcurmudgeon.toolbox.pipe;

/**
 * Various possible contracts for data transport.
 *
 * @author OldCurmudgeon
 */
public enum Contract {
  /**
   * No packet will be dropped/lost.
   *
   * ALL packets posted to this end of the pipe WILL arrive at the other.
   *
   * Mostly used for normal file transfer etc.
   */
  NoDroppedPackets,
  /**
   * Packets delivered in order.
   *
   * All packets arriving at that end retain their original order.
   *
   * Note that a packet-dropping pipe can drop packets but the order must be maintained.
   */
  InOrder,
  /**
   * Packets delivered on a specified schedule or at a specified rate.
   *
   * Packets sent have a time contract for delivery.
   *
   * Use for continuous media etc.
   */
  Timely,
  /**
   * Capacity issues on `put` result in blocking.
   *
   * Default is to throw exception.
   */
  BlockOnPut,
  /**
   * Capacity issues on `get` result in blocking.
   *
   * Default is to throw exception.
   */
  BlockOnGet;
}
