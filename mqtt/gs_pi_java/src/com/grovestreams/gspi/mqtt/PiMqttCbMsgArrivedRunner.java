package com.grovestreams.gspi.mqtt;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import com.grovestreams.gspi.mqtt.publishers.MqqtOrgIdentifier;
import com.grovestreams.gspi.mqtt.publishers.MqttMySettings;
import com.grovestreams.gspi.mqtt.subscribers.MqqtProvisioner;
import com.grovestreams.gspi.mqtt.subscribers.MqttCertRoller;
import com.grovestreams.gspi.mqtt.subscribers.MqttTrustStoreUpdater;
import com.grovestreams.gspi.mqtt.util.Util;

public class PiMqttCbMsgArrivedRunner implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(PiMqttCbMsgArrivedRunner.class);

	PiMqttClient mqttClient;
	String topic;
	MqttMessage message;

	public PiMqttCbMsgArrivedRunner(PiMqttClient mqttClient, String topic, MqttMessage message)
			throws SocketException, UnknownHostException {
		super();
		this.mqttClient = mqttClient;
		this.topic = topic;
		this.message = message;
	}

	@Override
	public void run() {
		String deviceId = mqttClient.getDeviceId();

		try {
			LOG.info("messageArrived: " + topic);

			if (topic.equals(mqttClient.getOrgUid() + "/cid/" + deviceId + "/reply")) {
				//Client will check the replyId inside the message to determine if it is their reply

				MqqtProvisioner provisioner = new MqqtProvisioner(mqttClient);
				provisioner.getMqttReply().handleReply(topic, message);
				
				MqttCertRoller certRotator = new MqttCertRoller(mqttClient);
				certRotator.getMqttReply().handleReply(topic, message);
				
				MqttMySettings mySettings = new MqttMySettings(mqttClient);
				mySettings.getMqttReply().handleReply(topic, message);
				
			} else if (topic.equals("manage/cert/myorg/cid/" + deviceId + "/reply")) {
				
				MqqtOrgIdentifier orgIdentifier = new MqqtOrgIdentifier(mqttClient);
				orgIdentifier.getMqttReply().handleReply(topic, message);
				
			} else if (topic.equals(mqttClient.getOrgUid() + "/manage/cert/roll/cid/" + deviceId)) {

				MqttCertRoller certRotator = new MqttCertRoller(mqttClient);
				certRotator.rollCert();
				
			} else if (topic.equals(mqttClient.getOrgUid() + "/manage/cert/update_truststore/cid/" + deviceId)) {

				MqttTrustStoreUpdater trustStoreUpdater = new MqttTrustStoreUpdater(mqttClient);
				trustStoreUpdater.updateTrustStore(message.getPayload());
				
			} else if (topic.equals(mqttClient.getOrgUid() + "/manage/restart/cid/" + deviceId)) {

				Util.exeCmd(".", "reboot");
				
			} else if (topic.equals(mqttClient.getOrgUid() + "/manage/provision/cid/" + deviceId)) {

				MqqtProvisioner provisioner = new MqqtProvisioner(mqttClient);
				provisioner.downloadAndProvision();

			}  else {
				LOG.info("Subscribed topic arrived but there was no handler: " + topic);
			}

		} catch (Exception e) {

			LOG.error("Exception",e);
		}

	}

}
