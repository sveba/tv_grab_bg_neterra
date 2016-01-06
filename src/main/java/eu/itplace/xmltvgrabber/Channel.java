/**
 * Channel.java erzeugt am 05.01.16
 * <p>
 * Eigentum der TeamBank AG Nürnberg
 */
package eu.itplace.xmltvgrabber;

import java.util.Map;
import java.util.TreeMap;

/**
 * TODO
 *
 * @author Svetoslav Batchovski
 */
public class Channel {
    private String id;
    private String displayName;
    private Map<Long, EpgEvent> epgEvents = new TreeMap<>();

    public Map<Long, EpgEvent> getEpgEvents() {
        return epgEvents;
    }

    public void setEpgEvents(Map<Long, EpgEvent> epgEvents) {
        this.epgEvents = epgEvents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
