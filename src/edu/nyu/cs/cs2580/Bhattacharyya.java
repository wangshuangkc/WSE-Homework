package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

/**
 * Calculate coefficient
 * Created by kc on 11/18/16.
 */
public class Bhattacharyya {
  private final String _cwd = System.getProperty("user.dir");
  private String _prfOutPath;
  private String _qsimOut;
  
  public Bhattacharyya(String prfOutPath, String qsimOutPath) throws IOException {
    _prfOutPath = _cwd + "/" + prfOutPath;
    _qsimOut = _cwd + "/" + qsimOutPath;
  }
  
  private Map<String, Float> termPrfMap(String queryPrfOut) throws IOException {
    Map<String, Float> termPrf = new HashMap<>();
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(_cwd + "/" + queryPrfOut), "UTF-8"));
    String line = null;
    while ((line = br.readLine()) != null) {
      String[] entry = line.split("\t");
      if (entry.length != 2) {
        System.out.println("Invalid PRF output file: " + queryPrfOut);
        return null;
      }
      termPrf.put(entry[0], Float.valueOf(entry[1]));
    }
    
    return termPrf;
  }
  
  private Map<String, String> queryPrfoutMap() throws IOException {
    Map<String, String> queryPrfout = new HashMap<>();
    File file = new File(_prfOutPath);
    BufferedReader br = new BufferedReader(new FileReader(_prfOutPath));
    String line = null;
    while ((line = br.readLine()) != null) {
      String[] entry = line.split(":");
      if (entry.length != 2) {
        System.out.println("Invalid PRF output path: " + _prfOutPath);
        return null;
      }
      queryPrfout.put(entry[0], entry[1]);
    }
    
    return queryPrfout;
  }
  
  public void outputCoeff() throws IOException {
    File output = new File(_qsimOut);
    FileWriter writer = new FileWriter(output);
    Map<String, String> queryPrfout = queryPrfoutMap();
    
    Vector<String> queries = new Vector<>();
    
    for (String query : queryPrfout.keySet()) {
      queries.add(query);
    }
    float sum = 0f;
    float temp = 0f;
    for (int i = 0; i < queries.size(); i++) {
      String queryStart = queries.get(i);
      for (int j = i + 1; j < queries.size(); j++) {
        String queryEnd = queries.get(j);
        Map<String, Float> termPrfSmall = termPrfMap(queryPrfout.get(queryStart));
        Map<String, Float> termPrfBig = termPrfMap(queryPrfout.get(queryEnd));
        for (String term : termPrfSmall.keySet()) {
          if (termPrfBig.containsKey(term)) {
            temp = termPrfSmall.get(term) * termPrfBig.get(term);
            sum += temp;
          }
        }
        writer.write(queryStart + "\t" + queryEnd + "\t" + sum + "\n");
      }
    }
    writer.close();
  }
  
  public static void main(String[] args) throws IOException {
    Bhattacharyya bha = new Bhattacharyya("prf.tsv", "qsim.tsv");
    bha.outputCoeff();
  }
}
  
  

