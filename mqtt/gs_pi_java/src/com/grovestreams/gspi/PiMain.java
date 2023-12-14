package com.grovestreams.gspi;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  PiMain.java
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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.distance.DistanceRunner;
import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.publishers.MqqtOrgIdentifier;
import com.grovestreams.gspi.mqtt.publishers.MqttMySettings;
import com.grovestreams.gspi.mqtt.util.Util;
import com.grovestreams.gspi.sysmetrics.PublishSysMetricsRunner;
import com.pi4j.Pi4J;

public class PiMain {
	//private static final Logger LOG = LogManager.getLogger(PiMain.class);
	 private static Logger LOG = LoggerFactory.getLogger(PiMain.class);

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
	
	private static PiMqttClient mqttClient;
	private static PublishSysMetricsRunner publishReadingsRunner;
	private static DistanceRunner distanceRunner;
	;
	private static com.pi4j.context.Context pi4j /*= Pi4J.newAutoContext()*/;


	public static void main(String[] args) {
		//System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");

		
		final String PROPERTY_FILE_NAME = "/gspi.properties";

		try {
			Properties properties = new Properties();
			InputStream is = PiMain.class.getResourceAsStream(PROPERTY_FILE_NAME);
			if (is != null) {
				properties.load(is);
			} else {
				properties.load(new FileInputStream(PROPERTY_FILE_NAME));
			}

			//The shutdown hook is called when the service is stopping. Or user uses ctrl-c during console mode
			Thread runtimeHookThread = new Thread() {
				public void run() {
					shutdownHook();
				}
			};
			
			Runtime.getRuntime().addShutdownHook(runtimeHookThread);
			
			 
		    pi4j = Pi4J.newAutoContext();
			
		    //Next section is for quick testing and debugging of hardware
		    /*
			LedController ledController = new LedController(pi4j);		
			DistanceSensor distanceSensor = new DistanceSensor(pi4j);
			for (int i=0; i<5; i++) {
				double distance = distanceSensor.getDistance();
				LOG.info(String.format("%f cm", distance));
				TimeUnit.SECONDS.sleep(1);
			}
			
			ledController.greenOn();
			TimeUnit.SECONDS.sleep(5);
			ledController.greenOff();
			
			ledController.yellowOn();
			TimeUnit.SECONDS.sleep(5);
			ledController.yellowOff();
			
			ledController.redOn();
			TimeUnit.SECONDS.sleep(5);
			ledController.redOff();
			*/
		    
				
			String deviceId  = Util.getDeviceId(properties);
			
			mqttClient = new PiMqttClient(properties, deviceId);
			
			LOG.info("Connecting to MQTT Host ...");

			mqttClient.connect();

			//Now get the orgUid. Prevents us from hard-coding an orgUid in the code and allows the device
			// to pub/sub into the org that the certificate is registered within
			LOG.info("Getting org uid...");
			
			MqqtOrgIdentifier orgIdentifier = new MqqtOrgIdentifier(mqttClient);
			String orgUid = orgIdentifier.getOrgUid();
			mqttClient.setOrgUid(orgUid);
			
			LOG.info("Reconnecting with orgUid: " + orgUid + " ...");
			
			//Setup an MQTT5 will. This example is using a component stream to store a status value so that we can setup alerts on it. 
			// The server will publish the will whenever the connection is dropped thus changing the component status to zero.
			//The status stream value is set to one when a connection succeeds in the Callback class
			//Ensure the will topic is in the certificate policy in your organization
			LOG.info("Setting will topic ...");
			mqttClient.setWill(orgUid + "/feed/cid/" + mqttClient.getDeviceId() + "/sid/status/data/0", PiMqttClient.getMqttMessage(""));
			
			//Reconnect with orgUid and set the proper subscribes in Callback class 
			mqttClient.connect();
						
			LOG.info("Attempting to load device settings...");
			MqttMySettings mySettings = new MqttMySettings(mqttClient);
			Map<String, Object> settings = mySettings.getSettings();


			//Upload Pi metrics (memory, disk, cpu)
			//Offset runner times to try and keep them from running at the same time
			publishReadingsRunner = new PublishSysMetricsRunner(mqttClient);	
			scheduler.scheduleAtFixedRate(publishReadingsRunner, 0, (int)settings.get(MqttMySettings.SYS_METRICS_RATE), TimeUnit.SECONDS);
			
			
			//Upload distance and LED status
			boolean enableDistanceAndLedLogic = Boolean.valueOf(properties.getProperty("ENABLE_DISTANCE_AND_LED_LOGIC", "true"));
			if (enableDistanceAndLedLogic) {
				TimeUnit.MICROSECONDS.sleep(2500);//Check distance every 2s, starting 2.5s after startup
				distanceRunner = new DistanceRunner(mqttClient, pi4j, settings);	
				scheduler.scheduleAtFixedRate(distanceRunner, 0, (int)2, TimeUnit.SECONDS); 
			}
			
			while (mqttClient != null && pi4j != null) {
				TimeUnit.MILLISECONDS.sleep(500);
			}
			

		} catch (Exception e) {
			LOG.error("Exception!", e);
			
			shutdownHook();
		}
	}

	public static void shutdownHook() {
		try {
			LOG.info("ShutdownHook started");
			

			if (scheduler != null) {
				LOG.info("scheduler shutdown ...");
				scheduler.shutdown();
			}

			if (publishReadingsRunner != null) {
				publishReadingsRunner.cancel();
			}
			
			if (distanceRunner != null) {
				distanceRunner.cancel();
			}
			
			if (scheduler != null) {
				LOG.info("scheduler awaitTermination ...");
				try {
					if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
						scheduler.shutdownNow();
						if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
							LOG.info("scheduler did not shutdown ...");
						}
					}
				} catch (Exception e) {
					LOG.error("Exception!", e);
				}
			}
			
			if (pi4j != null) {
				LOG.info("pi4j shutdown ...");
			
				pi4j.shutdown();
				pi4j = null;
			}
			
			if (mqttClient != null) {
				LOG.info("mqttClient disconnectAndClose ...");
				mqttClient.disconnectAndClose();
				mqttClient = null;
			}
			

		} catch (Exception e) {
			LOG.error("Exception!", e);
		}

		LOG.info("ShutdownHook completed");
	}


}
