package com.grovestreams.gspi.mqtt.publishers;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqttMySettings.java
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.util.Compressor;
import com.grovestreams.gspi.mqtt.util.MqttReply;
import com.grovestreams.gspi.mqtt.util.MqttReply.CompressType;

public class MqttMySettings {
	private static final Logger LOG = LoggerFactory.getLogger(MqttMySettings.class);

	//Component Stream IDs for settings
	public static final String SYS_METRICS_RATE = "sysMetricsRate";
	
	public static final String LED_GREEN_START = "ledGreenStart";
	public static final String LED_GREEN_STOP = "ledGreenStop";
	public static final String LED_YELLOW_START = "ledYellowStart";
	public static final String LED_YELLOW_STOP = "ledYellowStop";
	public static final String LED_RED_START = "ledRedStart";
	public static final String LED_RED_STOP = "ledRedStop";
	
	
	PiMqttClient mqttClient;	
	static MqttReply mqttReply = new MqttReply();

	public MqttMySettings(PiMqttClient mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	public MqttReply getMqttReply() {
		return mqttReply;
	}
	

	public Map<String, Object> getSettings() throws Exception {
		Map<String, Object> settingsMap = new HashMap<>(2);
		//Settings are stored in streams with the component
		//Get them via msg broker call that makes an http api call
		
		try {
			mqttReply.clear();
			
			//Use http advanced batch Feed GET api (via mqqt)
			// https://grovestreams.com/developers/apibatchfeed.html#ag1
			String topic = mqttClient.getOrgUid() + "/api/http";
			//Add the http api's url  call to end of the above topic - the topic needs to exist within the certificate's policy!!!!
			topic += "/feed";
			
			//We want the latest stream values
			JSONArray jstreamIds = new JSONArray();
			jstreamIds.put(createJStreamId(mqttClient.getDeviceId(), SYS_METRICS_RATE));
			jstreamIds.put(createJStreamId(mqttClient.getDeviceId(), LED_GREEN_START));
			jstreamIds.put(createJStreamId(mqttClient.getDeviceId(), LED_GREEN_STOP));
			jstreamIds.put(createJStreamId(mqttClient.getDeviceId(), LED_YELLOW_START));
			jstreamIds.put(createJStreamId(mqttClient.getDeviceId(), LED_YELLOW_STOP));
			jstreamIds.put(createJStreamId(mqttClient.getDeviceId(), LED_RED_START));
			jstreamIds.put(createJStreamId(mqttClient.getDeviceId(), LED_RED_STOP));
			String itemsById = jstreamIds.toString();

			
			JSONObject jobj = new JSONObject();
			//jobj.put("method", "GET"); //Don't set. Default is GET
			
		    Map<String, String> requestParams = new HashMap<>();
		    requestParams.put("startDate", "0");
		    requestParams.put("endDate", "0");
		    requestParams.put("flatten", "true");//Flatten param will return streams in the order they were requested
		    requestParams.put("itemsById", URLEncoder.encode(itemsById, "UTF-8")); //Encode query params that need it - like this one!!!!!
		    
		    String encodedQueryString = requestParams.entrySet().stream()
		    .map(e -> e.getKey() + "=" + e.getValue())
		    .collect(Collectors.joining("&"));
		
			jobj.put("queryString", encodedQueryString); 
			
			
			JSONArray jheaders = new JSONArray();
			//jheaders.put(getHeader("Accept", "application/json")); //Not required
			jheaders.put(getHeader("Content-type", "application/json"));
			//jheaders.put(getHeader("accept-encoding", "deflate")); //Don't add. This http entity compressor flag is automatically added by the Message Broker 
			jobj.put("headers", jheaders);
			
			
			MqttMessage message = PiMqttClient.getMqttMessage(jobj.toString());
			message.setQos(0);
			
			//Setup reply (have the message broker compress the reply payload to save I/O)
			String replyTo = mqttClient.getOrgUid() + "/cid/" + mqttClient.getDeviceId() + "/reply";
			String replyId = MqttReply.prepareForReply(replyTo, 0, message, CompressType.DEFLATE);

			mqttClient.publish(topic, message);
			
			byte[] compressedPayload = mqttReply.waitUntilReply(mqttClient, replyId,  5*60); //timeout is 5 minutes		
			byte[] uncompressedPayload = Compressor.uncompressZLib(compressedPayload);

			
			String spayload = new String(uncompressedPayload, StandardCharsets.UTF_8);
			jobj = new JSONObject(spayload);
			JSONObject jfeed = jobj.getJSONObject("feed");
			JSONArray jstreams = jfeed.getJSONArray("stream");
			
			settingsMap.put(SYS_METRICS_RATE, Integer.valueOf(jstreams.getJSONObject(0).getInt("lastValue")));
			settingsMap.put(LED_GREEN_START, Integer.valueOf(jstreams.getJSONObject(1).getInt("lastValue")));
			settingsMap.put(LED_GREEN_STOP, Integer.valueOf(jstreams.getJSONObject(1).getInt("lastValue")));
			settingsMap.put(LED_YELLOW_START, Integer.valueOf(jstreams.getJSONObject(1).getInt("lastValue")));
			settingsMap.put(LED_YELLOW_STOP, Integer.valueOf(jstreams.getJSONObject(1).getInt("lastValue")));
			settingsMap.put(LED_RED_START, Integer.valueOf(jstreams.getJSONObject(1).getInt("lastValue")));
			settingsMap.put(LED_RED_STOP, Integer.valueOf(jstreams.getJSONObject(1).getInt("lastValue")));

		} catch (Exception e) {
			LOG.error("Exception", e);
						
			LOG.info("Load device exception occurred. Using defaults from properties file.");
			
			//Defaults on exception
			settingsMap.put(SYS_METRICS_RATE, Integer.valueOf(mqttClient.getProperties().getProperty("DEFAULT_PUBLISH_SYS_METRICS_RATE", "60")));
			settingsMap.put(LED_GREEN_START, Integer.valueOf(mqttClient.getProperties().getProperty("DEFAULT_LED_GREEN_START", "200")));
			settingsMap.put(LED_GREEN_STOP, Integer.valueOf(mqttClient.getProperties().getProperty("DEFAULT_LED_GREEN_STOP", "1000")));
			settingsMap.put(LED_YELLOW_START, Integer.valueOf(mqttClient.getProperties().getProperty("DEFAULT_LED_YELLOW_START", "100")));
			settingsMap.put(LED_YELLOW_STOP, Integer.valueOf(mqttClient.getProperties().getProperty("DEFAULT_LED_YELLOW_STOP", "200")));
			settingsMap.put(LED_RED_START, Integer.valueOf(mqttClient.getProperties().getProperty("DEFAULT_LED_RED_START", "0")));
			settingsMap.put(LED_RED_STOP, Integer.valueOf(mqttClient.getProperties().getProperty("DEFAULT_LED_RED_STOP", "100")));
			
		}
		
		return settingsMap;
	}
	
	private JSONObject createJStreamId(String compId, String streamId) {
		JSONObject jstreamId = new JSONObject();
		jstreamId.put("compId", compId);
		jstreamId.put("streamId", streamId);
		
		return jstreamId;
	}
	
	private JSONObject getHeader(String name, String value) {
		JSONObject jheader = new JSONObject();
		jheader.put("name",  name);
		jheader.put("value",  value);
		
		return jheader;
	}
}
