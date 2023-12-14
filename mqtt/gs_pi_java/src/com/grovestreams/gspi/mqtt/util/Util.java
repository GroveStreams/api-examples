package com.grovestreams.gspi.mqtt.util;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  Util.java
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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	public static String getMacAddress() throws UnknownHostException, SocketException {
		String mac = "";

		InetAddress ip = InetAddress.getLocalHost();
		if (ip != null) {
			NetworkInterface ni = NetworkInterface.getByInetAddress(ip);
			if (ni != null) {
				byte[] macAddressBytes = ni.getHardwareAddress();

				StringBuilder macAddressBuilder = new StringBuilder();
				for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
					String macAddressHexByte = String.format("%02x", macAddressBytes[macAddressByteIndex]);
					macAddressBuilder.append(macAddressHexByte);

					if (macAddressByteIndex != macAddressBytes.length - 1) {
						macAddressBuilder.append(":");
					}
				}
				mac = macAddressBuilder.toString();
			}
		}

		return mac;
	}
	
	public static String getCpuSerialNumber() throws IOException, InterruptedException{
		//Unique per Pi Zero 2 W
		String sNum = "";
		String cpuInfo = exeCmd(Util.getAppDir(), "cat /proc/cpuinfo");
		
		//cpuInfo = "Hardware	: BCM2835\n"
		//		+ "Revision	: 902120\n"
		//		+ "Serial		: 000000001b44b91e\n"
		//		+ "Model		: Raspberry Pi Zero 2 W Rev 1.0";
		
		
		int index = cpuInfo.indexOf("Serial		: ");
		if (index >= 0) {
			cpuInfo = cpuInfo.substring(index + "Serial		: ".length());
			sNum = cpuInfo.substring(0, cpuInfo.indexOf("Model"));		
		}

		return sNum;
	}
	
	public static String getHostName() throws IOException, InterruptedException{
		String hostName = exeCmd(Util.getAppDir(), "hostname");
		
		return hostName;
	}

	public static String getDeviceName(Properties properties) throws IOException, InterruptedException {
		// Needs to be unique within a GS organization folder

		// From properties file
		String cname = properties.getProperty("DEVICE_NAME");
		if (cname != null && !cname.isEmpty()) {
			LOG.info("DeviceName from gspi.properties file DEVICE_NAME: " + cname);
		}

		// From CPU Serial Number (unique per Pi Zero 2 W)
		if (cname == null || cname.isEmpty()) {
			cname = getHostName();
			if (cname != null && !cname.isEmpty()) {
				LOG.info("DeviceName from Operating System hostname (/etc/hostname): " + cname);
			}
			String cid = getDeviceId(properties);
			cname = cname + "(" + cid + ")";
			
		}

		if (cname == null || cname.isEmpty()) {
			// Last resort
			cname = "gs_pi";
			LOG.info("DeviceName from hard coded value: " + cname);
		}
		


		return cname;
	}
	
	public static String getDeviceId(Properties properties) throws IOException, InterruptedException {
		// Needs to be unique for all devices in a GS organization
		// A component name can be different from its ID,  but the name still needs to be unique within a GS Organization folder
		//  This will use the name and id together so that it is readable and unqique.

		// From properties file
		String cid = properties.getProperty("DEVICE_ID");
		if (cid != null && !cid.isEmpty()) {
			LOG.info("DeviceId from gspi.properties file DEVICE_ID: " + cid);
		}

		// From CPU Serial Number (unique per Pi Zero 2 W)
		if (cid == null || cid.isEmpty()) {
			cid = getCpuSerialNumber();
			if (cid != null && !cid.isEmpty()) {
				LOG.info("DeviceId from CPU Serial Number: " + cid);
			}
		}
		
		if (cid == null || cid.isEmpty()) {
			cid = Util.getMacAddress();
			if (cid != null && !cid.isEmpty()) {
				LOG.info("DeviceId from MAC Address: " + cid);
			}
		}

		if (cid == null || cid.isEmpty()) {
			// Last resort
			cid = "gs_pi";
			LOG.info("DeviceId from hard coded value: " + cid);
		}

		return cid;
	}
	


	public static boolean isWindows() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

		return isWindows;
	}

	public static String exeCmd(String dir, String command) throws IOException, InterruptedException {
		
		LOG.info("Executing command. Dir={}, Command={}", dir, command);


		Process exec = Runtime.getRuntime().exec(command, null, new File(dir));
		exec.waitFor();

		InputStream stdout = exec.getInputStream();
		InputStream stderr = exec.getErrorStream();

		StreamEater stdoutEater = new StreamEater(stdout, false);
		StreamEater stderrEater = new StreamEater(stderr, true);
		stdoutEater.start();
		stderrEater.start();
		
		exec.waitFor();
		
		stdoutEater.waitUntilDone();
	
		return stdoutEater.getOutput();
	}
	
	public static byte[] readFileAsBytes(String filePath) throws IOException {
		Path path = Paths.get(filePath);

		byte[] fileContent = Files.readAllBytes(path);
		
		return fileContent;
	}
	
	public static String readFileAsString(String filePath) throws IOException {
		
		return new String(readFileAsBytes(filePath), StandardCharsets.UTF_8);
	}
	
	public static String getAppDir() {
		//Return directory of the running java application
		return System.getProperty("user.dir");
	}

}
