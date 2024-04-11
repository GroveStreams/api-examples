package com.grovestreams.gspi.mqtt.subscribers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.PiMain;
import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.util.MqttReply;
import com.grovestreams.gspi.mqtt.util.Util;

public class MqqtProvisioner {
	private static final Logger LOG = LoggerFactory.getLogger(MqqtProvisioner.class);
	
	static final String INSTALL_FILE_NAME = "gspi.zip";
	
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
	
		//Restart so new files are used
  	    restart();
	}
	

	public static void restart() throws IOException, InterruptedException {
		
		//Next section will shutdown gspi, but doesn't seem to start it
		//String fullFilePath = Util.getAppDir() + "/gspi.sh restart";
		//LOG.info("Restarting gspi so newly installed files are used: {}", fullFilePath);
		//Util.exeCmd(".", fullFilePath);
		
		LOG.info("Rebooting pi");
		PiMain.shutdownHook();
		Util.exeCmd(".", "sudo reboot");
	}

	private void downloadFile() throws Exception {
		//Make a call to download the installation file from a component file stream
		// The below call will download the latest file for that stream
		// The installation file, gspi.zip, must be stored stored in a component file stream: compId=installs, streamId=gspi
		// It uses the {orgUid}/api/http/{httpApiUrl} call
		//   Substitute {httpApiUrl} with the call you are making. It's what comes after https://grovestreams.com/api/
		//   Set the other required HTTP info: method (GET, PUT, POST, DELETE), queryString part of url, Headers)
		//   The http call will be assembled in the GS server message broker and forwarded to the web servers using the credentials of the Certificate user reference
		
		mqttReply.clear();
		
		
		String topic = mqttClient.getOrgUid() + "/api/http/";
		//Add the http api's url  call to end of the above topic - the full topic needs to exist within the certificate's policy
		// Install file is stored in a component file stream: compId=installs streamId=gspi
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
	
		LOG.info("Downloading installation file {}", INSTALL_FILE_NAME);
		mqttClient.publish(topic, message);
		
		byte[] payload = mqttReply.waitUntilReply(mqttClient, replyId,  5*60); //timeout is 5 minutes	
		
		//The http call downloaded the file as base64 string. Convert it back to bytes
		byte[] decodedBytes = Base64.getDecoder().decode(payload);

		//Success - save the file
		String filePathName = getInstallFilePathName();
		
		LOG.info("Writing installation file {}", filePathName);

		
	    Path path = Paths.get(filePathName);
		Files.write(path, decodedBytes);
	}
	
	private void installFile() throws IOException, InterruptedException {
		String appDir = Util.getAppDir();
		String filePathName = getInstallFilePathName();
		
		//Unzip to the directory its in
		LOG.info("Unzipping installation file {}", filePathName);

		Util.exeCmd(".", "unzip -o " + filePathName + " -d " + appDir);
		
		LOG.info("Setting sh file permissions for executing");
		Util.exeCmd(".", "chmod +x " + appDir + "/gspi.sh ");
		String certDir = mqttClient.getCertDir();
		Util.exeCmd(".", "chmod +x " + certDir + "/gencerts.sh ");
	
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
