package org.cloudbus.cloudsim.sdn;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.monitor.power.PowerUtilizationHistoryEntry;
import org.cloudbus.cloudsim.sdn.monitor.power.PowerUtilizationInterface;

import java.util.ArrayList;
import java.util.List;

public class VmSchedulerSpaceSharedEnergy extends VmSchedulerSpaceShared implements PowerUtilizationInterface {

    private List<PowerUtilizationHistoryEntry> utilizationHistories = null;
    private static double powerOffDuration = 0; //if host is idle for 1 hours, it's turned off.


    public VmSchedulerSpaceSharedEnergy(List<? extends Pe> pelist) {
        super(pelist);
    }

    @Override
    protected void setAvailableMips(double availableMips) {
        super.setAvailableMips(availableMips);
        addUtilizationEntry();
    }

    @Override
    public void addUtilizationEntryTermination(double terminatedTime) {
        if(this.utilizationHistories != null)
            this.utilizationHistories.add(new PowerUtilizationHistoryEntry(terminatedTime, 0));
    }

    @Override
    public List<PowerUtilizationHistoryEntry> getUtilizationHisotry() {
        return utilizationHistories;
    }

    @Override
    public double getUtilizationEnergyConsumption() {
        double total=0;
        double lastTime=0;
        double lastUtilPercentage=0;
        if(this.utilizationHistories == null)
            return 0;

        for(PowerUtilizationHistoryEntry h:this.utilizationHistories) {
            double duration = h.startTime - lastTime;
            double utilPercentage = lastUtilPercentage;
            double power = calculatePower(utilPercentage);
            double energyConsumption = power * duration;

            // Assume that the host is turned off when duration is long enough
            if(duration > powerOffDuration && lastUtilPercentage == 0)
                energyConsumption = 0;

            total += energyConsumption;
            lastTime = h.startTime;
            lastUtilPercentage = h.utilPercentage;
        }
        return total/3600;	// transform to Whatt*hour from What*seconds
    }

    private void addUtilizationEntry() {
        double time = CloudSim.clock();
        double totalMips = getTotalMips();
        double usingMips = totalMips - this.getAvailableMips();
        if(usingMips < 0) {
            System.err.println("addUtilizationEntry : using mips is negative, No way!");
        }
        if(utilizationHistories == null)
            utilizationHistories = new ArrayList<PowerUtilizationHistoryEntry>();
        this.utilizationHistories.add(new PowerUtilizationHistoryEntry(time, usingMips/getTotalMips()));
    }

    private double getTotalMips() {
        return this.getPeList().size() * this.getPeCapacity();
    }

    private double calculatePower(double u) {
        return 120 + 154 * u;
    }


}
