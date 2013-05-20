package search.system.peer.search;

import common.configuration.SearchConfiguration;
import common.peer.PeerAddress;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
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
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;
import search.simulator.snapshot.Snapshot;
import search.system.peer.AddIndexText;
import search.system.peer.IndexPort;
import tman.system.peer.tman.AddEntryACK;
import tman.system.peer.tman.AddEntryRequest;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;

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

    //Add Entries through Leader
    private int lastIdWritten = 0;
    private ArrayList<UUID> requestIds = new ArrayList<UUID>();
    private ArrayList<UUID> receivedAcks = new ArrayList<UUID>();
    private HashMap<UUID, WebRequest> pendingWebResp = new HashMap<UUID, WebRequest>();

    //Partitioning
    private int modPartition;
    private int numberOfPartitions;
    private HashMap<Integer, PeerAddress> otherOverlaysMap = new HashMap<Integer, PeerAddress>();
    private  HashMap<Long, ArrayList<String>> searchResults =  new HashMap<Long, ArrayList<String>>();
    private int garbageIndex = -1;

//-------------------------------------------------------------------	
    public Search() {

        subscribe(handleInit, control);
        subscribe(handleUpdateIndex, timerPort);
        subscribe(handleGarbageRequesId, timerPort);
        subscribe(handleAddEntryACKTimer, timerPort);
        subscribe(handleWebRequest, webPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleTManSample, tmanSamplePort);
        subscribe(handleAddIndexText, indexPort);
        subscribe(getUpdatesRequestHandler, networkPort);
        subscribe(getUpdatesResponseHandler, networkPort);
        subscribe(handleAddEntryRequest, tmanSamplePort);
        subscribe(handleAddEntryACK, tmanSamplePort);
        subscribe(handleWebResponseTimeout, timerPort);
        subscribe(handleSearchRequest, networkPort);
        subscribe(handleSearchResults, networkPort);
    }
//-------------------------------------------------------------------	
    Handler<SearchInit> handleInit = new Handler<SearchInit>() {
        public void handle(SearchInit init) {
            self = init.getSelf();

            numberOfPartitions = init.getPartitionAmount();
            modPartition = self.getPeerAddress().getId() % numberOfPartitions;

            num = init.getNum();
            searchConfiguration = init.getConfiguration();
            period = searchConfiguration.getPeriod();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new UpdateIndexTimeout(rst));
            trigger(rst, timerPort);

            // TODO super ugly workaround...
            IndexWriter writer;
            try {
                writer = new IndexWriter(index, config);
                writer.commit();
                writer.close();
            } catch (IOException e) {
            // TODO Auto-generated catch block
                e.printStackTrace();
            }



            Snapshot.updateNum(self, num);
//            String title = "The Art of Computer Science";
//            int id = 100;
//            String magnet = "a896f7155237fb27e2eaa06033b5796d7ae84a1d";
//            //addEntry(title,id, magnet);
//            trigger(new AddEntryRequest(self, self, self, title, magnet, id), tmanSamplePort);
//            //Start Timer for ACK
//            ScheduleTimeout rst2 = new ScheduleTimeout(10000);
//            rst2.setTimeoutEvent(new AddEntryACKTimeout(rst2, id, title, magnet));
//            trigger(rst2, timerPort);

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


            //Get the missing ranges less than the first value in the index - check first if peer has index
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

//            logger.info(String.format("===========Missing ranges on %s ============", self.getPeerAddress().getId()));
//            for(Range range : missingValues){
//                logger.info(String.format("%s - Range [%s, %s]", self.getPeerAddress().getId(), range.getLeft(), range.getRight()));
//            }
//
//            logger.info(String.format("%s - Last: %s", self.getPeerAddress().getId(), lastExisting));
//            logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++");

            trigger(new GetUpdatesRequest(missingValues, lastExisting, self, selectedPeer), networkPort);
        }
    };


    Handler<GetUpdatesRequest> getUpdatesRequestHandler = new Handler<GetUpdatesRequest>() {
        @Override
        public void handle(GetUpdatesRequest getUpdatesRequest) {
            ArrayList<Range> ranges = getUpdatesRequest.getMissingRanges();
            int lastExisting =  getUpdatesRequest.getLastExisting();

            ArrayList<BasicTorrentData> missingData = new ArrayList<BasicTorrentData>();

            //if current peer has no index to reply
            if (indexStore.size() == 0)
                return ;

            //if sender requesting peer has no index
            if (lastExisting == -1 ) {
                ranges.add(new Range(0, indexStore.get(indexStore.size()-1)));
            }
            //Get the last missing values
            else if (lastExisting < indexStore.get(indexStore.size()-1)) {
                ranges.add(new Range(lastExisting+1, indexStore.get(indexStore.size()-1)));
            }

            try {
                missingData = retrieveRecordFromIndexRange(ranges);
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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

//            logger.info("=========== Index on "+ self.getPeerAddress().getId()+" ===================");
//            for (Integer val : indexStore) {
//                logger.info(String.format("%s %s", self.getPeerAddress().getId(), String.valueOf(val)));
//            }
//            logger.info("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

            garbageCollection();
        }
    };


    private ArrayList<BasicTorrentData> retrieveRecordFromIndexRange(ArrayList<Range> ranges) throws ParseException, IOException {
        // the "title" arg specifies the default field to use when no field is explicitly specified in the query.
        IndexSearcher searcher = null;
        IndexReader reader;


        Query q;
        int hitsPerPage = 10;
        TopScoreDocCollector collector;
        ScoreDoc[] hits;
        int j;
        int docId;
        Document d;
        ArrayList<BasicTorrentData> results = new ArrayList<BasicTorrentData>();

        try {
            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        for (int i = 0 ; i<ranges.size() ; i++) {
            q = NumericRangeQuery.newIntRange("id", ranges.get(i).getLeft(), ranges.get(i).getRight(), true, true);
            collector  = TopScoreDocCollector.create(hitsPerPage, true);
            searcher.search(q, collector);
            hits = collector.topDocs().scoreDocs;
            j = 0;
            while ( j < hits.length && j < 10 ) {
                docId = hits[j].doc;
                d = searcher.doc(docId);
                results.add(new BasicTorrentData(Integer.parseInt(d.get("id")), d.get("title"), d.get("magnet")));
                j++;
            }
            if (results.size() >= 10)
                break;
        }

        /*Keep the first 10 items*/
        for (int i=10 ; i<results.size() ; i++)
            results.remove(i);

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
                //Add to searchResults HashMap the eventId
                searchResults.put(event.getId(), new ArrayList<String>());

                //trigger event to myself and other overlays given eventId and args[1]
                trigger(new SearchRequest(self, self, event.getId(), args[1]), networkPort);
                for (Map.Entry<Integer, PeerAddress> entry : otherOverlaysMap.entrySet()) {
                    trigger(new SearchRequest(self, entry.getValue(), event.getId(), args[1]), networkPort);
                }

                //Start WebResponse Timer and parse event and args[1] as parameters
                ScheduleTimeout rst = new ScheduleTimeout(500);
                rst.setTimeoutEvent(new WebResponseTimeout(rst, event, args[1]));
                trigger(rst, timerPort);

                //response = new WebResponse(searchPageHtml(args[1]), event, 1, 1);
            } else if (args[0].compareToIgnoreCase("add") == 0) {
                addEntryHtml(args[1] , args[2], event);
            } else {
                response = new WebResponse(searchPageHtml(event.getTarget()), event, 1, 1);
                trigger(response, webPort);
            }

        }
    };


    //Handle WebResponseTimeout: get from HashMap the results gathered from Search results, trigger the response and then delete it
    Handler<WebResponseTimeout> handleWebResponseTimeout = new Handler<WebResponseTimeout>() {
        @Override
        public void handle(WebResponseTimeout event) {
            WebResponse response;
            String title = event.getQ();
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

            ArrayList<String> results = new ArrayList<String>();
            results =  searchResults.get(event.getEvent().getId());
            sb.append("Found ").append(results.size()).append(" entries.<ul>");
            for (int i = 0; i < results.size() ; i++) {
                sb.append("<li>").append(i + 1).append(". ").append(results.get(i)).append("</li>");
            }
            sb.append("</ul>");

            searchResults.remove(event.getEvent().getId());

            sb.append("</body></html>");

            //return sb.toString();
            response = new WebResponse(sb.toString(), event.getEvent(), 1, 1);
            trigger(response, webPort);

        }
    };

    //Handle Search Request: search for a query
    Handler<SearchRequest> handleSearchRequest = new Handler<SearchRequest>() {
        @Override
        public void handle(SearchRequest event) {
            String q = event.getQ();
            ArrayList<String> result = new ArrayList<String>();
            try {
                result = querySearch(q);
            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
                result.add(ex.getMessage());
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
                result.add(ex.getMessage());
            }
            trigger(new SearchResults(self, event.getPeerSource(), event.getEventId(), result), networkPort);
        }
    };


    //Handle Search Results: Add to HashMap
    Handler<SearchResults> handleSearchResults = new Handler<SearchResults>() {
        @Override
        public void handle(SearchResults event) {
            if (searchResults.containsKey(event.getEventId())) {
                ArrayList<String> tmp =  searchResults.get(event.getEventId());
                tmp.addAll(event.getResult());
                searchResults.remove(event.getEventId());
                searchResults.put(event.getEventId(), tmp);
            }
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

    private void addEntryHtml(String title, String magnet, WebRequest event) {
        UUID requestId = UUID.randomUUID();
        pendingWebResp.put(requestId, event);
        trigger(new AddEntryRequest(self, self, self, title, magnet, requestId), tmanSamplePort);
        ScheduleTimeout rst = new ScheduleTimeout(10000);
        rst.setTimeoutEvent(new AddEntryACKTimeout(rst, requestId, title, magnet));
        trigger(rst, timerPort);
    }

    private void addEntry(String title, int id, String magnet) throws IOException {

        //logger.info(self.getPeerAddress().getId() + " - adding index entry: {}-{}-{}"+magnet, title, id);

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

        lastIdWritten+=1;

        logger.info(String.format("%s Added %s %s %s", self.getPeerAddress().getId(), id, title, magnet));
        indexStore.add(id);
        Collections.sort(indexStore);
    }

    private ArrayList<String> querySearch(String querystr) throws ParseException, IOException {
        ArrayList<String> results = new ArrayList<String>();

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
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            results.add(d.get("id")+"\t"+d.get("title")+"\t"+d.get("magnet"));
        }

        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
        return results;
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
            ArrayList<PeerAddress> cyclonPartners = event.getSample();
            cyclonPartners = removePartnersNotFromYourPartiotion(cyclonPartners);
            // receive a new list of neighbours
            neighbours = cyclonPartners;
            // Pick a node or more, and exchange index with them
        }
    };

    private ArrayList<PeerAddress> removePartnersNotFromYourPartiotion(ArrayList<PeerAddress> cyclonPartners) {
        ArrayList<PeerAddress> result = new ArrayList<PeerAddress>();

        for(int i=0; i<cyclonPartners.size(); i++) {
            int partnerMod = cyclonPartners.get(i).getPeerAddress().getId() % numberOfPartitions;
            if(partnerMod == modPartition)
                result.add(cyclonPartners.get(i));
            else
                otherOverlaysMap.put(partnerMod, cyclonPartners.get(i));
        }

        return result;
    }
    
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            // receive a new list of neighbours
            ArrayList<PeerAddress> sampleNodes = event.getSample();
            // Pick a node or more, and exchange index with them
        }
    };

    //Garbage Collector of request IDs
    Handler<GarbageRequestIdTimeout> handleGarbageRequesId = new Handler<GarbageRequestIdTimeout>() {
        @Override
        public void handle(GarbageRequestIdTimeout event) {
            if(requestIds.contains(event.getRequestId()))
                requestIds.remove((Object) event.getRequestId());
        }
    };

    //Leader - Receives a message from TMan for adding an entry to the index
    Handler<AddEntryRequest> handleAddEntryRequest = new Handler<AddEntryRequest>() {
        @Override
        public void handle(AddEntryRequest event) {
            String title = event.getTitle();
            String magnet = event.getMagnet();
            UUID requestID = event.getRequestID();

            //Check if the request ID already exist
            if (requestIds.contains(requestID))
                return;
            requestIds.add(requestID);


            //Add the entry to the index
            logger.info("Leader "+self.getPeerAddress().getId()+" adds the request ID: "+requestID);
            try {
                addEntry(title, lastIdWritten+1, magnet);
                //Send ACK
                trigger(new AddEntryACK(self, self , event.getInitiator(), event.getRequestID(), lastIdWritten), tmanSamplePort);

            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            //lastIdWritten += 1;

            //Start a timer to remove it after 1 min
            ScheduleTimeout rst = new ScheduleTimeout(60000);
            rst.setTimeoutEvent(new GarbageRequestIdTimeout(rst, requestID));
            trigger(rst, timerPort);
        }
    };

//-------------------------------------------------------------------	
    Handler<AddIndexText> handleAddIndexText = new Handler<AddIndexText>() {
        @Override
        public void handle(AddIndexText event) {
            Random r = new Random(System.currentTimeMillis());
            UUID requestId = UUID.randomUUID() ;
            /*Generate random magnet link*/
            String magnet = new BigInteger(130, r).toString(32);

            //Trigger Add Request to T-Man
            //System.out.println("peer " + self.getPeerId() + " trying to add " + requestId);
            trigger(new AddEntryRequest(self, self, self, event.getText(), magnet, requestId), tmanSamplePort);


            //Start Timer for ACK
            ScheduleTimeout rst = new ScheduleTimeout(10000);
            rst.setTimeoutEvent(new AddEntryACKTimeout(rst, requestId, event.getText(), magnet));
            trigger(rst, timerPort);
        }
    };

    //Entry successfully inserted
   Handler<AddEntryACK> handleAddEntryACK = new Handler<AddEntryACK>() {
        @Override
        public void handle(AddEntryACK event) {
            if (!receivedAcks.contains(event.getRequestID()))
                receivedAcks.add(event.getRequestID());


            /*Print out*/
            if (pendingWebResp.containsKey(event.getRequestID())) {
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

                String[] args = pendingWebResp.get(event.getRequestID()).getTarget().split("-");

                /*Add it to the index*/
                try {
                    addEntry(args[1], event.getEntryId(), args[2]);
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }


                sb.append("Entry: ").append(event.getEntryId()).append(" - ").append(args[1]).append(" - ").append(args[2]);
                sb.append("</body></html>");
                //return sb.toString();
                WebResponse response = new WebResponse(sb.toString(), pendingWebResp.get(event.getRequestID()), 1, 1);
                trigger(response, webPort);
                pendingWebResp.remove(event.getRequestID());
            }
        }
    };

    //Check if ACK has received
   Handler<AddEntryACKTimeout> handleAddEntryACKTimer = new Handler<AddEntryACKTimeout>() {
        @Override
        public void handle(AddEntryACKTimeout event) {
            if (receivedAcks.size()>0 && receivedAcks.contains(event.getRequestId())) {
                receivedAcks.remove((Object) event.getRequestId() );
            }
            else  {
                trigger(new AddEntryRequest(self, self, self, event.getTitle(), event.getMagnet(), event.getRequestId()), tmanSamplePort);
                //Start Timer for ACK
                ScheduleTimeout rst = new ScheduleTimeout(10000);
                rst.setTimeoutEvent(new AddEntryACKTimeout(rst, event.getRequestId(), event.getTitle(), event.getMagnet()));
                trigger(rst, timerPort);
            }
        }
    };
}
