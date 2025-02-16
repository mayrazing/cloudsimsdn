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
import java.util.Optional;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Consts;

public class CloudletSchedulerSpaceSharedMonitor extends CloudletSchedulerSpaceShared implements CloudletSchedulerMonitor {
	// For monitoring
	private double prevMonitoredTime = 0;
	private double timeoutLimit = Double.POSITIVE_INFINITY;

	public CloudletSchedulerSpaceSharedMonitor(double timeOut) {
		super();
		this.timeoutLimit = timeOut;
	}
	
	@Override
	public double updateCloudletsProcessing(double currentTime, List<Double> mipsShare) {
		double ret = super.updateCloudletsProcessing(currentTime, mipsShare);
		processTimeout(currentTime);
		return ret;
	}

	@Override
	public double updateCurrentCapacity() {
		getCurrentMipsShare().removeIf(mips -> mips <= 0);

		final double[] capacity = {0.0};
		Optional.ofNullable(getCurrentMipsShare()).ifPresent(mips -> {
			for (Double mipsValue : mips) {
				capacity[0] += mipsValue;
			}
			capacity[0] /= getCurrentMipsShare().size();
			setCurrentCapacity(capacity[0]);
		});
		return capacity[0];
	}
	
	@Override
	public List<Cloudlet> getFailedCloudlet() {
        List<Cloudlet> failedCls = new ArrayList<Cloudlet>(getCloudletFailedList());
		getCloudletFailedList().clear();
		return failedCls;
	}

	protected void processTimeout(double currentTime) {
		// Check if any cloudlet is timed out.
		if(timeoutLimit > 0 && Double.isFinite(timeoutLimit)) {
			double timeout = currentTime - this.timeoutLimit;
			{
				List<Cloudlet> timeoutCloudlet = new ArrayList<Cloudlet>();
				for (Cloudlet cl : getCloudletExecList()) {
					if(cl.getSubmissionTime() < timeout) {
						cl.updateStatus(Cloudlet.CloudletStatus.FAILED);
						cl.finalizeCloudlet();
						timeoutCloudlet.add(cl);
						usedPes -= cl.getNumberOfPes();
					}
				}
				getCloudletExecList().removeAll(timeoutCloudlet);
				getCloudletFailedList().addAll(timeoutCloudlet);
			}
			{			
				List<Cloudlet> timeoutCloudlet = new ArrayList<Cloudlet>();
				for (Cloudlet cl : getCloudletWaitingList()) {
					if(cl.getSubmissionTime() < timeout) {
						cl.updateStatus(Cloudlet.CloudletStatus.FAILED);
						cl.finalizeCloudlet();
						timeoutCloudlet.add(cl);
					}
				}
				getCloudletWaitingList().removeAll(timeoutCloudlet);
				getCloudletFailedList().addAll(timeoutCloudlet);
			}
			
		}
	}

	@Override
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

	protected double getCapacity(List<Double> mipsShare) {
		double capacity = 0.0;
		int cpus = 0;
		for (Double mips : mipsShare) {
			capacity += mips;
			if (mips > 0.0) {
				cpus++;
			}
		}
		capacity /= cpus;
		return capacity;
	}

	@Override
	public boolean isVmIdle() {
		if(runningCloudlets() > 0)
			return false;
        return getCloudletWaitingList().isEmpty();
    }

	@Override
	public double getTimeSpentPreviousMonitoredTime(double currentTime) {
        return currentTime - prevMonitoredTime;
	}

	@Override
	public int getCloudletTotalPesRequested() {
		return getCurrentMipsShare().size();
	}
	
	
	public int getNumAllCloudlets() {
		return super.cloudletExecList.size() + super.cloudletFailedList.size() + super.getCloudletFinishedList().size() +
				super.cloudletPausedList.size() + super.cloudletWaitingList.size();
	}
}
