package com.grovestreams.gspi.mqtt;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grovestreams.gspi.mqtt.publishers.MqqtOrgIdentifier;
import com.grovestreams.gspi.mqtt.publishers.MqttMySettings;
import com.grovestreams.gspi.mqtt.subscribers.MqqtProvisioner;
import com.grovestreams.gspi.mqtt.subscribers.MqttCertRoller;
import com.grovestreams.gspi.mqtt.subscribers.MqttTrustStoreUpdater;

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
		String orgUid = mqttClient.getOrgUid();

		try {
			LOG.info("messageArrived topic: {} orgUid: {} deviceId: {} ", topic, orgUid, deviceId);

			if (topic.equals(orgUid + "/cid/" + deviceId + "/reply")) {
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
				
			} else if (topic.equals(orgUid + "/manage/cert/roll/cid/" + deviceId)) {

				MqttCertRoller certRotator = new MqttCertRoller(mqttClient);
				certRotator.rollCert();
				
			} else if (topic.equals(orgUid + "/manage/cert/update_truststore/cid/" + deviceId)) {

				MqttTrustStoreUpdater trustStoreUpdater = new MqttTrustStoreUpdater(mqttClient);
				trustStoreUpdater.updateTrustStore(message.getPayload());
				
			} else if (topic.equals(orgUid + "/manage/restart/cid/" + deviceId)) {

				MqqtProvisioner.restart();
				
			} else if (topic.equals(orgUid + "/manage/provision/cid/" + deviceId)) {

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
