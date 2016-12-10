package edu.nyu.cs.cs2580;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;


/**
 * Created by kc on 11/18/16.
 */
public class PRF implements Serializable {
  private int _numTerms;
  private Vector<ScoredDocument> _scoredDocs;
  private final Indexer _indexer;
  private Map<String, Float> termProbability = new HashMap<String, Float>();
  private Map<String, Integer> termFrequency = new HashMap<String, Integer>();
  private static final Set<String> STOP_WORDS = Helper.getStopWords();



  public PRF(Vector<ScoredDocument> scoredDocs, int numTerms, Indexer indexer) {
    _numTerms = numTerms;
    _scoredDocs = scoredDocs;
    _indexer = indexer;
    System.out.println("construct prf");
  }
  
  /**
   * Calculate the probability for a term
   * w: term
   * D: _scoredDocs
   * get a Map<TermId, Frequence> for unique terms of DocumentIndexed doc: doc.getUniqueTerms()
   * getUniqueTerms() may not be allowed to use
   * @param term
   * @return
   */
  float totalWordCount = 0;
  float frequency = 0;
  float probability = 0;
  float totalProbability = 0;
  float normalizedProbability = 0;

  private float calculateProb(String term) {

    for(ScoredDocument document : _scoredDocs){
      totalWordCount += ((DocumentIndexed)document.getDoc()).getDocTotalTerms();
    }
    frequency = termFrequency.get(term);
    probability = frequency / totalWordCount;
    
    return probability;
  }

  private void getUniqTerms(Document doc) throws IOException {
    totalWordCount += ((DocumentIndexed)doc).getDocTotalTerms();
    String offsetFileName = _indexer._options._indexPrefix + "/prfoffset.idx";
    RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "r");
    String prfMapFileName = _indexer._options._indexPrefix + "/prfmap.idx";
    RandomAccessFile prfMapFile = new RandomAccessFile(prfMapFileName, "r");

    System.out.println("start load files");
    long offset;
    long nextOffset;
    int did = doc._docid;
    System.out.println("start load file: " + did);
    if (did == 0) {
      offset = 0;
    } else {
      offsetFile.seek(8L * (did - 1));
      offset = offsetFile.readLong();
    }

    offsetFile.seek(8L * did);
    nextOffset = offsetFile.readLong();
    System.out.println("get offset: " + offset + "-" + nextOffset);

    int size = (int)(nextOffset - offset);
    System.out.println("byte size: " + size);
    prfMapFile.seek(offset);
    byte[] temp = new byte[size];
    for (int i = 0; i < size; i++) {
      temp[i] = prfMapFile.readByte();
    }
    System.out.println("get temp: " + temp.length);
    String record = new String(temp, "UTF-8");
    Scanner s = new Scanner(record).useDelimiter("\t");
    while (s.hasNext()) {
      String token = s.next();
      System.out.println("get token: " + token);
      if (!s.hasNext()) {
        System.out.println("not has next");
        break;
      }
      int frequent = 0;
      try {
        frequent = Integer.parseInt(s.next());
      } catch (NumberFormatException e) {
        continue;
      }
      if (!STOP_WORDS.contains(token) && !termFrequency.containsKey(token)) {
        termFrequency.put(token, frequent);
      } else if (!STOP_WORDS.contains(token) && termFrequency.containsKey(token)) {
        termFrequency.put(token, termFrequency.get(token) + frequent);
      }
    }
    s.close();
    offsetFile.close();
    prfMapFile.close();
  }

  public void constructResponse(StringBuffer response) throws IOException {
    int temp = 0;
    int sum = 0;

    for(ScoredDocument document : _scoredDocs) {
      getUniqTerms(document.getDoc());
    }

    //Calculate probablity of each term in the map, update map.
    for (String term : termFrequency.keySet()) {
      termProbability.put(term,calculateProb(term));
    }

    //Sort map.
    sortMapByValue(termProbability);

    //Renormalize probability and save them.
    List<String> keys = new ArrayList<String>(termProbability.keySet());

    for (int i = 0; i <_numTerms ; i++) {
      totalProbability += termProbability.get(keys.get(i));
    }
    
    for (int i = 0; i <_numTerms ; i++) {
      normalizedProbability = termProbability.get(keys.get(i)) / totalProbability;
      response.append(keys.get(i) + "\t" + normalizedProbability + "\n");
    }

    String outFile = "data/prfOutput.tsv";
    File output = new File(outFile);
    FileWriter writer = new FileWriter(output);
    writer.write(response.toString());
    writer.close();
  }

  public Map<String, Float> sortMapByValue(Map<String, Float> map) {
  
    List<Map.Entry<String, Float>> list = new LinkedList<>(map.entrySet());
  
    Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
      @Override
      public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
        return o1.getValue().compareTo(o2.getValue());
      }
    });
  
    Map<String, Float> sortedMap = new HashMap<String, Float>();
    for (Map.Entry<String, Float> entry : list) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }
  
    return sortedMap;
  }
  
  public static void main(String[] args) throws IOException {
    SearchEngine.Options op = new SearchEngine.Options("conf/engine.conf");
    String corpusPath = op._corpusPrefix;
    File corpusDir = new File(corpusPath);
    
    for (File corpusDoc : corpusDir.listFiles()) {
      if (CorpusAnalyzer.isValidDocument(corpusDoc)) {
        //numViews.put(convertToUTF8(corpusDoc.getName()), 0);
      }
    }
  }
}
