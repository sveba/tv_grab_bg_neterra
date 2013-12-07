package eu.itplace.xmltvgrabber;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NeterraGrabber {
	private static final String URL = "http://www.neterra.tv/content/tv_guide/";
	private static final int DAYS = 3;
	private static final SimpleDateFormat DF = new SimpleDateFormat("yyyyMMddHHmmss");

	private static Map<String, String> channels = new HashMap<String, String>();
	static {
		channels.put("bnt2", "BNT2");
		channels.put("News7", "NEWS7");
		channels.put("BGonAir", "BGonAIR");
		channels.put("diemaf", "Diema Family +1");
		channels.put("tv7", "TV7");
		channels.put("nova", "NOVA");
		channels.put("bnt1", "BNT1");
	}

	public static void main(String[] args) {
		NeterraGrabber grabber = new NeterraGrabber();
		Document doc = grabber.createDoc();
		grabber.printXml(doc);
	}

	/**
	 * Prints the xml to the sysout
	 * 
	 * @param doc
	 */
	public void printXml(Document doc) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult console = new StreamResult(System.out);
			transformer.transform(source, console);
		} catch (TransformerFactoryConfigurationError | TransformerException e) {
			e.printStackTrace();
		}

	}

	public Document createDoc() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document doc = docBuilder.newDocument();

		Element tv = doc.createElement("tv");
		tv.setAttribute("generator-info-name", "shari");
		doc.appendChild(tv);

		Element channel;
		Element channelName;
		for (Entry<String, String> entry : channels.entrySet()) {
			channel = doc.createElement("channel");
			channel.setAttribute("id", entry.getKey());

			channelName = doc.createElement("display-name");
			channelName.setAttribute("lang", "bg");
			channelName.setTextContent(entry.getValue());

			channel.appendChild(channelName);
			tv.appendChild(channel);
		}

		for (int i = 0; i < DAYS; i++) {
			addEpgForDay(i, doc, tv);
		}

		return doc;
	}

	private void addEpgForDay(int day, Document doc, Element tv) {
		JSONObject jsonEpg = getJsonEpg(day);
		addEpg(day, jsonEpg, doc, tv);
	}

	/**
	 * Retrieves the json response and filters only the media object from it.
	 * 
	 * @return
	 */
	public JSONObject getJsonEpg(int i) {
		JSONObject returned = new JSONObject();
		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(URL + i);
		httpget.addHeader("accept", "application/json");
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				String result = EntityUtils.toString(entity);
				returned = new JSONObject(result);
				JSONObject media = (JSONObject) returned.get("media");
				return media;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return returned;
	}

	/**
	 * Create XML-Doc
	 * 
	 * @param jsonEpg
	 * @return
	 */
	private void addEpg(int day, JSONObject jsonEpg, Document doc, Element tv) {
		Element programme;
		Element title;
		Calendar startCal;
		Calendar stopCal;
		for (Entry<String, String> entry : channels.entrySet()) {
			Iterator keys = jsonEpg.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				JSONObject sender = (JSONObject) jsonEpg.get(key);
				String jsonChannel = sender.getString("media_name");
				String jsonChannel2 = sender.getString("product_file_tag");
				if (jsonChannel != null
						&& (jsonChannel.equalsIgnoreCase(entry.getKey()) || jsonChannel2.equalsIgnoreCase(entry.getKey()))) {
					JSONArray epgs = (JSONArray) sender.get("epg");

					for (int i = 0; i < epgs.length(); i++) {
						JSONObject epg = (JSONObject) epgs.get(i);
						programme = doc.createElement("programme");
						programme.setAttribute("channel", entry.getKey());
						startCal = new GregorianCalendar();
						startCal.setTimeInMillis(epg.getLong("start_time_unix") * 1000);

						stopCal = new GregorianCalendar();
						stopCal.setTimeInMillis(epg.getLong("end_time_unix") * 1000);

						programme.setAttribute("start", DF.format(startCal.getTime()));
						programme.setAttribute("stop", DF.format(stopCal.getTime()));

						title = doc.createElement("title");
						title.setAttribute("lang", "bg");
						title.setTextContent(epg.getString("epg_prod_name"));

						programme.appendChild(title);
						tv.appendChild(programme);
					}
				}
			}
		}
	}
}
