package org.cloudbus.cloudsim.sdn.policies.vmallocation;

import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.sdn.physicalcomponents.switches.EdgeSwitch;

import java.util.List;

public class HostGroup implements Comparable<HostGroup> {
    EdgeSwitch edge=null;
    List<HostEntity> hosts=null;
    int numHosts=0;
    double availableMips=0;
    double availableBw=0;

    @Override
    public int compareTo(HostGroup o) {
        return (int) (o.availableMips-this.availableMips);
    }

    public boolean contains(HostEntity o) {
        return this.hosts.contains(o);
    }
}
