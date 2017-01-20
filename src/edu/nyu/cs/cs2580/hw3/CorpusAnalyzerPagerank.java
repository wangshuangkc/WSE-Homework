package edu.nyu.cs.cs2580.hw3;

import java.io.*;
import java.util.ArrayList;
import java.util.*;
import java.util.List;

import edu.nyu.cs.cs2580.hw3.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {
  private Map<String, ArrayList<String>> graph = new HashMap<>();
  private Map<String, Integer>  dict = new HashMap<>();
  final float LAMBDA = 0.9f;
  final int T = 2;

  public CorpusAnalyzerPagerank(Options options) {
    super(options);
  }

  /**
   * This function processes the corpus as specified inside {@link Options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   * 
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   * 
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
   *
   * @throws IOException
   */
  @Override
  public void prepare() throws IOException {
    System.out.println("Preparing " + this.getClass().getName());
    ArrayList<String> allFileNames = getAllFile();
    String corpusFile = _options._corpusPrefix;
    File dir = new File(corpusFile);
    File[] directoryListing = dir.listFiles();
    int index = 0;
    for(File file: directoryListing){
      String fileName = file.getName();
      if(fileName.startsWith(".")){
        continue;
      }
      dict.put(Helper.convertToUTF8(fileName), index);//make the dictionary
      index++;
      graph.put(Helper.convertToUTF8(fileName), new ArrayList<String>());
      HeuristicLinkExtractor hle = new HeuristicLinkExtractor(file);
      String nextLink = hle.getNextInCorpusLinkTarget();
      while(nextLink != null){
        if(allFileNames.contains(Helper.convertToUTF8(nextLink))){
          ArrayList<String> temp = graph.get(Helper.convertToUTF8(fileName));
          temp.add(Helper.convertToUTF8(nextLink));
          graph.put(Helper.convertToUTF8(fileName), temp);
        }
        nextLink = hle.getNextInCorpusLinkTarget();
      }
    }
    return;
  }

  private ArrayList<String> getAllFile(){
    ArrayList<String> result = new ArrayList<>();
    String corpusFile = _options._corpusPrefix;
    File dir = new File(corpusFile);
    File[] directoryListing = dir.listFiles();
    for(File file : directoryListing){
      if (!file.isFile()) {
        continue;
      }
      String fileName = file.getName();
      if(fileName.startsWith(".")){
        continue;
      }
      result.add(Helper.convertToUTF8(fileName));
    }
    return result;
  }

  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   * 
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    //get ready for the link matrix W
    float[][] matrixW = new float[graph.size()][graph.size()];
    Map<String, Float> pageRankWithKey = new HashMap<String, Float>();
    for(Map.Entry<String, ArrayList<String>> entry: graph.entrySet()){
      String key = entry.getKey();
      pageRankWithKey.put(key, 0.0f);
      List<String> value = entry.getValue();
      int index = dict.get(key);
      for(int i = 0; i < value.size(); i++){
        matrixW[index][dict.get(value.get(i))] = 1;
      }
    }
    /*for(int i = 0; i < matrixW.length; i++){
      for(int j = 0; j < matrixW[0].length; j++){
        if(j == matrixW[0].length - 1){
          System.out.println(matrixW[i][j]);
        } else {
          System.out.print(matrixW[i][j] + " ");
        }
      }
    }
    System.out.println("-----------------");

    for(int i = 0; i < matrixW.length; i++){
      for(int j = 0; j < matrixW[0].length; j++){
        if(j == matrixW[0].length - 1){
          System.out.println(matrixW[i][j]);
        } else {
          System.out.print(matrixW[i][j] + " ");
        }
      }
    }*/
    //manageRedirect(matrixW);
    //get ready for the transition matrix T
    int[]count = new int[graph.size()];
    for(int i = 0; i < graph.size(); i++){
      for(int j = 0; j < graph.size(); j++){
        if(matrixW[i][j] == 1.0){
          count[i]++;
        }
      }
    }

    float[][] matrixT = new float[graph.size()][graph.size()];
    for(int i = 0; i < graph.size(); i++){
      for(int j = 0; j < graph.size(); j++){
        if(matrixW[i][j] == 1.0){
          matrixT[i][j] = 1.0f / count[i];
        }
      }
    }


    //calculate google matrix
    float[][] tempMatrix = new float[graph.size()][graph.size()];
    for(int i = 0; i < graph.size(); i++){
      for(int j = 0; j < graph.size(); j++){
        tempMatrix[i][j] = 1.0f / graph.size();
      }
    }


    //Get the transpose of matrixT and times Lambda
    float[][] transition = new float[matrixT.length][matrixT[0].length];
    for(int i = 0; i < transition.length; i++){
      for(int j = 0; j < transition[0].length; j++){
        transition[j][i] = matrixT[i][j] * LAMBDA;
      }
    }
    //caculate the google matrix
    float[][] google = new float[transition.length][transition[0].length];
    for(int i = 0; i < tempMatrix.length; i++){
      for(int j = 0; j < tempMatrix[0].length; j++){
        google[i][j] = transition[i][j] + tempMatrix[i][j] * (1.0f - LAMBDA);
      }
    }
    //Simulation with T steps
    float[][] vec = new float[graph.size()][1];
    for(int i = 0; i < graph.size(); i++){
      vec[i][0] = 1.0f;
    }

    float[][] pageRank = new float[graph.size()][1];
    for(int step = 0; step < T; step++){
      if(step != 0){
        vec = pageRank;
        pageRank = new float[graph.size()][1];
      }
      for (int i = 0; i < google.length; i++) {
        for (int j = 0; j < google[0].length; j++) {
          pageRank[i][0] = pageRank[i][0] + (google[i][j] * vec[j][0]);
        }
      }
    }
    //Store the pagerank into disk
    for(Map.Entry<String, Float> entry: pageRankWithKey.entrySet()){
      String key = entry.getKey();
      pageRankWithKey.put(key, pageRank[dict.get(key)][0]);
    }
    File dir = new File(_options._indexPrefix);
    if (!dir.exists()) {
      dir.mkdir();
    }
    String indexFile = _options._indexPrefix + "/pagerank.idx";
    System.out.println("Store pagerank to: " + indexFile);
    //PrintWriter pr = new PrintWriter("/Users/WeisenZhao/Documents/page_rank.txt","UTF-8");
    //for(Map.Entry<String, Float> entry : pageRankWithKey.entrySet()){
    //  String key = entry.getKey();
    //  float value = entry.getValue();
    //  pr.println(key + "\t" + value);
    //}
    //pr.close();
    ObjectOutputStream writer =
            new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(pageRankWithKey);
    writer.close();
  }
  
  private void manageRedirect(float[][] linkMatrix){
    Map<Integer, ArrayList<Integer>> map = new HashMap<>();
    //create the links map
    for(int i = 0; i < linkMatrix.length; i++){
      ArrayList<Integer> links = new ArrayList<>();
      for(int j = 0; j < linkMatrix[0].length; j++){
        if(linkMatrix[i][j] == 1.0){
          links.add(j);
        }
      }
      map.put(i, links);
    }

    for(Map.Entry<Integer, ArrayList<Integer>> entry: map.entrySet()){
      ArrayList<Integer> links = entry.getValue();
      int index = 0;
      while(!links.isEmpty() || index < links.size()){
        int firstLink = links.get(index);
        ArrayList<Integer> next = map.get(firstLink);
        if(next.isEmpty()){
          index++;
          continue;
        }
        for(Integer element : next){
          if(!links.contains(element)){
            links.add(element);
          }
        }
        map.put(firstLink, new ArrayList<Integer>());
        links.remove(firstLink);
      }
    }

    linkMatrix = new float[graph.size()][graph.size()];
    for(Map.Entry<Integer, ArrayList<Integer>> entry: map.entrySet()){
      int key = entry.getKey();
      ArrayList<Integer> value = entry.getValue();
      for(Integer element: value){
        linkMatrix[key][element] = 1.0f;
      }
    }
  }
  /**
   * During indexing mode, this function loads the PageRank values computed
   * during mining mode to be used by the indexer.
   *
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    String indexFile = _options._indexPrefix + "/pagerank.idx";
    System.out.println("Load pagerank from: " + indexFile);
    ObjectInputStream reader =
            new ObjectInputStream(new FileInputStream(indexFile));
    try {
      Map<String, Float> pagerankWithKey = (HashMap<String, Float>) reader.readObject();
      return pagerankWithKey;
    } catch(ClassNotFoundException e){
      System.out.println("No Object found");
    }
    return null;
  }
}
