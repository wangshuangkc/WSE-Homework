package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3 based on your {@code RankerFavorite}
 * from HW2. The new Ranker should now combine both term features and the
 * document-level features including the PageRank and the NumViews. 
 */
public class RankerComprehensive extends Ranker {
  private double _lambda = 0.5;

  public RankerComprehensive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> results = null;
    QueryPhrase qp = new QueryPhrase(query._query);
    qp.processQuery();
    
    try {
      Vector<ScoredDocument> scoredDocs = new Vector<>();
      DocumentIndexed doc = (DocumentIndexed) _indexer.nextDoc(qp, -1);
      //System.out.println("find potential doc: " + doc._docid);
      while (doc != null) {
        scoredDocs.add(scoreDocument(qp, doc._docid));
        doc = (DocumentIndexed) _indexer.nextDoc(qp, doc._docid);
      }
  
      Collections.sort(scoredDocs, new Comparator<ScoredDocument>() {
        @Override
        public int compare(ScoredDocument doc1, ScoredDocument doc2) {
          return doc2.compareTo(doc1);
        }
      });
      
      results = new Vector<>();
      int cnt = numResults;
      System.out.println("scored docs all: " + scoredDocs.size());
      for (ScoredDocument sDoc : scoredDocs) {
        if (cnt == 0) {
          break;
        }
        results.add(sDoc);
        cnt--;
      }
      System.out.println("scored docs: " + results.size());
    } catch (Exception e) {
      System.out.println("Error: ranking failed");
      e.printStackTrace();
    }
    
    return results;
  }
  
  private ScoredDocument scoreDocument(QueryPhrase query, int docid) {
    DocumentIndexed doc = (DocumentIndexed)_indexer.getDoc(docid);
    long docTotalTerm = doc.getDocTotalTerms();
    if (doc == null) {
      System.out.println("No document with Id: " + docid);
      return null;
    }
    
    float relevance = 0.0f;
    for (String token : query._tokens) {
      int tf = _indexer.documentTermFrequency(token, docid);
      ;
      relevance += Math.log((1 - _lambda) * tf / docTotalTerm +
          _lambda * _indexer.corpusTermFrequency(token) / _indexer._totalTermFrequency);
    }
    relevance = (float)Math.pow(10, relevance);
    System.out.println("relevance: " + relevance);
    
    float pageRank = doc.getPageRank();
    int numview = doc.getNumViews();
    
    double score = 0.5 * relevance + 0.25 * pageRank + 0.25 * Math.log(numview + 1);
    
    return new ScoredDocument(doc, score);
  }
  
  public double getLambda() {
    return _lambda;
  }
  
  public void setLambda(double lambda) {
    _lambda = lambda;
  }
}

