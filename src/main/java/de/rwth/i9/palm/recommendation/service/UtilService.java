package de.rwth.i9.palm.recommendation.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openide.util.Lookup;

/**
 * Master Thesis at The Learning Technologies Research Group (LuFG Informatik 9,
 * RWTH Aachen University), Year 2014 - 2015
 * 
 * This consists of methods that is general between all recommendation
 * algorithms.
 * 
 * 
 * @author Peyman Toreini
 * @version 1.1
 */
public class UtilService {
	/**
	 * It finds an ID of a researcher Name
	 * 
	 * @param researcherName
	 * @return
	 */
	public static int getID(String researcherName) {
		int researcherID = 0;
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			// 3.execute query
			ResultSet myRs = myStmt
					.executeQuery("SELECT id FROM `authors` WHERE name='"
							+ researcherName + "'");
			while (myRs.next()) {
				researcherID = myRs.getInt("id");
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return researcherID;
	}

	/**
	 * 
	 * IT finds all ID of the researchers
	 * @return
	 */
	public static ArrayList<Integer> findAllIDs() {
		ArrayList<Integer> AllIDs = new ArrayList<Integer>();
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			// 3.execute query
			ResultSet myRs = myStmt.executeQuery("SELECT id FROM `authors`");
			while (myRs.next()) {
				AllIDs.add(myRs.getInt("id"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return AllIDs;
	}

	/**
	 * It finds a name of a researcher ID
	 * 
	 * @param rID
	 * @return
	 */
	public static String getNamefor1ID(Integer rID) {
		String rName = "";
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			// 3.execute query
			ResultSet myRs = myStmt
					.executeQuery("SELECT name FROM `authors` WHERE id IN ("
							+ rID + ");");
			while (myRs.next()) {
				rName = myRs.getString("name");
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return rName;
	}

	/**
	 * 
	 * It creates Interest ID Table
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Integer> interestIDTableMap() throws SQLException {
		Map<String, Integer> interestIDTableMap = new TreeMap<String, Integer>(
				String.CASE_INSENSITIVE_ORDER);
		// 1.get the connection to db
		Connection myConn = DbService.ConnectToDB();
		// 2.create statement
		Statement myStmt = myConn.createStatement();
		ResultSet myRs2 = myStmt
				.executeQuery("SELECT interestID,interestName FROM interestsid");
		// save interest table result in a map which is a key-value variable
		while (myRs2.next()) {
			interestIDTableMap.put(myRs2.getString("interestName"),
					myRs2.getInt("interestID"));
		}
		return interestIDTableMap;
	}

	/**
	 * It finds id for 1 interest
	 * 
	 * @param recievedMap
	 * @param iName
	 * @return
	 */
	public static Integer get1InterestID(Map<String, Integer> recievedMap,
			String iName) {
		Map<String, Integer> palmResearchers = recievedMap;
		Integer iID = null;
		for (Entry<String, Integer> entry : palmResearchers.entrySet()) {
			if (entry.getKey().equals(iName)) {
				iID = entry.getValue();
			}
		}
		return iID;
	}

	/**
	 * It finds interest of 1 user
	 * 
	 * @param researcherID
	 * @return
	 */
	public static ArrayList<String> getInterestOf1User(Integer researcherID) {
		ArrayList<String> iListof1User = new ArrayList<String>();
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("select DISTINCT authorid,result,score from results WHERE authorid IN ("
							+ researcherID + ")ORDER BY score DESC;");
			// Save interest table result in a map which is a key-value variable
			while (myRs.next()) {
				iListof1User.add(myRs.getString("result"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return iListof1User;
	}

	/**
	 * It finds interest of 1 user
	 * 
	 * @param researcherID
	 * @return
	 */
	public static ArrayList<String> getUniqueInterestNameOf1User(
			Integer researcherID) {
		ArrayList<String> iListof1User = new ArrayList<String>();
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("select DISTINCT result from results WHERE authorid='"
							+ researcherID + "'");
			// save interest table result in a map which is a key-value variable
			while (myRs.next()) {
				iListof1User.add(myRs.getString("result"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return iListof1User;
	}
	public static ArrayList<String> getUniqueInterestNameOf1User(
			String researcherID) {
		ArrayList<String> iListof1User = new ArrayList<String>();
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("select DISTINCT result from results WHERE authorid='"
							+ researcherID + "'");
			// save interest table result in a map which is a key-value variable
			while (myRs.next()) {
				iListof1User.add(myRs.getString("result"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return iListof1User;
	}
	/**
	 * It finds interest of 1 user which user ID is Long Type It uses to find
	 * Interests of users in collaborative filtering method
	 * 
	 * @param researcherID
	 * @return
	 */
	public static ArrayList<String> getInterestOf1UserForLong(long researcherID) {
		ArrayList<String> iListof1User = new ArrayList<String>();
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("select authorid,result,score from results WHERE authorid IN ("
							+ researcherID + ")ORDER BY score DESC;");
			// save interest table result in a map which is a key-value variable
			while (myRs.next()) {
				iListof1User.add(myRs.getString("result"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return iListof1User;
	}

	/**
	 * It finds interest of 1 user which user ID is Long Type It uses to find
	 * Interests of users in collaborative filtering method
	 * 
	 * @param researcherID
	 * @return
	 */
	public static ArrayList<String> getUniqueInterestNameOf1UserForLong(
			long researcherID) {
		ArrayList<String> iListof1User = new ArrayList<String>();
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("select DISTINCT result from results WHERE authorid IN ("
							+ researcherID + ");");
			// save interest table result in a map which is a key-value variable
			while (myRs.next()) {
				iListof1User.add(myRs.getString("result"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return iListof1User;
	}

	/**
	 * 
	 * This method finds the pearson similarity degree of the 2 users in PALM
	 * 
	 * @param researcherID
	 * @param userID
	 * @param model
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 * @throws TasteException
	 */
	public static double ComputePearsonSimDegree(int researcherID, int userID,
			DataModel model) throws SQLException, IOException, TasteException {
		UserSimilarity similarity = null;
		try {
			similarity = new PearsonCorrelationSimilarity(model);
		} catch (TasteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Returns the degree of similarity, of two users, based on the their
		// preferences.
		double pSimDegree = similarity.userSimilarity(researcherID, userID);
		// System.out.println("Pearson Sim. degree is : " + pSimDegree);
		return pSimDegree;
	}

	/**
	 * 
	 * Make file for Collaborative Filtering based on all data
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void makeFileForCFbasedAllData() throws SQLException,
			IOException {
		String fileName = "data/PalmDatasetForGeneralCF.csv";
		try {
			Connection myConn = DbService.ConnectToDB();
			Statement myStmt = myConn.createStatement();
			Statement myStmt1 = myConn.createStatement();
			ResultSet allInterestData = myStmt1
					.executeQuery("SELECT interestID,interestName FROM interestsid");
			// save interest table result in a map which is a key-value variable
			// use Tree and Case insensitive because I have some education and
			// Education in DB
			Map<String, Integer> interestIDTableMap = new TreeMap<String, Integer>(
					String.CASE_INSENSITIVE_ORDER);
			while (allInterestData.next()) {
				interestIDTableMap.put(
						allInterestData.getString("interestName"),
						allInterestData.getInt("interestID"));
			}
			ResultSet myRs = myStmt
					.executeQuery("select authorid,result,score from results");
			FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			// 4.process the result
			while (myRs.next()) {
				// I check Null because I have 4 null record on my file,mahout
				// does not work with null
				if (myRs.getString("authorid") != null
						&& interestIDTableMap.get(myRs.getString("result")) != null) {
					bufferedWriter.write(myRs.getString("authorid") + ","
							+ interestIDTableMap.get(myRs.getString("result"))
							+ "," + myRs.getString("score"));
					bufferedWriter.newLine();
				}
			}
			bufferedWriter.close();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * @param iName
	 * @return
	 */
	public static Integer getInterestID(String iName) {
		Integer ItemID = 0;
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("SELECT interestID FROM interestsid WHERE interestName='"
							+ iName + "';");
			// save interest table result in a map which is a key-value variable
			while (myRs.next()) {
				ItemID = myRs.getInt("interestID");
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return ItemID;
	}

	/**
	 * @param iID
	 * @return
	 */
	public static String getInterestName(Integer iID) {
		String ItemName = "";
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("SELECT interestName FROM interestsid WHERE interestID='"
							+ iID + "';");
			// save interest table result in a map which is a key-value variable
			while (myRs.next()) {
				ItemName = myRs.getString("interestName");
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return ItemName;
	}

	/**
	 * It removes last character of a text. Here it uses to remove "," in MySQL
	 * queries It uses to prepare list on WHERE .... IN parts of queries.
	 * 
	 * @param text
	 * @return
	 */
	public static String lastCharRemover(String text) {
		// remove last character of the string if it is ,
		if (text.length() > 0 && text.charAt(text.length() - 1) == ',') {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}

	/**
	 * @param rID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<Integer> getCoAuthorIDs(int rID)
			throws SQLException, IOException {
		ArrayList<Integer> rIDs = new ArrayList<Integer>();
		Connection myConn = DbService.ConnectToDB();
		String userPub = UtilService.userPublicationsFinder(rID);
		Statement myStmt = myConn.createStatement();
		// find user coAuthors
		// System.out.println("user id is:"+rID);
		// System.out.println("user pub is :" + userPub);
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				rIDs.add(coAuthors.getInt("authorid"));
			}
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return rIDs;
	}

	/**
	 * @param rID
	 * @return
	 * @throws SQLException
	 */
	public static String userPublicationsFinder(int rID) throws SQLException {
		Connection myConn = DbService.ConnectToDB();
		Statement myStmt = myConn.createStatement();
		// find user publication
		ResultSet userPub = myStmt
				.executeQuery("SELECT publicationid FROM `authorspublications` WHERE authorid='"
						+ rID + "'");
		String userPubResult = "";
		while (userPub.next()) {
			userPubResult += "\"" + userPub.getString("publicationid") + "\""
					+ ",";
		}
		userPubResult = lastCharRemover(userPubResult);
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return userPubResult;
	}

	/**
	 * @param rIDs
	 * @return
	 */
	public static Map<Integer, String> getName(ArrayList<Integer> rIDs) {
		String rIDQuery = "";
		Map<Integer, String> rIDNameMap = new TreeMap<Integer, String>();
		try {
			for (int i = 0; i < rIDs.size(); i++) {
				rIDQuery += "\"" + rIDs.get(i) + "\"" + ",";
			}
			rIDQuery = UtilService.lastCharRemover(rIDQuery);
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			// 3.execute query
			ResultSet myRs = myStmt
					.executeQuery("SELECT id,name FROM `authors` WHERE id IN ("
							+ rIDQuery + ");");
			while (myRs.next()) {
				rIDNameMap.put(myRs.getInt("id"), myRs.getString("name"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return rIDNameMap;
	}
	public static Map<String, String> getNames(ArrayList<String> rIDs) {
		String rIDQuery = "";
		Map<String, String> rIDNameMap = new TreeMap<String, String>();
		try {
			for (int i = 0; i < rIDs.size(); i++) {
				rIDQuery += "\"" + rIDs.get(i) + "\"" + ",";
			}
			rIDQuery = UtilService.lastCharRemover(rIDQuery);
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			// 3.execute query
			ResultSet myRs = myStmt
					.executeQuery("SELECT id,name FROM `authors` WHERE id IN ("
							+ rIDQuery + ");");
			while (myRs.next()) {
				rIDNameMap.put(myRs.getString("id"), myRs.getString("name"));
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return rIDNameMap;
	}

	/**
	 * @param Names
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public static String namesListForQuery(ArrayList<String> Names)
			throws JSONException, IOException {
		String coAuthorsNameStringForQuery = "";
		ArrayList<String> coAuthorsName = Names;
		// add '' and , to list of names
		for (int i = 0; i < coAuthorsName.size(); i++) {
			coAuthorsNameStringForQuery += "\"" + coAuthorsName.get(i) + "\""
					+ ",";
		}
		coAuthorsNameStringForQuery = lastCharRemover(coAuthorsNameStringForQuery);
		return coAuthorsNameStringForQuery;
	}

	/**
	 * @param namesForQuery
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public static String idsListForQuery(String namesForQuery)
			throws JSONException, IOException {
		String coAuthorID = "";
		// 1.get the connection to db
		Connection myConn;
		try {
			myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			// 3.execute query
			ResultSet myRs = myStmt
					.executeQuery("SELECT id,name FROM `authors` WHERE name IN ("
							+ namesForQuery + ");");
			while (myRs.next()) {
				coAuthorID += "\"" + myRs.getString("id") + "\"" + ",";
			}
			if (myStmt != null)
				myStmt.close();
			if (myConn != null)
				myConn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		coAuthorID = lastCharRemover(coAuthorID);
		return coAuthorID;
	}

	/**
	 * @param degreeResult2
	 * @return
	 */
	static Map<String, Integer> sortByComparator3(
			Map<String, Integer> degreeResult2) {
		// Convert Map to List
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
				degreeResult2.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
					Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it
				.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public static Map<String, Integer> sortByComparatorInt(
			Map<String, Integer> degreeResult2) {
		// Convert Map to List
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(
				degreeResult2.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
					Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it
				.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	/**
	 * @param pageRankResult2
	 * @return
	 */
	static Map<Integer, Float> sortByComparator2(
			Map<Integer, Float> pageRankResult2) {
		// Convert Map to List
		List<Map.Entry<Integer, Float>> list = new LinkedList<Map.Entry<Integer, Float>>(
				pageRankResult2.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<Integer, Float>>() {
			public int compare(Map.Entry<Integer, Float> o1,
					Map.Entry<Integer, Float> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<Integer, Float> sortedMap = new LinkedHashMap<Integer, Float>();
		for (Iterator<Map.Entry<Integer, Float>> it = list.iterator(); it
				.hasNext();) {
			Map.Entry<Integer, Float> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	/**
	 * @param pageRankResult2
	 * @return
	 */
	public static Map<Integer, Double> sortByIntComparator(
			Map<Integer, Double> pageRankResult2) {
		// Convert Map to List
		List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(
				pageRankResult2.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1,
					Map.Entry<Integer, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<Integer, Double> sortedMap = new LinkedHashMap<Integer, Double>();
		for (Iterator<Map.Entry<Integer, Double>> it = list.iterator(); it
				.hasNext();) {
			Map.Entry<Integer, Double> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	
	public static Map<String, Double> sortByComparator(
			Map<String, Double> pageRankResult2) {
		// Convert Map to List
		List<Map.Entry<String, Double>> list = new LinkedList<>( pageRankResult2.entrySet() );
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1,
					Map.Entry<String, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Double> sortedMap = new TreeMap<>( new Comparator<String>() {
			@Override
			public int compare( String o1, String o2 )
			{
				Comparable valueA = (Comparable) pageRankResult2.get(o1);
				Comparable valueB = (Comparable) pageRankResult2.get(o2);
				return valueB.compareTo(valueA);
			}
		} );
		sortedMap.putAll( pageRankResult2 );
		/*for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it
				.hasNext();) {
			Map.Entry<String, Double> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}*/
		return sortedMap;
	}
	
	public static Map<String, Double> sortByIDComparator(
			Map<String, Double> pageRankResult2 ) {
		// Convert Map to List
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(
				pageRankResult2.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1,
					Map.Entry<String, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Map.Entry<String, Double> entry : list ) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	/**
	 * it finds number of the nodes
	 * 
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	/*static int NumberofneighborsFinder(int RID) throws SQLException,
			IOException {
		UtilCFCo.SNA1dFileCreator(RID);
		ProjectController pc = Lookup.getDefault().lookup(
				ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();
		// Get graph model and attribute model of current workspace
		GraphModel graphModel = Lookup.getDefault()
				.lookup(GraphController.class).getModel();
		//AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
		ImportController importController = Lookup.getDefault().lookup(
				ImportController.class);
		// Import file
		Container container;
		File file = new File("data/SNA3dfile.csv");
		container = importController.importFile(file);
		container.getLoader().setEdgeDefault(EdgeDefault.UNDIRECTED); // Force
																		// DIRECTED
		// Append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);
		UndirectedGraph graph = graphModel.getUndirectedGraph();
		// get max possible degree of the graph to normalize degree value
		int MaxDegreeofaNode = graph.getNodeCount() - 1;
		return MaxDegreeofaNode;
	}*/

	/**
	 * Create JSON file for Degree
	 * 
	 * @param rDegree
	 * @param rIDName3
	 * @param maxDegreeofaNode
	 * @return
	 * @throws JSONException
	 * @throws TasteException
	 * @throws IOException
	 * @throws SQLException
	 */
	static JSONArray DegreeJsonCreator(Map<String, Integer> rDegree,
			Map<String, String> rIDName3, String researcherID,
			int maxDegreeofaNode, UtilINImp imp) throws JSONException, SQLException,
			IOException, TasteException {
		JSONArray recBasedDegree = new JSONArray();
		for (Entry<String, Integer> entry : rDegree.entrySet()) {
			int NofcommonI = NofCommonInterests(researcherID, entry.getKey());
			double JaccardSim = UtilService.FindJaccardSimilarity(researcherID,
					entry.getKey(), imp);
			// show double till only 2 decimal
			double NormDegree = ((double) entry.getValue() / maxDegreeofaNode);
			NormDegree = Math.round(NormDegree * 100);
			NormDegree = NormDegree / 100;
			JSONObject obj = new JSONObject();
			obj.put("rID", entry.getKey());
			obj.put("Name", rIDName3.get(entry.getKey()));
			obj.put("DegreeValue", NormDegree);
			obj.put("NofCommonInterest", NofcommonI);
			obj.put("SimInPercent", Math.round(JaccardSim * 100));
			recBasedDegree.put(obj);
		}
		// System.out.println(recBasedDegree);
		return recBasedDegree;
	}

	/**
	 * @param rIDCloseness
	 * @param rIDName
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws TasteException
	 * @throws IOException
	 * @throws SQLException
	 */
	static JSONArray ClosenessJsonCreator(Map<Integer, Double> rIDCloseness,
			Map<Integer, String> rIDName, int researcherID)
			throws JSONException, SQLException, IOException, TasteException {
		JSONArray recBasedCloseness = new JSONArray();
		for (Entry<Integer, Double> entry : rIDCloseness.entrySet()) {
			int NofcommonI = NofCommonInterests(researcherID, entry.getKey());
			double JaccardSim = UtilService.FindJaccardSimilarity(researcherID,
					entry.getKey());
			JSONObject obj = new JSONObject();
			obj.put("rID", entry.getKey());
			obj.put("Name", rIDName.get(entry.getKey()));
			obj.put("ClosenessValue", entry.getValue());
			obj.put("NofCommonInterest", NofcommonI);
			obj.put("SimInPercent", Math.round(JaccardSim * 100));
			recBasedCloseness.put(obj);
		}
		return recBasedCloseness;
	}

	/**
	 * Create JSON file for EigenVector
	 * 
	 * @param rIDEigenvector
	 * @param rIDName
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws TasteException
	 * @throws IOException
	 * @throws SQLException
	 */
	public static JSONArray EigenvectorJsonCreator(
			Map<Integer, Double> rIDEigenvector, Map<Integer, String> rIDName,
			int researcherID) throws JSONException, SQLException, IOException,
			TasteException {
		JSONArray recBasedEigenvector = new JSONArray();
		for (Entry<Integer, Double> entry : rIDEigenvector.entrySet()) {
			int NofcommonI = NofCommonInterests(researcherID, entry.getKey());
			double JaccardSim = UtilService.FindJaccardSimilarity(researcherID,
					entry.getKey());
			JSONObject obj = new JSONObject();
			obj.put("rID", entry.getKey());
			obj.put("Name", rIDName.get(entry.getKey()));
			obj.put("EigenVectorValue", entry.getValue());
			obj.put("NofCommonInterest", NofcommonI);
			obj.put("SimInPercent", Math.round(JaccardSim * 100));
			recBasedEigenvector.put(obj);
		}
		// System.out.println(recBasedEigenvector);
		return recBasedEigenvector;
	}
	public static JSONArray EigenvectorJsonCreator(
			Map<String, Double> rIDEigenvector, Map<String, String> rIDName,
			String researcherID, UtilINImp imp) throws JSONException, SQLException, IOException,
			TasteException {
		JSONArray recBasedEigenvector = new JSONArray();
		for (Entry<String, Double> entry : rIDEigenvector.entrySet()) {
			int NofcommonI = NofCommonInterests(researcherID, entry.getKey());
			double JaccardSim = UtilService.FindJaccardSimilarity(researcherID,
					entry.getKey(), imp);
			JSONObject obj = new JSONObject();
			obj.put("rID", entry.getKey());
			obj.put("Name", rIDName.get(entry.getKey()));
			obj.put("EigenVectorValue", entry.getValue());
			obj.put("NofCommonInterest", NofcommonI);
			obj.put("SimInPercent", Math.round(JaccardSim * 100));
			recBasedEigenvector.put(obj);
		}
		// System.out.println(recBasedEigenvector);
		return recBasedEigenvector;
	}
	
	/**
	 * Create JSON files for Betweenness
	 * 
	 * @param rIDName
	 * @param rIDPageRank
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws TasteException
	 * @throws IOException
	 * @throws SQLException
	 */
	public static JSONArray BetweennessJsonCreator( Map<String, String> rIDName,
			Map<String, Number> rIDBet, String researcherID, UtilINImp util )
			throws JSONException, SQLException, IOException, TasteException {
		JSONArray recBasedBet = new JSONArray();
		for (Entry<String, Number> entry : rIDBet.entrySet()) {
			int NofcommonI = util.getCommonInterests(researcherID, entry.getKey());
			double JaccardSim = util.FindJaccardSimilarity(researcherID,
					entry.getKey());
			JSONObject obj = new JSONObject();
			obj.put("rID", entry.getKey());
			obj.put("rName", rIDName.get(entry.getKey()));
			obj.put("ExpertIn", entry.getKey());
			obj.put("BetweennessValue", entry.getValue());
			obj.put("NofCommonInterest", NofcommonI);
			obj.put("SimInPercent", Math.round(JaccardSim * 100));
			recBasedBet.put(obj);
		}
		return recBasedBet;
	}

	/**
	 * find common interest lists of recommended users and main researcher
	 * 
	 * @param rDegree
	 * @param rIDs
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 */
	static JSONArray addCommonInterestListToJson(JSONArray FinalJsonArray,
			ArrayList<Integer> rIDs, int researcherID) throws JSONException {
		ArrayList<String> listR1 = getUniqueInterestNameOf1User(researcherID);
		for (int j = 0; j < rIDs.size(); j++) {
			JSONArray commonIListArray = new JSONArray();
			JSONObject obj3 = new JSONObject();
			JSONArray jsonArray2 = new JSONArray();
			// System.out.println("rID size is : "+ rIDs.size());
			ArrayList<String> listR2 = getUniqueInterestNameOf1User(rIDs.get(j));
			JSONObject obj = new JSONObject();
			obj.put("rName", getNamefor1ID(researcherID));
			obj.put("simRName", getNamefor1ID(rIDs.get(j)));
			for (int i = 0; i < listR1.size(); i++) {
				if (listR2.contains(listR1.get(i))) {
					JSONObject obj1 = new JSONObject();
					obj1.put("iName", listR1.get(i));
					commonIListArray.put(obj1);
				}
			}
			obj.put("iList", commonIListArray);
			// System.out.println("iList"+j+ commonIListArray.length());
			jsonArray2.put(obj);
			obj3.put("iListInfo" + j, jsonArray2);
			// System.out.println("iListInfo"+j+ jsonArray2.length());
			FinalJsonArray.put(obj3);
		}
		// System.out.println("interest list array :"+FinalJsonArray);
		return FinalJsonArray;
	}

	/**
	 * 
	 * @param researcherID
	 * @param rIDBetweenness
	 * @return
	 * @throws TasteException
	 * @throws IOException
	 * @throws SQLException
	 */
	public static Map<Integer, Double> ComputeImportance(
			Map<Integer, Double> rIDSNAMetrics, int researcherID)
			throws SQLException, IOException, TasteException {
		Map<Integer, Double> importanceResult = new LinkedHashMap<Integer, Double>();
		for (Entry<Integer, Double> entry : rIDSNAMetrics.entrySet()) {
			Double CMetric = entry.getValue();
			double JaccardSim = FindJaccardSimilarity(researcherID,
					entry.getKey());
			Double sum = (CMetric * 0.5) + (JaccardSim * 0.5);
			importanceResult.put(entry.getKey(), sum);
		}
		Map<Integer, Double> sortedResult = sortByIntComparator(importanceResult);
		return sortedResult;
	}
	public static Map<String, Double> ComputeImportance(
			Map<String, Double> rIDSNAMetrics, String researcherID, UtilINImp imp)
			throws SQLException, IOException, TasteException {
		Map<String, Double> importanceResult = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : rIDSNAMetrics.entrySet()) {
			Double CMetric = entry.getValue();
			double JaccardSim = FindJaccardSimilarity(researcherID,
					entry.getKey(), imp);
			Double sum = (CMetric * 0.5) + (JaccardSim * 0.5);
			importanceResult.put(entry.getKey(), sum);
		}
		Map<String, Double> sortedResult = sortByIDComparator(importanceResult);
		return sortedResult;
	}
	
	/**
	 * 
	 * This one is for Degree
	 * 
	 * @param researcherID
	 * @param maxDegreeofaNode
	 * @param rIDBetweenness
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws TasteException
	 */
	static Map<String, Double> ComputeImportance2(
			Map<String, Integer> rIDSNAMetrics, String researcherID,
			int maxDegreeofaNode, UtilINImp imp) throws SQLException, IOException,
			TasteException {
		Map<String, Double> importanceResult = new LinkedHashMap<String, Double>();
		for (Entry<String, Integer> entry : rIDSNAMetrics.entrySet()) {
			double CMetric = ((double) entry.getValue() / maxDegreeofaNode);
			double JaccardSim = FindJaccardSimilarity(researcherID,
					entry.getKey(), imp);
			Double sum = (CMetric * 0.5) + (JaccardSim * 0.5);
			importanceResult.put(entry.getKey(), sum);
		}
		Map<String, Double> sortedResult = sortByComparator(importanceResult);
		return sortedResult;
	}

	/**
	 * It finds number of common interests between 2 users which ID is Integer
	 * 
	 * @param ResearcherID1
	 * @param ResearcherID2
	 * @return
	 */
	static int NofCommonInterests(Integer ResearcherID1, Integer ResearcherID2) {
		ArrayList<String> listR1 = getUniqueInterestNameOf1User(ResearcherID1);
		ArrayList<String> listR2 = getUniqueInterestNameOf1User(ResearcherID2);
		int NofcommonI = 0;
		for (int i = 0; i < listR1.size(); i++) {
			if (listR2.contains(listR1.get(i))) {
				NofcommonI++;
			}
		}
		return NofcommonI;
	}
	public static int NofCommonInterests(String ResearcherID1, String ResearcherID2) {
		ArrayList<String> listR1 = getUniqueInterestNameOf1User(ResearcherID1);
		ArrayList<String> listR2 = getUniqueInterestNameOf1User(ResearcherID2);
		int NofcommonI = 0;
		for (int i = 0; i < listR1.size(); i++) {
			if (listR2.contains(listR1.get(i))) {
				NofcommonI++;
			}
		}
		return NofcommonI;
	}
	
	/**
	 * It finds number of common interests which one user has long type ID in
	 * Collaborative filtering of all,CoAuthor,2d.3d network I use this
	 * 
	 * @param ResearcherID1
	 * @param ResearcherID2
	 * @return
	 */
	static int NofCommonInterestsforLong(int ResearcherID1, long ResearcherID2) {
		ArrayList<String> listR1 = getUniqueInterestNameOf1User(ResearcherID1);
		ArrayList<String> listR2 = getUniqueInterestNameOf1UserForLong(ResearcherID2);
		int NofcommonI = 0;
		for (int i = 0; i < listR1.size(); i++) {
			if (listR2.contains(listR1.get(i))) {
				NofcommonI++;
			}
		}
		return NofcommonI;
	}

	/**
	 * This compute Jaccard similarity
	 * 
	 * @param ResearcherID1
	 * @param ResearcherID2
	 * @return
	 */
	static double FindJaccardSimilarity(int ResearcherID1, long ResearcherID2) {
		ArrayList<String> listR1 = getUniqueInterestNameOf1User(ResearcherID1);
		ArrayList<String> listR2 = getUniqueInterestNameOf1UserForLong(ResearcherID2);
		ArrayList<String> intersect = new ArrayList<String>();
		ArrayList<String> union = new ArrayList<String>();
		intersect.clear();
		intersect.addAll(listR1);
		intersect.retainAll(listR2);
		union.clear();
		union.addAll(listR1);
		union.addAll(listR2);
		double result = (double) intersect.size() / (double) union.size();
		return result;
	}
	static double FindJaccardSimilarity(String ResearcherID1, String ResearcherID2, UtilINImp imp) {
		List<Object> listR1 = imp.getAuthorTotalInterests( ResearcherID1 ); //getUniqueInterestNameOf1User(ResearcherID1);
		List<Object> listR2 = imp.getAuthorTotalInterests( ResearcherID2 ); //getUniqueInterestNameOf1User(ResearcherID2);
		ArrayList<Object> intersect = new ArrayList<Object>();
		ArrayList<Object> union = new ArrayList<Object>();
		intersect.clear();
		intersect.addAll(listR1);
		intersect.retainAll(listR2);
		union.clear();
		union.addAll(listR1);
		union.addAll(listR2);
		double result = (double) intersect.size() / ( union.size() == 0 ? 1.0 : ( double ) union.size() );
		return result;
	}
	
	/**
	 * Find common interest in percent for both 2 integers
	 * 
	 * @param ResearcherID1
	 * @param ResearcherID2
	 * @return
	 */
	static double FindSimilarityInPercent2(int ResearcherID1, int ResearcherID2) {
		ArrayList<String> listR1 = getUniqueInterestNameOf1User(ResearcherID1);
		ArrayList<String> listR2 = getUniqueInterestNameOf1User(ResearcherID2);
		ArrayList<String> intersect = new ArrayList<String>();
		ArrayList<String> union = new ArrayList<String>();
		intersect.clear();
		intersect.addAll(listR1);
		intersect.retainAll(listR2);
		union.clear();
		union.addAll(listR1);
		union.addAll(listR2);
		double result = (double) intersect.size() / (double) union.size();
		return result;
	}

	/**
	 * It finds recommended IDs of final JSON
	 * 
	 * @param cfCResult
	 * @return
	 */
	public static ArrayList<String> FindRIdsInFinalJson(JSONArray cfCResult) {
		ArrayList<String> rIDs = new ArrayList<>();
		JSONObject mainJSON = cfCResult.getJSONObject(0);
		JSONArray jsonMainArr = mainJSON.getJSONArray("sResearchers");
		for (int j = 0; j < jsonMainArr.length(); j++) {
			JSONObject childJSONObject = jsonMainArr.getJSONObject(j);
			String rID = null;
			if ( childJSONObject.has( "rID" ) )
				rID = childJSONObject.getString("rID");
			else if ( childJSONObject.has( "AuthorID" ) )
				rID = childJSONObject.getString("AuthorID");
			rIDs.add(rID);
		}
		return rIDs;
	}

	/**
	 * It finds recommended Items Names of final Json
	 * 
	 * @param cfCResult
	 * @return
	 */
	public static ArrayList<String> FindINamesInFinalJson(JSONArray cfCResult) {
		// parse all recommended Researcher IDs
		ArrayList<String> iNames = new ArrayList<String>();
		JSONObject mainJSON1 = cfCResult.getJSONObject(1);
		JSONArray jsonMainArr1 = mainJSON1.getJSONArray("rItems");
		for (int j = 0; j < jsonMainArr1.length(); j++) {
			JSONObject childJSONObject = jsonMainArr1.getJSONObject(j);
			String iName;
			if (!childJSONObject.has("itemName")) {
			} else {
				iName = childJSONObject.getString("itemName");
				iNames.add(iName);
			}
		}
		return iNames;
	}

	/**
	 * It adds those publications of recommended users which has the interest
	 * name on their title, to the JSON file
	 * 
	 * @param cfCResult
	 * @param rIDs
	 * @param iNames
	 * @return
	 * @throws SQLException
	 */
	static JSONArray addPubsOfIToJson(JSONArray cfCResult,
			ArrayList<Integer> rIDs, ArrayList<String> iNames)
			throws SQLException {
		for (int i = 0; i < iNames.size(); i++) {
			JSONArray pubOfIJsonArray = new JSONArray();
			JSONObject pubofIObj = new JSONObject();
			ArrayList<String> checkedPTitle = new ArrayList<String>();
			for (int j = 0; j < rIDs.size(); j++) {
				String userPub = UtilService
						.userPublicationsFinder(rIDs.get(j));
				try {
					Connection myConn = DbService.ConnectToDB();
					// 2.create statement
					Statement myStmt = myConn.createStatement();
					// 3.execute query
					ResultSet myRs = myStmt
							.executeQuery("SELECT * FROM `publications` WHERE (INSTR(title,'"
									+ iNames.get(i)
									+ "') OR INSTR(abstract,'"
									+ iNames.get(i)
									+ "') OR INSTR(keywords,'"
									+ iNames.get(i)
									+ "')) AND id IN ("
									+ userPub + ");");
					while (myRs.next()) {
						if (checkedPTitle.contains(myRs.getString("title"))) {
						} else {
							checkedPTitle.add(myRs.getString("title"));
							JSONObject obj = new JSONObject();
							obj.put("iName", iNames.get(i));
							obj.put("pID", myRs.getInt("id"));
							obj.put("pTitle", myRs.getString("title"));
							obj.put("pAuthors", myRs.getString("authors"));
							obj.put("pAbstract", myRs.getString("abstract"));
							obj.put("pKeywords", myRs.getString("keywords"));
							pubOfIJsonArray.put(obj);
						}
					}
					if (myStmt != null)
						myStmt.close();
					if (myConn != null)
						myConn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			pubofIObj.put("publicationsOfI" + i, pubOfIJsonArray);
			cfCResult.put(pubofIObj);
		}
		return cfCResult;
	}
}
