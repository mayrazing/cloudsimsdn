/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn.physicalcomponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.sdn.CloudSimSDNTags;
import org.cloudbus.cloudsim.sdn.CloudletSchedulerMonitor;
import org.cloudbus.cloudsim.sdn.Packet;
import org.cloudbus.cloudsim.sdn.nos.NetworkOperatingSystem;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmAllocationInGroup;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmAllocationPolicyPriorityFirst;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmGroup;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.sdn.workload.Activity;
import org.cloudbus.cloudsim.sdn.workload.Processing;
import org.cloudbus.cloudsim.sdn.workload.Request;
import org.cloudbus.cloudsim.sdn.workload.Transmission;

/**
 * Extended class of Datacenter that supports processing SDN-specific events.
 * In addtion to the default Datacenter, it processes Request submission to VM,
 * and application deployment request. 
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class SDNDatacenter extends Datacenter {
	private NetworkOperatingSystem nos;
	private HashMap<Integer,Request> requestsTable = new HashMap<Integer, Request>();
	private static HashMap<Integer,Datacenter> globalVmDatacenterMap = new HashMap<Integer, Datacenter>();
	
	private static boolean isMigrateEnabled = false;
	
	// For results
	public static int migrationCompleted = 0;
	public static int migrationAttempted = 0;

	
	public SDNDatacenter(String name, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval, NetworkOperatingSystem nos) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

		this.nos=nos;

		//nos.init();
		if(vmAllocationPolicy instanceof VmAllocationPolicyPriorityFirst) {
			((VmAllocationPolicyPriorityFirst)vmAllocationPolicy).setTopology(nos.getPhysicalTopology());
		}
	}

	public static Datacenter findDatacenterGlobal(int vmId) {
		// Find a data center where the VM is placed
		return globalVmDatacenterMap.get(vmId);
	}

	public void addVm(Vm vm){
		getVmList().add(vm);
		if (vm.isBeingInstantiated()) vm.setBeingInstantiated(false);
		vm.updateCloudletsProcessing(CloudSim.clock(), getVmAllocationPolicy().getGuestTable().get(vm.getUid()).getGuestScheduler().getAllocatedMipsForVm(vm));
	}
		
	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		processVmCreateEvent((SDNVm) ev.getData(), ack);
		if(ack) {
			Vm vm = (Vm)ev.getData();
			send(nos.getId(), 0/*CloudSim.getMinTimeBetweenEvents()*/, CloudActionTags.VM_CREATE_ACK, vm);
		}
	}
	
	protected boolean processVmCreateEvent(SDNVm vm, boolean ack) {
		boolean result = getVmAllocationPolicy().allocateHostForGuest(vm);
		if (ack) {
			int[] data = new int[3];
			data[0] = getId();
			data[1] = vm.getId();

			if (result) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudActionTags.VM_CREATE_ACK, data);
		}

		if (result) {
			globalVmDatacenterMap.put(vm.getId(), this);
			
			getVmList().add(vm);

			if (vm.isBeingInstantiated()) {
				vm.setBeingInstantiated(false);
			}

			vm.updateCloudletsProcessing(CloudSim.clock(), getVmAllocationPolicy().getGuestTable().get(vm.getUid()).getGuestScheduler()
					.getAllocatedMipsForVm(vm));
		}

		return result;
	}
	
	protected boolean processVmCreateDynamic(SimEvent ev) {
		Object[] data = (Object[]) ev.getData();
		SDNVm vm = (SDNVm) data[0];
		NetworkOperatingSystem callbackNOS = (NetworkOperatingSystem) data[1];

		boolean result = processVmCreateEvent(vm, true);
		data[0] = vm;
		data[1] = result;
		send(callbackNOS.getId(), 0/*CloudSim.getMinTimeBetweenEvents()*/, CloudSimSDNTags.SDN_VM_CREATE_DYNAMIC_ACK, data);
		
		return result;
	}

	protected void processVmCreateInGroup(SimEvent ev, boolean ack) {
		@SuppressWarnings("unchecked")
		List<Object> params =(List<Object>)ev.getData();
		
		Vm vm = (Vm)params.get(0);
		VmGroup vmGroup=(VmGroup)params.get(1);

		boolean result = ((VmAllocationInGroup)getVmAllocationPolicy()).allocateHostForVmInGroup(vm, vmGroup);

		if (ack) {
			int[] data = new int[3];
			data[0] = getId();
			data[1] = vm.getId();

			if (result) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			send(vm.getUserId(), 0, CloudActionTags.VM_CREATE_ACK, data);
			send(nos.getId(), 0, CloudActionTags.VM_CREATE_ACK, vm);
		}

		if (result) {
			getVmList().add(vm);

			if (vm.isBeingInstantiated()) {
				vm.setBeingInstantiated(false);
			}

			vm.updateCloudletsProcessing(CloudSim.clock(), getVmAllocationPolicy().getGuestTable().get(vm.getUid()).getGuestScheduler()
					.getAllocatedMipsForVm(vm));
		}
	}	
	
	@Override
	protected void processVmMigrate(SimEvent ev, boolean ack) {
		migrationCompleted++;
		
		// Change network routing.
		@SuppressWarnings("unchecked")
		Map<String, Object> migrate = (HashMap<String, Object>) ev.getData();

		Vm vm = (Vm) migrate.get("vm");
		Host newHost = (Host) migrate.get("host");
		Host oldHost = (Host) vm.getHost();

		// Migrate the VM to another host.
		super.processVmMigrate(ev, ack);
		
		nos.processVmMigrate(vm, (SDNHost)oldHost, (SDNHost)newHost);
	}
	
	@Override
	public void processOtherEvent(SimEvent ev){
        if (ev.getTag().equals(CloudSimSDNTags.REQUEST_SUBMIT)) {
            processRequestSubmit((Request) ev.getData());
        } else if (ev.getTag().equals(CloudSimSDNTags.SDN_PACKET_COMPLETE)) {
            processPacketCompleted((Packet) ev.getData());
        } else if (ev.getTag().equals(CloudSimSDNTags.SDN_PACKET_FAILED)) {
            processPacketFailed((Packet) ev.getData());
        } else if (ev.getTag().equals(CloudSimSDNTags.SDN_VM_CREATE_IN_GROUP)) {
            processVmCreateInGroup(ev, false);
        } else if (ev.getTag().equals(CloudSimSDNTags.SDN_VM_CREATE_IN_GROUP_ACK)) {
            processVmCreateInGroup(ev, true);
        } else if (ev.getTag().equals(CloudSimSDNTags.SDN_VM_CREATE_DYNAMIC)) {
            processVmCreateDynamic(ev);
        } else {
            System.out.println("Unknown event recevied by SdnDatacenter. Tag:" + ev.getTag());
        }
	}

	public void processUpdateProcessing() {
		updateCloudletProcessing(); // Force Processing - TRUE!
		checkCloudletCompletion();
	}

	@Override
	protected void processCloudletSubmit(SimEvent ev, boolean ack) {
		// gets the Cloudlet object
		Cloudlet cl = (Cloudlet) ev.getData();
		
		// Clear out the processed data for the previous time slot before Cloudlet submitted
		updateCloudletProcessing();

		try {
			// checks whether this Cloudlet has finished or not
			if (cl.isFinished()) {
				String name = CloudSim.getEntityName(cl.getUserId());
				Log.println(getName() + ": Warning - Cloudlet #" + cl.getCloudletId() + " owned by " + name
						+ " is already completed/finished.");
				Log.println("Therefore, it is not being executed again");
				Log.println();

				// NOTE: If a Cloudlet has finished, then it won't be processed.
				// So, if ack is required, this method sends back a result.
				// If ack is not required, this method don't send back a result.
				// Hence, this might cause CloudSim to be hanged since waiting
				// for this Cloudlet back.
				if (ack) {
					int[] data = new int[3];
					data[0] = getId();
					data[1] = cl.getCloudletId();
					data[2] = CloudSimTags.FALSE;

					// unique tag = operation tag
					sendNow(cl.getUserId(), CloudActionTags.CLOUDLET_SUBMIT_ACK, data);
				}

				sendNow(cl.getUserId(), CloudActionTags.CLOUDLET_RETURN, cl);

				return;
			}

			// process this Cloudlet to this CloudResource
			cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics()
					.getCostPerBw());

			int userId = cl.getUserId();
			int vmId = cl.getGuestId();
			// time to transfer the files
			double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

			SDNHost host = (SDNHost)getVmAllocationPolicy().getHost(vmId, userId);
			Vm vm = (Vm)host.getGuest(vmId, userId);
			CloudletScheduler scheduler = vm.getCloudletScheduler();
			
			double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime); // This estimated time is useless

			//host.adjustMipsShare();
			//estimatedFinishTime = scheduler.getNextFinishTime(CloudSim.clock(), scheduler.getCurrentMipsShare());

			// Check the new estimated time by using host's update VM processing funciton.
			// This function is called only to check the next finish time
			estimatedFinishTime = host.updateCloudletsProcessing(CloudSim.clock());
			
			double estimatedFinishDelay = estimatedFinishTime - CloudSim.clock();
			//estimatedFinishTime -= CloudSim.clock();

			// if this cloudlet is in the exec queue
			if (estimatedFinishDelay > 0.0 && estimatedFinishTime < Double.MAX_VALUE) {
				estimatedFinishTime += fileTransferTime;
				//Log.printLine(getName() + ".processCloudletSubmit(): " + "Cloudlet is going to be processed at: "+(estimatedFinishTime + CloudSim.clock()));
				
				// gurantees a minimal interval before scheduling the event
				if (estimatedFinishDelay < CloudSim.getMinTimeBetweenEvents()) {
					estimatedFinishDelay = CloudSim.getMinTimeBetweenEvents();
				}

				send(getId(), estimatedFinishDelay, CloudActionTags.VM_DATACENTER_EVENT);
			}

			if (ack) {
				int[] data = new int[3];
				data[0] = getId();
				data[1] = cl.getCloudletId();
				data[2] = CloudSimTags.TRUE;

				// unique tag = operation tag
				sendNow(cl.getUserId(), CloudActionTags.CLOUDLET_SUBMIT_ACK, data);
			}
		} catch (ClassCastException c) {
			Log.println(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
			c.printStackTrace();
		} catch (Exception e) {
			Log.println(getName() + ".processCloudletSubmit(): " + "Exception error.");
			e.printStackTrace();
		}

		checkCloudletCompletion();
	}

	@Override
	protected void checkCloudletCompletion() {
		if(!nos.isApplicationDeployed())
		{
			super.checkCloudletCompletion();
			return;
		}

		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.<Vm>getGuestList()) {
				// Check all completed Cloudlets
				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					if (cl != null) {
						// For completed cloudlet -> process next activity.
						Request req = requestsTable.remove(cl.getCloudletId());
						req.getPrevActivity().setFinishTime(CloudSim.clock());
					
						if (req.isFinished()){
							// All requests are finished, no more activities to do. Return to user
							send(req.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimSDNTags.REQUEST_COMPLETED, req);
						} else {
							//consume the next activity from request. It should be a transmission.
							processNextActivity(req);
						}
					}
				}
				
				// Check all failed Cloudlets (time out)
				List<Cloudlet> failedCloudlet = ((CloudletSchedulerMonitor) (vm.getCloudletScheduler())).getFailedCloudlet();
				for(Cloudlet cl:failedCloudlet) {
					processCloudletFailed(cl);
				}
			}
		}
	}
	
	private void processRequestSubmit(Request req) {
		Activity ac = req.getNextActivity();
		
		if(ac instanceof Processing) {
			processNextActivity(req);
		}
		else {
			System.err.println("Request should start with Processing!!");
		}
	}

	private void processCloudletFailed(Cloudlet cl) {
		Log.printLine(CloudSim.clock() + ": " + getName() + ".processCloudletFailed(): Cloudlet failed: "+cl);
		
		Request req = requestsTable.remove(cl.getCloudletId());
		Activity prev = req.getPrevActivity();
		if(prev != null)
			prev.setFailedTime(CloudSim.clock()); // Set as finished.
		Activity next = req.getNextActivity();
		if(next != null)
			next.setFailedTime(CloudSim.clock()); // Set as finished.
		
		Request lastReq = req.getTerminalRequest(); 
		send(req.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimSDNTags.REQUEST_FAILED, lastReq);
	}

	
	private void processPacketFailed(Packet pkt) {
		Log.printLine(CloudSim.clock() + ": " + getName() + ".processPacketFailed(): Packet failed: "+pkt);		

		pkt.setPacketFailedTime(CloudSim.clock());
		Request req = pkt.getPayload();
		
		Request lastReq = req.getTerminalRequest(); 
		send(req.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimSDNTags.REQUEST_FAILED, lastReq);
	}

	private void processPacketCompleted(Packet pkt) {
		pkt.setPacketFinishTime(CloudSim.clock());
		Request req = pkt.getPayload();
		processNextActivity(req);
	}
	
	private void processNextActivity(Request req) {
//		Log.printLine(CloudSim.clock() + ": " + getName() + ": Process next activity: " +req);		
		Activity ac = req.removeNextActivity();
		ac.setStartTime(CloudSim.clock());

		if(ac instanceof Transmission) {
			processNextActivityTransmission((Transmission)ac);
		}
		else if(ac instanceof Processing) {
			processNextActivityProcessing(((Processing) ac), req);
		} else {
			Log.println(CloudSim.clock() + ": " + getName() + ": Activity is unknown..");
		}
	}
	
	private void processNextActivityTransmission(Transmission tr) {
		Packet pkt = tr.getPacket();
		
		//send package to router via channel (NOS)
		pkt = nos.addPacketToChannel(pkt);
		pkt.setPacketStartTime(CloudSim.clock());
		tr.setRequestedBW(nos.getRequestedBandwidth(pkt));
	}
	
	private void processNextActivityProcessing(Processing proc, Request reqAfterCloudlet) {
		Cloudlet cl = proc.getCloudlet();
		proc.clearCloudlet();
		
		requestsTable.put(cl.getCloudletId(), reqAfterCloudlet);
		sendNow(getId(), CloudActionTags.CLOUDLET_SUBMIT, cl);
		
		// Set the requested MIPS for this cloudlet.
		int userId = cl.getUserId();
		int vmId = cl.getGuestId();
		
		Host host = (Host)getVmAllocationPolicy().getHost(vmId, userId);
		if(host == null) {
			Vm orgVm = nos.getSFForwarderOriginalVm(vmId);
			if(orgVm != null) {
				vmId = orgVm.getId();
				cl.setGuestId(vmId);
				host = (Host)getVmAllocationPolicy().getHost(vmId, userId);
			}
			else {
				throw new NullPointerException("Error! cannot find a host for Workload:"+ proc+". VM="+vmId);
			}
		}
		Vm vm = (Vm)host.getGuest(vmId, userId);
		double mips = vm.getMips();
		proc.setVmMipsPerPE(mips);
	}
	
	public void printDebug() {
		System.err.println(CloudSim.clock()+": # of currently processing Cloudlets: "+this.requestsTable.size());
	}
	
	public void startMigrate() {
		if (isMigrateEnabled) {
			Log.println(CloudSim.clock()+": Migration started..");

			List<VmAllocationPolicy.GuestMapping> migrationMap = getVmAllocationPolicy().optimizeAllocation(
					getVmList());

			if (migrationMap != null && !migrationMap.isEmpty()) {
				migrationAttempted += migrationMap.size();
				
				// Process cloudlets before migration because cloudlets are processed during migration process..
				updateCloudletProcessing();
				checkCloudletCompletion();

				for (VmAllocationPolicy.GuestMapping migrate : migrationMap) {
					Vm vm = (Vm) migrate.vm();
					Host targetHost = (Host) migrate.host();
//					Host oldHost = vm.getHost();
					
					Log.formatLine(
							"%.2f: Migration of %s to %s is started",
							CloudSim.clock(),
							vm,
							targetHost);
					
					targetHost.addMigratingInGuest(vm);


					/** VM migration delay = RAM / bandwidth **/
					// we use BW / 2 to model BW available for migration purposes, the other
					// half of BW is for VM communication
					// around 16 seconds for 1024 MB using 1 Gbit/s network
					send(
							getId(),
							vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
							CloudActionTags.VM_MIGRATE,
							migrate);
				}
			}
			else {
				//Log.printLine(CloudSim.clock()+": No VM to migrate");
			}
		}		
	}
	
	public NetworkOperatingSystem getNOS() {
		return this.nos;
	}

	public String toString() {
		return "SDNDataCenter:(NOS="+nos.toString()+")";
	}
}
