package com.yashketkar.assignment2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class QueryRelevanceScore {

	public List<QueryTermScores> queryTermScoresList;
	public Set<String> relevantDocumentIDs;
	private String queryID;

	public QueryRelevanceScore(String queryID) {
		this.queryID = queryID;
		queryTermScoresList = new ArrayList<QueryTermScores>();
		relevantDocumentIDs = new HashSet<String>();
	}

	public void addQueryTermScores(QueryTermScores queryTermScores) {
		queryTermScoresList.add(queryTermScores);
	}

	public double getDocumentScore(String docNo) {
		double documentScoreForQuery = 0;

		for (QueryTermScores queryTermScores : queryTermScoresList) {
			documentScoreForQuery += queryTermScores.getDocumentScore(docNo);
		}

		return documentScoreForQuery;
	}

	public void addRelevantDocument(String docNO) {
		relevantDocumentIDs.add(docNO);
	}

	public Map<String, Double> getDocumentIdToScoreMap() {
		Map<String, Double> documentScoreMap = new HashMap<String, Double>();

		for (String docNo : relevantDocumentIDs) {
			double score = this.getDocumentScore(docNo);
			documentScoreMap.put(docNo, score);
		}

		return documentScoreMap;
	}

	public void saveTopThousandResults(String outputFilePath) throws IOException {
		Map<String, Double> documentIdToScoreMap = this.getDocumentIdToScoreMap();

		File outputFile = new File(outputFilePath);
		outputFile.getParentFile().mkdirs();
		if (outputFile.exists() == false) {
			outputFile.createNewFile();
		}
		FileWriter fileWriter = new FileWriter(outputFile, true);

		ScoreConsumer scoreConsumer = new ScoreConsumer(fileWriter, this.queryID);

		documentIdToScoreMap.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.limit(1000).forEachOrdered(scoreConsumer);

		fileWriter.flush();
		fileWriter.close();
	}
}

class ScoreConsumer implements Consumer<Map.Entry<String, Double>> {
	private FileWriter fileWriter;
	private String queryID;
	private int documentRank;

	public ScoreConsumer(FileWriter fileWriter, String queryID) {
		this.fileWriter = fileWriter;
		this.queryID = queryID;
		this.documentRank = 1;
	}

	@Override
	public void accept(Entry<String, Double> entry) {
		try {
			fileWriter.append(this.queryID);
			fileWriter.append(" " + "Q0");
			fileWriter.append(" " + entry.getKey());
			fileWriter.append(" " + documentRank);
			fileWriter.append(" " + entry.getValue());
			fileWriter.append(" " + "run-1 \n");
			documentRank++;
		} catch (IOException e) {
			System.out.println("Unable to write- " + entry.getKey());
			e.printStackTrace();
		}
	}
}

class QueryTermScores {
	public Map<String, Double> documentIdToScoreMap;

	public QueryTermScores(String queryTerm) {
		documentIdToScoreMap = new HashMap<String, Double>();
	}

	public void addDocumentScore(String docNO, double score) {
		documentIdToScoreMap.put(docNO, score);
	}

	public double getDocumentScore(String docNo) {
		double documentScore = 0;
		if (documentIdToScoreMap.containsKey(docNo)) {
			documentScore = documentIdToScoreMap.get(docNo);
		}
		return documentScore;
	}
}