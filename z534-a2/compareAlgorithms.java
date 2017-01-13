package com.yashketkar.assignment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class compareAlgorithms {

	public static final String INDEX_DIR_PATH = "D:\\A2\\index";
	public static final String OUTPUT_DIR = "D:\\A2\\Output\\";
	public static final String QUERY_TITLE = "title";
	public static final String QUERY_DESC = "description";
	public static final String FILE_PATH = "D:\\A2\\topics.51-100";

	public static void saveTopDocs(TopDocs topDocs, IndexSearcher indexSearcher, String queryID, String outputFilePath)
			throws IOException {
		File outputFile = new File(outputFilePath);
		outputFile.getParentFile().mkdirs();
		if (outputFile.exists() == false) {
			outputFile.createNewFile();
		}
		FileWriter fileWriter = new FileWriter(outputFile, true);

		ScoreDoc[] scoreDocs = topDocs.scoreDocs;

		for (int docIndex = 0; docIndex < scoreDocs.length; docIndex++) {
			ScoreDoc scoreDoc = scoreDocs[docIndex];
			String docNo = indexSearcher.doc(scoreDoc.doc).get("DOCNO");
			fileWriter.append(queryID);
			fileWriter.append(" " + "Q0");
			fileWriter.append(" " + docNo);
			fileWriter.append(" " + (docIndex + 1));
			fileWriter.append(" " + scoreDoc.score);
			fileWriter.append(" " + "run-1 \n");
		}
		fileWriter.flush();
		fileWriter.close();
	}

	public static void topThousandResults(Similarity similarity, String algorithmName) throws Exception {
		TrecTopicsReader trecTopicReader = new TrecTopicsReader();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(FILE_PATH));
		QualityQuery[] qualityQueries = trecTopicReader.readQueries(bufferedReader);

		IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR_PATH)));
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		StandardAnalyzer analyzer = new StandardAnalyzer();
		indexSearcher.setSimilarity(similarity);

		QueryParser queryParser = new QueryParser("TEXT", analyzer);

		for (int queryIndex = 0; queryIndex < qualityQueries.length; queryIndex++) {
			QualityQuery qualityQuery = qualityQueries[queryIndex];
			String queryID = qualityQuery.getQueryID();
			{
				String titleStringQuery = qualityQuery.getValue(QUERY_TITLE);
				String cleanedTitleQuery = searchTRECtopics.updateTitleQueryString(titleStringQuery);
				Query titleQuery = queryParser.parse(QueryParserUtil.escape(cleanedTitleQuery));
				TopDocs topDocs = indexSearcher.search(titleQuery, 1000);
				String outputFilePath = OUTPUT_DIR + "/" + algorithmName + "ShortQuery" + ".txt";
				saveTopDocs(topDocs, indexSearcher, queryID, outputFilePath);

			}
			{
				String descStringQuery = qualityQuery.getValue(QUERY_DESC);
				String cleanedDescQuery = searchTRECtopics.cleanDescQueryString(descStringQuery);
				Query descQuery = queryParser.parse(QueryParserUtil.escape(cleanedDescQuery));
				TopDocs topDocs = indexSearcher.search(descQuery, 1000);
				String outputFilePath = OUTPUT_DIR + "/" + algorithmName + "LongQuery" + ".txt";
				saveTopDocs(topDocs, indexSearcher, queryID, outputFilePath);
			}
		}
		System.out.println("All the queries from TREC 51-100 executed successfully for - " + algorithmName);
	}

	public static void main(String[] args) {
		try {
			topThousandResults(new ClassicSimilarity(), "DEFAULT_VECTOR");
			topThousandResults(new BM25Similarity(), "BM25");
			topThousandResults(new LMDirichletSimilarity(), "LMDirichlet");
			topThousandResults(new LMJelinekMercerSimilarity((float) 0.7), "LMJelinek");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
