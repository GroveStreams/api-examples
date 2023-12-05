package com.grovestreams.gspi.mqtt.subscribers;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqttCertRoller.java
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
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.publishers.MqqtOrgIdentifier;
import com.grovestreams.gspi.mqtt.util.MqttReply;
import com.grovestreams.gspi.mqtt.util.Util;

public class MqttCertRoller {
	private static final Logger LOG = LoggerFactory.getLogger(MqttCertRoller.class);
	
	PiMqttClient mqttClient;
	static MqttReply mqttReply = new MqttReply();

	public MqttCertRoller(PiMqttClient mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	public MqttReply getMqttReply() {
		return mqttReply;
	}


	public void rollCert() throws Exception {
		//Generate new certificates while not interrupting ongoing processing
		
		//1. Generate CA, private key, and public key. Put them in a new keystore
		//2. Register both new public keys with the GS org
	    //3. Test new certs
	    //    Failure: Stick with old stores, delete new certs and stores. 
	    //    Success: Switch to new private and public stores 
	    //        Test new certs. Ping GS
	    //     	     Success: Unregister old certs with gs and delete old key and trust stores , reconnect mqqtclient with new keys	
	    //           Failure: Stick with old stores, delete new certs and stores.
		
		
		int curCertVer =  mqttClient.getCertVersion();
		int newCertVer = curCertVer + 1;
		
		//1. Generate new certs
		LOG.info("Roll: Generating new certs...");
		genCerts(newCertVer);
		
		mqttReply.clear();
		
		LOG.info("Roll: Registering new certs on server...");

		//2. Register new certs (server will copy from old cert settings such as folder security and such). Distinguished name needs to be unique. certVer is added to them
		String topic = String.format(mqttClient.getOrgUid() + "/manage/cert/register/cid/%s", mqttClient.getDeviceId());	
		String replyId = registerCertsWithGs(topic, curCertVer, newCertVer);

		try {
			 mqttReply.waitUntilReply(mqttClient, replyId,  5*60*1000); //timeout is 5 minutes
		} catch (Exception e) {
			removeFiles(newCertVer);
			throw(e);
		}
		
		//Server can take up to 90 secs to use a new cert
		//  30 secs for the cert to get published to all servers
		//  1 min for server memory caches to refresh
		LOG.info("Roll: Waiting 91 secs for new certs to deploy and for memory caches to refresh accross servers...");
		TimeUnit.SECONDS.sleep(91);
		
		//3. Test the new certs with a separate connection
		LOG.info("Roll: Testing connecting with new certs...");
		if (testConnection(newCertVer)) {
			
			//Change mqttClient being used for all ops to new certs
	    	mqttClient.setCertVersion(newCertVer);
	    	mqttClient.connect();
	    	

			//Remove all old certs
			LOG.info("Roll: Testing succeeded. Removing old certs from server...");
	    	removeCertsWithGs(curCertVer, newCertVer);
			LOG.info("Roll: Done. Cleanning up.");
			removeoldFiles(curCertVer);
			
		} else {
			LOG.info("Roll: Testing failed.");
		
			//Failed - remove new certs
			removeFiles(newCertVer);
		}
		
	}

	private boolean testConnection(int certVer) throws IOException, MqttException {
		//Test with a new connection
		PiMqttClient mqttClientTester = new PiMqttClient(mqttClient.getProperties(), mqttClient.getDeviceId(), mqttClient.getDeviceId()+"_test");
		int curVer = mqttClient.getCertVersion();
		
		try {
			mqttClientTester.setCertVersion(certVer);
			mqttClientTester.connect();
			
			//Test
			MqqtOrgIdentifier orgIdentifier = new MqqtOrgIdentifier(mqttClientTester);
			orgIdentifier.getOrgUid();
			
			//worked
			mqttClientTester.disconnectAndClose();
			
		} catch (Exception e) {
			//Failed Test 
			return false;
		} finally {
			mqttClientTester.disconnectAndClose();
			mqttClient.setCertVersion(curVer);
		}	
		
		return true;
	}

	private String registerCertsWithGs(String topic, int curCertVer, int newCertVer) throws Exception {
		String certDir = mqttClient.getCertDir() + "/";
		String deviceName = Util.getDeviceName(mqttClient.getProperties());
				
		JSONObject jobj = new JSONObject();	
		JSONArray jcurCerts = new JSONArray();
		JSONArray jnewCerts = new JSONArray();
		
		jobj.put("curCerts", jcurCerts);
		jobj.put("newCerts", jnewCerts);
		
		jcurCerts.put(Util.readFileAsString(certDir + deviceName + curCertVer + ".crt").toString());
		jcurCerts.put(Util.readFileAsString(certDir + deviceName + "-ca" + curCertVer + ".crt").toString());

		jnewCerts.put(Util.readFileAsString(certDir + deviceName + newCertVer + ".crt").toString());
		jnewCerts.put(Util.readFileAsString(certDir + deviceName + "-ca" + newCertVer + ".crt").toString());

		MqttMessage message = PiMqttClient.getMqttMessage(jobj.toString());
		

		//Setup reply 
		String replyId = MqttReply.prepareForReply(mqttClient, message);				
		message.setQos(1);

		mqttClient.publish(topic, message);
		
		return replyId;
	}

	private void removeCertsWithGs(int curCertVer, int newCertVer) throws Exception {
		try {
			String certDir = mqttClient.getCertDir() + "/";
			String deviceName = Util.getDeviceName(mqttClient.getProperties());
			String deviceId = mqttClient.getDeviceId();

			//Do it with a new connection so as to not disrupt other threads - use old certs. 
			// Certs being unregistered have to be within authentication cert chain thus - use old certs
			PiMqttClient mqttClientPreviousVer = new PiMqttClient(mqttClient.getProperties(), deviceId, deviceId+"_unreg");
			mqttClientPreviousVer.setCertVersion(curCertVer);
			try {
				mqttClientPreviousVer.connect();
				JSONObject jobj = new JSONObject();
				JSONArray jcerts = new JSONArray();
				jobj.put("certs", jcerts);
				jcerts.put(Util.readFileAsString(certDir + deviceName + curCertVer + ".crt").toString());
				jcerts.put(Util.readFileAsString(certDir + deviceName + "-ca" + curCertVer + ".crt").toString());
				String topic = String.format(mqttClient.getOrgUid() + "/manage/cert/unregister/cid/%s",
						deviceId);
				MqttMessage message = PiMqttClient.getMqttMessage(jobj.toString());
				//Setup reply 

				String replyId = MqttReply.prepareForReply(mqttClient, message);
				message.setQos(1);
				
				mqttClientPreviousVer.publish(topic, message);
				
				mqttReply.clear();
				mqttReply.waitUntilReply(mqttClient, replyId, 5 * 60 * 1000); //timeout is 5 minutes

				mqttClientPreviousVer.disconnectAndClose();
			} finally {
				mqttClientPreviousVer.setCertVersion(newCertVer);
			}

		} catch (Exception e) {
			mqttClient.setCertVersion(newCertVer);
			LOG.error("Exception",e);
		} 
		
	}
	
	private void genCerts(int certVer) throws IOException, InterruptedException {
		String certDir = mqttClient.getCertDir();
			
		String command ="sh gencerts.sh " + certVer;
		Util.exeCmd(certDir, command);
	}

	
	private void removeoldFiles(int certVer) throws IOException, InterruptedException {
		
		for (int i=1; i<= certVer; i++) {			
			removeFiles(i);
		}
	}
	
	private void removeFiles(int certVer) throws IOException, InterruptedException {
		String certDir = mqttClient.getCertDir() + "/";
		String deviceName = Util.getDeviceName(mqttClient.getProperties());

		
		new File(certDir + "ca-keystore" + certVer + ".p12").delete();
		new File(certDir + "keystore" + certVer + ".p12").delete();
		new File(certDir + deviceName + "-ca" + certVer + ".crt").delete();
		new File(certDir + deviceName + certVer + ".crt").delete();
		new File(certDir + deviceName + certVer + ".csr").delete();
	}

}
