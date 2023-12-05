package com.grovestreams.gspi.distance;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  LedController.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;

public class LedController {
	private static final Logger LOG = LoggerFactory.getLogger(LedController.class);

	private DigitalOutput ledGreen;
	private DigitalOutput ledYellow;
	private DigitalOutput ledRed;

	private static final int GPIO_GREEN = 17;  // PIN 11 = GPIO 17
	private static final int GPIO_YELLOW = 27; // PIN 13 = GPIO 27
	private static final int GPIO_RED = 22;    // PIN 15 = GPIO 22

	public LedController(Context pi4j) throws Exception {
		super();

		ledGreen = createLed(pi4j, "LED Green", GPIO_GREEN);
		ledYellow = createLed(pi4j, "LED Yellow", GPIO_YELLOW);
		ledRed = createLed(pi4j, "LED Red", GPIO_RED);
	}

	private DigitalOutput createLed(Context pi4j, String pinName, int pinNum) throws Exception {

		DigitalOutputConfigBuilder ledConfig = DigitalOutput.newConfigBuilder(pi4j).id(pinName).name(pinName)
				.address(pinNum).shutdown(DigitalState.LOW).initial(DigitalState.LOW)
				.provider("pigpio-digital-output");

		DigitalOutput led = pi4j.create(ledConfig);

		return led;
	}

	public void greenOn() {
		
		if (!ledGreen.equals(DigitalState.HIGH)) {
			LOG.info("led green on");
			ledGreen.high();
		}
	}

	public void greenOff() {

		if (!ledGreen.equals(DigitalState.LOW)) {
			LOG.info("led green off");
			ledGreen.low();
		}
	}

	public void yellowOn() {

		if (!ledYellow.equals(DigitalState.HIGH)) {
			LOG.info("led yellow on");
			ledYellow.high();
		}
	}

	public void yellowOff() {

		if (!ledYellow.equals(DigitalState.LOW)) {
			LOG.info("led yellow off");
			ledYellow.low();
		}
	}

	public void redOn() {

		if (!ledRed.equals(DigitalState.HIGH)) {
			LOG.info("led red on");
			ledRed.high();
		}
	}

	public void redOff() {

		if (!ledRed.equals(DigitalState.LOW)) {
			LOG.info("led red off");
			ledRed.low();
		}
	}
}
