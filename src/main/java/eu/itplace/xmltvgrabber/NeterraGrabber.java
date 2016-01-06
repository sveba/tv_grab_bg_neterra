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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NeterraGrabber {
    private static final String URL = "http://www.neterra.tv/content/tv_guide/";
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyyMMddHHmmss Z");

    public static final String NETERRA_PRODUCT_NAME = "product_name";
    public static final String NETERRA_PRODUCT_FILE_TAG = "product_file_tag";
    public static final String NETERRA_EPG = "epg";
    public static final String NETERRA_EPG_PROD_NAME = "epg_prod_name";
    public static final String NETERRA_START_TIME_UNIX = "start_time_unix";
    public static final String NETERRA_END_TIME_UNIX = "end_time_unix";
    public static final String NETERRA_DESCRIPTION = "description";

    private Map<String, Channel> channels = new TreeMap<>();
    private List<JSONObject> neterraEpgs = new ArrayList<>();

    /**
     * Create the xml file
     * @return
     */
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

        for (Channel channel : channels.values()) {
            addChannelTag(doc, tv, channel);
        }

        for (Channel channel : channels.values()) {
            for(Map.Entry<Long, EpgEvent> entry: channel.getEpgEvents().entrySet()) {
                addEpgTag(doc, tv, entry.getValue());
            }
        }

        return doc;
    }

    /**
     * Retrieves the json response and filters only the media object from it.
     *
     * @return
     */
    public void getNeterraEpgs(int days) {
        JSONObject returned;
        HttpGet httpget;
        HttpResponse response;
        HttpClient httpclient = HttpClients.createDefault();

        for (int i = 0; i < days; i++) {
            httpget = new HttpGet(URL + i);
            httpget.addHeader("accept", "application/json");

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
                    neterraEpgs.add(media);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getChannelsAndEpgs() {
        String key;
        String channelId;
        JSONObject neterraChannel;
        Channel channel;
        Iterator it;

        for (JSONObject neterraEpg : neterraEpgs) {
            it = neterraEpg.keys();
            while (it.hasNext()) {
                key = (String) it.next();
                neterraChannel = (JSONObject) neterraEpg.get(key);

                channelId = neterraChannel.getString(NETERRA_PRODUCT_FILE_TAG);

                channel = channels.containsKey(channelId) ? channels.get(channelId) : new Channel();

                channel.setDisplayName(neterraChannel.getString(NETERRA_PRODUCT_NAME).trim());
                channel.setId(channelId);
                channel.getEpgEvents().putAll(convertEpgEvents(neterraChannel, channel));

                channels.put(channel.getId(), channel);
            }
        }
    }

    private Map<Long, EpgEvent> convertEpgEvents(JSONObject neterraChannel, Channel channel) {
        Map<Long, EpgEvent> channelEpgEvents = new TreeMap<>();

        if (neterraChannel != null) {
            EpgEvent epgEvent;
            JSONArray epgItems;
            JSONObject epgItem;

            epgItems = (JSONArray) neterraChannel.get(NETERRA_EPG);

            for (int i = 0; i < epgItems.length(); i++) {
                epgEvent = new EpgEvent();

                epgItem = (JSONObject) epgItems.get(i);

                epgEvent.setChannelId(channel.getId());
                epgEvent.setName(epgItem.getString(NETERRA_EPG_PROD_NAME));
                epgEvent.setStart(epgItem.getLong(NETERRA_START_TIME_UNIX));
                epgEvent.setEnd(epgItem.getLong(NETERRA_END_TIME_UNIX));
                epgEvent.setDescription(epgItem.getString(NETERRA_DESCRIPTION));

                channelEpgEvents.put(epgEvent.getStart(), epgEvent);
            }
        }

        return channelEpgEvents;
    }

    /**
     * Add channel-tag to the xml
     * @param doc
     * @param tv
     * @param channel
     */
    private void addChannelTag(Document doc, Element tv, Channel channel) {
        Element channelEl;
        Element channelNameEl;

        channelEl = doc.createElement("channel");
        channelEl.setAttribute("id", channel.getId());

        channelNameEl = doc.createElement("display-name");
        channelNameEl.setAttribute("lang", "bg");
        channelNameEl.setTextContent(channel.getDisplayName());

        channelEl.appendChild(channelNameEl);
        tv.appendChild(channelEl);
    }

    /**
     * Adds epg-tag to the xml
     *
     * @param doc
     * @param tv
     * @param epgEvent
     */
    private void addEpgTag(Document doc, Element tv, EpgEvent epgEvent) {
        Element programme;
        Element title;
        Element description;
        Calendar startCal;
        Calendar stopCal;

        programme = doc.createElement("programme");
        programme.setAttribute("channel", epgEvent.getChannelId().trim());

        startCal = new GregorianCalendar();
        startCal.setTimeInMillis(epgEvent.getStart() * 1000);

        stopCal = new GregorianCalendar();
        stopCal.setTimeInMillis(epgEvent.getEnd() * 1000);

        programme.setAttribute("start", DF.format(startCal.getTime()));
        programme.setAttribute("stop", DF.format(stopCal.getTime()));

        title = doc.createElement("title");
        title.setAttribute("lang", "bg");
        title.setTextContent(epgEvent.getName());

        description = doc.createElement("desc");
        description.setAttribute("lang", "bg");
        description.setTextContent(epgEvent.getDescription());

        programme.appendChild(title);
        programme.appendChild(description);

        tv.appendChild(programme);
    }
}
