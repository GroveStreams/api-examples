package com.grovestreams.gspi.distance;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  DistanceSensor.java
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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;

public class DistanceSensor {
	private static final Logger LOG = LogManager.getLogger(DistanceSensor.class);
	
	//For JSN-SR04T sensor

	private DigitalOutput trigOut;
	private DigitalInput echoIn;

	private static final int GPIO_TRIG= 24; 
	private static final int GPIO_ECHO = 23; 
	
	private static final long TIMEOUT_NANOS = 100L * 1000 * 1000;


	public DistanceSensor(Context pi4j) throws Exception {
		super();

		DigitalOutputConfigBuilder trigOutConfig = DigitalOutput.newConfigBuilder(pi4j).id("TRIG").name("TRIG")
				.address(GPIO_TRIG).shutdown(DigitalState.LOW).initial(DigitalState.LOW).provider("pigpio-digital-output");
		trigOut = pi4j.create(trigOutConfig);
		
		
		DigitalInputConfigBuilder echoInConfig =  DigitalInput.newConfigBuilder(pi4j).id("ECHO").name("ECHO")
				.address(GPIO_ECHO).provider("pigpio-digital-input");
		echoIn = pi4j.create(echoInConfig);
		
	}


	public double getDistance() throws InterruptedException {
		double distance = -1.0;
		

		trigOut.low();
	
		// Let the sensor settle for a while
		TimeUnit.MILLISECONDS.sleep(100);
		
		/////////Begin Pulse Read 
		
		//Avoid tweaking code in this section. It can impact readings. 
		// Adding logging in here will impact readings
		// Adding timeouts impacts readings, but one is needed for the pulseStart detection
		//   loop as testing shows certain distances (like very short) can cause an infinite loop
				
		//Send sound pulse
		trigOut.high();
		TimeUnit.MICROSECONDS.sleep(10);
		trigOut.low();


		//Wait for pulse to begin
		long pulseStart = java.lang.System.nanoTime();
		long timeoutEnd = pulseStart + TIMEOUT_NANOS;
		while (echoIn.isLow() && pulseStart < timeoutEnd) {
			pulseStart = java.lang.System.nanoTime();
		}
		
		if (pulseStart >= timeoutEnd) {
			LOG.info("Wait for pulse to begin timeout!!");
			return -1;
		}

		//Wait for pulse to end
		while (echoIn.isHigh()) {
		}
		long pulseEnd = java.lang.System.nanoTime();

		/////////End Pulse Read
		
		//Calc elapsed in secs while converting from nanosecs
		double elapsedSecs = (pulseEnd - pulseStart) /(1000d*1000d*1000d);
		LOG.debug("elapsedSecs = " + elapsedSecs);

		distance = elapsedSecs * 17150d ;
		LOG.debug(String.format("distance = %.2f cm", distance));
		
		if (distance < 25 || distance > 450) {
			//out of range
			distance = -1; 
		}
		
		return distance;
	}

}
