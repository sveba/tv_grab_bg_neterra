/**
 * AppRunner.java erzeugt am 05.01.16
 * <p>
 * Eigentum der TeamBank AG Nürnberg
 */
package eu.itplace.xmltvgrabber;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * TODO
 *
 * @author Svetoslav Batchovski
 */
public class App {
    public static void main(String[] args) {
        NeterraGrabber grabber = new NeterraGrabber();
        grabber.getNeterraEpgs(5);
        grabber.getChannelsAndEpgs();
        Document doc = grabber.createEpgXml();
        printXml(doc);
    }

    /**
     * Prints the xml to the sysout
     *
     * @param doc
     */
    private static void printXml(Document doc) {
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

}
