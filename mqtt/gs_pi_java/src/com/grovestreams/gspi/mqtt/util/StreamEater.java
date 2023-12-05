package com.grovestreams.gspi.mqtt.util;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  StreamEater.java
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StreamEater extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(StreamEater.class);
	BufferedReader br;
	
	private boolean logOutput = false;
	private String output = "";
	private boolean done = false;

	/** Construct a StreamEater on an InputStream. */
	public StreamEater(InputStream is, boolean logOutput) {
		this.logOutput = logOutput;
		this.br = new BufferedReader(new InputStreamReader(is));
	}

	public String getOutput() {
		return output;
	}
	
	public void waitUntilDone() throws InterruptedException {
		while (!done) {
			TimeUnit.MILLISECONDS.sleep(5);
		}
	}

	public void run() {
		try {
			String line;
			while ((line = br.readLine()) != null) {
				output += line;
				if (logOutput) {
					LOG.info(line);	
				}
						
			}
			
		} catch (IOException e) {
			LOG.error("Exception",e);
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				LOG.error("Exception",e);
			}
			done = true;
		}
	}
}