/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.policies.vmallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.physicalcomponents.Node;
import org.cloudbus.cloudsim.sdn.physicalcomponents.PhysicalTopology;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.physicalcomponents.PhysicalTopology.NodeType;
import org.cloudbus.cloudsim.sdn.physicalcomponents.switches.EdgeSwitch;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.overbooking.VmMigrationPolicyGroupInterface;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

// 1. Check the priority of the VM group
// 2. Put the highest priority first
public class VmAllocationPolicyPriorityFirst extends VmAllocationPolicyGroupConnectedFirst {

	private PhysicalTopology topology;

	public VmAllocationPolicyPriorityFirst(List<? extends HostEntity> list,
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
	public boolean allocateHostForVmInGroup(Vm vm, VmGroup vmGrp) {
		VmGroupPriority vmGroup = (VmGroupPriority)vmGrp;
		if(getVmMigrationPolicy() instanceof VmMigrationPolicyGroupInterface) {
			((VmMigrationPolicyGroupInterface) getVmMigrationPolicy()).addVmInVmGroup(vm, vmGroup);
		}

		List<HostEntity> connectedHosts = getHostListVmGroup(vmGroup);
		// Other VMs in the group has been already allocated
		if(!connectedHosts.isEmpty()) {
			// Try to put this VM into one of the correlated hosts			
			if(allocateHostForGuest(vm, findHostForGuest(vm, connectedHosts))) {
				return true;
			}
		}
		
		// For Priority VMs, find the most available host group (determined by edge connection)
		if(VmGroupPriority.isPriorityVmGroup(vmGroup)) {
			// If other VMs in the group has been already allocated, find the group 
			if(!connectedHosts.isEmpty()) {
				Collection<HostEntity> hostCandidates = new LinkedHashSet<>();
				for(HostEntity h : connectedHosts) {
					HostGroup hg = this.getAdjacentHostGroupSameEdge(h);
					hostCandidates.addAll(hg.hosts);
				}
			
				// Try to put this VM into the edge switch as the other VMs
				if(allocateHostForGuest(vm, findHostForGuest(vm, new ArrayList<>(hostCandidates)))) {
					return true;
				}
				
				// Find the same pod
				hostCandidates = new LinkedHashSet<>();
				List<HostGroup> hGroups = this.getAdjacentHostGroupSamePod(connectedHosts.getFirst());
				Collections.sort(hGroups);
				
				for(HostGroup hg: hGroups) {
					hostCandidates.addAll(hg.hosts);
				}
				// Try to put this VM into the same pod as the other VMs
				if(allocateHostForGuest(vm, findHostForGuest(vm, new ArrayList<>(hostCandidates)))) {
					return true;
				}				
			}
			
			// Find the most available pod. 
			List<HostGroup> groups = new LinkedList<HostGroup>(getHostGroupMap().values());
			Collections.sort(groups);
			// Try to put this VM into the most available pod
			if(allocateHostForGuest(vm, findHostForGuest(vm, groups.getFirst().hosts))) {
				return true;
			}
		}
		return allocateHostForGuest(vm);	// Use the Most Full First
	}
	
	@SuppressWarnings("unchecked")
	private Map<Node,HostGroup> getHostGroupMap() {
		Collection<Node> edges = topology.getNodesType(NodeType.Edge);
		HashMap<Node,HostGroup> groups = new HashMap<Node,HostGroup>();
		for(Node e: edges) {
			HostGroup hg = new HostGroup();
			hg.edge = (EdgeSwitch) e;
			hg.hosts  = new ArrayList<HostEntity>((Collection<? extends SDNHost>)(Collection<? extends Node>)topology.getConnectedNodesLow(e));
			for(HostEntity h:hg.hosts) {
				hg.numHosts++;
				hg.availableMips += ((SDNHost)h).getAvailableMips();
				hg.availableBw += ((SDNHost)h).getAvailableBandwidth();
			}
			groups.put(hg.edge, hg);
		}
		
		return groups;
	}
	
	private Node findEdgeSwitch(HostEntity host) {
		return (EdgeSwitch) topology.getConnectedNodesHigh((SDNHost)host).iterator().next();
	}
	
	protected HostGroup getAdjacentHostGroupSameEdge(HostEntity host) {
		// Search the list of adjacent host group.
		Node edge = findEdgeSwitch(host);
		Map<Node,HostGroup> groupMap = getHostGroupMap();

		return groupMap.get(edge);
		
	}
	
	protected List<HostGroup> getAdjacentHostGroupSamePod(HostEntity host) {
		Node edge = findEdgeSwitch(host);
		
		List<HostGroup> groups = new ArrayList<HostGroup>();
		
		Collection<Node> aggrs = topology.getConnectedNodesHigh(edge);
		Collection<Node> allEdges = new HashSet<Node>();
		for(Node agg:aggrs)
		{
			allEdges.addAll(topology.getConnectedNodesLow(agg));
		}
		allEdges.remove(edge);
		
		Map<Node,HostGroup> groupMap = getHostGroupMap();

		for(Node e:allEdges) {
			groups.add(groupMap.get(e));
		}
		return groups;
		
	}

	private List<HostEntity> getHostListVmGroup(VmGroup vmGroup) {
		LinkedHashSet<HostEntity> hosts = new LinkedHashSet<>();
		for(SDNVm vm : vmGroup.<SDNVm>getVms()) {
			HostEntity h = getGuestTable().get(vm.getUid());
			if(h != null)
				hosts.add(h);
		}
		return new ArrayList<>(hosts);
	}

	public PhysicalTopology getTopology() {
		return topology;
	}
	public void setTopology(PhysicalTopology top) {
		this.topology = top;
	}
}

