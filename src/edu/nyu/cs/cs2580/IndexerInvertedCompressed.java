package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
  
  private static final long serialVersionUID = 1077111905740085031L;
  private static final String WIKI_STOPWORD = " - Wikipedia, the free encyclopedia";
  private static final String WIKI_URL = "https://en.wikipedia.org/wiki/";
  private static final int MAX_DOC_IDX = 500;
  private static final String DATA_FILE_NAME = "/data.idx";
  private static final String INDEX_FILE_NAME = "/compressed.idx";
  private static final String OFFSET_FILE_NAME = "/compressed_offset.idx";
  private static final String PRF_FILE_NAME = "/prfmap.idx";
  private static final String PRF_OFFSET_FILE_NAME = "/prfoffset.idx";
  
  private Map<String, Integer> _dictionary = new HashMap<>();
  private Vector<DocumentIndexed> _documents = new Vector<>();
  private Map<Integer, Term> _index = new HashMap<>();
  private Map<String, Float> _pageRanks;
  private Map<String, Integer> _numViews;
  private int[] cachedPostingIdxes;
  
  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }
  
  @Override
  public void constructIndex() throws IOException {
    long start = System.currentTimeMillis();
    
    CorpusAnalyzer analyzer = CorpusAnalyzer.Factory.getCorpusAnalyzerByOption(SearchEngine.OPTIONS);
    _pageRanks = (HashMap<String, Float>)analyzer.load();
    LogMiner miner = LogMiner.Factory.getLogMinerByOption(SearchEngine.OPTIONS);
    _numViews = (HashMap<String, Integer>)miner.load();

    String prfOffsetFile = _options._indexPrefix + PRF_OFFSET_FILE_NAME;
    RandomAccessFile prfOffset = new RandomAccessFile(prfOffsetFile, "rw");
    long offset = 0l;

    int tmpIdxCnt = 0;

    try {
      System.out.println("Construct index from: " + _options._corpusPrefix);
      File corpusDir = new File(_options._corpusPrefix);
      for (File file : corpusDir.listFiles()) {
        if (!file.isFile() || !CorpusAnalyzer.isValidDocument(file)) {
          continue;
        }
  
        long size = processDocument(file);
        offset += size;
        prfOffset.writeLong(offset);
        
        if (_numDocs % MAX_DOC_IDX == 0) {
          writePartialIndices(tmpIdxCnt++);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    prfOffset.close();

    if (_numDocs % MAX_DOC_IDX != 0) {
      writePartialIndices(tmpIdxCnt++);
    }
    mergePartialIndex(tmpIdxCnt);
  
    System.out.println("Indexed " + Integer.toString(_numDocs) + " docs with " +
            Long.toString(_totalTermFrequency) + " terms.");
    
    saveDataIndex();
    
    System.out.println("Time: " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() -
            start));
  }
  
  private long processDocument(File file) throws IOException{
    int did = _numDocs++;
    int pos = 0;
    long result = 0l;
    int docTotalTerms = 0;
    DocumentIndexed doc = new DocumentIndexed(did);
    String baseName = Helper.convertToUTF8(file.getName());
    doc.setUrl(WIKI_URL + baseName);
    doc.setPageRank(_pageRanks.get(baseName));
    doc.setNumViews(_numViews.get(baseName));

    HashMap<String,Integer> prfMap = new HashMap<>();
    String prfFile = _options._indexPrefix + PRF_FILE_NAME;
    BufferedOutputStream prfMapWriter = new BufferedOutputStream(new FileOutputStream(prfFile,true));

    org.jsoup.nodes.Document parseDoc = Jsoup.parse(file, "UTF-8");
    String title = parseDoc.title().replace(WIKI_STOPWORD, "");
    doc.setTitle(title);
    String content = parseDoc.body().text();
    parseDoc = null;
    
    Scanner[] scanners = new Scanner[2];
    scanners[0] = new Scanner(doc.getTitle()).useDelimiter("\\s+");
    scanners[1] = new Scanner(content).useDelimiter("\\s+");
    for (Scanner scanner: scanners) {
      while(scanner.hasNext()) {
        String token = Helper.porterStem(scanner.next());
        if(token == null || token.trim().isEmpty()) {
          continue;
        }

        if (!prfMap.containsKey(token)) {
          prfMap.put(token, 1);
        } else {
          prfMap.put(token, prfMap.get(token) + 1);
        }

        _totalTermFrequency++;
        docTotalTerms++;
        
        if (!_dictionary.containsKey(token)) {
          _dictionary.put(token, _dictionary.size());
        }
        int idx = _dictionary.get(token);
        if (!_index.containsKey(idx)) {
          _index.put(idx, new Term(did));
        }
        List<Posting> plist = _index.get(idx)._postingList;
        if (plist.get(plist.size() - 1)._docid == did) {
          plist.get(plist.size() - 1).appendOccurrence(++pos);
        } else {
          Posting post = new Posting(did);
          post.appendOccurrence(++pos);
          plist.add(post);
        }
      }
      scanner.close();
    }
    
    doc.setDocTotalTerms(docTotalTerms);
    _documents.add(doc);

    for (Map.Entry<String, Integer> e: prfMap.entrySet()) {
      String temp = e.getKey() + "\t" + e.getValue() + "\t";
      byte[] b = temp.getBytes("UTF-8");
      result += b.length;
      prfMapWriter.write(b);
    }
    prfMap.clear();
    prfMapWriter.close();

    return result;
  }
  
  private void writePartialIndices(int idx) throws IOException {
    String indexFile = _options._indexPrefix + "/" + idx + ".idx";
    System.out.println("Save partial indices to: " + indexFile);
    BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));
    Integer[] keys = _index.keySet().toArray(new Integer[_index.keySet().size()]);
    Arrays.sort(keys);
    for (int key: keys) {
      Term info = _index.get(key);
      writer.write(key + " " + info.getCorpusFreq() + " " + info.getDocFreq());
      for (Posting posting: info._postingList) {
        writer.write(" " + posting._docid + "," + printList(posting._occurrence));
      }
      writer.newLine();
    }
    writer.close();
    _index.clear();
    System.gc();
  }
  
  private String printList(List<Integer> list) {
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < list.size(); i++) {
      if (i == list.size() - 1) {
        sb.append(list.get(i));
      } else {
        sb.append(list.get(i) + ",");
      }
    }
    
    return sb.toString();
  }
  
  private void mergePartialIndex(int idxCnt) throws IOException{
    BufferedReader[] readers= new BufferedReader[idxCnt];
    for (int i = 0; i < idxCnt; i++) {
      String partialFile = _options._indexPrefix + "/" + i + ".idx";
      readers[i] = new BufferedReader(new FileReader(partialFile));
    }
    
    String fullIdx = _options._indexPrefix + INDEX_FILE_NAME;
    System.out.println("Merge partial indices into: " + fullIdx);
    DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fullIdx)));
    
    String offsetFileName = _options._indexPrefix + OFFSET_FILE_NAME;
    RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "rw");
    
    boolean[] toMove = new boolean[idxCnt];
    Arrays.fill(toMove, true);
    int min_id;
    int[] termIds = new int[idxCnt];
    Term[] terms = new Term[idxCnt];
    long offset = 0;
    
    while(true) {
      min_id = Integer.MAX_VALUE;
      for (int i = 0; i < idxCnt; i++) {
        if (toMove[i]) {
          String line = readers[i].readLine();
          if (line == null) {
            termIds[i] = Integer.MAX_VALUE;
            terms[i] = null;
          } else {
            Scanner s = new Scanner(line);
            termIds[i] = Integer.parseInt(s.next());
            int corpusFreq = Integer.parseInt(s.next());
            int docFreq = Integer.parseInt(s.next());
            List<Posting> plist = new ArrayList<>();
            while (s.hasNext()) {
              String[] posting = s.next().split(",");
              Posting p = new Posting(Integer.parseInt(posting[0]));
              for (int j = 1; j < posting.length; j++) {
                p.appendOccurrence(Integer.parseInt(posting[j]));
              }
              plist.add(p);
            }
            terms[i] = new Term(corpusFreq, docFreq, plist);
          }
          toMove[i] = false;
        }
        min_id = Math.min(min_id, termIds[i]);
      }
      if (min_id == Integer.MAX_VALUE) {
        break;
      }
      List<Term> toMerge = new ArrayList<>();
      for (int i = 0; i < idxCnt; i++) {
        if (termIds[i] == min_id) {
          toMerge.add(terms[i]);
          toMove[i] = true;
        }
      }
      long size = writeToIndex(min_id, toMerge, writer);
      offset += size;
      offsetFile.writeLong(offset);
    }
    for (BufferedReader r: readers) {
      r.close();
    }
    writer.close();
    offsetFile.close();
    
    for (int i = 0; i < idxCnt; i++) {
      String partialFile = _options._indexPrefix + "/" + i + ".idx";
      File file = new File(partialFile);
      file.delete();
    }
  }
  
  private long writeToIndex(int termId, List<Term> terms, DataOutputStream writer) throws
          IOException{
    
    long result = 0l;
    int corpusFreq = 0;
    int docFreq = 0;
    List<Posting> plist = new ArrayList<>();
    for (Term t: terms) {
      corpusFreq += t.getCorpusFreq();
      docFreq += t.getDocFreq();
      plist.addAll(t._postingList);
    }
    
    byte[] temp;
    temp = encode(termId);
    writer.write(temp);
    result += temp.length;
    temp = encode(corpusFreq);
    writer.write(temp);
    result += temp.length;
    temp = encode(docFreq);
    writer.write(temp);
    result += temp.length;
    for (Posting p: plist) {
      temp = encode(p._docid);
      writer.write(temp);
      result += temp.length;
      temp = encode(p.getDocTermFreq());
      writer.write(temp);
      result += temp.length;
      for (int o: p._occurrence) {
        temp = encode(o);
        writer.write(temp);
        result += temp.length;
      }
    }
    
    return result;
  }
  
  private byte[] encode(int num) {
    byte[] ret = null;
    if (num < 128) {
      ret = new byte[1];
      ret[0] = (byte) (num + 128);
      return ret;
    } else if (num < 16384) {
      ret = new byte[2];
      ret[0] = (byte) (num / 128);
      ret[1] = (byte) (num % 128 + 128);
    } else if (num < 2097152) {
      ret = new byte[3];
      ret[0] = (byte) (num / 16384);
      byte[] rest = encode(num % 16384);
      if (rest.length == 1) {
        ret[1] = 0;
        ret[2] = rest[0];
      } else {
        ret[1] = rest[0];
        ret[2] = rest[1];
      }
    } else if (num < 268435456) {
      ret = new byte[4];
      ret[0] = (byte) (num / 2097152);
      byte[] rest = encode(num % 2097152);
      if (rest.length == 1) {
        ret[1] = 0;
        ret[2] = 0;
        ret[3] = rest[0];
      } else if (rest.length == 2) {
        ret[1] = 0;
        ret[2] = rest[0];
        ret[3] = rest[1];
      } else if (rest.length == 3) {
        ret[1] = rest[0];
        ret[2] = rest[1];
        ret[3] = rest[2];
      }
    }
    
    return ret;
  }
  
  private void saveDataIndex() throws IOException{
    String dataFile = _options._indexPrefix + DATA_FILE_NAME;
    System.out.println("Store global data to: " + dataFile);
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(dataFile, false));
    writer.writeObject(_numDocs);
    writer.writeObject(_totalTermFrequency);
    writer.writeObject(_documents);
    writer.writeObject(_dictionary);
    writer.close();
  }
  
  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    readDataIndex();
  
    System.out.println(Integer.toString(_numDocs) + " documents loaded " +
            "with " + Long.toString(_totalTermFrequency) + " terms!");
    
    this.cachedPostingIdxes = new int[_dictionary.size()];
    Arrays.fill(cachedPostingIdxes, 0);
  }
  
  private void readDataIndex() throws IOException, ClassNotFoundException {
    String dataFile = _options._indexPrefix + DATA_FILE_NAME;
    System.out.println("Load global data from: " + dataFile);
    ObjectInputStream reader = new ObjectInputStream(new FileInputStream(dataFile));
    _numDocs = (int) reader.readObject();
    _totalTermFrequency = (long) reader.readObject();
    _documents = (Vector<DocumentIndexed>) reader.readObject();
    _dictionary = (Map<String, Integer>) reader.readObject();
    System.out.println("dict size: " + _dictionary.size());
    reader.close();
  }
  
  @Override
  public DocumentIndexed getDoc(int docid) {
    return _documents.get(docid);
  }
  
  /**
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public DocumentIndexed nextDoc(Query query, int docid) {
    QueryPhrase qp = new QueryPhrase(query._query);
    qp.processQuery();

    System.out.println("looking for common doc");
    while(true) {
      int candidate = next(query._tokens, docid);
      if (candidate == -1) {
        _index.clear();
        return null;
      }
      boolean containAll = true;
      for (String phrase : qp._phrase) {
        if (!containsPhrase(phrase, candidate)) {
          containAll = false;
          break;
        }
      }
      if (containAll) {
        return _documents.get(candidate);
      }
      docid = candidate - 1;
    }
  }
  
  private int next(Vector<String> tokens, int docid) {
    if (tokens.isEmpty()) {
      return -1;
    }
    int[] docs = new int[tokens.size()];
    for (int i = 0; i < tokens.size(); i++) {
      docs[i] = next(tokens.get(i), docid);
    }
    System.out.println("docs: " + Arrays.toString(docs));
    Arrays.sort(docs);
    if (docs[0] == -1) {
      return -1;
    }
    if (docs[0] == docs[docs.length - 1]) {
      return docs[0];
    }
    
    int max = docs[docs.length - 1];
    return next(tokens, max - 1);
  }
  
  private int next(String token, int docid) {
    if (!_dictionary.containsKey(token)) {
      return -1;
    }
    int idx = _dictionary.get(token);
    if (!_index.containsKey(idx)) {
      try {
        _index.put(idx, fetchInfo(idx));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    List<Posting> pl = _index.get(idx)._postingList;
    if (pl.get(pl.size() - 1)._docid <= docid) {
      return -1;
    }
    if (pl.get(0)._docid > docid) {
      cachedPostingIdxes[idx] = 0;
      return pl.get(0)._docid;
    }
    int cachedPIdx = cachedPostingIdxes[idx];
    if (cachedPIdx < 0 || pl.get(cachedPIdx)._docid > docid) {
      cachedPIdx = 0;
    }
    cachedPIdx = searchNextDoc(cachedPIdx,pl.size() - 1, docid, pl);
    cachedPostingIdxes[idx] = cachedPIdx;

    return pl.get(cachedPIdx)._docid;
  }

  private Term fetchInfo(int id) throws IOException{
    long offset;
    long next_offset;
    String offsetFileName = _options._indexPrefix + OFFSET_FILE_NAME;
    RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "r");
    if (id == 0) {
      offset = 0;
    } else {
      offsetFile.seek(8L * (id - 1));
      offset = offsetFile.readLong();
    }

    offsetFile.seek(8L * id);
    next_offset = offsetFile.readLong();

    int size = (int)(next_offset - offset);
    String indexFileName = _options._indexPrefix + INDEX_FILE_NAME;
    RandomAccessFile indexFile = new RandomAccessFile(indexFileName,"r");
    indexFile.seek(offset);

    List<Byte> byte_list = new ArrayList<>();
    for (int i = 0;i < size;i++) {
      byte_list.add(indexFile.readByte());
    }
    List<Integer> ints = convertToIntList(byte_list);

    int corpusFreq = ints.get(1);
    int docFreq = ints.get(2);
    List<Posting> list = new ArrayList<>();
    int cur = 3;
    while (cur < ints.size()) {
      int did = ints.get(cur++);
      Posting p = new Posting(did);
      int occur = ints.get(cur++);
      for (int i = 0; i < occur; i++) {
        p.appendOccurrence(ints.get(cur++));
      }
      list.add(p);
    }
    Term info = new Term(corpusFreq, docFreq, list);
    offsetFile.close();
    indexFile.close();

    return info;
  }

  private int searchNextDoc(int low, int high, int current, List<Posting> list) {
    while (high - low > 1) {
      int mid = (high - low) / 2 + low;
      if (list.get(mid)._docid <= current) {
        low = mid;
      } else {
        high = mid;
      }
    }

    return high;
  }

  private List<Integer> convertToIntList(List<Byte> list) {
    List<Byte> byteList = new ArrayList<>();
    List<Integer> ret = new ArrayList<>();
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) < 0) {
        byteList.add(list.get(i));
        ret.add(decode(byteList));
        byteList.clear();
      } else {
        byteList.add(list.get(i));
      }
    }
    
    return ret;
  }
  
  private int decode(List<Byte> byteList) {
    if (byteList.size() == 1) {
      return (byteList.get(0) + 128);
    } else if (byteList.size() == 2) {
      return (byteList.get(0) * 128 + (byteList.get(1) + 128));
    } else if (byteList.size() == 3) {
      return (byteList.get(0) * 16384 + byteList.get(1) * 128 + (byteList.get(2) + 128));
    } else {
      return (byteList.get(0) * 2097152 + byteList.get(1) * 16384 + byteList.get(2) * 128 + (byteList.get(3) + 128));
    }
  }
  
  private boolean containsPhrase(String phrase, int docid) {
    int pos = -1;
    String[] phrases = phrase.split(" ");
    while (true) {
      boolean contains = true;
      int first = nextPos(phrases[0], docid, pos);
      for (int i = 1; i < phrases.length - 1; i++) {
        int next = nextPos(phrases[i], docid, first + i - 1);
        if (next != first + i) {
          contains = false;
          break;
        }
      }
      if (contains) {
        return true;
      }
      pos = first;
    }
  }

  private int nextPos(String token, int docid, int pos) {
    if (!_dictionary.containsKey(token)) {
      return -1;
    }
    int idx = _dictionary.get(token);
    if (!_index.containsKey(idx)) {
      try {
        _index.put(idx, fetchInfo(idx));
      } catch(Exception e) {}
    }
    List<Posting> pl = _index.get(idx)._postingList;
    Posting p = pl.get(cachedPostingIdxes[idx]);
    if (p == null) {
      return -1;
    }
    if (p._docid != docid) {
      p = searchPosting(0, pl.size() - 1, docid, pl);
    }

    return searchNextOccurrence(0, p.getDocTermFreq() - 1, pos, p._occurrence);
  }
  
  private Posting searchPosting(int low, int high, int current, List<Posting> list) {
    if (list.get(0)._docid > current || list.get(list.size() - 1)._docid < current) {
      return null;
    }

    while (high - low > 1) {
      int mid = (high - low) / 2 + low;
      Posting p = list.get(mid);
      if (p._docid == current) {
        return p;
      } else if (p._docid < current) {
        low = mid;
      } else {
        high = mid;
      }
    }

    if (list.get(high)._docid == current) {
      return list.get(high);
    }
    if (list.get(low)._docid == current) {
      return list.get(low);
    }

    return null;
  }
  
  private int searchNextOccurrence(int low, int high, int current, List<Integer> list) {
    if (list.get(0) > current) {
      return list.get(0);
    }
    if (list.get(list.size() - 1) < current) {
      return -1;
    }

    while (high - low > 1) {
      int mid = (high - low) / 2 + low;
      int element = list.get(mid);
      if (element <= current) {
        low = mid;
      } else {
        high = mid;
      }
    }

    return list.get(high);
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if (_dictionary.containsKey(term)) {
      return 0;
    }
    int idx = _dictionary.get(term);
    
    return cachedInfo(idx).getDocFreq();
  }
  
  @Override
  public int corpusTermFrequency(String term) {
    if (!_dictionary.containsKey(term)) {
      return 0;
    }
    int rep = _dictionary.get(term);
    
    return cachedInfo(rep).getCorpusFreq();
  }
  
  @Override
  public int documentTermFrequency(String term, int docid) {
    if (docid >= _numDocs || !_dictionary.containsKey(term)) {
      return 0;
    }
    
    int rep = _dictionary.get(term);
    
    Term info = cachedInfo(rep);
    List<Posting> pl = info._postingList;
    for (int i = 0; i < pl.size(); i++) {
      if (pl.get(i)._docid == docid) {
        return pl.get(i).getDocTermFreq();
      }
      if (pl.get(i)._docid > docid) {
        return 0;
      }
    }
    
    return 0;
  }
  
  private Term cachedInfo(int idx) {
    if (_index.containsKey(idx)) {
      return _index.get(idx);
    } else {
      Term info = null;
      try {
        info = fetchInfo(idx);
      } catch(Exception e) {}
      
      _index.put(idx, info);
      
      return info;
    }
  }
  
  class Posting {
    public int _docid;
    public List<Integer> _occurrence = new ArrayList<>();
    
    public Posting(int docid){
      _docid = docid;
    }
    
    public int getDocTermFreq() {
      return _occurrence.size();
    }
    
    public void appendOccurrence(int pos) {
      _occurrence.add(pos);
    }
  }
  
  class Term {
    private int _corpusFreq = 0;
    private int _docFreq = 0;
    public List<Posting> _postingList = new ArrayList<>();
    
    public Term(int corpusFreq, int docFreq, List<Posting> list) {
      _corpusFreq = corpusFreq;
      _docFreq = docFreq;
      _postingList.addAll(list);
    }
    
    public Term(int docid) {
      _corpusFreq = 0;
      _docFreq = 0;
      _postingList.add(new Posting(docid));
    }
    
    public int getCorpusFreq() {
      if (_corpusFreq == 0) {
        for (Posting p : _postingList) {
          _corpusFreq += p.getDocTermFreq();
        }
      }
      
      return _corpusFreq;
    }
    
    public int getDocFreq() {
      if (_docFreq == 0) {
        _docFreq = _postingList.size();
      }
      
      return _docFreq;
    }
  }
  
  public String[] getUniqTerms() {
    System.out.println("getting uniq terms");

    return _dictionary.keySet().toArray(new String[_dictionary.size()]);
  }
}