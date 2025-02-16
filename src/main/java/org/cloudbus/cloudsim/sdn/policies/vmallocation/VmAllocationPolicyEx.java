/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.policies.vmallocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.monitor.power.PowerUtilizationMaxHostInterface;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

// Assumption: Hosts are homogeneous
// This class holds free MIPS/BW information after allocation is done.
// The class holds all hosts information

public class VmAllocationPolicyEx extends VmAllocationPolicy implements PowerUtilizationMaxHostInterface {
	//protected HostSelectionPolicy hostSelectionPolicy = null;
	protected VmMigrationPolicy vmMigrationPolicy = null;
	protected SelectionPolicy<? extends HostEntity> hostSelectionPolicy = null;

	protected final double hostTotalMips;
	protected final long hostTotalBw;
	protected final int hostTotalPes;
	
	/** The vm table. */
	//private Map<String, Host> vmTable;

	/** The used resources */
	private Map<String, Integer> usedPes;
	private Map<String, Double> usedMips;
	private Map<String, Long> usedBw;

	/** The resources in migration */
	private Map<String, Integer> migrationPes;
	private Map<String, Double> migrationMips;
	private Map<String, Long> migrationBw;

	/** The free resources */
	private List<Integer> freePes;
	private List<Double> freeMips;
	private List<Long> freeBw;
	
	/**
	 * Creates the new VmAllocationPolicySimple object.
	 * 
	 * @param list the list
	 * @pre $none
	 * @post $none
	 */
	public VmAllocationPolicyEx(List<? extends HostEntity> list,
								SelectionPolicy<? extends HostEntity> hostSelectionPolicy,
								VmMigrationPolicy vmMigrationPolicy)
	{
		super(list);
		
		this.hostSelectionPolicy = hostSelectionPolicy;
		this.vmMigrationPolicy = vmMigrationPolicy;
//		if(this.hostSelectionPolicy != null)
//			this.hostSelectionPolicy.setVmAllocationPolicy(this);
//		if(this.vmMigrationPolicy != null)
//			this.vmMigrationPolicy.setVmAllocationPolicy(this);

		setFreePes(new ArrayList<Integer>());
		setFreeMips(new ArrayList<Double>());
		setFreeBw(new ArrayList<Long>());

		for (HostEntity host : getHostList()) {
			getFreePes().add(host.getNumberOfPes());
			getFreeMips().add(host.getTotalMips());
			getFreeBw().add(host.getBw());
		}

		hostTotalMips = getHostList().getFirst().getTotalMips();
		hostTotalBw =  getHostList().getFirst().getBw();
		hostTotalPes =  getHostList().getFirst().getNumberOfPes();
		
		migrationPes = new HashMap<String, Integer>();
		migrationMips = new HashMap<String, Double>();
		migrationBw = new HashMap<String, Long>();
	}


	@Override
	public boolean allocateHostForGuest(GuestEntity guest, HostEntity host) {
		if (host == null) { // Can't be allocated because host is empty
			Log.printlnConcat(CloudSim.clock(), ": ", "No Datacenter Found", ": Allocation of ", guest.getClassName(), " #", guest.getId(), " is failed (No Suitable Host Found!)");
			return false;
		}

		String datacenterName = host.getDatacenter().getName();
		if (host == guest) { // cannot be hosted on itself (VirtualEntity edge-case)
			Log.printlnConcat(CloudSim.clock(), ": ", datacenterName, ".guestAllocator: Allocation of ", guest.getClassName(), " #", guest.getId(), " to ", host.getClassName(), " #", host.getId(), " failed (cannot be allocated on itself)");
			return false;
		}
		if (host.isBeingInstantiated()){ // cannot be hosted by an unallocated host (VirtualEntity edge-case)
			Log.printlnConcat(CloudSim.clock(), ": ", datacenterName, ".guestAllocator: Allocation of ", guest.getClassName(), " #", guest.getId(), " to ", host.getClassName(), " #", host.getId(), " failed because the host entity is not instantiated");
			return false;
		}
		if (host.isFailed()){ // cannot be hosted by a failed host (VirtualEntity edge-case)
			Log.printlnConcat(CloudSim.clock(), ": ", datacenterName, ".guestAllocator: Allocation of ", guest.getClassName(), " #", guest.getId(), " to ", host.getClassName(), " #", host.getId(), " failed because the host entity is in a failed state");
			return false;
		}
		// TODO::zmy test start
//		if(finaliseResourceAfterMigration((SDNVm) guest)) {
//			// VM was in migration, the resource is already reserved during migration preparation process.
//			// Do not need to duplicate resource reservation. return
//			return false;
//		}
		// Error check
//		if(((SDNVm) guest).getGuestScheduler().getPeMap().get(guest.getUid()) != null) {
//			System.err.println(guest+" is already in the host! " + host);
//			System.exit(1);
//		}
		// TODO::zmy test end

		if (host.guestCreate(guest)) { // if vm has been succesfully created in the host
			getGuestTable().put(guest.getUid(), host);
			reserveResource(host, (SDNVm) guest);
			Log.formatLine(
					"%.2f: VM #" + guest.getId() + " has been allocated to the host #" + host.getId(),
					CloudSim.clock());
			logMaxNumHostsUsed();
			return true;
		}

		return false;
	}

	protected boolean allocateHostForVm(Vm vm, List<Host> candidateHosts) {
		if (getGuestTable().containsKey(vm.getUid())) { // if this vm was not created
			return false;
		}
		boolean result = false;

		for(Host host : candidateHosts) {
			result = host.guestCreate(vm);
			if (result) {
				// if vm were succesfully created in the host
				getGuestTable().put(vm.getUid(), host);
				reserveResource(host, (SDNVm) vm);
				break;
			}
		}

		if(!result) {
			System.err.println("VmAllocationPolicyEx: WARNING:: Cannot create VM!!!!");
		}

		logMaxNumHostsUsed();
		return result;
	}

	public boolean isResourceAllocatable(Host host, SDNVm vm)
	{
		int idx = findHostIdx(host);
		
		//int pe = vm.getNumberOfPes(); // Do not check PE for overbooking: sharable
		double mips = vm.getTotalMips(); //getCurrentRequestedTotalMips();
		long bw = vm.getBw(); //CurrentRequestedBw();
		
		double freeMips = getFreeMips().get(idx);
		long freeBw = getFreeBw().get(idx);
		
		double overbookingRatioMips = getOverRatioMips(vm, host);
		double overbookinRatioBw = getOverRatioBw(vm, host);
				
		// Check whether the host can hold this VM or not.
		if (freeMips < mips * overbookingRatioMips) {
			System.err.format("%s:not enough MIPS: avail=%f,req=%f (OR=%.2f) / BW avail=%d, req=%d (OR=%.2f)\n", host.toString(),
					freeMips, mips, overbookingRatioMips,
					freeBw, bw, overbookinRatioBw);
			return false;
		}
		
		if( freeBw < bw * overbookinRatioBw) {
			System.err.format("%s:not enough BW: avail=%f, req=%f (OR=%.2f) / BW avail=%d, req=%d (OR=%.2f)\n", host.toString(),
					freeMips, mips, overbookingRatioMips,
					freeBw, bw, overbookinRatioBw);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Creates a migration map that describes which VM to migrate to which host
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#optimizeAllocation(List<? extends GuestEntity>)
	 */
	@Override
	public List<GuestMapping> optimizeAllocation(List<? extends GuestEntity> vmList) {
//		if(vmMigrationPolicy != null)
//			return vmMigrationPolicy.getMigrationMap(this.<SDNHost>getHostList());
		
		return null;
	}

	
	protected int maxNumHostsUsed=0;
	public void logMaxNumHostsUsed() {
		// Get how many are used
		int numHostsUsed=0;
		for(int freePes:getFreePes()) {
			if(freePes < hostTotalPes) {
				numHostsUsed++;
			}
		}
		if(maxNumHostsUsed < numHostsUsed)
			maxNumHostsUsed = numHostsUsed;
		Log.printLine("Number of online hosts:"+numHostsUsed + ", max was ="+maxNumHostsUsed);
	}
	
	public int getMaxNumHostsUsed() {
		return maxNumHostsUsed;
	}

	/**
	 * Releases the host used by a VM.
	 *
	 * @param vm the vm
	 * @pre $none
	 * @post none
	 */
	@Override
	public void deallocateHostForGuest(GuestEntity vm) {
		HostEntity host = getGuestTable().remove(vm.getUid());
		if (host != null) {
			host.guestDestroy(vm);
			removeResource(host, vm);
		}
	}

	@Override
	public HostEntity findHostForGuest(GuestEntity guest) {
		return null;
	}

//	/**
//	 * Gets the vm table.
//	 *
//	 * @return the vm table
//	 */
//	public Map<String, Host> getVmTable() {
//		return vmTable;
//	}

//	/**
//	 * Sets the vm table.
//	 *
//	 * @param vmTable the vm table
//	 */
//	protected void setVmTable(Map<String, Host> vmTable) {
//		this.vmTable = vmTable;
//	}

	/**
	 * Gets the used pes.
	 *
	 * @return the used pes
	 */
	protected Map<String, Integer> getUsedPes() {
		return usedPes;
	}

	/**
	 * Sets the used pes.
	 *
	 * @param usedPes the used pes
	 */
	protected void setUsedPes(Map<String, Integer> usedPes) {
		this.usedPes = usedPes;
	}

	/**
	 * Gets the free pes.
	 *
	 * @return the free pes
	 */
	protected List<Integer> getFreePes() {
		return freePes;
	}

	/**
	 * Sets the free pes.
	 *
	 * @param freePes the new free pes
	 */
	protected void setFreePes(List<Integer> freePes) {
		this.freePes = freePes;
	}

	protected Map<String, Double> getUsedMips() {
		return usedMips;
	}
	protected void setUsedMips(Map<String, Double> usedMips) {
		this.usedMips = usedMips;
	}
	protected Map<String, Long> getUsedBw() {
		return usedBw;
	}
	protected void setUsedBw(Map<String, Long> usedBw) {
		this.usedBw = usedBw;
	}
	protected List<Double> getFreeMips() {
		return this.freeMips;
	}
	protected void setFreeMips(List<Double> freeMips) {
		this.freeMips = freeMips;
	}
	
	protected List<Long> getFreeBw() {
		return this.freeBw;
	}
	protected void setFreeBw(List<Long> freeBw) {
		this.freeBw = freeBw;
	}
	

	protected int findHostIdx(Host h) {
		for(int i=0; i< getHostList().size(); i++) {
			if(getHostList().get(i).equals(h)) {
				return i;
			}
		}
		return -1;
	}


	protected static double convertWeightedMetric(double mipsPercent, double bwPercent) {
        return mipsPercent * bwPercent;
	}
	
	public double[] buildFreeResourceMetric(List<? extends HostEntity> hosts) {
		double[] freeResources = new double[hosts.size()];
		for (int i = 0; i < hosts.size(); i++) {
			HostEntity h = hosts.get(i);
			
			double mipsFreePercent = getAvailableMips((Host)h)/ this.hostTotalMips;
			double bwFreePercent = (double)getAvailableBw((Host)h) / this.hostTotalBw;
			freeResources[i] = convertWeightedMetric(mipsFreePercent, bwFreePercent);
		}
		
		return freeResources;
	}

	public double[] buildFreeResourceMetricForGuest(List<? extends HostEntity> hosts, GuestEntity vm) {
		double[] freeResources = buildFreeResourceMetric(hosts);
		int numHosts = getHostList().size();
		if(vm instanceof SDNVm) {
			SDNVm svm = (SDNVm) vm;
			if(svm.getHostName() != null) {
				// allocate this VM to the specific Host!
				for (int i = 0; i < numHosts; i++) {
					SDNHost h = (SDNHost)(getHostList().get(i));
					if(svm.getHostName().equals(h.getName())) {
						freeResources[i] = Double.MAX_VALUE;
					}
				}
			}
		}
		return freeResources;
	}


	
	protected double getAvailableMips(Host host) {
		int idx = findHostIdx(host);
        return getFreeMips().get(idx);
	}
	
	protected long getAvailableBw(Host host) {
		int idx = findHostIdx(host);
        return getFreeBw().get(idx);
	}

	protected double getOverRatioMips(SDNVm vm, Host host) {
		return 1.0;	// 100% requested resource is given. No overbooking
	}
	
	protected double getOverRatioBw(SDNVm vm, Host host) {
		return 1.0;	// 100% requested resource is given. No overbooking
	}
	
	public void updateResourceAllocation(Host host) {
		// Update the resource allocation ratio of every VM
		return;
	}
	
	// Temporary resource reservation for migration purpose
	// Remove required amount from the available resource of target host
	protected void reserveResourceForMigration(Host host, SDNVm vm) {
		int idx = findHostIdx(host);
		
		double overbookingRatioMips =getOverRatioMips(vm, host);
		double overbookinRatioBw =getOverRatioBw(vm, host);
		
		int pe = vm.getNumberOfPes();
		double adjustedMips = vm.getTotalMips()*overbookingRatioMips;
		long adjustedBw = (long) (vm.getBw()*overbookinRatioBw);
		
		migrationPes.put(vm.getUid(), pe);
		getFreePes().set(idx, getFreePes().get(idx) - pe);
		
		migrationMips.put(vm.getUid(), adjustedMips);
		getFreeMips().set(idx, (getFreeMips().get(idx) - adjustedMips));

		migrationBw.put(vm.getUid(), adjustedBw);
		getFreeBw().set(idx, getFreeBw().get(idx) - adjustedBw);

		Log.println(CloudSim.clock() + ": reserveResourceForMigration() " + vm + " MIPS:"+adjustedMips+"(OR:"+overbookingRatioMips+")");
		Log.println(CloudSim.clock() + ": reserveResourceForMigration() " + vm + " BW:"+ adjustedBw+"(OR:"+overbookinRatioBw+")");
	}
	
	private boolean finaliseResourceAfterMigration(SDNVm vm) {
		Integer pe = migrationPes.remove(vm.getUid());
		Double mips = migrationMips.remove(vm.getUid());
		Long bw = migrationBw.remove(vm.getUid());
		if(pe == null) {
			// This VM was not in migration
			return false;
		}

		if(getUsedPes().get(vm.getUid()) != null) {
			System.out.println(vm+ " VM resource reservation is not released yet! ");
			System.exit(1);
		}

		getUsedPes().put(vm.getUid(), pe);
		getUsedMips().put(vm.getUid(), mips);
		getUsedBw().put(vm.getUid(), bw);

		return true;
	}

	// Reserve resource in the Host for the VM
	private void reserveResource(HostEntity hostEnt, SDNVm vm) {
		vm.addMigrationHistory((SDNHost) hostEnt);

		if(finaliseResourceAfterMigration((SDNVm) vm)) {
			// VM was in migration, the resource is already reserved during migration preparation process.
			// Do not need to duplicate resource reservation. return
			return;
		}

		// Error check
		if(getUsedPes().get(vm.getUid()) != null) {
			System.err.println(vm+" is already in the host! " + hostEnt);
			System.exit(1);
		}

		Host host = (Host) hostEnt;
		int idx = findHostIdx(host);

		double overbookingRatioMips =getOverRatioMips(vm, host);
		double overbookinRatioBw =getOverRatioBw(vm, host);

		int pe = vm.getNumberOfPes();
		double adjustedMips = vm.getTotalMips()*overbookingRatioMips;
		long adjustedBw = (long) (vm.getBw()*overbookinRatioBw);

		getUsedPes().put(vm.getUid(), pe);
		getFreePes().set(idx, getFreePes().get(idx) - pe);

		getUsedMips().put(vm.getUid(), adjustedMips);
		getFreeMips().set(idx, getFreeMips().get(idx) - adjustedMips);

		getUsedBw().put(vm.getUid(), adjustedBw);
		getFreeBw().set(idx, getFreeBw().get(idx) - adjustedBw);

		Log.println(CloudSim.clock() + ": reserveResource() " + vm + " MIPS:"+adjustedMips+"(OR:"+overbookingRatioMips+")");
		Log.println(CloudSim.clock() + ": reserveResource() " + vm + " BW:"+ adjustedBw+"(OR:"+overbookinRatioBw+")");
	}

	protected void removeResource(HostEntity host, GuestEntity vm) {
		if (host != null) {
			int idx = getHostList().indexOf(host);

			Integer pes = getUsedPes().remove(vm.getUid());
			getFreePes().set(idx, getFreePes().get(idx) + pes);

			Double mips = getUsedMips().remove(vm.getUid());
			getFreeMips().set(idx, getFreeMips().get(idx) + mips);

			Long bw = getUsedBw().remove(vm.getUid());
			getFreeBw().set(idx, getFreeBw().get(idx) + bw);
		}
	}



}

