package com.grovestreams.gspi.mqtt.util;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqttReply.java
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
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.mqtt.PiMqttClient;

public class MqttReply {
	private static final Logger LOG = LoggerFactory.getLogger(MqttReply.class);

	String topic = "";
	MqttMessage message = null;
	
	public enum CompressType {
		NONE,
		DEFLATE
	}

	
	public void handleReply(String topic, MqttMessage message) {
        synchronized (this)
        {
			this.topic = topic;
			this.message = message;
        }
	}

	
	public void clear() {
        synchronized (this)
        {
			this.topic = "";
			this.message = null;
        }
	}

	
	public byte[] waitUntilReply(PiMqttClient mqttClient, String replyId, long timeoutSeconds) throws InterruptedException, MqttPersistenceException, MqttException { 
		return waitUntilReply(mqttClient.getOrgUid() + "/cid/" + mqttClient.getDeviceId() + "/reply", replyId, timeoutSeconds);
	}

		
	public byte[] waitUntilReply(String replyTopic, String replyId, long timeoutSeconds) throws InterruptedException, MqttPersistenceException, MqttException {

		//Called from PiMqttCallback
		long currentTimestamp = System.currentTimeMillis();	
		boolean arrived = false;
		
		long start = System.currentTimeMillis();
		int waitMillis = 50;
		int iter = 1;
		while(!arrived && currentTimestamp + timeoutSeconds*1000 > System.currentTimeMillis()) {
			TimeUnit.MILLISECONDS.sleep(waitMillis);
		
			if (this.message != null) {
				MqttProperties props = message.getProperties();
				if (props != null) {
					if (props.getCorrelationData() != null) {
						String msgReplyId = new String(message.getProperties().getCorrelationData(), StandardCharsets.UTF_8);
						if (msgReplyId.equals(replyId)) {
							//Got a reply && IDs match
							//No success property indicates it was successful
						
							String successProp = PiMqttClient.getUserProperty(message, "success");
							if (successProp != null && successProp.equalsIgnoreCase("false")) {
								//Get error message
								String errMsg = PiMqttClient.getUserProperty(message, "message");
								if (errMsg == null) {
									errMsg = "Unknown server error";
								}
								//Error Throw
								throw new RuntimeException(String.format("Exception from %s: %s. ", topic, errMsg));
							}
							arrived = true;
						}
					}
				}
			}
			
			if (iter % 40 == 0) {
				double secs = (System.currentTimeMillis() - start) / 1000;
				LOG.info(String.format("waitUntilReply(%.6f secs): replyTopic: %s: replyId: %s", secs, replyTopic, replyId));
			}
			iter++;
		}
		
		if (!arrived) {
			throw new RuntimeException(String.format("Reply %s timed out after %d seconds. ", topic, timeoutSeconds));
		} 

		return message.getPayload();
    
	}
	
	public static String prepareForReply(PiMqttClient mqttClient, MqttMessage message) throws MqttPersistenceException, MqttException {
		return prepareForReply(mqttClient.getOrgUid() + "/cid/" + mqttClient.getDeviceId() + "/reply", 0, message, CompressType.NONE);
	}
	
	public static String prepareForReply(String replyTopic, MqttMessage message) throws MqttPersistenceException, MqttException {
		return prepareForReply(replyTopic, 0, message, CompressType.NONE);
	}
	
	
	public static String prepareForReply(String replyTopic, Integer replyQos, MqttMessage message, CompressType compressType) throws MqttPersistenceException, MqttException {
				
		String replyId = UUID.randomUUID().toString();
		message.setProperties( message.getProperties() == null ? new MqttProperties() : message.getProperties());
		message.getProperties().setCorrelationData(replyId.getBytes());
		message.getProperties().setResponseTopic(replyTopic);
		
		PiMqttClient.setUserProperty(message, "replyqos", String.valueOf(replyQos));
		
		if (replyQos != null && replyQos != 0) {
			message.getProperties().getUserProperties().add(new UserProperty("replyqos", String.valueOf(replyQos)));
		}
		
		if (compressType == CompressType.DEFLATE) {
			PiMqttClient.setUserProperty(message, "replyenc", "deflate");
		}
	 
		return replyId;
	}

}
