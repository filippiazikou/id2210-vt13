package search.system.peer.search;

import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import search.simulator.snapshot.Snapshot;
import common.configuration.SearchConfiguration;
import common.peer.PeerAddress;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;
import search.system.peer.AddIndexText;
import search.system.peer.IndexPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 * @author jdowling
 */
public final class Search extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(Search.class);
    Positive<IndexPort> indexPort = positive(IndexPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<Web> webPort = negative(Web.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanSamplePort = positive(TManSamplePort.class);

    ArrayList<PeerAddress> neighbours = new ArrayList<PeerAddress>();
    Random randomGenerator = new Random();
    ArrayList<Integer> indexStore = new ArrayList<Integer>();
    private PeerAddress self;
    private long period;
    private double num;
    private SearchConfiguration searchConfiguration;
    // Apache Lucene used for searching
    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
    Directory index = new RAMDirectory();
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);

    private int latestMissingIndexValue =0;
    private int garbageIndex = -1;

//-------------------------------------------------------------------	
    public Search() {

        subscribe(handleInit, control);
        subscribe(handleUpdateIndex, timerPort);
        subscribe(handleWebRequest, webPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleTManSample, tmanSamplePort);
        subscribe(handleAddIndexText, indexPort);
        subscribe(getUpdatesRequestHandler, networkPort);
        subscribe(getUpdatesResponseHandler, networkPort);
    }
//-------------------------------------------------------------------	
    Handler<SearchInit> handleInit = new Handler<SearchInit>() {
        public void handle(SearchInit init) {
            self = init.getSelf();
            num = init.getNum();
            searchConfiguration = init.getConfiguration();
            period = searchConfiguration.getPeriod();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new UpdateIndexTimeout(rst));
            trigger(rst, timerPort);

            Snapshot.updateNum(self, num);
            try {
                String title = "The Art of Computer Science";
                int id = 100;
                String magnet = "a896f7155237fb27e2eaa06033b5796d7ae84a1d";
                addEntry(title,id, magnet);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            }
        }
    };
//-------------------------------------------------------------------	
    Handler<UpdateIndexTimeout> handleUpdateIndex = new Handler<UpdateIndexTimeout>() {
        public void handle(UpdateIndexTimeout event) {
            if(neighbours.size() == 0)
                return;

            int rand = randomGenerator.nextInt(neighbours.size());
            PeerAddress selectedPeer = neighbours.get(rand);

            ArrayList<Range> missingValues = new ArrayList<Range>();

            int i=0;
            int lastExisting = -1;


            //Get the missing range less than the first value in the index - check first if peer has index first
            if (indexStore.size() != 0 && !indexStore.get(i).equals(0)) {
                lastExisting =  indexStore.get(indexStore.size()-1);
                Range range = new Range(0,indexStore.get(0)-1);
                missingValues.add(range);
            }
            /*Get all the rest missing ranges till the max value*/
            while (i<indexStore.size()-1) {
                if(indexStore.get(i).equals(indexStore.get(i+1)-1)) {
                    i++;
                    continue;
                }
                Range range = new Range(indexStore.get(i)+1, indexStore.get(i+1)-1);
                missingValues.add(range);
                i++;
            }

            logger.info(String.format("===========Missing ranges on %s ============", self.getPeerAddress().getId()));
            for(Range range : missingValues){
                logger.info(String.format("%s - Range [%s, %s]", self.getPeerAddress().getId(), range.getLeft(), range.getRight()));
            }

            logger.info(String.format("%s - Last: %s", self.getPeerAddress().getId(), lastExisting));
            logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++");

            trigger(new GetUpdatesRequest(missingValues, lastExisting, self, selectedPeer), networkPort);
        }
    };

    Handler<GetUpdatesRequest> getUpdatesRequestHandler = new Handler<GetUpdatesRequest>() {
        @Override
        public void handle(GetUpdatesRequest getUpdatesRequest) {
            ArrayList<Range> ranges = getUpdatesRequest.getMissingRanges();
            int lastExisting =  getUpdatesRequest.getLastExisting();

            ArrayList<BasicTorrentData> missingData = new ArrayList<BasicTorrentData>();

            //Get from missing ranges
            for(Range range : ranges) {
                try {
                    missingData.addAll(retrieveRecordFromIndexRange(range.getLeft(), range.getRight()));
                } catch (ParseException e) {
                    continue;
                } catch (IOException e) {
                    continue;
                }
            }
            //if sender peer has no index
            if (lastExisting == -1) {
                try {
                    missingData.addAll(retrieveRecordFromIndexRange(0, indexStore.get(indexStore.size()-1)));
                } catch (ParseException e) {

                } catch (IOException e) {

                }
            }
            //Get the last missing values
            else if (lastExisting < indexStore.get(indexStore.size()-1)) {
                try {
                    missingData.addAll(retrieveRecordFromIndexRange(lastExisting+1, indexStore.get(indexStore.size()-1)));
                } catch (ParseException e) {

                } catch (IOException e) {

                }
            }
            if(missingData.size() == 0) return;
            trigger(new GetUpdatesResponse(self, getUpdatesRequest.getPeerSource(), missingData), networkPort);
        }
    };

    Handler<GetUpdatesResponse> getUpdatesResponseHandler =new Handler<GetUpdatesResponse>() {
        @Override
        public void handle(GetUpdatesResponse getUpdatesResponse) {
            ArrayList<BasicTorrentData> basicTorrentData = getUpdatesResponse.getTorrentData();

            for(BasicTorrentData data : basicTorrentData) {
                if(!indexStore.contains(data.getId())) {
                    try {
                        addEntry(data.getTitle(), data.getId(), data.getMagnet());
                    } catch (IOException e) {
                        indexStore.remove(data.getId());
                        continue;
                    }
                }
            }

            logger.info("=========== Index on "+ self.getPeerAddress().getId()+" ===================");
            for (Integer val : indexStore) {
                logger.info(String.format("%s %s", self.getPeerAddress().getId(), String.valueOf(val)));
            }
            logger.info("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

            garbageCollection();
        }
    };


    private ArrayList<BasicTorrentData> retrieveRecordFromIndexRange(int from, int to) throws ParseException, IOException {
        // the "title" arg specifies the default field to use when no field is explicitly specified in the query.
        IndexSearcher searcher = null;
        IndexReader reader;




       // Query q = new QueryParser(Version.LUCENE_42, "id", analyzer).parse("["+from+" TO "+to+"]");


        try {
            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        Query q = NumericRangeQuery.newIntRange("id", from, to, true, true);
        //TopDocs topDocs = searcher.search(query, 10);

        int hitsPerPage = 10;
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);

        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        ArrayList<BasicTorrentData> results = new ArrayList<BasicTorrentData>();


        //System.out.println("Number of hits: "+hits.length);
        int i = 0;
        int docId;
        Document d;
        while ( i < hits.length && i < 10 ) {
            docId = hits[i].doc;
            d = searcher.doc(docId);
            results.add(new BasicTorrentData(Integer.parseInt(d.get("id")), d.get("title"), d.get("magnet")));
            i++;
        }
       // System.out.println("Results returned: "+results.size()+" for range "+from+" to "+to+" : ");
//        for (BasicTorrentData res : results)
//            System.out.println(res.getId()+" "+res.getTitle());
        return results;
    }

    private BasicTorrentData retrieveRecordFromIndex(int value) throws ParseException, IOException {
        // the "title" arg specifies the default field to use when no field is explicitly specified in the query.
        //Query q = new QueryParser(Version.LUCENE_42, "id", analyzer).parse(value);
        Query q = NumericRangeQuery.newIntRange("id", value, value, true, true);
        IndexSearcher searcher = null;
        IndexReader reader;
        try {
            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        int hitsPerPage = 1;
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);

        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        if(hits.length == 0) return null;

        int docId = hits[0].doc;
        Document d = searcher.doc(docId);
        return new BasicTorrentData(Integer.parseInt(d.get("id")), d.get("title"), d.get("magnet"));
    }

    private boolean isInRange(int value, Range range) {
        return range.getLeft() <= value && value <= range.getRight();
    }

    private void garbageCollection() {
        while (indexStore.contains(garbageIndex+1)) {
            indexStore.remove(garbageIndex+1);
            garbageIndex += 1;
        }
    }

    Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
        public void handle(WebRequest event) {
            if (event.getDestination() != self.getPeerAddress().getId()) {
                return;
            }

            String[] args = event.getTarget().split("-");

            logger.debug("Handling Webpage Request");
            WebResponse response;
            if (args[0].compareToIgnoreCase("search") == 0) {
                response = new WebResponse(searchPageHtml(args[1]), event, 1, 1);
            } else if (args[0].compareToIgnoreCase("add") == 0) {
                response = new WebResponse(addEntryHtml(args[1], Integer.getInteger(args[2]) , args[3]), event, 1, 1);
            } else {
                response = new WebResponse(searchPageHtml(event
                        .getTarget()), event, 1, 1);
            }
            trigger(response, webPort);
        }
    };

    private String searchPageHtml(String title) {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C");
        sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
        sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
        sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
        sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
        sb.append("<title>Kompics P2P Bootstrap Server</title>");
        sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
        sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
        sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
        sb.append("ID2210 (Decentralized Search for Piratebay)</h2><br>");
        try {
            query(sb, title);
        } catch (ParseException ex) {
            java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            sb.append(ex.getMessage());
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            sb.append(ex.getMessage());
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String addEntryHtml(String title, int id, String magnet) {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C");
        sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
        sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
        sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
        sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
        sb.append("<title>Adding an Entry</title>");
        sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
        sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
        sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
        sb.append("ID2210 Uploaded Entry</h2><br>");
        try {
            addEntry(title, id, magnet);
            sb.append("Entry: ").append(title).append(" - ").append(id).append(" - ").append(magnet);
        } catch (IOException ex) {
            sb.append(ex.getMessage());
            java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private void addEntry(String title, int id, String magnet) throws IOException {
        IndexWriter w = new IndexWriter(index, config);
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("magnet", magnet, Field.Store.YES));
        // You may need to make the StringField searchable by NumericRangeQuery. See:
        // http://stackoverflow.com/questions/13958431/lucene-4-0-indexwriter-updatedocument-for-numeric-term
        // http://lucene.apache.org/core/4_2_0/core/org/apache/lucene/document/IntField.html
        doc.add(new IntField("id", id, Field.Store.YES));
        w.addDocument(doc);
        w.close();

        logger.info(String.format("%s Added %s", self.getPeerAddress().getId(), id));
        indexStore.add(id);
        Collections.sort(indexStore);
//
//
//        if (idVal == latestMissingIndexValue + 1) {
//            latestMissingIndexValue++;
//        }
    }

    private String query(StringBuilder sb, String querystr) throws ParseException, IOException {

        // the "title" arg specifies the default field to use when no field is explicitly specified in the query.
        Query q = new QueryParser(Version.LUCENE_42, "title", analyzer).parse(querystr);
        IndexSearcher searcher = null;
        IndexReader reader = null;
        try {
            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        int hitsPerPage = 10;
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);

        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        // display results
        sb.append("Found ").append(hits.length).append(" entries.<ul>");
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            sb.append("<li>").append(i + 1).append(". ").append(d.get("id")).append("\t").append(d.get("title")).append("\t").append(d.get("magnet")).append("</li>");
        }
        sb.append("</ul>");

        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
        return sb.toString();
    }
    
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            // receive a new list of neighbours
            neighbours = event.getSample();
            // Pick a node or more, and exchange index with them
        }
    };
    
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            // receive a new list of neighbours
            ArrayList<PeerAddress> sampleNodes = event.getSample();
            // Pick a node or more, and exchange index with them
        }
    };
    
//-------------------------------------------------------------------	
    Handler<AddIndexText> handleAddIndexText = new Handler<AddIndexText>() {
        @Override
        public void handle(AddIndexText event) {
            Random r = new Random(System.currentTimeMillis());
            int id = r.nextInt(100000);

            /*Generate random magnet link*/
            String magnet = new BigInteger(130, r).toString(32);


            logger.info(self.getPeerAddress().getId()
                    + " - adding index entry: {}-{}-"+magnet, event.getText(), id);
            try {
                addEntry(event.getText(), id, magnet);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalArgumentException(ex.getMessage());
            }
        }
    };
    
}
