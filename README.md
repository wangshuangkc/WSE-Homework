 # WSE-Homework
Homework for Web Search Engine Course


Compile:
<pre>javac -cp lib/jsoup-1.10.1.jar src/edu/nyu/cs/cs2580/*.java</pre>

Mining Phase:
<pre>java -cp src:lib/jsoup-1.10.1.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine --mode=mining</pre> --options=conf/engine.conf

Index Phase:
<pre>java -cp src:lib/jsoup-1.10.1.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine --mode=index --options=conf/engine.conf</pre>

Start Server:
<pre>java -cp src:lib/jsoup-1.10.1.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine --mode=serve --port=25801 --options=conf/engine.conf</pre>

Run Query:
<pre>curl 'localhost:25801/search?query=%22web%20search%22&ranker=comprehensive'</pre>

Run Query Expansion:
<pre>
rm -f prf*.tsv
i=0
while read q; do
  i=$((i + 1));
  prfout=prf-$i.tsv;
  http://localhost:25801/prf?query=$q&ranker=comprehensive&numdocs=10&numterms=5 > $prfout;
  echo $q:$prfout >> prf.tsv
done < queries.tsv
java -cp src edu.nyu.cs.cs2580.Bhattacharyya prf.tsv qsim.tsv </pre>
