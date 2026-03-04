import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.response.SimpleSolrResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.embedded.JettyConfig;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Path;

public class HighlighterExtensionTest extends SolrTestCaseJ4 {
    private static MiniSolrCloudCluster cluster;
    private static CloudSolrClient solrClient;

    private static final String COLLECTION = "test_collection";
    private static final String HANDLER = "/select";
    private static final String CONFIG_PATH = "src/test/resources/solr/configsets/myconfig";
    private static final String CONFIG_NAME = "myconfig";
    private static final int NUM_NODES = 1;
    private static final int NUM_SHARDS = 3;
    private static final int NUM_REPLICAS = 1;


    @BeforeClass
    public static void setupCluster() throws Exception {
        Path baseDir = createTempDir();

        JettyConfig jettyConfig = JettyConfig.builder()
                .setPort(0)          // random free port
                .build();

        cluster = new MiniSolrCloudCluster(
                NUM_NODES,
                baseDir,
                MiniSolrCloudCluster.DEFAULT_CLOUD_SOLR_XML,
                jettyConfig
        );

        // Upload configset
        cluster.uploadConfigSet(
                Path.of(CONFIG_PATH),
                CONFIG_NAME
        );

        // Create collection
        CollectionAdminRequest.createCollection(
                COLLECTION,
                CONFIG_NAME,
                NUM_SHARDS,   // shards
                NUM_REPLICAS    // replicas
        ).process(cluster.getSolrClient());

        cluster.waitForActiveCollection(
                COLLECTION,
                NUM_SHARDS,
                NUM_SHARDS * NUM_REPLICAS
        );

        solrClient = cluster.getSolrClient();
        populateCollection();
    }

    @AfterClass
    public static void removeCluster() throws Exception {
        if (solrClient != null) {
            solrClient.close();
        }
        if (cluster != null) {
            cluster.shutdown();
        }
    }


    private static void addMyDoc(String id, String[] fieldValueEnglish, String[] fieldValueSpanish) throws SolrServerException, IOException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("name_en", fieldValueEnglish);
        doc.addField("name_es", fieldValueSpanish);
        solrClient.add(COLLECTION,doc);
    }

    private static void populateCollection() throws SolrServerException, IOException {
        addMyDoc("1", new String[] {"Ender's Game"}, new String[] {"Los juegos de Ender"});
        addMyDoc("2",  new String[] {"The Game of Thrones (vol I)","The Game of Thrones (vol II)","The Game of Thrones (vol III)"}, new String[] {"Juego de Tronos (vol I)","Juego de Tronos (vol II)","Juego de Tronos (vol III)"});
        addMyDoc("3",  new String[] {"Game of Game"}, new String[] {"Juego de juego"});
        addMyDoc("4",  new String[] {"The Belgariad Boxed Set: Pawn of Prophecy / Queen of Sorcery / Magician's Gambit / Castle of Wizardry / Enchanters' End Game"},new String[] {"Cronicas de Belgarath: La Senda de la Profecía / La Reina de la Hechicería / La Luz del Orba / El Castillo de la Magia / La Ciudad de las Tinieblas"});
        solrClient.commit(COLLECTION);
    }

    //region Highlighting with a without offsets
    @Test
    public void predefinedHighlighting() throws Exception {
        SolrParams params = new ModifiableSolrParams()
                .set("q", "name_en:Game")
                .set("fl", "id")
                .set("hl", "true")
                .set("hl.fl", "name_en")
                .set("hl.snippets", "2")
                .set("hl.offsets", "false");
        //The line below throws exception because the NamedList<Object> in the server side clashes with the type SolrJ expects in the hightlighting section
        //rsp = solrClient.query(COLLECTION,params);
        GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.POST, HANDLER, params);
        SimpleSolrResponse response = request.process(solrClient, COLLECTION);
        assertEquals("{1={name_en=[Ender's <em>Game</em>]}, 3={name_en=[<em>Game</em> of <em>Game</em>]}, 2={name_en=[The <em>Game</em> of Thrones (vol I), The <em>Game</em> of Thrones (vol II)]}, 4={name_en=[The Belgariad Boxed Set: Pawn of Prophecy / Queen of Sorcery / Magician's Gambit / Castle of Wizardry / Enchanters' End <em>Game</em>]}}",response._get("highlighting").toString());
    }

    @Test
    public void highlightWithOffsets() throws Exception {
        SolrParams params = new ModifiableSolrParams()
                .set("q", "name_en:Game")
                .set("fl", "id")
                .set("hl", "true")
                .set("hl.fl", "name_en")
                .set("hl.snippets", "2")
                .set("hl.offsets", "true");

        //The line below throws exception because the NamedList<Object> in the server side clashes with the type SolrJ expects in the hightlighting section
        //rsp = solrClient.query(COLLECTION,params);
        GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.POST, HANDLER, params);
        SimpleSolrResponse response = request.process(solrClient, COLLECTION);
        assertEquals("{1={name_en={snippets=Ender's <b>Game</b>, passages=[{startOffsetUtf16=0, matchStartsUtf16=[8, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[12, 0, 0, 0, 0, 0, 0, 0]}]}}, 3={name_en={snippets=<b>Game</b> of <b>Game</b>, passages=[{startOffsetUtf16=0, matchStartsUtf16=[0, 8, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[4, 12, 0, 0, 0, 0, 0, 0]}]}}, 2={name_en={snippets=The <b>Game</b> of Thrones (vol I)... The <b>Game</b> of Thrones (vol II), passages=[{startOffsetUtf16=0, matchStartsUtf16=[4, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[8, 0, 0, 0, 0, 0, 0, 0]}, {startOffsetUtf16=28, matchStartsUtf16=[32, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[36, 0, 0, 0, 0, 0, 0, 0]}]}}, 4={name_en={snippets=The Belgariad Boxed Set: Pawn of Prophecy / Queen of Sorcery / Magician's Gambit / Castle of Wizardry / Enchanters' End <b>Game</b>, passages=[{startOffsetUtf16=0, matchStartsUtf16=[120, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[124, 0, 0, 0, 0, 0, 0, 0]}]}}}", response._get("highlighting").toString());
    }
    //endregion

    //region Highlighting with a without offsets with custom tags
    @Test
    public void predefinedHighlighting_changeTags() throws Exception {
        SolrParams params = new ModifiableSolrParams()
                .set("q", "name_en:Game")
                .set("fl", "id")
                .set("hl", "true")
                .set("hl.fl", "name_en")
                .set("hl.snippets", "2")
                .set("hl.offsets", "false")
                .set("hl.tag.pre", "<mytag>")
                .set("hl.tag.post", "</mytag>");
        //The line below throws exception because the NamedList<Object> in the server side clashes with the type SolrJ expects in the hightlighting section
        //rsp = solrClient.query(COLLECTION,params);
        GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.POST, HANDLER, params);
        SimpleSolrResponse response = request.process(solrClient, COLLECTION);
        assertEquals("{1={name_en=[Ender's <mytag>Game</mytag>]}, 3={name_en=[<mytag>Game</mytag> of <mytag>Game</mytag>]}, 2={name_en=[The <mytag>Game</mytag> of Thrones (vol I), The <mytag>Game</mytag> of Thrones (vol II)]}, 4={name_en=[The Belgariad Boxed Set: Pawn of Prophecy / Queen of Sorcery / Magician's Gambit / Castle of Wizardry / Enchanters' End <mytag>Game</mytag>]}}", response._get("highlighting").toString());
    }

    @Test
    public void highlightWithOffsets_changeTags() throws Exception {
        SolrParams params = new ModifiableSolrParams()
                .set("q", "name_en:Game")
                .set("fl", "id")
                .set("hl", "true")
                .set("hl.fl", "name_en")
                .set("hl.snippets", "2")
                .set("hl.offsets", "true")
                .set("hl.tag.pre", "<mytag>")
                .set("hl.tag.post", "</mytag>");
        //The line below throws exception because the NamedList<Object> in the server side clashes with the type SolrJ expects in the hightlighting section
        //rsp = solrClient.query(COLLECTION,params);
        GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.POST, HANDLER, params);
        SimpleSolrResponse response = request.process(solrClient, COLLECTION);
        assertEquals("{1={name_en={snippets=Ender's <mytag>Game</mytag>, passages=[{startOffsetUtf16=0, matchStartsUtf16=[8, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[12, 0, 0, 0, 0, 0, 0, 0]}]}}, 3={name_en={snippets=<mytag>Game</mytag> of <mytag>Game</mytag>, passages=[{startOffsetUtf16=0, matchStartsUtf16=[0, 8, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[4, 12, 0, 0, 0, 0, 0, 0]}]}}, 2={name_en={snippets=The <mytag>Game</mytag> of Thrones (vol I)... The <mytag>Game</mytag> of Thrones (vol II), passages=[{startOffsetUtf16=0, matchStartsUtf16=[4, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[8, 0, 0, 0, 0, 0, 0, 0]}, {startOffsetUtf16=28, matchStartsUtf16=[32, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[36, 0, 0, 0, 0, 0, 0, 0]}]}}, 4={name_en={snippets=The Belgariad Boxed Set: Pawn of Prophecy / Queen of Sorcery / Magician's Gambit / Castle of Wizardry / Enchanters' End <mytag>Game</mytag>, passages=[{startOffsetUtf16=0, matchStartsUtf16=[120, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[124, 0, 0, 0, 0, 0, 0, 0]}]}}}", response._get("highlighting").toString());
    }
    //endregion

    //region Highlighting with a without offsets using virtual fields/wildcars
    @Test
    public void predefinedHighlighting_virtualField() throws Exception {
        SolrParams params = new ModifiableSolrParams()
                .set("q", "Ender") //search in virtual field
                .set("fl", "id")
                .set("hl", "true")
                .set("hl.fl", "name_*") //virtual fields are not allowed here, but in this case it is equivalent
                .set("hl.snippets", "2")
                .set("hl.offsets", "false");
        //The line below throws exception because the NamedList<Object> in the server side clashes with the type SolrJ expects in the hightlighting section
        //rsp = solrClient.query(COLLECTION,params);
        GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.POST, HANDLER, params);
        SimpleSolrResponse response = request.process(solrClient, COLLECTION);
        assertEquals("{1={name_en=[<em>Ender</em>'s Game], name_es=[Los juegos de <em>Ender</em>]}}", response._get("highlighting").toString());
    }

    @Test
    public void highlightWithOffsets_virtualField() throws Exception {
        SolrParams params = new ModifiableSolrParams()
                .set("q", "Ender") //search in virtual field
                .set("fl", "id")
                .set("hl", "true")
                .set("hl.fl", "name_*") //virtual fields are not allowed here, but in this case it is equivalent
                .set("hl.snippets", "2")
                .set("hl.offsets", "true");
        //The line below throws exception because the NamedList<Object> in the server side clashes with the type SolrJ expects in the hightlighting section
        //rsp = solrClient.query(COLLECTION,params);
        GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.POST, HANDLER, params);
        SimpleSolrResponse response = request.process(solrClient, COLLECTION);
        assertEquals("{1={name_en={snippets=<b>Ender</b>'s Game, passages=[{startOffsetUtf16=0, matchStartsUtf16=[0, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[5, 0, 0, 0, 0, 0, 0, 0]}]}, name_es={snippets=Los juegos de <b>Ender</b>, passages=[{startOffsetUtf16=0, matchStartsUtf16=[14, 0, 0, 0, 0, 0, 0, 0], matchEndsUtf16=[19, 0, 0, 0, 0, 0, 0, 0]}]}}}", response._get("highlighting").toString());
    }
    //endregion

}
