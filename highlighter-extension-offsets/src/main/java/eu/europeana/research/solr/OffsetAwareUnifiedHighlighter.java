package eu.europeana.research.solr;

import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.highlight.UnifiedSolrHighlighter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;

public class OffsetAwareUnifiedHighlighter extends UnifiedSolrHighlighter {
    final static Logger logger = LoggerFactory.getLogger(OffsetAwareUnifiedHighlighter.class);

    private boolean offsetsEnabled(SolrQueryRequest req) {
        return req != null && req.getParams().getBool("hl.offsets", false);
    }

    //Using the default value in org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
    private String getPreTag(SolrQueryRequest req){
        if (req!=null){
            return req.getParams().get("hl.tag.pre","<b>");
        }
        return "<b>";
    }

    //Using the default value in org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
    private String getPostTag(SolrQueryRequest req){
        if (req!=null){
            return req.getParams().get("hl.tag.post","</b>");
        }
        return "</b>";
    }

    //Using the default value in org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
    //The Solr documentation indicates this parameter is likely to be removed in the future
    private String getEllipsis(SolrQueryRequest req){
        if (req!=null){
            return req.getParams().get("hl.tag.ellipsis","... ");
        }
        return "... ";
    }

    //Using the default value in org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
    private boolean escapeHTML(SolrQueryRequest req) {
        return req != null && req.getParams().get("hl.encoder", "").equals("html");
    }



    @Override
    protected NamedList<Object> encodeSnippets(String[] keys, String[] fieldNames, Map<String, String[]> snippets) {
        NamedList<Object> list = new SimpleOrderedMap<>();

        for(int i = 0; i < keys.length; ++i) {
            NamedList<Object> summary = new SimpleOrderedMap<>();

            for(String field : fieldNames) {
                String snippet = ((String[])snippets.get(field))[i];
                if (snippet != null) {
                    String[] sparts = snippet.split(SNIPPET_SEPARATOR);
                    if (sparts.length >= 4) {
                        summary.add(field,getEntries(sparts)); //Custom code
                    } else {
                        summary.add(field, sparts);
                    }
                }
            }
            list.add(keys[i], summary);
        }
        return list;
    }

    private static NamedList<Object> getEntries(String[] sparts) {
        NamedList<Object> summary_parts = new NamedList<>();
        summary_parts.add("snippets",sparts[0]);
        List<NamedList> passages_parts = new ArrayList<>();
        for (int i=1; i< (sparts.length - 2); i+=3) {
            NamedList<Object> ppart = new NamedList<>();
            ppart.add("startOffsetUtf16", sparts[i]);
            ppart.add("matchStartsUtf16", sparts[i+1]);
            ppart.add("matchEndsUtf16", sparts[i+2]);
            passages_parts.add(ppart);
        }
        summary_parts.add("passages",passages_parts);
        return summary_parts;
    }


    @Override
    public NamedList<Object> doHighlighting(DocList docs, org.apache.lucene.search.Query query, SolrQueryRequest req, String[] defaultFields) throws IOException {
        if (offsetsEnabled(req)) {
            int[] docIDs = this.toDocIDs(docs);
            String[] keys = this.getUniqueKeys(req.getSearcher(), docIDs);
            String[] fieldNames = this.getHighlightFields(query, req, defaultFields);
            int[] maxPassages = new int[fieldNames.length];
            for(int i = 0; i < fieldNames.length; ++i) {
                maxPassages[i] = req.getParams().getFieldInt(fieldNames[i], "hl.snippets", 1);
            }

            //Custom code
            UnifiedHighlighter.Builder builder = new UnifiedHighlighter.Builder(super.getHighlighter(req).getIndexSearcher(), super.getHighlighter(req).getIndexAnalyzer());
            LuceneHighlighterExtension luceneHighlighterExtension = new LuceneHighlighterExtension(builder, getPreTag(req), getPostTag(req), getEllipsis(req), escapeHTML(req), SNIPPET_SEPARATOR);

            Map<String, String[]> snippets = fieldNames.length == 0 ? Collections.emptyMap() : luceneHighlighterExtension.highlightFields(fieldNames, query, docIDs, maxPassages);
            return this.encodeSnippets(keys, fieldNames, snippets);
        } else {
            return super.doHighlighting(docs, query, req, defaultFields);
        }
    }

}


