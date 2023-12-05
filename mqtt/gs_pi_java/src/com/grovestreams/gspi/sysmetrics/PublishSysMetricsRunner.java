package com.grovestreams.gspi.sysmetrics;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  PublishSysMetricsRunner.java
 *
 * This file is part of the GsPi project. More information about
 * this project can be found here:  https://grovestreams.com/
 * **********************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.publishers.MqttFeedPublisher;
import com.grovestreams.gspi.mqtt.util.Reading;
import com.grovestreams.gspi.mqtt.util.Readings;



public class PublishSysMetricsRunner implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(PublishSysMetricsRunner.class);

	PiMqttClient mqttClient;
	MqttFeedPublisher mqttPublisher;

	boolean cancelled = false;
	

	public PublishSysMetricsRunner(PiMqttClient mqttClient ) throws SocketException, UnknownHostException {
		super();
		this.mqttClient = mqttClient;
		
		mqttPublisher = new MqttFeedPublisher(this.mqttClient);
	}

	@Override
	public void run() {
		try {
			
	//		LOG.info("Starting PublishReadingsRunner");
			 
			Date currDate = new Date();
			long now = currDate.getTime();
			
			Readings readings = new Readings();

			File cDrive = new File("/");
			readings.add(new Reading("disk space free", now, cDrive.getFreeSpace()/1048576d));

			MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
			double megaBytesUsed = memoryMXBean.getHeapMemoryUsage().getUsed()/1048576d;
			readings.add(new Reading("used heap memory", now, megaBytesUsed));
			
			OperatingSystemMXBean operatingSystemMXBean = 
			          (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();	       
			readings.add(new Reading("sys load 1m avg", now, operatingSystemMXBean.getSystemLoadAverage()));
		
			if (!cancelled) {
				this.mqttPublisher.publish(readings);
			}
				
	        
		} catch (Exception e) {
			LOG.error("Exception!", e);
		}

	}
	


	public boolean cancel() {
	
		LOG.info("Cancelling because of shutdown...");
		
		
		try {
			
			cancelled = true;
			
		} catch (Exception e) {
			LOG.error("Exception!", e);
		}
		
		return true;
	}


}
