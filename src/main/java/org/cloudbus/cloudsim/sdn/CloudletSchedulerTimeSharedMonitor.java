/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Consts;

public class CloudletSchedulerTimeSharedMonitor extends CloudletSchedulerTimeShared implements CloudletSchedulerMonitor {
	private double timeoutLimit = Double.POSITIVE_INFINITY;
	// For monitoring
	private double prevMonitoredTime = 0;
	private double vmMips = 0;
	
	
	public CloudletSchedulerTimeSharedMonitor(long vmMipsPerPE, double timeout) {
		vmMips = vmMipsPerPE;
		timeoutLimit = timeout;
	}

	public long getTotalProcessingPreviousTime(double currentTime, List<Double> mipsShare) {
		long totalProcessedMIs = 0;
		double timeSpent = currentTime - prevMonitoredTime;
		double capacity = getCapacity(mipsShare);
		
		for (Cloudlet cl : getCloudletExecList()) {
			totalProcessedMIs += (long) (capacity * timeSpent * cl.getNumberOfPes() * Consts.MILLION);
		}
		
		prevMonitoredTime = currentTime;
		return totalProcessedMIs;
	}
	
	@Override
	public double getTimeSpentPreviousMonitoredTime(double currentTime) {
        return currentTime - prevMonitoredTime;
	}

	@Override
	public boolean isVmIdle() {
        return runningCloudlets() <= 0;
    }

	@Override
	public int getCloudletTotalPesRequested() {
		int pesInUse = 0;
		for (Cloudlet cl : getCloudletExecList()) {
			pesInUse += cl.getNumberOfPes();
		}
		return pesInUse;
	}

	@Override
	public double updateCloudletsProcessing(double currentTime, List<Double> mipsShare) {
		double ret = super.updateCloudletsProcessing(currentTime, mipsShare);
		processTimeout(currentTime);
		return ret;
	}
	
	@Override
	public List<Cloudlet> getFailedCloudlet() {
        List<Cloudlet> failed = new ArrayList<Cloudlet>(getCloudletFailedList());
		getCloudletFailedList().clear();
		return failed;
	}

	private void processTimeout(double currentTime) {
		// Check if any cloudlet is timed out.
		if(timeoutLimit > 0 && Double.isFinite(timeoutLimit)) {
			double timeout = currentTime - this.timeoutLimit;
			List<Cloudlet> timeoutCloudlet = new ArrayList<Cloudlet>();
			
			for (Cloudlet cl : getCloudletExecList()) {
				if(cl.getSubmissionTime() < timeout) {
					timeoutCloudlet.add(cl);
				}
			}
			getCloudletFailedList().addAll(timeoutCloudlet);
			getCloudletExecList().removeAll(timeoutCloudlet);			
		}
		
	}

	private double getCapacity(List<Double> mipsShare) {
		setCurrentMipsShare(mipsShare);
		double capacity = getCurrentCapacity();
		double maxPeCapacityPerCloudlet = vmMips * Configuration.CPU_REQUIRED_MIPS_PER_WORKLOAD_PERCENT;
		if(capacity > maxPeCapacityPerCloudlet) {
			capacity = maxPeCapacityPerCloudlet;
		}
		return capacity;
	}
}
