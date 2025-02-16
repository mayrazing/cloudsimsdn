/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.policies.vmallocation.overbooking;

import java.util.List;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.Configuration;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmMigrationPolicy;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

public class OverbookingVmAllocationPolicyPowerNet extends OverbookingVmAllocationPolicyConsolidateConnected {
	public OverbookingVmAllocationPolicyPowerNet(List<? extends HostEntity> list,
												 SelectionPolicy<HostEntity> hostSelectionPolicy,
												 VmMigrationPolicy vmMigrationPolicy) {
		super(list, hostSelectionPolicy, vmMigrationPolicy);
	}

	@Override
	protected double getOverRatioMips(GuestEntity vm, HostEntity host) {
		return Configuration.OVERBOOKING_RATIO_INIT;
	}

	@Override
	protected double getOverRatioBw(GuestEntity vm, HostEntity host) {
		return Configuration.OVERBOOKING_RATIO_INIT;
	}
}
