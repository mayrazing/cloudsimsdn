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
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmAllocationInGroup;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmGroup;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmMigrationPolicy;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

public class OverbookingVmAllocationPolicyConsolidateConnected extends OverbookingVmAllocationPolicy implements VmAllocationInGroup {
	public OverbookingVmAllocationPolicyConsolidateConnected(
			List<? extends HostEntity> list,
			SelectionPolicy<HostEntity> hostSelectionPolicy,
			VmMigrationPolicy vmMigrationPolicy) {
		super(list, hostSelectionPolicy, vmMigrationPolicy);
	}

	/**
	 * Allocates a host for a given VM Group.
	 * 
	 * @param vm VM specification
	 * @return $true if the host could be allocated; $false otherwise
	 * @pre $none
	 * @post $none
	 */
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
			// Try to put this VM into one of the correlated hosts
			if(allocateHostForGuest(vm, findHostForGuest(vm, connectedHosts))) {
				return true;
			}
			else {
				// Cannot create VM to correlated hosts. Use the Most Full First
				return allocateHostForGuest(vm);
			}
		}
	}

	protected List<HostEntity> getHostListVmGroup(VmGroup vmGroup) {
		List<HostEntity> hosts = new ArrayList<>();
		for(SDNVm vm : vmGroup.<SDNVm>getVms()) {
			HostEntity h = getGuestTable().get(vm.getUid());
			if(h != null)
				hosts.add(h);
		}
		return hosts;
	}
	
}
