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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

/**
 * TODO
 *
 * @author Svetoslav Batchovski
 */
public class App {
    public static void main(String[] args) throws IOException {

        OptionParser parser = new OptionParser();

        OptionSpec<Integer> daysOpt =  parser.accepts("d", "number of EPG days to get")
            .withOptionalArg()
            .ofType(Integer.class)
            .defaultsTo(5);

        OptionSpec<String> tzOpt = parser.accepts("t", "Timezone. Example GMT+1 or GMT-5")
            .withOptionalArg()
            .ofType(String.class)
            .defaultsTo("GMT");

        OptionSpec<File> outputFileOpt = parser
            .accepts("o", "output filename")
            .withRequiredArg()
            .ofType(File.class);

        OptionSpec<?> helpOpt = parser
            .accepts("h", "show help")
            .forHelp();

        OptionSet opt = parser.parse(args);

        TimeZone timeZone = TimeZone.getTimeZone(tzOpt.value(opt));
        int days = daysOpt.value(opt);
        File outputFile = outputFileOpt.value(opt);

        if(opt.has("h")){
            parser.printHelpOn(System.out);
            return;
        }

        NeterraGrabber grabber = new NeterraGrabber();
        grabber.getNeterraEpgs(days);
        grabber.getChannelsAndEpgs();
        Document doc = grabber.createEpgXml(timeZone);

        StreamResult stream = new StreamResult(System.out);

        if(opt.has( "o" )){
            stream = new StreamResult(outputFileOpt.value(opt));
        }

        printXml(doc, stream);
    }

    /**
     * Prints the xml to the sysout
     *
     * @param doc
     */
    private static void printXml(Document doc, StreamResult stream) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            DOMSource source = new DOMSource(doc);

            transformer.transform(source, stream);
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            e.printStackTrace();
        }

    }

}
