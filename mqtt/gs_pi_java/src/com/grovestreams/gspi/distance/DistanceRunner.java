package com.grovestreams.gspi.distance;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  DistanceRunner.java
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
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.publishers.MqttFeedPublisher;
import com.grovestreams.gspi.mqtt.publishers.MqttMySettings;
import com.grovestreams.gspi.mqtt.util.Reading;
import com.pi4j.context.Context;



public class DistanceRunner implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(DistanceRunner.class);

	PiMqttClient mqttClient;
	MqttFeedPublisher mqttPublisher;
	DistanceSensor distanceSensor;
	LedController ledController;	
	
	int ledGreenStart;
	int ledGreenStop;
	int ledYellowStart;
	int ledYellowStop;
	int ledRedStart;
	int ledRedStop;


	double previousReading = -1;
	boolean cancelled = false;
	

	public DistanceRunner(PiMqttClient mqttClient, Context pi4j, Map<String, Object> settings ) throws Exception {
		super();
		
		this.mqttClient = mqttClient;
		
		this.mqttPublisher = new MqttFeedPublisher(this.mqttClient);
		
		this.distanceSensor = new DistanceSensor(pi4j);
		this.ledController = new LedController(pi4j);	
		
		this.ledGreenStart = (int)settings.get(MqttMySettings.LED_GREEN_START);
		this.ledGreenStop = (int)settings.get(MqttMySettings.LED_GREEN_STOP);
		this.ledYellowStart = (int)settings.get(MqttMySettings.LED_YELLOW_START);
		this.ledYellowStop = (int)settings.get(MqttMySettings.LED_YELLOW_STOP);
		this.ledRedStart = (int)settings.get(MqttMySettings.LED_RED_START);
		this.ledRedStop = (int)settings.get(MqttMySettings.LED_RED_STOP);
		
		LOG.info("Initialized");
	}

	@Override
	public void run() {
		try {

			Date currDate = new Date();
			long now = currDate.getTime();
			
			
			double distance = distanceSensor.getDistance();
			
			if (!cancelled && distance >= ledGreenStart && distance < ledGreenStop) {
				LOG.info(String.format("GREEN: distance=%.2f start=%d stop=%d", distance, ledGreenStart, ledGreenStop));
				ledController.greenOn();
				ledController.yellowOff();
				ledController.redOff();
			} else if (!cancelled && distance >= ledYellowStart && distance < ledYellowStop) {
				LOG.info(String.format("YELLOW: distance=%.2f start=%d stop=%d", distance, ledYellowStart, ledYellowStop));
				ledController.greenOff();
				ledController.yellowOn();
				ledController.redOff();				
			} else if (!cancelled && distance >= ledRedStart && distance < ledRedStop) {
				LOG.info(String.format("RED: distance=%.2f start=%d stop=%d", distance, ledRedStart, ledRedStop));
				ledController.greenOff();
				ledController.yellowOff();
				ledController.redOn();				
			} else if (!cancelled) {
				LOG.info(String.format("???: distance=%.2f ledGreenStart=%d ledGreenStop=%d ledYellowStart=%d ledYellowStop=%d ledRedStart=%d ledRedStop=%d", 
						distance, ledGreenStart, ledGreenStop, ledYellowStart, ledYellowStop, ledRedStart, ledRedStop));
			
				ledController.greenOff();
				ledController.yellowOff();
				ledController.redOff();				
			}
			
			
			//Send Reading to GroveStreams if it has changed by 2 cm
			if (!cancelled && distance >= 0 && Math.abs(distance - previousReading) >= 2) {
				previousReading = distance;
				this.mqttPublisher.publish(new Reading("distance", now, distance));
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
