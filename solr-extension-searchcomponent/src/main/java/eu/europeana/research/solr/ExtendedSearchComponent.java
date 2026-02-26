package eu.europeana.research.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;

public class ExtendedSearchComponent extends SearchComponent implements SolrCoreAware {


    private static final Logger log =
            LoggerFactory.getLogger(ExtendedSearchComponent.class);

    private static volatile Alias alias = null;
    private String aliasFile;

    @Override
    public void init(org.apache.solr.common.util.NamedList<?> args) {
        // We need an attribute in the SearchComponent in the configuration indicating
        // the name of the file required for the thematic collection alias
        alias = null;
        aliasFile = (String) args.get("queryAliasFile");
        if (aliasFile == null) {
            String msg = "Required attribute 'queryAliasFile' with the name of the thematic collections queries file not found in the Search component configuration";
            log.error(msg);
            throw new SolrException(SolrException.ErrorCode.NOT_FOUND, msg);
        }
    }

    //It should load the configuration from Zookeeper if it is there, if not, it falls back to the conf directory in each core
    @Override
    public void inform(SolrCore solrCore) {
        // Load rules when core is reloaded and ready
        if (alias == null) {
            synchronized (ExtendedSearchComponent.class) {
                if (alias == null) {
                    try {
                        alias = Alias.load(
                                solrCore.getResourceLoader(),
                                aliasFile
                        );
                        log.info("Query aliases loaded");
                    } catch (XPathExpressionException | IOException | ParserConfigurationException | SAXException e) {
                        String msg = "Failed to load query aliases: " + aliasFile;
                        log.error(msg);
                        //throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, msg,new Exception(msg,e));
                        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, msg);
                    }
                }
            }
        }
    }


    @Override
    public void prepare(ResponseBuilder rb) {
        SolrParams params = rb.req.getParams();
        ModifiableSolrParams newParams = new ModifiableSolrParams(params);

        // Rewrite q
        String q = params.get("q");
        if (q != null) {
            newParams.set("q", alias.modifyValues(q));
        }

        // Rewrite fq (can be multiple)
        String[] fqs = params.getParams("fq");
        if (fqs != null) {
            List<String> rewritten = new ArrayList<>();
            for (String fq : fqs) {
                rewritten.add(alias.modifyValues(fq));
            }
            newParams.set("fq", rewritten.toArray(new String[0]));
        }

        rb.req.setParams(newParams);
    }

    @Override
    public void process(ResponseBuilder responseBuilder) throws IOException {
        //Nothing to do here
    }


    @Override
    public String getDescription() {
        return "SearchComponent that rewrites queries in parameters q and fq based on query aliases contained the file " + aliasFile + " - to be applied as first-components";
    }

}




