package com.cala.scanner.classes;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XMLParser {
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final String XML_TAG_TEXT = "text";
    private static final String XML_TAG_DATE = "date";
    private static final String XML_TAG_INFO = "info";
    private static final String XML_TAG_INFOS = "infos";

    private static final int TYPE_ELEMENT = 1;
    private static final int TYPE_ATTRIBUTE = 2;

    private final int FOLDER_EMPTY = 11;
    private final int FOLDER_FILLED = 10;
    private final int NO_FOLDER = -11;

    private final int FILE_EMPTY = 22;//file without infos and header
    private final int FILE_HEADER = 21;//file contains header only
    private final int FILE_FILLED = 20;//file contains header and infos
    private final int NO_FILE = -21;

    private final int ACCESS_DENIED = 0;

    private final String FILE_NAME = "saved";
    private final String FOLDER_NAME = "Scanner";

    private String folderPath;
    private String filePath;

    private Document document;

    public XMLParser(Context context) {
        try {
            //get the private stoage directory
            this.folderPath = context.getExternalFilesDir(null).getAbsolutePath() + File.separator;
        } catch(NullPointerException e){
            e.printStackTrace();
        }
        this.filePath = folderPath + FOLDER_NAME + File.separator;

        if (checkFolderAndFile() != ACCESS_DENIED)
            toDocument();
    }

    public boolean delete(Info info) {
        List<Info> infos = read();
        int i, len;

        if ((len = infos.size()) > 0)
            for (i = 0; i < len; i++)
                if (infos.get(i).equals(info))
                    if (this.document != null) {
                        infos.remove(i);
                        return writeNew(infos);
                    }
        return false;
    }

    public boolean find(Info info) {
        int i, size;
        List<Info> infos = read();

        if ((size = infos.size()) > 0)
            for (i = 0; i < size; i++)
                if (infos.get(i).equals(info))
                    return true;
        return false;
    }

    public List<Info> read() {
        List<Info> infos = new ArrayList<>();
        if (this.document != null)
            try {
                NodeList nList = this.document.getElementsByTagName(XML_TAG_INFO);
                for (int i = 0; i < nList.getLength(); i++) {
                    Element element = (Element) nList.item(i);//get element

                    String text = getValue(XML_TAG_TEXT, element, TYPE_ELEMENT);
                    Date date = Date.toDate(getValue(XML_TAG_DATE, element, TYPE_ATTRIBUTE));
                    infos.add(new Info(text, date));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return infos;
    }

    public boolean update(Info oldInfo, Info newInfo) {
        List<Info> infos = read();
        int i, len;

        if ((len = infos.size()) > 0)
            for (i = 0; i < len; i++)
                if (infos.get(i).equals(oldInfo))
                    if (this.document != null)
                        return refresh(newInfo, i);
        return false;
    }

    public boolean write(Info info) {
        if (!find(info)) {
            Element root;
            switch (checkFolderAndFile()) {
                case FILE_EMPTY:
                case FILE_HEADER:
                    root = this.document.createElement(XML_TAG_INFOS);
                    this.document.appendChild(root);
                    break;
                case FILE_FILLED:
                    root = this.document.getDocumentElement();
                    break;
                case ACCESS_DENIED:
                default:
                    return false;
            }
            return add(info, root);
        }
        return false;
    }

    private boolean add(Info i, Element root) {
        //info
        Element info = this.document.createElement(XML_TAG_INFO);
        root.appendChild(info);

        //date
        info.setAttribute(XML_TAG_DATE, i.date.toString());

        //text
        Element text = this.document.createElement(XML_TAG_TEXT);
        text.appendChild(this.document.createTextNode(i.getText()));
        info.appendChild(text);

        return toFile();
    }

    private String getValue(String tag, Element element, int type) {
        switch (type) {
            case TYPE_ELEMENT:
                return element.getElementsByTagName(tag).item(0).getChildNodes().item(0).getNodeValue();
            case TYPE_ATTRIBUTE:
                return element.getAttributeNode(tag).getValue();
            default:
                return null;
        }
    }

    private boolean refresh(Info info, int pos) {
        Node node = this.document.getElementsByTagName(XML_TAG_INFO).item(pos);
        Element element = (Element) node;
        //update text
        element.getElementsByTagName(XML_TAG_TEXT).item(0).getFirstChild().setTextContent(info.getText());
        //update date
        element.setAttribute(XML_TAG_DATE, info.date.toString());

        return toFile();
    }

    private boolean writeNew(List<Info> infos) {

        if (newDocument()) {
            Element root;
            boolean error;
            int i, len;

            if ((len = infos.size()) > 0) {
                root = this.document.createElement(XML_TAG_INFOS);
                this.document.appendChild(root);

                for (i = 0, error = false; i < len && !error; i++)
                    error = !add(infos.get(i), root);

                return !error;
            }
            //if list is empty I only need to create new document
            return true;
        }
        return false;
    }

    private boolean newDocument() {
        try {
            //clear document
            this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            return toFile();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean toFile() {
        try {
            //write into XML file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //indent  code
            DOMSource source = new DOMSource(this.document);
            StreamResult result = new StreamResult(new File(filePath + FILE_NAME));

            transformer.transform(source, result);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean toDocument() {
        try {
            File xmlFile = new File(filePath + FILE_NAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            this.document = dBuilder.parse(xmlFile);
            this.document.getDocumentElement().normalize();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            //if document does not exists / is empty
            return newDocument();
        }
    }

    private int checkFolderAndFile() {
        switch (folderExistsAndNotEmpty()) {
            case FOLDER_FILLED:
                switch (fileExistsAndNotEmpty()) {
                    case FILE_FILLED:
                        return FILE_FILLED;
                    case FILE_EMPTY:
                        return FILE_EMPTY;
                    case FILE_HEADER:
                        return FILE_HEADER;
                    case NO_FILE:
                        return newFile();
                    default:
                        return ACCESS_DENIED;
                }
            case FOLDER_EMPTY:
                return newFile();
            case NO_FOLDER:
                if (newFolder() != ACCESS_DENIED)
                    return newFile();
            default:
                return ACCESS_DENIED;
        }
    }

    private int fileExistsAndNotEmpty() {
        try {
            if (new File(filePath + FILE_NAME).exists()) {
                BufferedReader br = new BufferedReader(new FileReader(filePath + FILE_NAME));
                String line;
                if ((line = br.readLine()) != null)
                    if (line.startsWith(XML_HEADER)) {
                        if (br.readLine() != null) {
                            br.close();
                            return FILE_FILLED;
                        }
                        br.close();
                        return FILE_HEADER;
                    }
                br.close();
                return FILE_EMPTY;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return NO_FILE;
    }

    private int folderExistsAndNotEmpty() {
        File folder = new File(folderPath + FOLDER_NAME);

        if (folder.exists()) {
            File[] children = folder.listFiles();
            if (children.length > 0)
                return FOLDER_FILLED;
            return FOLDER_EMPTY;
        }

        return NO_FOLDER;
    }

    private int newFolder() {
        if (new File(folderPath + FOLDER_NAME).mkdir())
            return FOLDER_EMPTY;
        return ACCESS_DENIED;
    }

    private int newFile() {
        try {
            if (new File(filePath + FILE_NAME).createNewFile())
                return FILE_EMPTY;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ACCESS_DENIED;
    }
}