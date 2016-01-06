package de.euitplace.xmltvgrabber;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import eu.itplace.xmltvgrabber.NeterraGrabber;

public class NetteraGrabberTest {
	private static final String TEST_JSON = "test.json";

	// @Test
	// public void testDownload() {
	// NeterraGrabber grabber = new NeterraGrabber();
	// JSONObject mediaJson = grabber.getNeterraEpgs();
	// JSONObject.testValidity(mediaJson);
	// assertNotNull(mediaJson);
	// System.out.println(mediaJson.toString());
	// }

	@Test
	@Ignore
	public void testCreateDoc() {
		NeterraGrabber grabber = new NeterraGrabber();
		grabber.createEpgXml();
	}

	private JSONObject restore() {
		try {
			return new JSONObject(readFile(TEST_JSON, Charset.defaultCharset()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	// private void writeJsonToFile() {
	// JSONObject bnt = (JSONObject) media.get("79");
	// FileWriter file = new FileWriter(TEST_JSON);
	// file.write(returned.toString());
	// file.close();
	// }
}
