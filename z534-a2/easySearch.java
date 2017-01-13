package com.yashketkar.assignment2;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class easySearch {

	public static final String INDEX_DIR_PATH = "D:\\A2\\index";

	public static QueryRelevanceScore getQueryRelevanceScores(String queryString, String queryID, Similarity similarity)
			throws Exception {
		IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR_PATH)));
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
		indexSearcher.setSimilarity(similarity);
		QueryParser queryParser = new QueryParser("TEXT", standardAnalyzer);
		Query query = queryParser.parse(QueryParserUtil.escape(queryString));
		Set<Term> queryTermSet = new HashSet<Term>();
		query.createWeight(indexSearcher, false).extractTerms(queryTermSet);
		QueryRelevanceScore queryScore = new QueryRelevanceScore(queryID);
		List<LeafReaderContext> leafReaderContexts = indexReader.getContext().reader().leaves();
		for (Term queryTerm : queryTermSet) {
			int documentFrequencyForTerm = indexReader.docFreq(queryTerm);
			QueryTermScores queryTermScore = new QueryTermScores(queryTerm.text());
			for (LeafReaderContext leafReaderContext : leafReaderContexts) {
				PostingsEnum posting = MultiFields.getTermDocsEnum(leafReaderContext.reader(), "TEXT",
						new BytesRef(queryTerm.text()));
				if (posting != null) {
					while (posting.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
						int termFreqInDocument = posting.freq();
						int documentID = posting.docID() + leafReaderContext.docBase;
						String docNO = indexSearcher.doc(documentID).get("DOCNO");
						double normDocLeng = ((ClassicSimilarity) similarity)
								.decodeNormValue(leafReaderContext.reader().getNormValues("TEXT").get(posting.docID()));
						double documentLength = 1 / (normDocLeng * normDocLeng);
						double relevanceScoreForTerm = getScoreForTerm(termFreqInDocument, documentLength,
								documentFrequencyForTerm, indexReader.maxDoc());
						queryTermScore.addDocumentScore(docNO, relevanceScoreForTerm);
						queryScore.addRelevantDocument(docNO);
					}
				}
			}
			queryScore.addQueryTermScores(queryTermScore);
		}
		indexReader.close();
		return queryScore;
	}

	public static void main(String[] args) {
		try {
			Scanner sc = new Scanner(System.in);
			System.out.println("Enter Query String");
			String queryString = sc.nextLine();
			sc.close();
			String queryID = "1";
			ClassicSimilarity dSimi = new ClassicSimilarity();
			QueryRelevanceScore queryScore = getQueryRelevanceScores(queryString, queryID, dSimi);
			for (String docNo : queryScore.relevantDocumentIDs) {
				System.out.println("DocumentID: " + docNo + "     Score:" + queryScore.getDocumentScore(docNo));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static double getScoreForTerm(double termCountInDoc, double docLength, double termDocCount,
			double totalDocCount) {
		return (termCountInDoc / docLength) * Math.log(1 + (totalDocCount / termDocCount));
	}
}