# Introduction
Extension of the class org.apache.solr.highlight.UnifiedSolrHighlighter to allow the Unified highlighter to return the offsets of the matched terms.
Related to ticket https://issues.apache.org/jira/browse/SOLR-1954

# Requirements
- Solr Cloud v9.10.1

- Lucene v9.12
# Installation
- Add the JAR file to the Solr _lib_ folder

- Replace the highlight component by the extension in solrconfig.xml:
```
    <searchComponent name="highlight" class="solr.HighlightComponent">
        <highlighting class="eu.europeana.research.solr.OffsetAwareUnifiedHighlighter"/>
    </searchComponent>
```
Note that we are replacing the highlighting process with this class, so hl.method is ignored and only the Unified highlighter is used.

# Use
Activate the custom parameter _hl.offsets_:
```
hl.offsets = true
```
The offsets of the matching terms will be displayed for fields indexed with:
termOffsets, termPositions and termVectors.

It is expected that the rest of the parameters affecting the Unified highlighter can be used normally. We have specifically tested _hl.tag.pre_ and _hl.tag.post_.

# Example

Given these documents:
```
id, name_en
1, "Ender's Game"
2, "The Game of Thrones (vol I)","The Game of Thrones (vol II)","The Game of Thrones (vol III)"}
3  "Game of Game"
4, "The Belgariad Boxed Set: Pawn of Prophecy / Queen of Sorcery / Magician's Gambit / Castle of Wizardry / Enchanters' End Game"
```
and the parameters:
```
hl.offsets = true
hl.snippets = 2
```
The results are the following (indented here to facilitate reading):
```
{1={name_en={
    snippets=Ender's <b>Game</b>, 
    passages=[
        {startOffsetUtf16=0, 
        matchStartsUtf16=[8, 0, 0, 0, 0, 0, 0, 0], 
        matchEndsUtf16=[12, 0, 0, 0, 0, 0, 0, 0]}
        ]
    }
}, 
3={name_en={
    snippets=<b>Game</b> of <b>Game</b>, 
    passages=[
        {startOffsetUtf16=0, 
        matchStartsUtf16=[0, 8, 0, 0, 0, 0, 0, 0], 
        matchEndsUtf16=[4, 12, 0, 0, 0, 0, 0, 0]}
        ]
    }
}, 
2={name_en={
    snippets=The <b>Game</b> of Thrones (vol I)... The <b>Game</b> of Thrones (vol II), 
    passages=[
        {startOffsetUtf16=0, 
        matchStartsUtf16=[4, 0, 0, 0, 0, 0, 0, 0], 
        matchEndsUtf16=[8, 0, 0, 0, 0, 0, 0, 0]}, 
        {startOffsetUtf16=28, 
        matchStartsUtf16=[32, 0, 0, 0, 0, 0, 0, 0], 
        matchEndsUtf16=[36, 0, 0, 0, 0, 0, 0, 0]}
        ]
    }
}, 
4={name_en={
    snippets=The Belgariad Boxed Set: Pawn of Prophecy / Queen of Sorcery / Magician's Gambit / Castle of Wizardry / Enchanters' End <b>Game</b>, 
    passages=[{
        startOffsetUtf16=0, 
        matchStartsUtf16=[120, 0, 0, 0, 0, 0, 0, 0], 
        matchEndsUtf16=[124, 0, 0, 0, 0, 0, 0, 0]}
        ]
    }
}}
```