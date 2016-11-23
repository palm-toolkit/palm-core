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
 * Util file for algorithms based on 3-depth Co-authorship networks
 * 
 * @author Peyman Toreini
 * @version 1.1
 */
public class UtilC3d {
	static ArrayList<Integer> checkedUser = new ArrayList<Integer>();
	static ArrayList<Integer> directCoAuths = new ArrayList<Integer>();
	static String fileName = "data/SNA3dfile.csv";

	/**
	 * Add pairs of researchers
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
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				snDataArray.add(rID + "," + coAuthors.getInt("authorid"));
			}
		}
		// add user to the list after it found the Co-authors
		checkedUser.add(rID);
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return snDataArray;
	}

	/**
	 * @param rID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<String> userPairsAdder2(int rID)
			throws SQLException, IOException {
		ArrayList<String> allData = new ArrayList<String>();
		ArrayList<String> snDataArray = new ArrayList<String>();
		ArrayList<Integer> coAIDofR = new ArrayList<Integer>();
		Connection myConn = DbService.ConnectToDB();
		String userPub = UtilService.userPublicationsFinder(rID);
		Statement myStmt = myConn.createStatement();
		// find user coAuthors
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				snDataArray.add(rID + "," + coAuthors.getInt("authorid"));
				coAIDofR.add(coAuthors.getInt("authorid"));
			}
		}
		allData.addAll(snDataArray);
		// close DB connection
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		for (int i = 0; i < coAIDofR.size(); i++) {
			if (checkedUser.contains(coAIDofR.get(i))) {
			} else {
				ArrayList<String> snDataArray1 = userPairsAdder(coAIDofR.get(i));
				allData.addAll(snDataArray1);
			}
		}
		return allData;
	}

	/**
	 * It creates 3 depth SNA file of asked researcher
	 * 
	 * @param rID
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void SNA3dfileCreator(int rID) throws SQLException,
			IOException {
		checkedUser.clear();
		ArrayList<String> allData = new ArrayList<String>();
		ArrayList<String> snDataArray = userPairsAdder(rID);
		// System.out.println("Data Array is :" + snDataArray);
		allData.addAll(snDataArray);
		String userPub = UtilService.userPublicationsFinder(rID);
		Connection myConn = DbService.ConnectToDB();
		Statement myStmt = myConn.createStatement();
		// find user publication
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				ArrayList<String> snDataArray1 = userPairsAdder2(coAuthors
						.getInt("authorid"));
				allData.addAll(snDataArray1);
			}
		}
		FileWriter fileWriter = new FileWriter(fileName);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		for (int i = 0; i < allData.size(); i++) {
			// System.out.println(snDataArray.get(i));
			bufferedWriter.write(allData.get(i));
			if (i < allData.size() - 1) {
				bufferedWriter.newLine();
			}
		}
		bufferedWriter.close();
	}

	/**
	 * @param rID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<Integer> CoAuthFinder(int rID) throws SQLException,
			IOException {
		ArrayList<Integer> coIds = new ArrayList<Integer>();
		Connection myConn = DbService.ConnectToDB();
		String userPub = UtilService.userPublicationsFinder(rID);
		Statement myStmt = myConn.createStatement();
		// find user coAuthors
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			// I have added this part to recommend new users. users which the
			// researcher did not worked with them.
			if (directCoAuths.contains(coAuthors.getInt("authorid"))) {
			} else if (rID != coAuthors.getInt("authorid")) {
				coIds.add(coAuthors.getInt("authorid"));
			}
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return coIds;
	}

	/**
	 * @param rID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<Integer> CoAuthFinder2(int rID)
			throws SQLException, IOException {
		ArrayList<Integer> allData = new ArrayList<Integer>();
		Connection myConn = DbService.ConnectToDB();
		String userPub = UtilService.userPublicationsFinder(rID);
		Statement myStmt = myConn.createStatement();
		// find user coAuthors
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (directCoAuths.contains(coAuthors.getInt("authorid"))) {
			} else {
				allData.addAll(CoAuthFinder(coAuthors.getInt("authorid")));
			}
		}
		// close DB connection
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return allData;
	}

	/**
	 * It Finds IDs of indirect researchers who are in 3 depth Co-Authorship
	 * Network
	 * 
	 * @param researcherID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<Integer> find3dIndirectRIds(int researcherID)
			throws SQLException, IOException {
		directCoAuths.clear();
		Integer rID = researcherID;
		ArrayList<Integer> allCoIds = new ArrayList<Integer>();
		directCoAuths = CoAuthFinder(rID);
		/**
		 * I do not want to add 1d users to this list in order to avoid
		 * recommending co-author user.
		 * 
		 * 
		 * allCoIds.addAll(coIds);
		 **/
		String userPub = UtilService.userPublicationsFinder(rID);
		Connection myConn = DbService.ConnectToDB();
		Statement myStmt = myConn.createStatement();
		// find user publication
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				ArrayList<Integer> coIds2 = CoAuthFinder2(coAuthors
						.getInt("authorid"));
				allCoIds.addAll(coIds2);
			}
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return allCoIds;
	}

	public static ArrayList<Integer> find3dAllRIds(int researcherID)
			throws SQLException, IOException {
		ArrayList<Integer> coIds = new ArrayList<Integer>();
		Integer rID = researcherID;
		ArrayList<Integer> allCoIds = new ArrayList<Integer>();
		directCoAuths.clear();
		directCoAuths = CoAuthFinder(rID);
		coIds = CoAuthFinder(rID);
		allCoIds.addAll(coIds);
		String userPub = UtilService.userPublicationsFinder(rID);
		Connection myConn = DbService.ConnectToDB();
		Statement myStmt = myConn.createStatement();
		// find user publication
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				ArrayList<Integer> coIds2 = CoAuthFinder2(coAuthors
						.getInt("authorid"));
				allCoIds.addAll(coIds2);
			}
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return allCoIds;
	}

	/**
	 * It Finds names of indirect researchers who are in 3 depth Co-Authorship
	 * Network
	 * 
	 * @param researcherID
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static ArrayList<String> findC3dNames(int researcherID)
			throws SQLException, IOException {
		ArrayList<String> rNames = new ArrayList<String>();
		ArrayList<Integer> allCoIds = new ArrayList<Integer>();
		// allCoIds = find3dIndirectRIds(researcherID);
		allCoIds = find3dAllRIds(researcherID);
		Map<Integer, String> rIDName3 = UtilService.getName(allCoIds);
		for (Entry<Integer, String> entry : rIDName3.entrySet()) {
			rNames.add(entry.getValue());
		}
		return rNames;
	}

	/**
	 * 
	 * Create the file for CF based 3-depth co-authorhsip networks
	 * 
	 * @param researcherID
	 * @throws JSONException
	 * @throws IOException
	 * @throws SQLException
	 */
	public static void makeFileForCFbased3dC(int researcherID)
			throws JSONException, IOException, SQLException {
		ArrayList<String> rNames = findC3dNames(researcherID);
		// System.out.println("rnamse are" + rNames);
		String coAuthorsNames = UtilService.namesListForQuery(rNames);
		String coAuthorID = "";
		String fileName = "data/CFbased3dCFile.csv";
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
			// System.out.println(coAuthorID);
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