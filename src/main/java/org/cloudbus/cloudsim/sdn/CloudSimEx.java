/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */
 
package org.cloudbus.cloudsim.sdn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.EventQueue;
import org.cloudbus.cloudsim.core.SimEvent;

public class CloudSimEx extends CloudSim {
	private static long startTime;
	
	private static void setStartTimeMillis(long startedTime) {
		startTime=startedTime;
	}
	public static void setStartTime() {
		setStartTimeMillis(System.currentTimeMillis());
	}
	
	public static long getElapsedTimeSec() {
		long currentTime = System.currentTimeMillis();
		long elapsedTime = currentTime - startTime;
		elapsedTime /= 1000;
		
		return elapsedTime;
	}
	public static String getElapsedTimeString() {
		String ret ="";
		long elapsedTime = getElapsedTimeSec();
		ret = ""+elapsedTime/3600+":"+ (elapsedTime/60)%60+ ":"+elapsedTime%60;
		
		return ret;
	}

	public static int getNumFutureEvents(EventQueue deferred) {
		return future.size() + deferred.size();
	}

	public static int getNumFutureEvents(List<EventQueue> deferredList) {
		AtomicInteger numDeferred = new AtomicInteger();
		deferredList.forEach(deferred -> {
			numDeferred.addAndGet(deferred.size());
		});

		Log.println("getNumFutureEvents: future.size()="+future.size()+", deferred.size()="+numDeferred.get());
		return future.size() + numDeferred.get();
	}
	public static List<EventQueue> getDeferredList() {
		AtomicInteger numDeferred = new AtomicInteger();
		List<EventQueue> deferredList = new ArrayList<>();
		CloudSimEx.getEntityList().forEach(e -> {
			//Log.println("Entity: "+e.getName());
			EventQueue q = e.getIncomingEvents();
			//Log.println("Num of deffered events: "+q.size());
			numDeferred.addAndGet(q.size());
			deferredList.add(q);
		});
		return deferredList;
	}

	public static boolean hasMoreEvent(List<EventQueue> deferredList, CloudSimTags excludeEventTag) {
		if(!future.isEmpty()) {
			for (SimEvent ev : future) {
				if (ev.getTag() != excludeEventTag)
					return true;
			}
		}
		if(!deferredList.isEmpty()) {
			for (EventQueue deferred : deferredList) {
				for (SimEvent ev : deferred) {
					if (ev.getTag() != excludeEventTag)
						return true;
				}
			}
		}
		return false;
	}
	
	public static boolean hasMoreEvent(EventQueue deferred, CloudSimTags excludeEventTag) {
		if(!future.isEmpty()) {
            for (SimEvent ev : future) {
                if (ev.getTag() != excludeEventTag)
                    return true;
            }
		}
		if(!deferred.isEmpty()) {
            for (SimEvent ev : deferred) {
                if (ev.getTag() != excludeEventTag)
                    return true;
            }
		}
		return false;
	}
	
	public static double getNextEventTime() {
		if(!future.isEmpty()) {
			Iterator<SimEvent> fit = future.iterator();
			SimEvent first = fit.next();
			if(first != null)
				return first.eventTime();
		}
		return -1;
	}
}
