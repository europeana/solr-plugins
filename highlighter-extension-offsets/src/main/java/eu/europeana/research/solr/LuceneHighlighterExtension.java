package eu.europeana.research.solr;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.uhighlight.PassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;

import java.io.IOException;
import java.util.Map;

public class LuceneHighlighterExtension extends UnifiedHighlighter {
    private final String preTag;
    private final String postTag;
    private final String ellipsis;
    private final String snippet_separator;
    boolean escape;

    public LuceneHighlighterExtension(Builder builder, String preTag, String postTag, String ellipsis, boolean escape, String snippet_separator) {
        super(builder);
        this.preTag = preTag;
        this.postTag = postTag;
        this.ellipsis = ellipsis;
        this.escape = escape;
        this.snippet_separator = snippet_separator;
    }

    @Override
    public PassageFormatter getFormatter(String field){
        return new OffsetFormatter(preTag,postTag, ellipsis, escape, snippet_separator);
    }


}
