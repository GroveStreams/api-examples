package com.grovestreams.gspi.mqtt;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  PiMqttClient.java
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.client.security.SSLSocketFactoryFactory;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

import com.grovestreams.gspi.mqtt.util.Compressor;
import com.grovestreams.gspi.mqtt.util.Util;

public class PiMqttClient {
	private static final Logger LOG = LoggerFactory.getLogger(PiMqttClient.class);
	//Wrapper for paho mqqtClient
	
	private static final int CONNECT_TIMEOUT = 5; //seconds
	
	// Assign as char array so the string is not obvious when viewing the
	//  .jar file in an editor (this is common practice when hardcoding pwds)
	private static final char[] trustStorePwd = { 'g', 's', '_', 'p', 'i', '_', 's', 't', 'o', 'r', 'e', '_', 'P', 'a', 's', 's' }; 
	private static final char[] keyStorePwd =   { 'g', 's', '_', 'p', 'i', '_', 's', 't', 'o', 'r', 'e', '_', 'P', 'a', 's', 's'  }; 
	// PWds can also be assigned in the properties file - they can easily be viewed and changed there so not as secure as hardcoding

	
	private Properties properties;
	private MqttAsyncClient mqttClient;
	private PiMqttCb callback = null;
	private String deviceId = "";
	private String clientId = "";
	private String orgUid = "";
	
	private String willTopic = "";
	private MqttMessage willMessage = null;

	
	public PiMqttClient(Properties properties, String deviceId) {
		super();
		this.properties = properties;
		this.deviceId = deviceId;
		this.clientId = deviceId;
	}	
	
	public PiMqttClient(Properties properties, String deviceId, String clientId) {
		super();
		this.properties = properties;
		this.deviceId = deviceId;
		this.clientId = clientId;
	}
	
	public void setWill(String willTopic, MqttMessage willMessage) {
		//Applied during connect
		this.willTopic = willTopic;
		this.willMessage = willMessage;
	}
	
	public void connect() throws Exception {
        synchronized (this)
        {
        	//Will not return until connected.
			if (mqttClient != null) {
				disconnectAndClose();
			}
			
			long startTime = System.currentTimeMillis();
			
			callback = createClient();
			mqttClient.connect(getMqttConnectionOptions());
			
			
			while (!(mqttClient.isConnected() && callback.isConnectionComplete())) {
				try {
					
					if (!mqttClient.isConnected() && System.currentTimeMillis() > startTime + CONNECT_TIMEOUT*1000) {
						mqttClient.connect(getMqttConnectionOptions());
						startTime = System.currentTimeMillis();
					}
		
					TimeUnit.MILLISECONDS.sleep(500);
					
				} catch (Exception e) {
					LOG.error("Exception",e);
				    Random random = new Random();
				    int aLittleMore = random.nextInt(11); //Random so that all devices are not connecting at the exact same time if there is a "no connection scenario" ending.
					TimeUnit.SECONDS.sleep(20+aLittleMore);
				}
			}
				
        }
	}
	
	public void disconnectAndClose() throws MqttException {
       synchronized (this)
        {
			if (mqttClient != null) {
				if (mqttClient.isConnected()) {

					LOG.info("mqttClient.disconnect(5s)");
					try {
						mqttClient.disconnect(5000);
					} catch (MqttException e) {
						LOG.error("Exception",e);
					}
				}
				LOG.info("mqttClient.close() ...");
				try {
					mqttClient.close();
				} catch (MqttException e) {
					LOG.error("Exception",e);
				}
				
				mqttClient = null;
			}
        }
	}
	
	public void publish(String topic, MqttMessage message) throws MqttPersistenceException, MqttException {
  
        mqttClient.publish(topic, message);
        	
	}

	public void subscribe(String[] topicFilters, int[] qos) throws MqttPersistenceException, MqttException {

        mqttClient.subscribe(topicFilters, qos).waitForCompletion();;
        
	}		
	
	public void subscribe(String topic, int qos) throws MqttPersistenceException, MqttException {

        mqttClient.subscribe(topic, qos).waitForCompletion();
        
	}	
	
	public void unsubscribe(String topic) throws MqttPersistenceException, MqttException {

        mqttClient.unsubscribe(topic);
        
	}	
	
	public void setBufferOps() {

		//Buffer messages when MQTT connection is dropped
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(10000);
        disconnectedBufferOptions.setPersistBuffer(true);
        disconnectedBufferOptions.setDeleteOldestMessages(true);
        mqttClient.setBufferOpts(disconnectedBufferOptions);
        
	}
	
	////////////////////////////
	
	public Properties getProperties() {
		return properties;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public String getOrgUid() {
		return orgUid;
	}

	public void setOrgUid(String orgUid) {
		this.orgUid = orgUid;
	}

	public String getCertDir() {
		String appDir = Util.getAppDir();
		return appDir + "/" + properties.getProperty("CERT_DIR", "certs");
	}

	private String getCertVerFilePath() {
		return getCertDir() + "/" + properties.getProperty("CERT_VER_FILE", "cert_ver.txt");
	}	
	
	private String getKeyStoreFilePath() throws NumberFormatException, IOException {
		int certVer = getCertVersion();
		return getCertDir() + "/" + properties.getProperty("KEYSTORE", "keystore") + certVer + ".p12";
	}
	
	private String getKeyStorePwd() {
		String tsPwd = properties.getProperty("KEYSTOREPWD", new String(keyStorePwd));	
		return tsPwd;
	}
	
	public String getTrustStoreFilePath() throws NumberFormatException, IOException {
		return getCertDir()  + "/" + properties.getProperty("TRUSTSTORE", "truststore") + ".p12";
	}
	
	public String getTrustStorePwd() {
		String tsPwd = properties.getProperty("TRUSTSTOREPWD", new String(trustStorePwd));	
		return tsPwd;
	}
	
	private String getCacheDir()  {
		String appDir = Util.getAppDir();
		return appDir + "/" + properties.getProperty("MQTT_CACHE_DIR", "cache");
	}
	
	
	private MqttConnectionOptions getMqttConnectionOptions() throws NumberFormatException, IOException {

		String keyStore = getKeyStoreFilePath();
		String trustStore = getTrustStoreFilePath();

		String ksPwd = getKeyStorePwd();
		String tsPwd =  getTrustStorePwd();

		MqttConnectionOptions connOpts = new MqttConnectionOptions();
		//connOpts.setCleanStart(false);
		connOpts.setCleanStart(true); //Set to false for qos1 and qos2 reliability
		connOpts.setHttpsHostnameVerificationEnabled(false);
		connOpts.setConnectionTimeout(CONNECT_TIMEOUT);  
		connOpts.setKeepAliveInterval(65); //Set this low if you don't make frequent publishes. Connections consume resources on the server, but creating/destroying them do too. Balance.
		connOpts.setTopicAliasMaximum(1000); //Just don't want to overwhelm pi memory if a lot of unique topics are used
		
	    Random random = new Random();
	    int aLittleMore = random.nextInt(21); //Random so that all devices are not reconnecting at the exact same time if there is a "no connection scenario" ending.
		connOpts.setAutomaticReconnect(true);
		connOpts.setAutomaticReconnectDelay(1, 20 + aLittleMore);

		
		if (!this.willTopic.isEmpty()) {
			connOpts.setWill(willTopic, willMessage);
			
			MqttProperties willProps =  new MqttProperties();
			connOpts.setWillMessageProperties(willProps);
		}
		
		Properties sslProperties = new Properties();
		
		// SSL Certs
		sslProperties.put(SSLSocketFactoryFactory.KEYSTORE, keyStore);
		sslProperties.put(SSLSocketFactoryFactory.KEYSTOREPWD, ksPwd);
		sslProperties.put(SSLSocketFactoryFactory.KEYSTORETYPE, "PKCS12");

		sslProperties.put(SSLSocketFactoryFactory.TRUSTSTORE, trustStore);
		sslProperties.put(SSLSocketFactoryFactory.TRUSTSTOREPWD, tsPwd);
		sslProperties.put(SSLSocketFactoryFactory.TRUSTSTORETYPE, "PKCS12");

		sslProperties.put(SSLSocketFactoryFactory.CLIENTAUTH, true);
		connOpts.setSSLProperties(sslProperties);

		return connOpts;
	}

	private PiMqttCb createClient() throws SocketException, UnknownHostException, MqttException {
		String brokerUrl = properties.getProperty("BROKER_URL");

		String clientCacheDirectory = getCacheDir();
		
		MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(clientCacheDirectory);
		PiMqttCb callback = new PiMqttCb(this);
		
		LOG.info("MQTT Broker URL: " + brokerUrl);
		mqttClient = new MqttAsyncClient(brokerUrl, clientId, persistence);
		
		mqttClient.setCallback(callback);
		
		return callback;
	}
	
	public int getCertVersion() throws NumberFormatException, IOException {
		String certVerFilePath = getCertVerFilePath();
		
		File certVerFile = new File(certVerFilePath);
				
		int certVer = 1;
		if (certVerFile.exists()) {
			String sVer = Util.readFileAsString(certVerFilePath).replace("\n", "").trim();
			certVer = Integer.valueOf(sVer);
		}
		
		return certVer;
	}
	
	public void setCertVersion(int certVersion) throws IOException {
		String certVerFilePath= getCertVerFilePath();
		
	    FileWriter fileWriter = new FileWriter(certVerFilePath);
	    PrintWriter printWriter = new PrintWriter(fileWriter);
	    printWriter.print(certVersion);
		printWriter.close();
	}
	
	public static MqttMessage getMqttMessage(String body) throws Exception {
		MqttMessage message = new MqttMessage();
		
		if (body!= null && !body.isEmpty()) {
			//Compress it- GS is using Apache Commons CompressorStreamFactory. See here for supported encodings:
			//  https://commons.apache.org/proper/commons-compress/apidocs/org/apache/commons/compress/compressors/CompressorStreamFactory.html
			
			// To use gzip instead of zlib below change to this:
			//	message.getProperties().getUserProperties().add(new UserProperty("enc", "gz"));
			//  payload = Compressor.compressGZip(payload);
			
			byte[] payload = body.getBytes(StandardCharsets.UTF_8);
			byte[] compressedPayload =  Compressor.compressZLib(payload); //ZLib was a smaller payload so it's used instead of GZip
			
			LOG.info(String.format("Compression ratio: %f",  (float)compressedPayload.length/payload.length));	
			
			//Go with smaller payload - save I/O costs
			if (payload.length > compressedPayload.length) {
				
				//Set the compression encoding in user properties. 
				// This is Optional. GS, via apache compression libraries, will attempt to figure it out if this property is not set 
				MqttProperties msgProps = new MqttProperties();
				List<UserProperty> userProps = new ArrayList<>(1);
				msgProps.setUserProperties(userProps);	
				userProps.add(new UserProperty("enc","deflate"));
				message.setProperties(msgProps);
	
				message.setPayload(compressedPayload);
			} else {
				message.setPayload(payload);
			}
		}
		
		return message;
	}
	
	public static void setUserProperty(MqttMessage message, String propertyName, String propertyValue) {
		
		MqttProperties props = message.getProperties();
		if (props == null) {
			props = new MqttProperties();
			message.setProperties(props);
		}
		
		List<UserProperty> userProps = props.getUserProperties();
		if (userProps == null) {
			userProps = new ArrayList<>(1);
			props.setUserProperties(userProps);
		}
		
		userProps.add(new UserProperty(propertyName, propertyValue));
		
	}
	
	public static String getUserProperty(MqttMessage message, String propertyName) {
		MqttProperties props = message.getProperties();
		
		if (props != null) {
			List<UserProperty> userProps = props.getUserProperties();
			if (userProps != null && !userProps.isEmpty()) {
				for (UserProperty userProp : userProps) {
					if (userProp.getKey().equalsIgnoreCase(propertyName)) {
						
						return userProp.getValue();
					}
				}
			}
		}
		
		return null;
	}
}
