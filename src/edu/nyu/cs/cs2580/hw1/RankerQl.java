package edu.nyu.cs.cs2580.hw1;

import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;

/**
 * @CS2580: Use this template to implement the query likelihood ranker for HW1.
 * 
 * @author congyu
 * @author fdiaz
 */
public class RankerQl extends Ranker {

  public RankerQl(SearchEngine.Options options,
                  QueryHandler.CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    // @CS2580: fill in your code here.
    return all;
  }
}
