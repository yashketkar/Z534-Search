package com.yashketkar.assignment1;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class generateIndex {

	public static List<MyDocument> docs;
	public static IndexWriter writer;

	public static void main(String[] args) {
		generateIndex g = new generateIndex();
		Analyzer analyzer=new StandardAnalyzer();
		g.createIndex(analyzer,"Standard Analyzer");
		g.printResults();
	}

	public void readDirectory() {
		try (Stream<Path> paths = Files.walk(Paths.get("D:\\corpus"))) {
			paths.forEach(filePath -> {
				if (Files.isRegularFile(filePath)) {
					docs = new ArrayList<>();
					readFile(filePath.toString());
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readFile(String filePath) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {

				boolean bdocno = false;
				boolean bhead = false;
				boolean bbyline = false;
				boolean bdateline = false;
				boolean btext = false;

				MyDocument currentMyDocument;

				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					// System.out.println("Start Element :" + qName);
					if (qName.equalsIgnoreCase("DOC")) {
						currentMyDocument = new MyDocument();
					}
					if (qName.equalsIgnoreCase("DOCNO")) {
						bdocno = true;
					}
					if (qName.equalsIgnoreCase("HEAD")) {
						bhead = true;
					}
					if (qName.equalsIgnoreCase("BYLINE")) {
						bbyline = true;
					}
					if (qName.equalsIgnoreCase("DATELINE")) {
						bdateline = true;
					}
					if (qName.equalsIgnoreCase("TEXT")) {
						btext = true;
					}
				}

				public void endElement(String uri, String localName, String qName) throws SAXException {
					// System.out.println("End Element :" + qName);
					if (qName.equalsIgnoreCase("DOC")) {
						docs.add(currentMyDocument);
					}
					if (qName.equalsIgnoreCase("DOCNO")) {
						bdocno = false;
					}
					if (qName.equalsIgnoreCase("HEAD")) {
						bhead = false;
					}
					if (qName.equalsIgnoreCase("BYLINE")) {
						bbyline = false;
					}
					if (qName.equalsIgnoreCase("DATELINE")) {
						bdateline = false;
					}
					if (qName.equalsIgnoreCase("TEXT")) {
						btext = false;
					}
				}

				public void characters(char ch[], int start, int length) throws SAXException {
					if (bdocno) {
						String docno = new String(ch, start, length);
						currentMyDocument.setDocno(docno);
					}
					if (bhead) {
						String head = new String(ch, start, length);
						currentMyDocument.setHead(head);
					}
					if (bbyline) {
						String byline = new String(ch, start, length);
						currentMyDocument.setByline(byline);
					}
					if (bdateline) {
						String dateline = new String(ch, start, length);
						currentMyDocument.setDateline(dateline);
					}
					if (btext) {
						String text = new String(ch, start, length);
						currentMyDocument.setText(text);
					}
				}
			};
			saxParser.parse(new InputSource(new StringReader(
					new String("<xml>" + new String(Files.readAllBytes(Paths.get(filePath))) + "</xml>").replaceAll("&",
							"&amp;"))),
					handler);
			for (MyDocument d : docs) {
				Document lDoc = new Document();
				if (d.getDocno() != null) {
					lDoc.add(new StringField("DOCNO", d.getDocno(), Field.Store.YES));
				}
				if (d.getHead() != null) {
					lDoc.add(new TextField("HEAD", d.getHead(), Field.Store.YES));
				}
				if (d.getByline() != null) {
					lDoc.add(new TextField("BYLINE", d.getByline(), Field.Store.YES));
				}
				if (d.getDateline() != null) {
					lDoc.add(new TextField("DATELINE", d.getDateline(), Field.Store.YES));
				}
				if (d.getText() != null) {
					lDoc.add(new TextField("TEXT", d.getText(), Field.Store.YES));
				}
				writer.addDocument(lDoc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public  void createIndex(Analyzer analyzer, String type) {
		System.out.println("\nCreating Index for: " + type);
		try {
			String indexPath = "D:\\index";
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(OpenMode.CREATE);
			writer = new IndexWriter(dir, iwc);
			readDirectory();
			writer.forceMerge(1);
			writer.commit();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printResults() {
		try {
			String indexPath = "D:\\index";
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			// Print the total number of documents in the corpus
			System.out.println("Total number of documents in the corpus:" + reader.maxDoc());
			// Print the number of documents containing the term "new" in
			// <field>TEXT</field>.
			System.out.println("Number of documents containing the term \"new\" for field \"TEXT\": "
					+ reader.docFreq(new Term("TEXT", "new")));
			// Print the total number of occurrences of the term "new" across
			// all documents for <field>TEXT</field>.
			System.out.println("Number of occurrences of \"new\" in the field \"TEXT\": "
					+ reader.totalTermFreq(new Term("TEXT", "new")));
			Terms vocabulary = MultiFields.getTerms(reader, "TEXT");
			// Print the size of the vocabulary for <field>TEXT</field>,
			// applicable when the index has only one segment.
			System.out.println("Size of the vocabulary for this field: " + vocabulary.size());
			// Print the total number of documents that have at least one term
			// for <field>TEXT</field>
			System.out.println(
					"Number of documents that have at least one term for this field: " + vocabulary.getDocCount());
			// Print the total number of tokens for <field>TEXT</field>
			System.out.println("Number of tokens for this field: " + vocabulary.getSumTotalTermFreq());
			// Print the total number of postings for <field>TEXT</field>
			System.out.println("Number of postings for this field: " + vocabulary.getSumDocFreq());
			// // Print the vocabulary for <field>TEXT</field>
			// TermsEnum iterator = vocabulary.iterator();
			// BytesRef byteRef = null;
			// System.out.println("\n*******Vocabulary-Start**********");
			// while((byteRef = iterator.next()) != null) {
			// String term = byteRef.utf8ToString();
			// System.out.print(term+"\t");
			// }
			// System.out.println("\n*******Vocabulary-End**********");
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class MyDocument {

	// public String doc;
	// public String fileid;
	// public String first;
	// public String second;

	public String docno;
	public String head;
	public String byline;
	public String dateline;
	public String text;

	/**
	 * @return the docno
	 */
	public String getDocno() {
		return docno;
	}

	/**
	 * @param docno
	 *            the docno to set
	 */
	public void setDocno(String docno) {
		if (this.docno == null) {
			this.docno = docno;
		} else {
			this.docno += docno;
		}
	}

	/**
	 * @return the head
	 */
	public String getHead() {
		return head;
	}

	/**
	 * @param head
	 *            the head to set
	 */
	public void setHead(String head) {
		if (this.head == null) {
			this.head = head;
		} else {
			this.head += head;
		}
	}

	/**
	 * @return the byline
	 */
	public String getByline() {
		return byline;
	}

	/**
	 * @param byline
	 *            the byline to set
	 */
	public void setByline(String byline) {
		if (this.byline == null) {
			this.byline = byline;
		} else {
			this.byline += byline;
		}
	}

	/**
	 * @return the dateline
	 */
	public String getDateline() {
		return dateline;
	}

	/**
	 * @param dateline
	 *            the dateline to set
	 */
	public void setDateline(String dateline) {
		if (this.dateline == null) {
			this.dateline = dateline;
		} else {
			this.dateline += dateline;
		}

	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		if (this.text == null) {
			this.text = text;
		} else {
			this.text += text;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MyDocument [docno=" + docno + ", head=" + head + ", byline=" + byline + ", dateline=" + dateline
				+ ", text=" + text + "]";
	}

}
