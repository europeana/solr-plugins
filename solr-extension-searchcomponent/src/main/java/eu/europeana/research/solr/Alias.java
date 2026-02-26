package eu.europeana.research.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.xerces.dom.ElementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Alias {
    private static final Logger log = LoggerFactory.getLogger(Alias.class);

    private HashMap<String, HashMap<String, String>> aliasMap = null;

    public static Alias load(SolrResourceLoader loader, String aliasFile) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        try (InputStream is = loader.openResource(aliasFile)) {
            byte[] bytes = is.readAllBytes();
            return new Alias(bytes);
        }
    }

    private Alias(byte[] bytes) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        this.aliasMap = populateAliases(bytes);
    }


    public HashMap<String, HashMap<String, String>> getAliasMap() {
        return aliasMap;
    }

    private static HashMap<String, HashMap<String, String>> populateAliases(byte[] bytes) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        HashMap<String, HashMap<String, String>> allAliases = new HashMap<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));

        //NodeList aliasFields = (NodeList) XPathFactory.newInstance().newXPath().evaluate("alias-config", doc, XPathConstants.NODESET);
        NodeList aliasFields = doc.getElementsByTagName("alias-config");
        //System.out.println("Processing alias");
        for (int i = 0; i < aliasFields.getLength(); i++) {
            ElementImpl pseudofieldNode = (ElementImpl) aliasFields.item(i);
            String fieldName = pseudofieldNode.getElementsByTagName("alias-pseudofield").item(0).getTextContent();
            NodeList configs = pseudofieldNode.getElementsByTagName("alias-def");

            HashMap<String, String> aliasMap = new HashMap<>();
            for (int j = 0; j < configs.getLength(); j++) {
                ElementImpl configNode = (ElementImpl) configs.item(j);
                String alias = configNode.getElementsByTagName("alias").item(0).getTextContent();
                String query = configNode.getElementsByTagName("query").item(0).getTextContent();
                aliasMap.put(alias, query);
            }
            allAliases.put(fieldName, aliasMap);
        }

        return allAliases;
    }

    public String modifyValues(String query) {
        String queryRewritten = query;
        if (query.contains(":")) {
            for (String psField : getAliasMap().keySet()) {
                if (query.contains(psField + ":")) {
                    Pattern p = Pattern.compile("\\b" + psField + ":([\\w]+)\\b");
                    Matcher m = p.matcher(query);
                    m.reset();
                    if (!m.find()) {
                        String[] fieldBits = query.split(psField + ":");
                        String illegalField = "[Empty Field]";
                        if (fieldBits.length > 1) {
                            illegalField = fieldBits[1];
                            String[] illegalFieldBits = illegalField.split("\\s");
                            illegalField = illegalFieldBits[0];
                        }
                        String msg = "Collection \"" + illegalField + "\" is not well-formed; "
                                + "aliases may contain only alphanumberic characters and the \"_\" character.";
                        log.error(msg); //the request fail
                        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, msg);
                    }
                    m.reset();
                    while (m.find()) {
                        String all = query.substring(m.start(), m.end());
                        String[] bits = all.split(":");
                        String collectionName = bits[1];
                        HashMap<String, String> themeAliases = getAliasMap().get(psField);
                        if (themeAliases.containsKey(collectionName)) {
                            String fullQuery = themeAliases.get(collectionName);
                            queryRewritten = queryRewritten.replaceAll("\\b" + psField + ":" + collectionName + "\\b", "("+ fullQuery +")");
                        } else {
                            String msg = "Collection \"" + collectionName + "\" not defined";
                            log.error(msg); //the request fail
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, msg);
                        }
                    }
                }
            }
        }
        return queryRewritten;
    }

}
