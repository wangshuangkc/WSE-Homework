package edu.nyu.cs.cs2580;

import java.util.*;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  
  public DocumentIndexed(int docid) {
    super(docid);
  }
  
  // todo may not allow to use this
  private Map<Integer, Integer> _uniqueTerms = new HashMap<>();
  
  private int _docTotalTerms = 0;
  
  public void setDocTotalTerms(int docTotalTerms) {
    _docTotalTerms = docTotalTerms;
  }
  
  public int getDocTotalTerms() {
    return _docTotalTerms;
  }
}
