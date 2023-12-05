package com.grovestreams.gspi.mqtt.subscribers;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqqtProvisioner.java
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.json.JSONObject;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.util.MqttReply;
import com.grovestreams.gspi.mqtt.util.Util;

public class MqqtProvisioner {
	private static final Logger LOG = LoggerFactory.getLogger(MqqtProvisioner.class);
	
	static final String INSTALL_FILE_NAME = "gspi.zip2";
	
	PiMqttClient mqttClient;
	static MqttReply mqttReply = new MqttReply();

	public MqqtProvisioner(PiMqttClient mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	public MqttReply getMqttReply() {
		return mqttReply;
	}
	
	public void downloadAndProvision() throws Exception {
		
		downloadFile();
		
		installFile();
		
	//	Util.exeCmd(".", "reboot");
	}
	

	private void downloadFile() throws Exception {
		//Make a call to download the installation file from a component file stream
		// The below call will download the latest file for that stream
		// It uses the {orgUid}/api/http/{httpApiUrl} call
		// Substitue {httpApiUrl} withe the call you are making. It's what comes after https://grovestreams.com/api/
		// Set the other required HTTP info: method (GET, PUT, POST, DELETE), queryString part of url, Headers)
		// The http call will be assembled in the GS server message broker and forwarded to the webservers using the credentials of the Certificate user reference
		
		mqttReply.clear();
		
		
		String topic = mqttClient.getOrgUid() + "/api/http/";
		//Add the http api's url  call to end of the above topic - the full topic needs to exist within the certificate's policy
		topic += "comp/installs/stream/gspi/feed/file/" + INSTALL_FILE_NAME;
		
		JSONObject jobj = new JSONObject();
		//jobj.put("method", "GET"); //Don't set. Default is GET
		jobj.put("queryString", "start=0"); //Setting the start parameter will return the whole file as base64 string
		
		//Most http api calls need these headers, but not this one, so commented out
		//JSONArray jheaders = new JSONArray();
		//jheaders.put(getHeader("Accept", "application/json"));
		//jheaders.put(getHeader("Content-type", "application/json"));
		//jobj.put("headers", jheaders);
		//We could do the following to have the install file returned compressed to save bandwidth. But it's already a compressed file.
		//Jobj.put("Accept-Encoding", "deflate");
		
		
		MqttMessage message = PiMqttClient.getMqttMessage(jobj.toString());
		
		//Setup reply 
		String replyId = MqttReply.prepareForReply(mqttClient, message);
	
		mqttClient.publish(topic, message);
		
		byte[] payload = mqttReply.waitUntilReply(mqttClient, replyId,  5*60*1000); //timeout is 5 minutes		

		//Success - save the file
		String filePathName = getInstallFilePathName();
		
	    FileWriter fileWriter = new FileWriter(filePathName);
	    PrintWriter printWriter = new PrintWriter(fileWriter);
	    printWriter.print(new String(payload, StandardCharsets.UTF_8));
		printWriter.close();
	}
	
	private void installFile() throws IOException, InterruptedException {
		String appDir = Util.getAppDir();
		String filePathName = getInstallFilePathName();
		
		//Unzip to the directory its in
		Util.exeCmd(".", "unzip " + filePathName + " -d " + appDir);
	}

	private String getInstallFilePathName() {
		String appDir = Util.getAppDir();
		
		return appDir + "/" + INSTALL_FILE_NAME;
	}
	
	private JSONObject getHeader(String name, String value) {
		JSONObject jheader = new JSONObject();
		jheader.put("name",  name);
		jheader.put("value",  value);
		
		return jheader;
	}


}
