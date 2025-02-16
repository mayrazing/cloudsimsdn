/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.policies.vmallocation.overbooking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.policies.selecthost.HostSelectionPolicyCombinedMostFull;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmMigrationPolicy;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;

public class VmMigrationPolicyUnderutilizedMostFull extends VmMigrationPolicy {

	@Override
	protected Map<Vm, Host> buildMigrationMap(List<SDNHost> hosts) {
		Map<Vm, Host> vmToHost = new HashMap<Vm, Host>();
		// Check peak VMs and reallocate them into different host
		List<SDNVm> migrationOverVMList = getMostUtilizedVms(hosts);
		
		List<SDNHost> underHosts = OverbookingVmAllocationPolicy.getUnderutilizedHosts(hosts);
		
		for(SDNVm vmToMigrate:migrationOverVMList) {
			List<SDNHost> targetHosts = null;
			Host migratedHost = null;
			
			// 1. Check whether this VM fits into the under-utilized hosts
			if(!underHosts.isEmpty()) {
				// If the VM is connected to the other VMs, try to put this VM into one of the hosts
				targetHosts = this.vmAllocationPolicy.findHostsForGuestBySelectionPolicy(
						new HostSelectionPolicyCombinedMostFull<>(), vmToMigrate, underHosts);
				migratedHost = moveVmToHost(vmToMigrate, targetHosts);
			}
			
			// 2. Find Most Full.
			if(migratedHost == null) {
				// If VM is not connected to any other VMs: most full
				targetHosts = this.vmAllocationPolicy.findHostsForGuestBySelectionPolicy(
						new HostSelectionPolicyCombinedMostFull<>(), vmToMigrate, hosts);
				migratedHost = moveVmToHost(vmToMigrate, targetHosts);
			}
			
			// 3. No host can serve this VM, do not migrate.
			if(migratedHost == null) {
				System.err.println("VmAllocationPolicy: WARNING:: Cannot migrate VM!!!!"+vmToMigrate); 
				System.exit(0);
			}
			
			vmToHost.put(vmToMigrate, migratedHost);

		}
		return vmToHost;
	}
}
