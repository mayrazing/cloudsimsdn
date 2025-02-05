/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import org.cloudbus.cloudsim.core.CloudSimTags;

/**
 * Constant variables to use
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public enum CloudSimSDNTags implements CloudSimTags {
	// Deliver Cloudlet (computing workload) to VM
	SDN_PACKET_COMPLETE,
	SDN_PACKET_FAILED,

	SDN_INTERNAL_PACKET_PROCESS,

	SDN_VM_CREATE_IN_GROUP,

	SDN_VM_CREATE_IN_GROUP_ACK,

	SDN_VM_CREATE_DYNAMIC,

	SDN_VM_CREATE_DYNAMIC_ACK,

	SDN_INTERNAL_CHANNEL_PROCESS,

	REQUEST_SUBMIT,

	REQUEST_COMPLETED,

	REQUEST_OFFER_MORE,

	REQUEST_FAILED,

	APPLICATION_SUBMIT,

	APPLICATION_SUBMIT_ACK,

	MONITOR_UPDATE_UTILIZATION,
}
