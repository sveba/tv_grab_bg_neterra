package eu.itplace.xmltvgrabber;

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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

public class NeterraGrabber {
    private static final String URL = "http://www.neterra.tv/content/tv_guide/";
    private static final int DAYS = 5;
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyyMMddHHmmss Z");

    public static final String NETERRA_PRODUCT_NAME = "product_name";
    public static final String NETERRA_PRODUCT_FILE_TAG = "product_file_tag";
    public static final String NETERRA_EPG = "epg";
    public static final String NETERRA_EPG_PROD_NAME = "epg_prod_name";
    public static final String NETERRA_START_TIME_UNIX = "start_time_unix";
    public static final String NETERRA_END_TIME_UNIX = "end_time_unix";

    public static void main(String[] args) {
        NeterraGrabber grabber = new NeterraGrabber();
        Document doc = grabber.createEpgXml();
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

    public Document createEpgXml() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = docBuilder.newDocument();

        Element tv = doc.createElement("tv");
        tv.setAttribute("generator-info-name", "it-place.eu");
        doc.appendChild(tv);

        JSONObject jsonEpg = getJsonEpg();

        addChannelsTags(jsonEpg, doc, tv);
        addEpgTags(jsonEpg, doc, tv);

        return doc;
    }

    /**
     * Retrieves the json response and filters only the media object from it.
     *
     * @return
     */
    public JSONObject getJsonEpg() {
        JSONObject returned = new JSONObject();
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(URL + DAYS);
        httpget.addHeader("accept", "application/json");
        HttpResponse response;
        try {
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String result = EntityUtils.toString(entity);
                returned = new JSONObject(result);
                JSONObject media = null;
                try {
                    media = (JSONObject) returned.get("media");
                } catch (ClassCastException ex) {
                    // do nothing
                }
                return media;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returned;
    }

    private void addChannelsTags(JSONObject jsonEpg, Document doc, Element tv) {
        Element channelEl;
        Element channelNameEl;
        Iterator keys = jsonEpg.keys();
        String key;
        JSONObject sender;
        String channelName;
        String channelId;

        while (keys.hasNext()) {
            key = (String) keys.next();
            sender = (JSONObject) jsonEpg.get(key);
            channelName = sender.getString(NETERRA_PRODUCT_NAME);
            channelId = sender.getString(NETERRA_PRODUCT_FILE_TAG);

            channelEl = doc.createElement("channel");
            channelEl.setAttribute("id", channelId);

            channelNameEl = doc.createElement("display-name");
            channelNameEl.setAttribute("lang", "bg");
            channelNameEl.setTextContent(channelName);

            channelEl.appendChild(channelNameEl);
            tv.appendChild(channelEl);
        }
    }



    /**
     * Add EPG-Infos to
     *
     * @param jsonEpg
     * @return
     */
    private void addEpgTags(JSONObject jsonEpg, Document doc, Element tv) {
        if (jsonEpg != null) {
            String key;
            JSONObject sender;
            String channelId;
            EpgEvent epgEvent;
            JSONArray epgItems;
            JSONObject epgItem;

            Iterator keys = jsonEpg.keys();
            while (keys.hasNext()) {
                key = (String) keys.next();
                sender = (JSONObject) jsonEpg.get(key);
                channelId = sender.getString(NETERRA_PRODUCT_FILE_TAG);
                epgItems = (JSONArray) sender.get(NETERRA_EPG);

                for (int i = 0; i < epgItems.length(); i++) {
                    epgEvent = new EpgEvent();

                    epgItem = (JSONObject) epgItems.get(i);

                    epgEvent.setName(epgItem.getString(NETERRA_EPG_PROD_NAME));
                    epgEvent.setStart(epgItem.getLong(NETERRA_START_TIME_UNIX));
                    epgEvent.setEnd(epgItem.getLong(NETERRA_END_TIME_UNIX));
                    epgEvent.setDescription(epgItem.getString("description"));

                    addEvent(doc, tv, channelId, epgEvent);
                }
            }

        }
    }

    /**
     * Adds epg-Tag to the xml
     *
     * @param doc
     * @param tv
     * @param channelName
     * @param epgEvent
     */
    private void addEvent(Document doc, Element tv, String channelName, EpgEvent epgEvent) {
        Element programme;
        Element title;
        Calendar startCal;
        Calendar stopCal;
        String description;

        programme = doc.createElement("programme");
        programme.setAttribute("channel", channelName.trim());

        startCal = new GregorianCalendar();
        startCal.setTimeInMillis(epgEvent.getStart() * 1000);

        stopCal = new GregorianCalendar();
        stopCal.setTimeInMillis(epgEvent.getEnd() * 1000);

        programme.setAttribute("start", DF.format(startCal.getTime()));
        programme.setAttribute("stop", DF.format(stopCal.getTime()));

        title = doc.createElement("title");
        title.setAttribute("lang", "bg");

        description = epgEvent.getDescription();
        if (!epgEvent.getDescription().contains(epgEvent.getName())) {
            description = epgEvent.getName() + " - " + epgEvent.getDescription();
        }

        title.setTextContent(description);

        programme.appendChild(title);
        tv.appendChild(programme);
    }
}
