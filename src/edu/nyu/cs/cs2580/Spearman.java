package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

/**
 * Compares PageRank and NumViews
 * takes 2 arguments "path-to-pageranks", "path-to-numviews",
 * which contains (docid, pagerank) and (docid, numviews), and
 * returns the single value measuring correlation
 * puts the value in readme.txt
 *
 * Created by kc on 11/10/16.
 */
public class Spearman {

  private static final String _docMapName = "/docMap.idx";
  private final Map<String, Integer> _pageRank;
  private final Map<String, Integer> _numViews;
  
  public Spearman(String pathToPageRank, String pathToNumviews) {
    _pageRank = sortRank(readPRIndex(pathToPageRank));
    _numViews = sortRank(readNVIndex(pathToNumviews));
  }
  
  private Map<String, Float> readNVIndex(String pathToIndex) {
    Map<String, Integer> indexMap = null;
    try {
      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(pathToIndex));
      indexMap = (HashMap<String, Integer>)reader.readObject();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println("Error: invalid path to index: " + pathToIndex);
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      System.out.println("Error: invalid index file from path: " + pathToIndex);
      e.printStackTrace();
    }
    
    Map<String, Float> result = new HashMap<>();
    for (String key : indexMap.keySet()) {
      result.put(key, Float.valueOf(indexMap.get(key)));
    }
    
    return result;
  }
  
  private Map<String, Float> readPRIndex(String pathToIndex) {
    Map<String, Float> indexMap = null;
    try {
      ObjectInputStream reader = new ObjectInputStream(new FileInputStream(pathToIndex));
      indexMap = (HashMap<String, Float>)reader.readObject();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println("Error: invalid path to index: " + pathToIndex);
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      System.out.println("Error: invalid index file from path: " + pathToIndex);
      e.printStackTrace();
    }
    
    return indexMap;
  }
  
  private Map<String, Integer> sortRank(Map<String, Float> ranks) {
    Map<String, Integer> sortedMap = new HashMap<>();
    List<Map.Entry<String, Float>> entries = new ArrayList<>(ranks.entrySet());
    Collections.sort(entries, new Comparator<Map.Entry<String, Float>>() {
      public int compare(Map.Entry<String, Float> entry1, Map.Entry<String, Float> entry2) {
        if (entry2.getValue() > entry1.getValue()) {
          return 1;
        }
        if (entry2.getValue() < entry1.getValue()) {
          return -1;
        }
        return entry2.getKey().compareTo(entry1.getKey());
      }
    });
    
    int rank = 1;
    for (Map.Entry<String, Float> entry : entries) {
      sortedMap.put(entry.getKey(), rank++);
    }
    
    return sortedMap;
  }
  
  public double computeSpearman() {
    double coefficient = 0;
    double avgPR = 0;
    double avgNV = 0;
    
    for (String key : _pageRank.keySet()) {
      double pr = _pageRank.containsKey(key) ? _pageRank.get(key) : 0;
      double nv = _numViews.containsKey(key) ? _numViews.get(key) : 0;
      avgPR += pr;
      avgNV += nv;
    }
    avgPR /= _pageRank.size();
    avgNV /= _numViews.size();
  
    double pr_nv_z = 0;
    double pr_z_sq = 0;
    double nv_z_sq = 0;
    for (String key : _pageRank.keySet()) {
      double pr = _pageRank.containsKey(key) ? _pageRank.get(key) : 0;
      double nv = _numViews.containsKey(key) ? _numViews.get(key) : 0;
      pr_nv_z += (pr - avgPR) * (nv - avgNV);
      pr_z_sq += (pr - avgPR) * (pr - avgPR);
      nv_z_sq += (nv - avgNV) * (nv - avgNV);
    }
    
    return pr_nv_z / (Math.sqrt(pr_z_sq) * Math.sqrt(nv_z_sq));
  }
  
  public static void main(String[] args) {
    if (args.length < 2) {
    } else {
      Spearman spearman = new Spearman(args[0], args[1]);
      System.out.println("Spearman Coefficient: " + spearman.computeSpearman());
    }
  }
}
