package eu.itplace.xmltvgrabber;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Grabber {
    private static final String TEST_JSON = "test.json";
    private static final String url = "http://www.neterra.tv/content/tv_guide/0/live/60/";
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyyMMddhhmmss");

    private static Map<String, String> channels = new HashMap<String, String>();
    static {
	channels.put("79", "BNT2");
	channels.put("39", "NEWS7");
	channels.put("60", "BGonAIR");
	channels.put("93", "Diema Family +1");
	channels.put("2", "TV7");
	channels.put("32", "NOVA");
	channels.put("31", "BNT1");
    }

    public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerFactoryConfigurationError,
    TransformerException {
	Grabber grabber = new Grabber();
	JSONObject jsonObject = (JSONObject) grabber.restore().get("media");
	grabber.createDoc(jsonObject);
	// grabber.doInBackground();
	// String json = grabber.restore().toString();
	// log.info(json);
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

    static String readFile(String path, Charset encoding) throws IOException {
	byte[] encoded = Files.readAllBytes(Paths.get(path));
	return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }

    private JSONObject doInBackground() {
	JSONObject returned = new JSONObject();
	HttpClient httpclient = HttpClients.createDefault();
	HttpGet httpget = new HttpGet(url);
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
		//JSONObject bnt = (JSONObject) media.get("79");
		//log.info(bnt.toString());

		// FileWriter file = new FileWriter(TEST_JSON);
		// file.write(returned.toString());
		// file.close();
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return returned;
    }

    private void createDoc(JSONObject media) throws ParserConfigurationException, TransformerFactoryConfigurationError,
    TransformerException {
	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
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

	Element programme;
	Element title;
	Calendar startCal;
	Calendar stopCal;
	for (Entry<String, String> entry : channels.entrySet()) {
	    JSONObject sender = (JSONObject) media.get(entry.getKey());
	    JSONArray epgs = (JSONArray) sender.get("epg");
	    for (int i = 0; i < epgs.length(); i++) {
		JSONObject epg = (JSONObject) epgs.get(i);
		programme = doc.createElement("programme");
		programme.setAttribute("channel", entry.getKey());
		startCal = new GregorianCalendar();
		startCal.setTimeInMillis(epg.getLong("start_time_unix")*1000);

		stopCal = new GregorianCalendar();
		stopCal.setTimeInMillis(epg.getLong("end_time_unix")*1000);

		programme.setAttribute("start", DF.format(startCal.getTime()));
		programme.setAttribute("stop", DF.format(stopCal.getTime()));

		title = doc.createElement("title");
		title.setAttribute("lang", "bg");
		title.setTextContent(epg.getString("epg_prod_name"));

		programme.appendChild(title);
		tv.appendChild(programme);
	    }
	}

	// output DOM XML to console
	Transformer transformer = TransformerFactory.newInstance().newTransformer();
	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	DOMSource source = new DOMSource(doc);
	StreamResult console = new StreamResult(System.out);
	transformer.transform(source, console);

	//log.info(doc.toString());
    }
}
