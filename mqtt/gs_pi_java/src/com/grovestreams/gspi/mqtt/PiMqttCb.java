package com.grovestreams.gspi.mqtt;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  PiMqttCb.java
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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.mqtt.publishers.MqttStatus;

public class PiMqttCb implements MqttCallback {
	private static final Logger LOG = LoggerFactory.getLogger(PiMqttCb.class);

	private PiMqttClient mqttClient;
	private boolean connectionComplete = false;

	
	public PiMqttCb(PiMqttClient piMqttClient) {
		this.mqttClient = piMqttClient;
	}

	
	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		LOG.info("connectComplete");
		String deviceId = mqttClient.getDeviceId();

		// Buffer messages when MQTT connection is dropped
		mqttClient.setBufferOps();

		try {
			String orgUid = mqttClient.getOrgUid();
			
			List<String> topics = new ArrayList<>();
			List<Integer> qos = new ArrayList<>();
			
			
			if (!orgUid.isEmpty()) {
				
				topics.add( orgUid + "/cid/" + deviceId + "/reply"); //One reply to handle all request responses. Common pattern and best practice solution. Reduces server resources.
				qos.add(0);				

				topics.add( orgUid + "/manage/cert/roll/cid/" + deviceId);
				qos.add(0);				
				
				topics.add( orgUid + "/manage/cert/update_truststore/cid/" + deviceId);
				qos.add(0);
				
				topics.add( orgUid + "/manage/restart/cid/" + deviceId);
				qos.add(0);
				
				topics.add( orgUid + "/manage/provision/cid/" + deviceId);
				qos.add(0);				
			} 

		    String[] topicArr = new String[topics.size()];
			int[] qosArr = new int[qos.size()];
			
		    for(int j =0;j<qos.size();j++){
		    	topicArr[j] = topics.get(j);
		    	qosArr[j] = qos.get(j);
		      }

			mqttClient.subscribe(topicArr, qosArr);
			
			if (!orgUid.isEmpty()) {
				//Set the status back to one (connected)
				LOG.info("Setting device status to 1");
				MqttStatus mqttStatus = new MqttStatus(mqttClient);
				mqttStatus.publishStatus(1);
			}

			connectionComplete = true;

		} catch (Exception e) {
			LOG.error("Exception",e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		

		try {
			//Start a new thread to handle messages arriving because
			//  all messageArrived calls are handled by one thread and a deadlock will occur while waiting for reply
			PiMqttCbMsgArrivedRunner maRunner = new PiMqttCbMsgArrivedRunner(mqttClient, topic, message);
			Thread thread = new Thread(maRunner);
			thread.setName("PiMessageArrived");
			thread.start();

		} catch (Exception e) {
			LOG.error("Exception",e);
		}

	}
	
	public boolean isConnectionComplete() {
		return connectionComplete;
	}


	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		LOG.info("disconnected: " + disconnectResponse);

		connectionComplete = false;
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		LOG.info("mqttErrorOccurred", exception);

	}


	@Override
	public void deliveryComplete(IMqttToken token) {
		LOG.info("deliveryComplete");
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		LOG.info("authPacketArrived. ReasonCode: " + reasonCode);

	}

}
