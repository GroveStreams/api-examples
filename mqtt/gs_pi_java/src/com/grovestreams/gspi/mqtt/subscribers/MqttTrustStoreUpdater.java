package com.grovestreams.gspi.mqtt.subscribers;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  MqttTrustStoreUpdater.java
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.mqtt.PiMqttClient;
import com.grovestreams.gspi.mqtt.util.MqttReply;

public class MqttTrustStoreUpdater {
	private static final Logger LOG = LoggerFactory.getLogger(MqttTrustStoreUpdater.class);
	
	PiMqttClient mqttClient;
	static MqttReply mqttReply;

	public MqttTrustStoreUpdater(PiMqttClient mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	public MqttReply getMqttReply() {
		return mqttReply;
	}

	
	public void updateTrustStore(byte[] payload) throws Exception {
		String trustStoreFilePath = mqttClient.getTrustStoreFilePath();
		
		LOG.info("Updating trustStore " + trustStoreFilePath + " ...");
		
		String certString = new String(payload, StandardCharsets.UTF_8 );
		LOG.info("Updating with cert: \n" + certString);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(payload);
		X509Certificate x509Cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(bais);
		
		
		File trustFile = new File(trustStoreFilePath);
		
		KeyStore  trustStore = KeyStore.getInstance("pkcs12");
		FileInputStream trustIs = new FileInputStream(trustFile);
		try {
			trustStore.load(trustIs, mqttClient.getTrustStorePwd().toCharArray());
		} finally {
			trustIs.close();
		}
		
		trustStore.setCertificateEntry("gd-root", x509Cert);

		trustStore.store(new FileOutputStream(trustStoreFilePath), mqttClient.getTrustStorePwd().toCharArray());

		LOG.info("Reconnecting with updated trustStore " + trustStoreFilePath + " ...");
		
		mqttClient.connect();
	}
}
