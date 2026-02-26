package eu.europeana.research.solr;

import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.Passage;

import java.util.Arrays;

public class OffsetFormatter extends DefaultPassageFormatter {
    private final String snippet_separator;

    public OffsetFormatter(String preTag, String postTag,String ellipsis, boolean escape, String snippet_separator){
        super(preTag,postTag,ellipsis,escape);
        this.snippet_separator = snippet_separator;
    }


    @Override
    public String format(Passage[] passages, String content) {

        StringBuilder sb = new StringBuilder();
        //Object originalFormat = originalFormatter.format(passages,content);
        Object originalFormat = super.format(passages,content);
        sb.append(originalFormat);
        sb.append(snippet_separator);
        for (Passage p: passages){
            sb.append(p.getStartOffset());
            sb.append(snippet_separator);
            sb.append(Arrays.toString(p.getMatchStarts()));
            sb.append(snippet_separator);
            sb.append(Arrays.toString(p.getMatchEnds()));
            sb.append(snippet_separator);
        }
        return sb.toString();
    }

}
