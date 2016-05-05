/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oldcurmudgeon.toolbox.pipe;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * A Feature is a set of contracts.
 *
 * @author pcaswell
 */
public enum Feature {
  // Perfect identical copy at both ends.
  Mirror(Contract.NoDroppedPackets, Contract.InOrder);
  private final Set<Contract> contracts;

  Feature(Contract... contracts) {
    this.contracts = EnumSet.copyOf(Arrays.asList(contracts));
  }

  public Set<Contract> getContracts() {
    return contracts;
  }

}
