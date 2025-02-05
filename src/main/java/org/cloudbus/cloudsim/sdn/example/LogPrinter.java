/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.example;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.sdn.Configuration;
import org.cloudbus.cloudsim.sdn.SDNBroker;
import org.cloudbus.cloudsim.sdn.monitor.power.PowerUtilizationHistoryEntry;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNDatacenter;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.physicalcomponents.switches.Switch;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;
import org.cloudbus.cloudsim.sdn.workload.Activity;
import org.cloudbus.cloudsim.sdn.workload.Processing;
import org.cloudbus.cloudsim.sdn.workload.Request;
import org.cloudbus.cloudsim.sdn.workload.Transmission;
import org.cloudbus.cloudsim.sdn.workload.Workload;

/**
 * This class is to print out logs into console.
 * 
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */
public class LogPrinter {
	private static double hostEnergyConsumption;
	private static double switchEnergyConsumption;
	private static double hostTotalTime;
	private static double hostOverTime;
	private static double hostOverScaleTime;
	private static double vmTotalTime;
	private static double vmOverTime;
	private static double vmOverScaleTime;

	public static void printEnergyConsumption(List<Host> hostList, List<Switch> switchList, double finishTime) {
		hostEnergyConsumption= 0; switchEnergyConsumption = 0;hostTotalTime =0; hostOverTime =0; hostOverScaleTime=0;
		vmTotalTime =0; vmOverTime =0; vmOverScaleTime=0;
		
		/*
		Log.println("========== HOST POWER CONSUMPTION calculated based MIPS allocation ===========");
		for(Host host:hostList) {
			// Allocated MIPS based power consumption
			PowerUtilizationInterface scheduler =  (PowerUtilizationInterface) host.getVmScheduler();
			scheduler.addUtilizationEntryTermination(finishTime);
			
			double energy = scheduler.getUtilizationEnergyConsumption();
			Log.println("[MIPS allocation] Host #"+host.getId()+": "+energy);
			hostEnergyConsumptionMIPS+= energy;
//			printHostUtilizationHistory(scheduler.getUtilizationHisotry());
		}
		//*/
		
		Log.println("========== HOST POWER CONSUMPTION based on Actual Workload processing ===========");
		for(Host host:hostList) {
			// Actual workload based power consumption
			double consumedEnergy = ((SDNHost)host).getConsumedEnergy();
			Log.println("Host #"+host.getId()+": "+consumedEnergy);
			hostEnergyConsumption+= consumedEnergy;
		}
		
		Log.println("========== SWITCH POWER CONSUMPTION AND DETAILED UTILIZATION ===========");
		for(Switch sw:switchList) {
			//sw.addUtilizationEntryTermination(finishTime);
			double energy = sw.getConsumedEnergy();
			Log.println("Switch:"+sw.getName()+": "+energy);
			switchEnergyConsumption+= energy;

//			printSwitchUtilizationHistory(sw.getUtilizationHisotry());

		}
		Log.println("========== HOST Overload percentage ===========");
		for(Host host:hostList) {
			// Overloaded time
			double overScaleTime = ((SDNHost)host).overloadLoggerGetScaledOverloadedDuration();
			double overTime = ((SDNHost)host).overloadLoggerGetOverloadedDuration();
			double totalTime = ((SDNHost)host).overloadLoggerGetTotalDuration();
			double overPercent = (totalTime != 0) ? overTime/totalTime : 0; 
			Log.println("Overload Host #"+host.getId()+": "+overTime+"/"+totalTime+"="+overPercent + "... Scaled Overload duration= "+overScaleTime);
			hostTotalTime += totalTime;
			hostOverTime += overTime;
			hostOverScaleTime += overScaleTime;
		}
		
		Log.println("========== VM Overload percentage ===========");
		for(Host host:hostList) {
			for (SDNVm vm : host.<SDNVm>getGuestList()) {
				// Overloaded time
				double overScaleTime = vm.overloadLoggerGetScaledOverloadedDuration();
				double overTime = vm.overloadLoggerGetOverloadedDuration();
				double totalTime = vm.overloadLoggerGetTotalDuration();
				double overPercent = (totalTime != 0) ? overTime/totalTime : 0; 
				Log.println("Vm("+vm+"): "+overTime+"/"+totalTime+"="+overPercent + "... Scaled Overload duration= "+overScaleTime);
				vmTotalTime += totalTime;
				vmOverTime += overTime;
				vmOverScaleTime += overScaleTime;
			}
		}
	}
	public static void printTotalEnergy() {
		Log.println("========== TOTAL POWER CONSUMPTION ===========");
		Log.println("Host energy consumed: "+hostEnergyConsumption);
		Log.println("Switch energy consumed: "+switchEnergyConsumption);
		Log.println("Total energy consumed: "+(hostEnergyConsumption+switchEnergyConsumption));
		//Log.println("Host (MIPS based) energy consumed: "+hostEnergyConsumptionMIPS);
		
		Log.println("========== MIGRATION ===========");
		Log.println("Attempted: " + SDNDatacenter.migrationAttempted);
		Log.println("Completed: " + SDNDatacenter.migrationCompleted);

		Log.println("========== HOST OVERLOADED ===========");
		Log.println("Scaled overloaded: " +( 1.0-(hostTotalTime == 0? 0:hostOverScaleTime/hostTotalTime)));
		Log.println("Overloaded Percent: " + (hostTotalTime == 0? 0: hostOverTime / hostTotalTime));
//		Log.println("Total Time: " + hostTotalTime);
//		Log.println("Overloaded Time: " + hostOverTime);
		
		Log.println("========== VM OVERLOADED ===========");
		Log.println("Scaled overloaded: " + (1.0-(vmTotalTime == 0? 0:vmOverScaleTime/vmTotalTime)));
		Log.println("Overloaded Percent: " + (vmTotalTime == 0? 0: vmOverTime / vmTotalTime));
//		Log.println("Total Time: " + vmTotalTime);
//		Log.println("Overloaded Time: " + vmOverTime);
	}

	protected static void printHostUtilizationHistory(
			List<PowerUtilizationHistoryEntry> utilizationHisotry) {
		if(utilizationHisotry != null)
			for(PowerUtilizationHistoryEntry h:utilizationHisotry) {
				Log.println(h.startTime+", "+h.utilPercentage);
			}
	}
	
	static public String indent = ",";
	static public String tabSize = "10";
	static public String fString = 	"%"+tabSize+"s"+indent;
	static public String fInt = 	"%"+tabSize+"d"+indent;
	static public String fFloat = 	"%"+tabSize+".3f"+indent;
	
	public static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		Log.println();
		Log.println("========== OUTPUT ==========");
		
		Log.print(String.format(LogPrinter.fString, "Cloudlet_ID"));
		Log.print(String.format(LogPrinter.fString, "STATUS" ));
		Log.print(String.format(LogPrinter.fString, "DataCenter_ID"));
		Log.print(String.format(LogPrinter.fString, "VM_ID"));
		Log.print(String.format(LogPrinter.fString, "Length"));
		Log.print(String.format(LogPrinter.fString, "Time"));
		Log.print(String.format(LogPrinter.fString, "Start Time"));
		Log.print(String.format(LogPrinter.fString, "Finish Time"));
		Log.print("\n");

		//DecimalFormat dft = new DecimalFormat("######.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			printCloudlet(cloudlet);
		}
	}
	
	private static void printCloudlet(Cloudlet cloudlet) {
		Log.print(String.format(LogPrinter.fInt, cloudlet.getCloudletId()));

		if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
			Log.print(String.format(LogPrinter.fString, "SUCCESS"));
			Log.print(String.format(LogPrinter.fInt, cloudlet.getResourceId()));
			Log.print(String.format(LogPrinter.fInt, cloudlet.getGuestId()));
			Log.print(String.format(LogPrinter.fInt, cloudlet.getCloudletLength()));
			Log.print(String.format(LogPrinter.fFloat, cloudlet.getActualCPUTime()));
			Log.print(String.format(LogPrinter.fFloat, cloudlet.getSubmissionTime()));
			Log.print(String.format(LogPrinter.fFloat, cloudlet.getExecFinishTime()));
			Log.print("\n");
		}
		else {
			Log.println("FAILED");
		}
	}
	
	private static double startTime, finishTime;
	private static int[] appIdNum = new int[SDNBroker.lastAppId];
	private static double[] appIdTime = new double[SDNBroker.lastAppId];
	private static double[] appIdStartTime = new double[SDNBroker.lastAppId];
	private static double[] appIdFinishTime = new double[SDNBroker.lastAppId];
	private static double totalTime = 0.0;
	
	protected static void printWorkload(Workload wl) {
		double serveTime;
		
		startTime = finishTime = -1;

		Log.print(String.format(LogPrinter.fInt, wl.appId));
		printRequest(wl.request);
		
		serveTime= (finishTime - startTime);
		
		Log.print(String.format(LogPrinter.fFloat, serveTime));
		Log.println();
		
		totalTime += serveTime;
		
		appIdNum[wl.appId] ++;	//How many workloads in this app.
		appIdTime[wl.appId] += serveTime;
		if(appIdStartTime[wl.appId] <=0) {
			appIdStartTime[wl.appId] = wl.time;
		}
		appIdFinishTime[wl.appId] = wl.time;
	}
	
	public static void printWorkloadList(List<Workload> wls) {
		Log.print(String.format(LogPrinter.fString, "App_ID"));
		printRequestTitle(wls.get(0).request);
		Log.print(String.format(LogPrinter.fString, "ResponseTime"));
		Log.println();

		for(Workload wl:wls) {
			printWorkload(wl);
		}

		Log.println("========== AVERAGE RESULT OF WORKLOADS ===========");
		for(int i=0; i<SDNBroker.lastAppId; i++) {
			Log.println("App Id ("+i+"): "+appIdNum[i]+" requests, Start=" + appIdStartTime[i]+
					", Finish="+appIdFinishTime[i]+", Rate="+(double)appIdNum[i]/(appIdFinishTime[i] - appIdStartTime[i])+
					" req/sec, Response time=" + appIdTime[i]/appIdNum[i]);
		}
		
		//printGroupStatistics(WORKLOAD_GROUP_PRIORITY, appIdNum, appIdTime);
		
		Log.println("Average Response Time:"+(totalTime / wls.size()));
		
	}
/*
 * 
	public static void printWorkloadList(List<Workload> wls) {
		
		Log.println();
		Log.println("========== DETAILED RESPONSE TIME OF WORKLOADS ===========");

		if(wls.size() == 0) return;
		
		Log.print(String.format(LogPrinter.fString, "App_ID"));
		printRequestTitle(wls.get(0).request);
		Log.print(String.format(LogPrinter.fString, "ResponseTime"));
		Log.println();

		for(Workload wl:wls) {
			printWorkload(wl);
		}

		Log.println("========== AVERAGE RESULT OF WORKLOADS ===========");
		for(int i=0; i<SDNBroker.lastAppId; i++) {
			Log.println("App Id ("+i+"): "+appIdNum[i]+" requests, Start=" + appIdStartTime[i]+
					", Finish="+appIdFinishTime[i]+", Rate="+(double)appIdNum[i]/(appIdFinishTime[i] - appIdStartTime[i])+
					" req/sec, Response time=" + appIdTime[i]/appIdNum[i]);
		}
		
		//printGroupStatistics(WORKLOAD_GROUP_PRIORITY, appIdNum, appIdTime);
		
		Log.println("Average Response Time:"+(totalTime / wls.size()));
		
	}

 */
	private static void printRequestTitle(Request req) {
		//Log.print(String.format(LogPrinter.fString, "Req_ID"));
		//Log.print(String.format(LogPrinter.fFloat, req.getStartTime()));
		//Log.print(String.format(LogPrinter.fFloat, req.getFinishTime()));
		
		List<Activity> acts = req.getRemovedActivities();
		for(Activity act:acts) {
			if(act instanceof Transmission) {
				Transmission tr=(Transmission)act;
				Log.print(String.format(LogPrinter.fString, "Tr:Size"));
				Log.print(String.format(LogPrinter.fString, "Tr:Channel"));
				
				Log.print(String.format(LogPrinter.fString, "Tr:time"));
				Log.print(String.format(LogPrinter.fString, "Tr:Start"));
				Log.print(String.format(LogPrinter.fString, "Tr:End"));
				
				printRequestTitle(tr.getPacket().getPayload());
			}
			else {
				Log.print(String.format(LogPrinter.fString, "Pr:Size"));
				
				Log.print(String.format(LogPrinter.fString, "Pr:time"));
				Log.print(String.format(LogPrinter.fString, "Pr:Start"));
				Log.print(String.format(LogPrinter.fString, "Pr:End"));
			}
		}
	}
	
	public static void printRequest(Request req) {
		//Log.print(String.format(LogPrinter.fInt, req.getRequestId()));
		//Log.print(String.format(LogPrinter.fFloat, req.getStartTime()));
		//Log.print(String.format(LogPrinter.fFloat, req.getFinishTime()));
		
		List<Activity> acts = req.getRemovedActivities();
		for(Activity act:acts) {
			if(act instanceof Transmission) {
				Transmission tr=(Transmission)act;
				Log.print(String.format(LogPrinter.fInt, tr.getPacket().getSize()));
				Log.print(String.format(LogPrinter.fInt, tr.getPacket().getFlowId()));
				
				Log.print(String.format(LogPrinter.fFloat, tr.getPacket().getFinishTime() - tr.getPacket().getStartTime()));
				Log.print(String.format(LogPrinter.fFloat, tr.getPacket().getStartTime()));
				Log.print(String.format(LogPrinter.fFloat, tr.getPacket().getFinishTime()));
				
				printRequest(tr.getPacket().getPayload());
			}
			else {
				Processing pr=(Processing)act;
				Log.print(String.format(LogPrinter.fInt, pr.getCloudlet().getCloudletLength()));

				Log.print(String.format(LogPrinter.fFloat, pr.getCloudlet().getActualCPUTime()));
				Log.print(String.format(LogPrinter.fFloat, pr.getCloudlet().getSubmissionTime()));
				Log.print(String.format(LogPrinter.fFloat, pr.getCloudlet().getExecFinishTime()));

				if(startTime == -1) startTime = pr.getCloudlet().getExecStartTime();
				finishTime=pr.getCloudlet().getExecFinishTime();
			}
		}
	}
	
	public static void printGroupStatistics(int groupSeperateNum, int[] appIdNum, double[] appIdTime) {

		double prioritySum = 0, standardSum = 0;
		int priorityReqNum = 0, standardReqNum =0;
		
		for(int i=0; i<SDNBroker.lastAppId; i++) {
			double avgResponseTime = appIdTime[i]/appIdNum[i];
			if(i<groupSeperateNum) {
				prioritySum += avgResponseTime;
				priorityReqNum += appIdNum[i];
			}
			else {
				standardSum += avgResponseTime;
				standardReqNum += appIdNum[i];
			}
		}

		Log.println("Average Response Time(Priority):"+(prioritySum / priorityReqNum));
		Log.println("Average Response Time(Standard):"+(standardSum / standardReqNum));
	}
	
	public static void printConfiguration() {
		Log.println("========== CONFIGURATIONS ===========");
		Log.println("workingDirectory :"+Configuration.workingDirectory);
		
		
		//Log.println("minTimeBetweenEvents: "+Configuration.minTimeBetweenEvents);
		//Log.println("resolutionPlaces:"+Configuration.resolutionPlaces);
		//Log.println("timeUnit:"+Configuration.timeUnit);
		
		Log.println("overbookingTimeWindowInterval:"+ Configuration.overbookingTimeWindowInterval);	// Time interval between points 

		Log.println("OVERLOAD_THRESHOLD:"+ Configuration.OVERLOAD_THRESHOLD);
		Log.println("OVERLOAD_THRESHOLD_ERROR:"+ Configuration.OVERLOAD_THRESHOLD_ERROR);
		Log.println("OVERLOAD_THRESHOLD_BW_UTIL:"+ Configuration.OVERLOAD_THRESHOLD_BW_UTIL);
	
		Log.println("UNDERLOAD_THRESHOLD_HOST:"+ Configuration.UNDERLOAD_THRESHOLD_HOST);
		Log.println("UNDERLOAD_THRESHOLD_HOST_BW:"+ Configuration.UNDERLOAD_THRESHOLD_HOST_BW);
		Log.println("UNDERLOAD_THRESHOLD_VM:"+ Configuration.UNDERLOAD_THRESHOLD_VM);
		
		Log.println("DECIDE_SLA_VIOLATION_GRACE_ERROR:"+ Configuration.DECIDE_SLA_VIOLATION_GRACE_ERROR);
		

		Log.println("==================================================");
		Log.println("========== PARAMETERS ===========");
		Log.println("experimentFolder :" + Configuration.experimentFolder);
		
		Log.println("CPU_REQUIRED_MIPS_PER_WORKLOAD_PERCENT:"+ Configuration.CPU_REQUIRED_MIPS_PER_WORKLOAD_PERCENT);
		
		Log.println("monitoringTimeInterval:"+ Configuration.monitoringTimeInterval); // every 60 seconds, polling utilization.
		Log.println("overbookingTimeWindowNumPoints:"+ Configuration.overbookingTimeWindowNumPoints);	// How many points to track
		Log.println("migrationTimeInterval:"+ Configuration.migrationTimeInterval); // every 1 seconds, polling utilization.
	
		Log.println("OVERBOOKING_RATIO_MAX:"+ Configuration.OVERBOOKING_RATIO_MAX); 
		Log.println("OVERBOOKING_RATIO_MIN:"+ Configuration.OVERBOOKING_RATIO_MIN);
		Log.println("OVERBOOKING_RATIO_INIT:"+ Configuration.OVERBOOKING_RATIO_INIT);
		
		Log.println("OVERBOOKING_RATIO_UTIL_PORTION:"+ Configuration.OVERBOOKING_RATIO_UTIL_PORTION);	
		Log.println("OVERLOAD_HOST_PERCENTILE_THRESHOLD:"+ Configuration.OVERLOAD_HOST_PERCENTILE_THRESHOLD);
		
	}	
}
