package com.oldcurmudgeon.toolbox.pipe;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * A Feature is a set of contracts.
 *
 * @author OldCurmudgeon
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
