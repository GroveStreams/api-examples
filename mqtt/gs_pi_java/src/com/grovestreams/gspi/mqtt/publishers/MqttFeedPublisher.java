package com.grovestreams.gspi.mqtt.publishers;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqttFeedPublisher.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.json.JSONArray;
import org.json.JSONObject;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.util.Reading;
import com.grovestreams.gspi.mqtt.util.Readings;

public class MqttFeedPublisher {
	private static final Logger LOG = LoggerFactory.getLogger(MqttFeedPublisher.class);
 
	PiMqttClient mqttClient = null;
	
	//GS Subscribes to and will process and store stream data for these topics
	//{orgUid}/feed 
	//{orgUid}/feed/cid/{compId}
	//{orgUid}/feed/cid/{compId}/sid/{streamId}
	//{orgUid}/feed/cid/{compId}/sid/{streamId}/data/{data}
	//{orgUid}/feed/cid/{compId}/sid/{streamId}/data/{data}/time/{time}
	
	//{orgUid} will be determined on startup via an MQTT call to the server. 
	//   It will use the orgUid of the gs orgainzation that the x509 certificate is registered in
	
	public MqttFeedPublisher(PiMqttClient mqttClient) {
		super();
		this.mqttClient = mqttClient;

	}

	public void publish(String topic) throws MqttPersistenceException, MqttException {
		MqttMessage message = new MqttMessage();
		message.setQos(0);
		
		publish(topic, message);
	}
	
	public void publish(String topic, MqttMessage message) throws MqttPersistenceException, MqttException {

		mqttClient.publish(topic, message);
	}
	
	public void publish(Reading reading) throws Exception {
		//Publishing all of the data could be done with a large topic, that includes the streamId, Time, and Data and no message body, 
		// but MQTT servers scale better if there are fewer topics to track so we'll reduce the size of the topic for the publish.
		
		//Large topic, no message body call:
		//String topic = String.format("%s/feed/cid/%s/sid/%s/data/%s/time/%d", mqttClient.getOrgUid(), mqttClient.getDeviceId(), reading.getStreamId(), reading.getValue().toString(), reading.getTime() );
		//publish(topic);
		
		//Smaller topic with a message body
		Readings readings = new Readings();
		readings.add(reading);

		publish(readings);
		
	}

	public void publish(Readings readings) throws Exception {
		
		String topic = String.format("%s/feed/cid/%s", mqttClient.getOrgUid(), mqttClient.getDeviceId());

		String jReadings = assembleJson(readings);
		
		MqttMessage message = PiMqttClient.getMqttMessage(jReadings);
		
		message.setQos(0);
		
		publish(topic, message);
	}

	
	private String assembleJson(Readings readings) {
		JSONObject jobj = new JSONObject();
		
		JSONArray jdata = new JSONArray();
		
		for (int i=0; i<readings.size(); i++) {
			Reading reading = readings.get(i);
			
			JSONObject tuple = new JSONObject();
			tuple.put("t", reading.getTime());
			tuple.put("sid", reading.getStreamId());
			tuple.put("d", reading.getValue());
			
			jdata.put(tuple);
		}
		
		jobj.put("data", jdata);
		
		return jobj.toString();
	}
}
