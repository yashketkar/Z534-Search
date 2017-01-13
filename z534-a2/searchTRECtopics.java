package com.yashketkar.assignment2;

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class searchTRECtopics {

	public static final String OUTPUT_DIR = "D:\\A2\\Output\\";
	public static final String QUERY_TITLE = "title";
	public static final String QUERY_DESC = "description";
	public static final String FILE_PATH = "D:\\A2\\topics.51-100";

	public static void topThousandResults(Similarity similarity, String algorithmName) throws Exception {
		TrecTopicsReader trecTopicReader = new TrecTopicsReader();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(FILE_PATH));
		QualityQuery[] qualityQueries = trecTopicReader.readQueries(bufferedReader);

		for (int queryIndex = 0; queryIndex < qualityQueries.length; queryIndex++) {
			QualityQuery qualityQuery = qualityQueries[queryIndex];
			String queryID = qualityQuery.getQueryID();
			{
				String titleQuery = qualityQuery.getValue(QUERY_TITLE);
				String cleanedTitleQuery = updateTitleQueryString(titleQuery);
				QueryRelevanceScore titleQueryScore = easySearch.getQueryRelevanceScores(cleanedTitleQuery, queryID,
						similarity);
				String OutputFilePath = OUTPUT_DIR + "/" + algorithmName + "ShortQuery" + ".txt";
				titleQueryScore.saveTopThousandResults(OutputFilePath);
			}
			{
				String descQuery = qualityQuery.getValue(QUERY_DESC);
				String cleanedDescQuery = cleanDescQueryString(descQuery);
				QueryRelevanceScore descQueryScore = easySearch.getQueryRelevanceScores(cleanedDescQuery, queryID,
						similarity);
				String OutputFilePath = OUTPUT_DIR + "/" + algorithmName + "LongQuery" + ".txt";
				descQueryScore.saveTopThousandResults(OutputFilePath);
			}
		}
		System.out.println("All the queries from TREC 51-100 executed successfully.");
	}

	public static String updateTitleQueryString(String queryString) {
		String cleanedQuery = null;
		int colonIndex = queryString.indexOf(":");
		cleanedQuery = queryString.substring(colonIndex + 1, queryString.length());
		return cleanedQuery;
	}

	public static String cleanDescQueryString(String queryString) {
		String cleanedQuery = null;
		int smryIndex = queryString.indexOf("<smry>");
		if (smryIndex != -1) {
			cleanedQuery = queryString.substring(0, smryIndex);
		}
		return cleanedQuery;
	}

	public static void main(String[] args) {
		try {
			Similarity defaultSimilarity = new ClassicSimilarity();
			topThousandResults(defaultSimilarity, "MYALGO");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
