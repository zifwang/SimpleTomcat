package SimpleTomcat.util;

import javax.xml.bind.*;
import java.io.*;

/**
 * XMLUtil implements transfer between .xml file and object (entity)
 *  This implementation utilizes javax.xml package.
 */
public class XMLUtil {

    /**
     * Convert java object to .xml file output
     * @param obj: object
     * @return xml string
     * @throws JAXBException: JAXBException
     */
    public static String convertObjectToXml(Object obj) throws JAXBException {

        JAXBContext context = JAXBContext.newInstance(obj.getClass());
        // Create a Marshaller object that can be used to convert a java content tree into XML data.
        Marshaller marshaller = context.createMarshaller();
        // Set the particular property in the underlying implementation of Marshaller.
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                Boolean.TRUE);

        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(obj, stringWriter);

        return stringWriter.toString();
    }

    /**
     * Generate xml file from java object
     * @param obj: object
     * @param path: file path
     * @throws JAXBException: JAXBException
     */
    public static void convertObjectToXml(Object obj, String path) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(obj.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        // create file
        FileWriter fileWriter = new FileWriter(path);
        // transform obj to xml file
        marshaller.marshal(obj, fileWriter);
    }

    /**
     * Convert xml string to java object
     * @param clazz: object related class
     * @param xml: xml string
     * @return object: clazz object
     * @throws JAXBException: JAXBException
     */
    @SuppressWarnings("unchecked")
    public static Object convertXmlStrToObject(Class clazz, String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        // core implementation to transform xml to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader stringReader = new StringReader(xml);

        return unmarshaller.unmarshal(stringReader);
    }

    /**
     * convert xml file to java object
     * @param clazz: object related class
     * @param path: xml file path
     * @return: object: clazz object
     * @throws JAXBException: JAXBException
     * @throws FileNotFoundException: file not found
     */
    @SuppressWarnings("unchecked")
    public static Object convertXmlFileToObject(Class clazz, String path) throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        FileReader fileReader = new FileReader(path);
        return unmarshaller.unmarshal(fileReader);
    }
}
