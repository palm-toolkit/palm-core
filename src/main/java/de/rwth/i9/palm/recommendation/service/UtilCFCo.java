package de.rwth.i9.palm.recommendation.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.json.JSONException;

/**
 * Master Thesis at The Learning Technologies Research Group (LuFG Informatik 9,
 * RWTH Aachen University), Year 2014 - 2015
 * 
 * Util file for algorithms based on 1-depth co-authorship networks
 * 
 * @author Peyman Toreini
 * @version 1.1
 */
public class UtilCFCo {
	static String fileName = "data/SNA1dfile.csv";

	/**
	 * 
	 * create pairs of users to insert to the file
	 * 
	 * @param rID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<String> userPairsAdder(int rID)
			throws SQLException, IOException {
		ArrayList<String> snDataArray = new ArrayList<String>();
		Connection myConn = DbService.ConnectToDB();
		String userPub = UtilService.userPublicationsFinder(rID);
		Statement myStmt = myConn.createStatement();
		// find user coAuthors
		// System.out.println("user pub is :" + userPub);
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				snDataArray.add(rID + "," + coAuthors.getInt("authorid"));
			}
		}
		return snDataArray;
	}

	/**
	 * Get the names
	 * 
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

	/**
	 * 
	 * Find co-authors names
	 * 
	 * @param rID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<String> getCoAuthorsNames(int rID)
			throws SQLException, IOException {
		ArrayList<String> coAuthNames = new ArrayList<String>();
		ArrayList<Integer> rIDs = new ArrayList<Integer>();
		Connection myConn = DbService.ConnectToDB();
		String userPub = UtilService.userPublicationsFinder(rID);
		Statement myStmt = myConn.createStatement();
		// find user coAuthors
		// System.out.println("user pub is :" + userPub);
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				rIDs.add(coAuthors.getInt("authorid"));
			}
		}
		// System.out.println("RIDs is :" + rIDs);
		Map<Integer, String> rIDNames = getName(rIDs);
		for (Entry<Integer, String> entry : rIDNames.entrySet()) {
			coAuthNames.add(entry.getValue());
		}
		// System.out.println(coAuthNames);
		return coAuthNames;
	}

	/**
	 * 
	 * create 1d SN file
	 * 
	 * @param rID
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void SNA1dFileCreator(int rID) throws SQLException,
			IOException {
		ArrayList<String> snDataArray = userPairsAdder(rID);
		FileWriter fileWriter = new FileWriter(fileName);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		for (int i = 0; i < snDataArray.size(); i++) {
			// System.out.println(snDataArray.get(i));
			bufferedWriter.write(snDataArray.get(i));
			if (i < snDataArray.size() - 1) {
				bufferedWriter.newLine();
			}
		}
		bufferedWriter.close();
	}

	/**
	 * 
	 * Make for for co-authroship network
	 * 
	 * @param researcherID
	 * @throws JSONException
	 * @throws IOException
	 * @throws SQLException
	 */
	public static void makeFileForCFbasedCoAuthorship(int researcherID)
			throws JSONException, IOException, SQLException {
		ArrayList<String> results = getCoAuthorsNames(researcherID);
		// System.out.println("results is " + results);
		String coAuthorsNames = UtilService.namesListForQuery(results);
		String coAuthorID = "";
		String fileName = "data/PalmDatasetForCoAuthorshipCF.csv";
		try {
			// 1.get the connection to db
			Connection myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			Statement myStmt2 = myConn.createStatement();
			Statement myStmt3 = myConn.createStatement();
			Statement myStmt4 = myConn.createStatement();
			// 3.execute query
			ResultSet myRs = myStmt
					.executeQuery("SELECT id,name FROM `authors` WHERE name IN ("
							+ coAuthorsNames + ");");
			while (myRs.next()) {
				coAuthorID += "\"" + myRs.getString("id") + "\"" + ",";
			}
			coAuthorID = UtilService.lastCharRemover(coAuthorID);
			ResultSet myRs2 = myStmt2
					.executeQuery("SELECT interestID,interestName FROM interestsid");
			// save interest table result in a map which is a key-value variable
			Map<String, Integer> interestIDTableMap = new TreeMap<String, Integer>(
					String.CASE_INSENSITIVE_ORDER);
			while (myRs2.next()) {
				interestIDTableMap.put(myRs2.getString("interestName"),
						myRs2.getInt("interestID"));
			}
			// Assume default encoding.
			FileWriter fileWriter = new FileWriter(fileName);
			// Always wrap FileWriter in BufferedWriter.
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			ResultSet myRs3 = myStmt3
					.executeQuery("select authorid,result,score from results WHERE authorid IN ("
							+ coAuthorID + ");");
			while (myRs3.next()) {
				if (myRs3.getString("authorid") != null
						&& interestIDTableMap.get(myRs3.getString("result")) != null) {
					bufferedWriter.write(myRs3.getString("authorid") + ","
							+ interestIDTableMap.get(myRs3.getString("result"))
							+ "," + myRs3.getString("score"));
					bufferedWriter.newLine();
				}
			}
			// I should add the interests of the searched researcher to the file
			ResultSet myRs4 = myStmt4
					.executeQuery("select authorid,result,score from results WHERE authorid='"
							+ researcherID + "'");
			while (myRs4.next()) {
				if (myRs4.getString("authorid") != null
						&& interestIDTableMap.get(myRs4.getString("result")) != null) {
					bufferedWriter.write(myRs4.getString("authorid") + ","
							+ interestIDTableMap.get(myRs4.getString("result"))
							+ "," + myRs4.getString("score"));
					bufferedWriter.newLine();
				}
			}
			bufferedWriter.close();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}