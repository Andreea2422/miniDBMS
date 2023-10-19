package utils;

import model.Databases;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Utils {
    private static final String DBMS_XML_FILE = "D:\\UNI\\MASTER AN 1\\ISGBD\\MiniDBMS\\MainApp\\Server\\files\\MyCatalog.xml";

    public static Databases loadDBMSFromXML() {
        File xmlFile = new File(DBMS_XML_FILE);
        if (xmlFile.exists()) {
            try {
                JAXBContext context = JAXBContext.newInstance(Databases.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return (Databases) unmarshaller.unmarshal(xmlFile);
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void saveDBMSToXML(Databases dbms) {
        try {
            File xmlFile = new File(DBMS_XML_FILE);
            JAXBContext context = JAXBContext.newInstance(Databases.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(dbms, xmlFile);
            System.out.println("DBMS structure saved to " + DBMS_XML_FILE);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
