package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {
  
  public LogMinerNumviews(Options options) {
    super(options);
  }
  
  /**
   * This function processes the logs within the log directory as specified by
   * the {@link }. The logs are obtained from Wikipedia dumps and have
   * the following format per line: [language]<space>[article]<space>[#views].
   * Those view information are to be extracted for documents in our corpus and
   * stored somewhere to be used during indexing.
   *
   * Note that the log contains view information for all articles in Wikipedia
   * and it is necessary to locate the information about articles within our
   * corpus.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
  
    Map<String, Integer> numViews = new HashMap<String, Integer>();
    String corpusPath = _options._corpusPrefix;
    File corpusDir = new File(corpusPath);
    for (File corpusDoc : corpusDir.listFiles()) {
      if (CorpusAnalyzer.isValidDocument(corpusDoc)) {
        numViews.put(Helper.convertToUTF8(corpusDoc.getName()), 0);
      }
    }
    
    String logfile= _options._logPrefix + "/20160601-160000.log";
    BufferedReader reader = new BufferedReader(new FileReader(logfile));
    String line = null;
    while ((line = reader.readLine()) != null) {
      String[] nvs = line.split(" ");
      try {
        String title = URLDecoder.decode(nvs[1], "UTF-8");
        if (numViews.containsKey(title)) {
          int nv = Integer.valueOf(nvs[2]);
          numViews.put(title, nv);
        }
      } catch (Exception e) {}
    }
    reader.close();

    File file = new File(_options._indexPrefix);
    if (!file.exists()) {
      file.mkdir();
    }
    String numViewsFile = _options._indexPrefix + "/numViews.idx";
    System.out.println("Store numViews to: " + numViewsFile);
    ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(numViewsFile));
    writer.writeObject(numViews);
    writer.close();
  }

  /**
   * During indexing mode, this function loads the NumViews values computed
   * during mining mode to be used by the indexer.
   * 
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    String numViewsFile = _options._indexPrefix + "/numViews.idx";
    
    try {
      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(numViewsFile));
      Map<String, Integer> numViews = (HashMap<String, Integer>)reader.readObject();
      reader.close();
      return numViews;
    } catch (ClassNotFoundException e) {
      System.out.println("Error: not index for num view");
      return null;
    }
  }
}
