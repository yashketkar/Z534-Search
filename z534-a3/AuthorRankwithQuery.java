package com.yashketkar.assignment3;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.MapTransformer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class AuthorRankwithQuery {

	public static String index = "D:\\assignment3\\author_index\\";
	public static String fileName = "D:\\assignment3\\author.net";
	public static Map<String, Double> authorMap;

	public static void computePriors(String queryString) {
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer();
			searcher.setSimilarity(new BM25Similarity());

			QueryParser parser = new QueryParser("content", analyzer);
			Query query = parser.parse(queryString);
//			System.out.println("Searching for: " + query.toString("content"));

			TopDocs results = searcher.search(query, 300);

			// Print number of hits
//			int numTotalHits = results.totalHits;
//			System.out.println(numTotalHits + " total matching documents");

			// Print retrieved results
			ScoreDoc[] hits = results.scoreDocs;

			authorMap = new HashMap<String, Double>();

			double priorSum = 0;
			for (int i = 0; i < hits.length; i++) {
//				System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
				Document doc = searcher.doc(hits[i].doc);
//				System.out.println("Paper ID: " + doc.get("paperid"));
//				System.out.println("Author ID: " + doc.get("authorid"));
				priorSum += hits[i].score;
				if (authorMap.containsKey(doc.get("authorid"))) {
					// key exists
					Double old = authorMap.get(doc.get("authorid"));
					double sum = hits[i].score + old.doubleValue();
					authorMap.put(doc.get("authorid"), new Double(sum));
				} else {
					// key does not exists
					authorMap.put(doc.get("authorid"), new Double(hits[i].score));
				}
				// System.out.println("Author Name: "+doc.get("authorName"));
				// System.out.println("Content: "+doc.get("content"));
			}
			// Get a set of the entries
			Set<Map.Entry<String, Double>> set = authorMap.entrySet();
			// Get an iterator
			Iterator<Map.Entry<String, Double>> i = set.iterator();
			// Display elements
			while (i.hasNext()) {
				Map.Entry<String, Double> me = (Map.Entry<String, Double>) i.next();
				double value = me.getValue().doubleValue();
				value /= priorSum;
				authorMap.put(me.getKey(), value);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {

		String queryString = "Data Mining";
		computePriors(queryString);

		// AtomicReader atomicReader;
		// atomicReader = SlowCompositeReaderWrapper.wrap(reader);
		// FieldInfos f = atomicReader.getFieldInfos();
		// Iterator<FieldInfo> mi = f.iterator();
		//
		// while(mi.hasNext()) {
		// FieldInfo fi = (FieldInfo)mi.next();
		// System.out.println(fi.name);
		// }

		DirectedSparseGraph<String, String> graph = new DirectedSparseGraph<String, String>();

		try (Scanner scanner = new Scanner(new File(fileName))) {

			Map<String, String> verticesMap = new HashMap<String, String>();

			String firstLine = scanner.nextLine();
			String[] firstLineWords = firstLine.split("\\s+");
			int numofVertices = Integer.parseInt(firstLineWords[firstLineWords.length - 1]);
			for (int i = 0; i < numofVertices; i++) {
				String s = scanner.nextLine();
				String[] split = s.split("\\s+");
				verticesMap.put(split[0], split[1].substring(1, split[1].length()-1));
				graph.addVertex(split[1].substring(1, split[1].length()-1));
				if(authorMap.containsKey(split[1].substring(1, split[1].length()-1)) == false)
				{
					authorMap.put(split[1].substring(1, split[1].length()-1), new Double(0.0));
				}
			}

			Map<String, String> edgesMap = new HashMap<String, String>();

			String secondLine = scanner.nextLine();
			String[] secondLineWords = secondLine.split("\\s+");
			int numofEdges = Integer.parseInt(secondLineWords[secondLineWords.length - 1]);
			for (int i = 0; i < numofEdges; i++) {
				String s = scanner.nextLine();
				String[] split = s.split("\\s+");
				// System.out.println(split[0]+" " + split[1] + " " + split[2]);
				edgesMap.put(verticesMap.get(split[0]), verticesMap.get(split[1]));
				Pair<String> p = new Pair<String>(verticesMap.get(split[0]), verticesMap.get(split[1]));
				graph.addEdge(Integer.toString(i), p, EdgeType.DIRECTED);
			}

			double alpha = 0.1;
//			PageRank<String, String> ranker = new PageRank<String,String>(graph, alpha);
			Transformer<String, Double> authorMapTransformer = MapTransformer.getInstance(authorMap);			
			PageRankWithPriors<String, String> ranker = new PageRankWithPriors<String, String>(graph,
					authorMapTransformer, alpha);
			ranker.evaluate();

			Map<String, Double> result = new HashMap<String, Double>();
			for (String v : graph.getVertices()) {
				result.put(v, ranker.getVertexScore(v));
			}

			// Sort the results by descending order.
			result = sortByValue(result);
			// Get a set of the entries
			Set<Map.Entry<String, Double>> set = result.entrySet();
			// Get an iterator
			Iterator<Map.Entry<String, Double>> i = set.iterator();
			// Display elements
			// while(i.hasNext()) {
			System.out.println("The top 10 ranked authors for query \"" + queryString +  "\" are: ");
			System.out.println("Author ID\tPage Rank Score");
			for (int j = 0; j < 10; j++) {
				Map.Entry<String, Double> me = (Map.Entry<String, Double>) i.next();
				System.out.println(me.getKey()
						/* + "\t\t" + verticesMap.get(me.getKey()).toString() */ + "\t\t" + me.getValue());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
}
