package org.cloudbus.cloudsim.sdn.policies.vmallocation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.monitor.power.PowerUtilizationMaxHostInterface;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

import java.util.*;

public class VmAllocationWithSelectionPolicyEx extends VmAllocationWithSelectionPolicy implements PowerUtilizationMaxHostInterface {
    private VmMigrationPolicy vmMigrationPolicy = null;

    /** The resources in migration */
    private Map<String, Integer> migrationPes;
    private Map<String, Double> migrationMips;
    private Map<String, Long> migrationBw;

    public VmAllocationWithSelectionPolicyEx(List<? extends HostEntity> list,
                                             SelectionPolicy<HostEntity> selectionPolicy,
                                             VmMigrationPolicy vmMigrationPolicy) {
        super(list, selectionPolicy);
        setVmMigrationPolicy(vmMigrationPolicy);

        migrationPes = new HashMap<String, Integer>();
        migrationMips = new HashMap<String, Double>();
        migrationBw = new HashMap<String, Long>();
    }

    @Override
    public boolean allocateHostForGuest(GuestEntity guest, HostEntity host) {
        if (super.allocateHostForGuest(guest, host)) {
            updatePeStatus(guest, host);
            dealMigrationResources(host, guest);
            logMaxNumHostsUsed();
            return true;
        }
        return false;
    }

    protected boolean allocateHostForGuest(Vm vm, List<? extends HostEntity> candidateHosts) {
        if (getGuestTable().containsKey(vm.getUid())) { // if this vm was not created
            return false;
        }

        boolean result = false;
        for(HostEntity host : candidateHosts) {
            result = host.guestCreate(vm);
            if (result) {
                // if vm were succesfully created in the host
                getGuestTable().put(vm.getUid(), host);
                dealMigrationResources(host, vm);
                break;
            }
        }

        if(!result) {
            System.err.println("VmAllocationWithSelectionPolicyEx: WARNING:: Cannot create VM!!!!");
        }

        logMaxNumHostsUsed();
        return result;
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
            updatePeStatus(vm, host);
        }
    }

    /**
     * Creates a migration map that describes which VM to migrate to which host
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.VmAllocationPolicy#optimizeAllocation(List<? extends GuestEntity>)
     */
    @Override
    public List<GuestMapping> optimizeAllocation(List<? extends GuestEntity> vmList) {
        List<GuestMapping> migrationMapLists = new ArrayList<>();
        if (getVmMigrationPolicy() != null) {
            List<Map<String, Object>> migrationList = getVmMigrationPolicy().getMigrationMap(this.<SDNHost>getHostList());
            if (migrationList != null && !migrationList.isEmpty()) {
                for (Map<String, Object> migrate : migrationList) {
                    GuestEntity vm = (GuestEntity) migrate.get("vm");
                    HostEntity host = (HostEntity) migrate.get("host");
                    if (host != null) {
                        migrationMapLists.add(new GuestMapping(vm, host));
                    }
                }
            }
        }
        return migrationMapLists;
    }

    public void updateResourceAllocation(Host host) {
        // Update the resource allocation ratio of every VM
        return;
    }

    public <T extends HostEntity> List<T> findHostsForGuestBySelectionPolicy(SelectionPolicy<T> selectionPolicy,
                                                                  GuestEntity guest,
                                                                  List<T> candidates) {
        List<T> selectedHostList = new ArrayList<>();
        Set<T> excludedHostCandidates = new HashSet<>();
        int tries = 0;
        do{
            T selectedHost = selectionPolicy.select(candidates, guest, excludedHostCandidates);
            if(selectedHost == null){
                return null;
            }
            if (selectedHost.isSuitableForGuest(guest)) {
                selectedHostList.add(selectedHost);
            } else {
                excludedHostCandidates.add(selectedHost);
                tries ++;
            }
        } while (tries < getHostList().size());
        return selectedHostList;
    }

    public HostEntity findHostForGuest(GuestEntity guest, List<HostEntity> candidates) {
        super.clearExcludedHostCandidates();
        int tries = 0;

        do{
            HostEntity selectedHost = getSelectionPolicy().select(candidates, guest, getExcludedHostCandidates());
            if(selectedHost == null){
                return null;
            }

            if (selectedHost.isSuitableForGuest(guest)) {
                return selectedHost;
            } else {
                getExcludedHostCandidates().add(selectedHost);
                tries ++;
            }
        } while (tries < getHostList().size());
        return null;
    }

    protected void reserveResourcesForMigration(HostEntity host, GuestEntity vm) {
        double overbookingRatioMips = getOverRatioMips(vm, host);
        double overbookingRatioBw = getOverRatioBw(vm, host);

        int pe = vm.getNumberOfPes();
        double adjustedMips = vm.getTotalMips() * overbookingRatioMips;
        long adjustedBw = (long) (vm.getBw() * overbookingRatioBw);

        migrationPes.put(vm.getUid(), pe);
        //getFreePes().set(idx, getFreePes().get(idx) - pe);

        migrationMips.put(vm.getUid(), adjustedMips);
        //host.getGuestScheduler().allocatePesForGuest(vm, new ArrayList<>(List.of(adjustedMips)));
        //getFreeMips().set(idx, (getFreeMips().get(idx) - adjustedMips));

        migrationBw.put(vm.getUid(), adjustedBw);
        //host.getGuestBwProvisioner().allocateBwForGuest(vm, adjustedBw);
        //getFreeBw().set(idx, getFreeBw().get(idx) - adjustedBw);

        Log.println(CloudSim.clock() + ": reserveResourcesForMigration() " + vm + " MIPS:" + adjustedMips+"(OR:" + overbookingRatioMips+")");
        Log.println(CloudSim.clock() + ": reserveResourcesForMigration() " + vm + " BW:"+ adjustedBw+"(OR:" + overbookingRatioBw+")");
    }

    protected double getOverRatioMips(GuestEntity vm, HostEntity host) {
        return 1.0;	// 100% requested resource is given. No overbooking
    }

    protected double getOverRatioBw(GuestEntity vm, HostEntity host) {
        return 1.0;	// 100% requested resource is given. No overbooking
    }

    private void updatePeStatus(GuestEntity vm, HostEntity host) {
//        if (host.getGuestScheduler() instanceof VmSchedulerTimeShared) {
//            Map<String, List<Pe>> peMap = host.getGuestScheduler().getPeMap();
//            Optional.ofNullable(peMap)
//                    .filter(map -> !map.isEmpty())
//                    .ifPresent(map -> {
//                        // peMap is not null and not empty
//                        map.get(vm.getUid()).forEach(pe -> {
//                            if (pe.getPeProvisioner().getAvailableMips() == pe.getPeProvisioner().getMips()) {
//                                pe.setStatus(Pe.FREE);
//                            } else {
//                                pe.setStatus(Pe.BUSY);
//                            }
//                        });
//                    });
//        } else {
            host.getPeList().forEach(pe -> {
                if (pe.getPeProvisioner().getAvailableMips() == pe.getPeProvisioner().getMips()) {
                    pe.setStatus(Pe.FREE);
                } else {
                    pe.setStatus(Pe.BUSY);
                }
            });
        //}
    }

    // deal with migration resources in the Host for the VM
    private void dealMigrationResources(HostEntity hostEnt, GuestEntity vm) {
        ((SDNVm)vm).addMigrationHistory((SDNHost) hostEnt);
        Integer pe = migrationPes.remove(vm.getUid());
        Double mips = migrationMips.remove(vm.getUid());
        Long bw = migrationBw.remove(vm.getUid());

        if (pe != null) {
            // This vm is in migration
            Log.println(vm + " VM resource reservation needs to be used for migration");
//            getUsedPes().put(vm.getUid(), pe);
//            getUsedMips().put(vm.getUid(), (long) mips);
//            getUsedBw().put(vm.getUid(), (long) bw);
        }
    }



    protected int maxNumHostsUsed = 0;
    @Override
    public void logMaxNumHostsUsed() {
        // Get how many are used
        int numHostsUsed=0;
        int hostTotalPes = getHostList().getFirst().getNumberOfPes();
        for (HostEntity host : getHostList()) {
            //int hostTotalPes = host.getNumberOfPes();
            int freePes = host.getNumberOfFreePes();
            if(freePes < hostTotalPes) {
                numHostsUsed++;
                // TODO::zmy delete
                Log.println("usedPes:" + (hostTotalPes - freePes));
            }
        }

        if(maxNumHostsUsed < numHostsUsed)
            maxNumHostsUsed = numHostsUsed;
        Log.println("Number of online hosts:" + numHostsUsed + ", max was =" + maxNumHostsUsed);
    }

    @Override
    public int getMaxNumHostsUsed() {
        return maxNumHostsUsed;
    }

    public VmMigrationPolicy getVmMigrationPolicy() {
        return vmMigrationPolicy;
    }

    public void setVmMigrationPolicy(VmMigrationPolicy vmMigrationPolicy) {
        this.vmMigrationPolicy = vmMigrationPolicy;
        if (this.vmMigrationPolicy != null)
            this.vmMigrationPolicy.setVmAllocationPolicy(this);
    }
}
