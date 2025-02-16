/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.policies.vmallocation.overbooking;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmGroup;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmMigrationPolicy;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

public class OverBookingVmAllocationPolicyDistributeConnected extends OverbookingVmAllocationPolicyConsolidateConnected {
	
	public OverBookingVmAllocationPolicyDistributeConnected(
			List<? extends HostEntity> list,
			SelectionPolicy<HostEntity> hostSelectionPolicy,
			VmMigrationPolicy vmMigrationPolicy) {
		super(list, hostSelectionPolicy, vmMigrationPolicy);
	}
	
	@Override
	public boolean allocateHostForVmInGroup(Vm vm, VmGroup vmGroup) {
		if(getVmMigrationPolicy() instanceof VmMigrationPolicyGroupInterface) {
			((VmMigrationPolicyGroupInterface) getVmMigrationPolicy()).addVmInVmGroup(vm, vmGroup);
		}

		List<HostEntity> connectedHosts = getHostListVmGroup(vmGroup);
		if(connectedHosts.isEmpty()) {
			// This VM is the first VM to be allocated
			return allocateHostForGuest(vm);	// Use the Most Full First
		}
		else {
			// Other VMs in the group has been already allocated
			// Avoid the correlated hosts.
			List<HostEntity> allHosts = new ArrayList<>(this.<SDNHost>getHostList());
			allHosts.removeAll(connectedHosts);
			if(allocateHostForGuest(vm, findHostForGuest(vm, allHosts))) {
				return true;
			}
			else {
				// Cannot create VM to correlated hosts. Use the Most Full First
				return allocateHostForGuest(vm);
			}
		}
	}
}
