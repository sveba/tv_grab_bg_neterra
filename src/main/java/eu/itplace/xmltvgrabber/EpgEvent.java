/**
 * EpgEvent.java erzeugt am 04.01.16
 * <p>
 * Eigentum der TeamBank AG Nürnberg
 */
package eu.itplace.xmltvgrabber;

/**
 * TODO
 *
 * @author Svetoslav Batchovski
 */
public class EpgEvent {
    String name;
    String description;
    long start;
    long end;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}
