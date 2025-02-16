package org.cloudbus.cloudsim.sdn.policies.selecthost;

import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

import java.util.List;
import java.util.Set;

public class HostSelectionPolicyCombinedMostFull<T extends HostEntity> implements SelectionPolicy<T> {
    /**
     * VM Allocation Policy - BW and Compute combined, BFF.
     * When select a host to create a new VM, this policy chooses
     * the most full host in terms of both compute power and network bandwidth.
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

        // freeReousrces : Weighted-calculated free resource percentage in each host
        double[] freeResources = new double[candidates.size()];
        double hostTotalMips = candidates.getFirst().getTotalMips();
        double hostTotalBw = candidates.getFirst().getBw();
        for (int i = 0; i < candidates.size(); i++) {
            HostEntity h = candidates.get(i);
            double mipsFreePercent = h.getGuestScheduler().getAvailableMips()/ hostTotalMips;
            double bwFreePercent = h.getGuestBwProvisioner().getAvailableBw()/hostTotalBw;
            freeResources[i] = mipsFreePercent * bwFreePercent;
        }

        double minAvailable = Double.MAX_VALUE;
        T selectedHost = null;
        for (int i = 0; i < candidates.size(); i++) {
            if (excludedCandidates.contains(candidates.get(i))) {
                continue;
            }

            if (freeResources[i] < minAvailable) {
                minAvailable = freeResources[i];
                selectedHost = candidates.get(i);
            }
        }
        return selectedHost;
    }
}
