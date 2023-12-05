package com.grovestreams.gspi.mqtt.publishers;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqttStatus.java
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
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.json.JSONObject;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.util.Util;

public class MqttStatus {
	PiMqttClient mqttClient;
	
	public MqttStatus(PiMqttClient mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	public void publishStatus(int status) throws MqttPersistenceException, MqttException, Exception {
		//This could be the first call that creates the component in the GS organization. Include a component name 
		String cname = Util.getDeviceName(mqttClient.getProperties());
		JSONObject jobj = new JSONObject();
		JSONObject jop = new JSONObject();
		jop.put("cname", cname);
		jobj.put("op", jop);
		mqttClient.publish(mqttClient.getOrgUid() + "/feed/cid/" + mqttClient.getDeviceId()+ "/sid/status/data/" + status, PiMqttClient.getMqttMessage(jobj.toString()));

	}
	
}
