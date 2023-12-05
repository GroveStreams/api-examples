package com.grovestreams.gspi.mqtt.util;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Grove Streams, LLC
 * PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 * FILENAME      :  Compressor.java
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;


public class Compressor {

	public static byte[] compressZLib(byte[] in) throws Exception {

		// Create the compressor with highest level ofcompression
		Deflater compressor = new Deflater();

		// compressor.setLevel(Deflater.BEST_COMPRESSION);
		compressor.setLevel(Deflater.BEST_COMPRESSION);
		compressor.setStrategy(Deflater.FILTERED);

		// Give the compressor the data to compress
		compressor.setInput(in);
		compressor.finish();

		ByteArrayOutputStream bos = new ByteArrayOutputStream(in.length);

		// Compress the data
		byte[] buf = new byte[2048];
		while (!compressor.finished()) {
			int count = compressor.deflate(buf);
			bos.write(buf, 0, count);
		}

		try {
			bos.close();
		} catch (IOException e) {
			throw new Exception(e);
		}
		compressor.end();

		byte[] compressedData = bos.toByteArray();

		return compressedData;
	}

	//////////////////////////////////////////////////

	public static byte[] compressGZip(byte[] content) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);

			gzipOutputStream.write(content);
			gzipOutputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return byteArrayOutputStream.toByteArray();
	}

	///////////////////////////////////

	public static byte[] uncompressZLib(byte[] in) {
		Inflater decompressor = new Inflater();
		decompressor.setInput(in);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(in.length);

		// Decompress the data
		byte[] buf = new byte[2048];

		while (!decompressor.finished()) {
			try {

				int count = decompressor.inflate(buf);

				bos.write(buf, 0, count);
			} catch (DataFormatException e) {
				throw new RuntimeException(e);
			}
		}

		try {
			bos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		decompressor.end();

		return bos.toByteArray();
	}

}
