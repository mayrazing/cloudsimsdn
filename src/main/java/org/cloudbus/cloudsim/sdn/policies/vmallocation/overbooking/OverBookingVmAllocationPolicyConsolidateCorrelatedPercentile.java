/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.policies.vmallocation.overbooking;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmMigrationPolicy;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

public class OverBookingVmAllocationPolicyConsolidateCorrelatedPercentile extends OverbookingVmAllocationPolicyConsolidateConnected {
	
	public OverBookingVmAllocationPolicyConsolidateCorrelatedPercentile(
			List<? extends HostEntity> list,
			SelectionPolicy<HostEntity> hostSelectionPolicy,
			VmMigrationPolicy vmMigrationPolicy) {
		super(list, hostSelectionPolicy, vmMigrationPolicy);
	}
	
	@Override
	protected double getDynamicOverRatioMips(SDNVm vm, Host host) {
		double dor = super.getDynamicOverRatioMips(vm, host);
        return OverbookingPercentileUtils.translateToPercentage(vm.getName(), dor);
	}
}
