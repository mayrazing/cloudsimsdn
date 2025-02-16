package org.cloudbus.cloudsim.sdn.policies.selecthost;

import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

import java.util.List;
import java.util.Set;

public class HostSelectionPolicyCombinedLeastFull<T extends HostEntity> implements SelectionPolicy<T> {
    /**
     * VM Allocation Policy - BW and Compute combined, LFF.
     * When select a host to create a new VM, this policy chooses
     * the least full host in terms of both compute power and network bandwidth.
     *
     * @param candidates             the candidate list
     * @param obj                    For arbitrary data
     * @param excludedCandidates candidates to be ignored from list
     * @return the selected host entity
     */
    @Override
    public T select(List<T> candidates, Object obj, Set<T> excludedCandidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        String sdnVmHostName = null;
        if (obj instanceof SDNVm vm) {
            if (vm.getHostName() != null) {
                sdnVmHostName = vm.getHostName();
            }
        }

        // freeReousrces : Weighted-calculated free resource percentage in each host
        double[] freeResources = new double[candidates.size()];
        double hostTotalMips = candidates.getFirst().getTotalMips();
        double hostTotalBw = candidates.getFirst().getBw();
        for (int i = 0; i < candidates.size(); i++) {
            HostEntity h = candidates.get(i);
            if (sdnVmHostName != null && sdnVmHostName.equals(((SDNHost) h).getName())) {
                // todo::zmy whether to use Double.NEGATIVE_INFINITY/Double.MAX_VALUE
                freeResources[i] = Double.NEGATIVE_INFINITY;
                continue;
            }

            double mipsFreePercent = h.getGuestScheduler().getAvailableMips()/ hostTotalMips;
            double bwFreePercent = h.getGuestBwProvisioner().getAvailableBw()/hostTotalBw;
            freeResources[i] = mipsFreePercent * bwFreePercent;
        }

        double maxAvailable = Double.MIN_VALUE;
        T selectedHost = null;
        for (int i = 0; i < candidates.size(); i++) {
            if (excludedCandidates.contains(candidates.get(i))) {
                continue;
            }

            if (freeResources[i] > maxAvailable) {
                maxAvailable = freeResources[i];
                selectedHost = candidates.get(i);
            }
        }
        return selectedHost;
    }
}
