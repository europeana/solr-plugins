# Introduction
A custom search component that processes and replaces predefined tags in the Solr _q_ and _fq_ parameters with their mapped query expressions.
Derived from an earlier custom search handler developed at Europeana and updated for compatibility with Solr 9.10.1.

# Requirements
- Solr Cloud v9.10.1
- Lucene v9.12
# Installation
- Add the JAR file to the Solr _lib_ folder
- Add the component to solrconfig.xml, setting the parameter _queryAliasFile_ to the name of the alias definition file.
This file must be available in the configuration directory of each Solr core.
```
    <searchComponent name="aliasing" class="eu.europeana.research.solr.ExtendedSearchComponent">
        <str name="queryAliasFile">query_aliases.xml</str>
    </searchComponent>
```
- Include the new component in the list of first-components of your handler in solrconfig.xml:
```
    <requestHandler name="/select" class="solr.SearchHandler">
        <lst name="defaults">
            ...
        </lst>
        <arr name="first-components">
            <str>aliasing</str>
        </arr>
    </requestHandler>
```
# Use
Define the alias file using the following template:
```
<alias-configs>
    <alias-config>
        <alias-pseudofield>name_of_group</alias-pseudofield> <!-- name of the group -->
        <alias-defs>
            <alias-def>
                <alias>name_of_alias</alias> <!-- name of the alias -->
                <query>query_to_replace_alias</query> 
            </alias-def>
        </alias-defs>
    </alias-config>
</alias-configs>
```
Use the combination _name_of_group:name_of_alias_ anywhere in the _q_ and/or _fq_ parameters.
This combination will be replaced by the corresponding query expression defined in _\<query>_ before the query is processed by Solr.

The replacement query expression is automatically wrapped in parentheses to ensure it can be safely combined with other expressions in the same parameter.

You can define multiple aliases within a group, as well as multiple alias groups.

The alias file is loaded with the core. 
Any subsequent changes to the file will only take effect after reloading the collection. 
In SolrCloud the file should be found in the configuration of the collection in Zookeeper, 
if not there, it falls back to the conf directory in each core.
# Example

Given this alias file:
```
<alias-configs>
    <alias-config>
        <alias-pseudofield>collection</alias-pseudofield> <!-- name of the group -->
        <alias-defs>
            <alias-def>
                <alias>art</alias> <!-- name of the alias -->
                <query>art OR "Pop Art" OR "art nouveau" OR "art history"</query> 
            </alias-def>
        </alias-defs>
    </alias-config>
</alias-configs>
```
And given this query:
```
q = collection:art
```
It will be replaced by: 
```
q = (art OR "Pop Art" OR "art nouveau" OR "art history")
```
