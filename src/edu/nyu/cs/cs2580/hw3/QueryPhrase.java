package edu.nyu.cs.cs2580.hw3;

import java.util.Scanner;
import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public Vector<String> _phrase = new Vector<>();
  
  public QueryPhrase(String query) {
    super(query);
  }
  
  @Override
  public void processQuery() {
    if (_query == null) {
      return;
    }
    
    Scanner s = new Scanner(_query);
    StringBuffer sb = new StringBuffer();
    while (s.hasNext())
    {
      String current = s.next().trim();
      String str = null;
      if(current.startsWith("\"")) {
        str = Helper.porterStem(current.replace("\"", ""));
        sb.append(str + " ");
      } else if(current.endsWith("\"")){
        str = Helper.porterStem(current.replace("\"", ""));
        sb.append(current.replace("\"", ""));
        _phrase.add(sb.toString());
        sb = new StringBuffer();
      } else if (sb.length() > 0){
        str = Helper.porterStem(current);
        sb.append(" " + current + " ");
      } else {
        str = current;
      }
      _tokens.add(str);
    }
    System.out.println(_tokens.toString());
    System.out.println(_phrase.toString());
    s.close();
  }

  public static void main(String[] args) {
    QueryPhrase qp = new QueryPhrase("\"new york\" film");
    qp.processQuery();
  }
}
