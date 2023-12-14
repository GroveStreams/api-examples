package com.grovestreams.gspi.mqtt.publishers;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqqtOrgIdentifier.java
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

import org.eclipse.paho.mqttv5.common.MqttMessage;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.util.MqttReply;

public class MqqtOrgIdentifier {
	
	PiMqttClient mqttClient;
	static MqttReply mqttReply = new MqttReply();

	public MqqtOrgIdentifier(PiMqttClient mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	public MqttReply getMqttReply() {
		return mqttReply;
	}

	public String getOrgUid() throws Exception {
		MqttMessage message = PiMqttClient.getMqttMessage("");

		//Setup reply 
		String topic = "manage/cert/myorg/cid/" + mqttClient.getDeviceId();
		String replyTopic = "manage/cert/myorg/cid/" + mqttClient.getDeviceId()  + "/reply";
		
		mqttClient.subscribe(replyTopic, 0);
		
		String replyId = MqttReply.prepareForReply(replyTopic, message);
		message.setQos(0);

		mqttClient.publish(topic, message);

		byte[] payload = mqttReply.waitUntilReply(replyTopic, replyId, 5*60); //timeout is 5 minutes
		
		mqttClient.unsubscribe(replyTopic);
	
		
		return new String(payload, StandardCharsets.UTF_8);
	}

}
