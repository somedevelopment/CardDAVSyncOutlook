/*
 * Copyright (c) 2014 Swen Walkowski.
 * All rights reserved. Originator: Swen Walkowski.
 * Get more information about CardDAVSyncOutlook at https://github.com/somedevelopment/CardDAVSyncOutlook/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package utilities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import main.Status;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLUtilities {

    static public Document loadXMLFile(String strXMLFile) {
        try {
            Status.print("Load: " + strXMLFile);

            File fileXMLFile = new File(strXMLFile);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;

            dBuilder = dbFactory.newDocumentBuilder();
            dbFactory.setNamespaceAware(true);
            Document docXMLFile = dBuilder.parse(fileXMLFile);
            docXMLFile.getDocumentElement().normalize();

            return docXMLFile;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static public void printDocumentToSTDOUT(Document docXMLFile, OutputStream outStream) {
        try {
            Status.print("Print XML File");

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer;

            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");

            transformer.transform(new DOMSource(docXMLFile), new StreamResult(new OutputStreamWriter(outStream, "UTF-8")));
        } catch (UnsupportedEncodingException | TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @author
     * http://www.journaldev.com/1237/java-convert-string-to-xml-document-and-xml-document-to-string
     */
    static public String convertDocumentToString(Document docXMLFile) {
        try {
            Status.print("Save XML File");

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer;

            transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();

            transformer.transform(new DOMSource(docXMLFile), new StreamResult(writer));
            String strXMLAsString = writer.getBuffer().toString();
            return strXMLAsString;
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return null;
    }
//
//    public static Document convertStringToDocument(String xmlStr) {
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder;
//        try {
//            builder = factory.newDocumentBuilder();
//            Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) );
//            return doc;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
