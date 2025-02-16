/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.provisioners;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.sdn.Configuration;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;

/**
 * BwProvisionerSimple is a class that implements a simple best effort allocation policy: if there
 * is bw available to request, it allocates; otherwise, it fails.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class BwProvisionerOverbookable extends BwProvisionerSimple {
	private static final double overbookingRatioBw = 4.0;	// 20% overbooking allowed for BW

	public BwProvisionerOverbookable(long bw) {
		super(bw);
		setAvailableBw((long) getOverbookableBw(bw));	//overwrite available BW to overbookable BW
	}

	@Override
	public boolean allocateBwForGuest(GuestEntity guest, long bw) {
		// TODO::zmy test
		double overRation = Configuration.OVERBOOKING_RATIO_INIT;
		long old_bw = getAllocatedBwForGuest(guest);

		HostEntity host = guest.getHost();
		if (old_bw != 0) {
			overRation = getDynamicOverRatioBw((SDNVm)guest, (SDNHost)host);
		}

		long overBw = (long) (overRation * bw);
		if (getAvailableBw() + old_bw >= overBw) {
			setAvailableBw(getAvailableBw() + old_bw - overBw);
			getBwTable().put(guest.getUid(), overBw);
			guest.setCurrentAllocatedBw(overBw);
			return true;
		}

		return false;
	}

	protected double getDynamicOverRatioBw(SDNVm vm, Host host) {
		if(vm.getMonitoringValuesVmBwUtilization().getNumberOfPoints() == 0) {
			return Configuration.OVERBOOKING_RATIO_INIT;
		}

		double avgCC = getAverageCorrelationCoefficientBW((SDNVm) vm, (SDNHost)host);	// Average Correlation between -1 and 1
		double delta = Configuration.OVERBOOKING_RATIO_MAX - Configuration.OVERBOOKING_RATIO_MIN;
		if(avgCC >1 || avgCC <-1) {
			System.err.println("getDynamicOverRatioMips: CC is wrong! "+avgCC);
			System.exit(0);
		}
		return Configuration.OVERBOOKING_RATIO_MIN + (avgCC+1)*delta/2.0 ;	// AvgCC+1 is between 0 and 2
	}

	protected double getAverageCorrelationCoefficientBW(SDNVm newVm, SDNHost host) {
		if(host.getGuestList().isEmpty()) {
			//System.err.println("getAverageCorrelationCoefficient: No VM in the host");
			return -1;
		}
		double interval = Configuration.overbookingTimeWindowInterval;
		double timeWindow = Configuration.overbookingTimeWindowNumPoints * interval;
		double endTime = CloudSim.clock();
		double startTime = endTime - timeWindow > 0 ? endTime - timeWindow : 0;

		double sumCoef= 0.0;
		double [] newVmHistory = newVm.getMonitoringValuesVmBwUtilization().getValuePoints(startTime, endTime, interval);

		for(SDNVm v:host.<SDNVm>getGuestList()) {
			// calculate correlation coefficient between the target VM and existing VMs in the host.
			double [] vHistory = v.getMonitoringValuesVmBwUtilization().getValuePoints(startTime, endTime, interval);
			double cc = calculateCorrelationCoefficient(newVmHistory, vHistory);
			if(cc >= -1 && cc <= 1)
				sumCoef += cc;
		}

		return sumCoef / host.getGuestList().size();
	}

	private static PearsonsCorrelation pearson = new PearsonsCorrelation();
	private static double calculateCorrelationCoefficient(double [] x, double [] y) {
		if(x.length > 1)
			return pearson.correlation(x, y);
		return 0.0;
	}


	@Override
	public void deallocateBwForAllVms() {
		super.deallocateBwForAllVms();
		
		setAvailableBw((long) getOverbookableBw(getBw()));	//Overbooking
		getBwTable().clear();
	}

	private static double getOverbookableBw(long capacity) {
		double overbookedBw = capacity * overbookingRatioBw;
		return overbookedBw;		
	}

}
