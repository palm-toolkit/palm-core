package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.gephi.filters.FilterControllerImpl;
import org.gephi.filters.FilterModelImpl;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.FilterModel;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.attribute.AttributeRangeBuilder;
import org.gephi.filters.plugin.dynamic.DynamicRangeBuilder.DynamicRangeFilter;
import org.gephi.graph.GraphControllerImpl;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.Table;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.ForceVectorNodeLayoutData;
import org.gephi.layout.plugin.force.ForceVector;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2LayoutData;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openide.util.Lookup;
import org.openide.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.datasetcollect.service.AsynchronousCollectionService;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.helper.comparator.AuthorInterestByDateComparator;
import de.rwth.i9.palm.helper.comparator.AuthorInterestProfileByProfileNameLengthComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.InterestProfile;
import de.rwth.i9.palm.model.InterestProfileType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.AuthorDAO;
import de.rwth.i9.palm.persistence.InterestDAO;
import de.rwth.i9.palm.persistence.InterestProfileDAO;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.persistence.PublicationDAO;
import de.rwth.i9.palm.persistence.relational.AuthorDAOHibernate;
import de.rwth.i9.palm.persistence.relational.InterestProfileDAOHibernate;
import de.rwth.i9.palm.recommendation.service.DbService;
import de.rwth.i9.palm.recommendation.service.PublicationExtractionService;
import de.rwth.i9.palm.recommendation.service.UtilIN;
import de.rwth.i9.palm.recommendation.service.UtilService;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

/**
 * Master Thesis at The Learning Technologies Research Group (LuFG Informatik 9,
 * RWTH Aachen University), Year 2014 - 2015
 * 
 * This class consists of methods for SNA-based recommendations that works with
 * Interest Networks.
 * 
 * 
 * @author Peyman Toreini
 * @version 1.1
 */
public class UtilINImp implements UtilIN {
	static ArrayList<String> snDataArray = new ArrayList<String>();
	//static Map<String, String> interestIDTableMap = new TreeMap<String, String>(
	//static Map<String, String> interestIDTableMap = new TreeMap<String, String>(
	//		String.CASE_INSENSITIVE_ORDER);
	static String fileName = "data/InterestSNfile.csv";

	private List<String> co3DAuthors; 

	//@Autowired
	private ResearcherFeature researcherFeature;

	private AsynchronousCollectionService service;

	//@Autowired
	private PersistenceStrategy persistenceStrategy;

	private SessionFactory sessionFactory;

	private TopicExtractionService topicService;

	public static final int _RESEARCHER_NODE_CODE = 0;
	public static final int _AUTHOR_NODE_CODE = 1;
	public static final int _INTERST_NODE_CODE = 2;
	public static final int _PUBLICATION_NODE_CODE = 3;
	public static final int _DEFAULT_SIZE = 150;

	/**
	 * @throws SQLException
	 */
	//public static void interestIDTableMap(List<Interest> ints) throws SQLException {
	// 1.get the connection to db
	/*Connection myConn = DbService.ConnectToDB();
		// 2.create statement
		Statement myStmt = myConn.createStatement();
		ResultSet myRs2 = myStmt
				.executeQuery("SELECT interestID,interestName FROM interestsid");
		// save interest table result in a map which is a key-value variable
		while (myRs2.next()) {
			interestIDTableMap.put(myRs2.getString("interestName"),
					myRs2.getInt("interestID"));
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();*/		
	//}

	/*private void interestIDTableMap() {
		List<Interest> ints = persistenceStrategy.getInterestDAO().getAll();
		for(Interest i : ints) {
			interestIDTableMap.put(i.getId(), i.getTerm());
		}		
	}*/

	public UtilINImp ( PersistenceStrategy persistenceStrategy, TopicExtractionService topicExtractionService,
			SessionFactory sessionFactory )
	{
		this();
		this.persistenceStrategy = persistenceStrategy;
		this.topicService = topicExtractionService;
		this.sessionFactory = sessionFactory;
	}

	public UtilINImp() 
	{
		co3DAuthors = new LinkedList<>();
	}

	public List<Object> getAuthorTotalInterests( String author )
	{
		List<Object> rList = new LinkedList<>();
	 	Set<AuthorInterestProfile> list = persistenceStrategy.getAuthorDAO().getById( author ).getAuthorInterestProfiles();
	 	for ( AuthorInterestProfile prof : list )
	 	{
	 		Set<AuthorInterest> ints = prof.getAuthorInterests();
	 		for ( AuthorInterest aInt : ints )
	 		{
	 			rList.addAll( aInt.getTermWeights().values() );
	 		}
	 	}
	 	return rList;
	}
	
	/**
	 * @param rIDs
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, String> getItemNames(ArrayList<Integer> rIDs)
			throws SQLException {
		// String rIDQuery = "";
		Map<Integer, String> rIDNameMap = new TreeMap<Integer, String>();
		Map<String, Integer> InterestIDTableMap = UtilService
				.interestIDTableMap();
		String interetsname = null;
		for (int i = 0; i < rIDs.size(); i++) {
			for (Entry<String, Integer> entry : InterestIDTableMap.entrySet()) {
				if (entry.getValue().equals(rIDs.get(i)))
					interetsname = entry.getKey();
			}
			rIDNameMap.put(rIDs.get(i), interetsname);
		}
		return rIDNameMap;
	}

	public Map<String, Object> getAuthorInterestById( String authorId ) throws 
	UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( authorId == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author id missing" );
			return responseMap;
		}

		// get the author
		AuthorDAO authorDAO = new AuthorDAOHibernate( sessionFactory );
		Author author = authorDAO.getById( authorId );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found" );
			return responseMap;
		}
		// put some information
		responseMap.put( "status", "Ok" );

		Map<String, Object> targetAuthorMap = new LinkedHashMap<String, Object>();
		targetAuthorMap.put( "id", author.getId() );
		targetAuthorMap.put( "name", author.getName() );
		responseMap.put( "author", targetAuthorMap );

		Map<String, Object> termValueResult = new LinkedHashMap<>();

		String query = "SELECT it.id, it.term, tw.weight FROM nugraha.author_interest_profile auth_int " + 
				"INNER JOIN nugraha.author_interest auth ON auth.author_interest_profile_id=auth_int.id " + 
				"INNER JOIN nugraha.term_weight tw ON tw.AuthorInterest_id=auth.id " +
				"INNER JOIN nugraha.interest it ON it.id=tw.termWeights_KEY " +
				"WHERE auth_int.author_id = :authorID ORDER BY tw.weight DESC";
		Session session = sessionFactory.getCurrentSession();
		if ( session.isConnected() && session.isOpen() )
		{
			List<Object[]> profiles = sessionFactory.getCurrentSession().createSQLQuery( "CALL nugraha.GetAuthorInterests(:authorID)" )
					.setParameter( "authorID", authorId ).list();

			for ( Object[] profile : profiles )
			{
				String termID = String.valueOf( profile[0] );
				String term = String.valueOf( profile[1] );
				Double weight = Double.valueOf( String.valueOf( profile[2] ) );

				List<Object> termWeightObjects = new ArrayList<Object>();
				termWeightObjects.add( termID );
				termWeightObjects.add( term );
				termWeightObjects.add( weight );
				termValueResult.put( termID, termWeightObjects );
			}
		}
		/*Set<AuthorInterestProfile> profiles = author.getAuthorInterestProfiles();
		for ( AuthorInterestProfile authorInterestProfile : profiles )
		{
			Set<AuthorInterest> authorInterests = authorInterestProfile.getAuthorInterests();

			for ( AuthorInterest authorInterest : authorInterests )
			{
				List<Map.Entry<Interest, Double>> termWeights = new LinkedList<>( authorInterest.getTermWeights().entrySet() );

				Collections.sort( termWeights, new Comparator<Map.Entry<Interest, Double>>() {
					@Override
					public int compare( Entry<Interest, Double> authorInterest1, Entry<Interest, Double> authorInterest2 )
					{
						if ( authorInterest1 == null && authorInterest2 == null )
							return 0;

						if ( authorInterest1 == null )
							return -1;

						if ( authorInterest2 == null )
							return 1;

						return authorInterest1.getValue().compareTo( authorInterest2.getValue() );
					}
				}.reversed() );

				termWeights = termWeights.subList( 0, ( termWeights.size() > 5 ? 5 : termWeights.size()-1 ) );
				for ( Map.Entry<Interest, Double> termWeightMap : termWeights )
				{

					List<Object> termWeightObjects = new ArrayList<Object>();
					termWeightObjects.add( termWeightMap.getKey().getId() );
					termWeightObjects.add( termWeightMap.getKey().getTerm() );
					termWeightObjects.add( termWeightMap.getValue() );
					termValueResult.put( termWeightMap.getKey().getId(), termWeightObjects );
				}
			}
		}*/

		responseMap.put( "interest", new LinkedList<>( termValueResult.values() ) );
		return responseMap;
	}

	public Map<String, Object> getAuthorInterestById( String authorId, boolean isReplaceExistingResult ) throws 
	UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException {
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( authorId == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author id missing" );
			return responseMap;
		}

		// get the author
		AuthorDAO authorDAO = new AuthorDAOHibernate( sessionFactory );
		Author author = authorDAO.getById( authorId );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found" );
			return responseMap;
		}
		// put some information
		responseMap.put( "status", "Ok" );

		Map<String, Object> targetAuthorMap = new LinkedHashMap<String, Object>();
		targetAuthorMap.put( "id", author.getId() );
		targetAuthorMap.put( "name", author.getName() );
		responseMap.put( "author", targetAuthorMap );

		// check whether publication has been extracted
		// later add extractionServiceType checking
		//TopicExtractionService topicExtractionService = new TopicExtractionService();
		//topicService.extractTopicFromPublicationByAuthor( author );

		// check if interest need to be recalculate
		if ( !isReplaceExistingResult && author.isUpdateInterest() )
		{
			isReplaceExistingResult = true;

			author.setUpdateInterest( false );
			authorDAO.persist( author );
		}

		//logger.info( "start mining interest " );
		// get default interest profile
		InterestProfileDAO interestDAO = new InterestProfileDAOHibernate( sessionFactory );
		List<InterestProfile> interestProfilesDefault = interestDAO.getAllActiveInterestProfile( InterestProfileType.DEFAULT );

		// get default interest profile
		List<InterestProfile> interestProfilesDerived = interestDAO.getAllActiveInterestProfile( InterestProfileType.DERIVED );

		if ( interestProfilesDefault.isEmpty() && interestProfilesDerived.isEmpty() )
		{
			//logger.warn( "No active interest profile found" );
			return responseMap;
		}

		if ( author.getPublications() == null || author.getPublications().isEmpty() )
		{
			//logger.warn( "No publication found" );
			return responseMap;
		}

		// update for all author interest profile
		// updateAuthorInterest = true;
		/*if ( !isReplaceExistingResult )
		{
			// get interest profile from author
			Set<AuthorInterestProfile> authorInterestProfiles = author.getAuthorInterestProfiles();
			if ( authorInterestProfiles != null && !authorInterestProfiles.isEmpty() )
			{
				// check for missing default interest profile in author
				// only calculate missing one
				for ( Iterator<InterestProfile> interestProfileIterator = interestProfilesDefault.iterator(); interestProfileIterator.hasNext(); )
				{
					InterestProfile interestProfileDefault = interestProfileIterator.next();
					for ( AuthorInterestProfile authorInterestProfile : authorInterestProfiles )
					{
						if ( authorInterestProfile.getInterestProfile() != null && authorInterestProfile.getInterestProfile().equals( interestProfileDefault ) )
						{
							interestProfileIterator.remove();
							break;
						}
					}
				}

				// check for missing derivative interest profile
				for ( Iterator<InterestProfile> interestProfileIterator = interestProfilesDerived.iterator(); interestProfileIterator.hasNext(); )
				{
					InterestProfile interestProfileDerived = interestProfileIterator.next();
					for ( AuthorInterestProfile authorInterestProfile : authorInterestProfiles )
					{
						if ( authorInterestProfile.getInterestProfile() != null && authorInterestProfile.getInterestProfile().equals( interestProfileDerived ) )
						{
							interestProfileIterator.remove();
							break;
						}
					}
				}
			}
		}
		else
		{
			// clear previous results
			if ( author.getAuthorInterestProfiles() != null && !author.getAuthorInterestProfiles().isEmpty() )
			{
				author.getAuthorInterestProfiles().clear();
			}
		}*/

		List<AuthorInterestProfile> authorInterestProfiles = new ArrayList<AuthorInterestProfile>();
		authorInterestProfiles.addAll( author.getAuthorInterestProfiles() );
		// sort based on profile length ( currently there is no attribute to
		// store position)
		Collections.sort( authorInterestProfiles, new AuthorInterestProfileByProfileNameLengthComparator() );

		// the whole result related to interest
		List<Object> authorInterestResult = new ArrayList<Object>();
		Map<String, Object> termValueResult = new LinkedHashMap<>();

		for ( AuthorInterestProfile authorInterestProfile : authorInterestProfiles )
		{
			// put profile on map
			Map<String, Object> authorInterestResultProfilesMap = new HashMap<String, Object>();

			// get interest profile name and description
			String interestProfileName = authorInterestProfile.getName();
			String interestProfileDescription = authorInterestProfile.getDescription();

			// get authorInterest set on profile
			Set<AuthorInterest> authorInterests = authorInterestProfile.getAuthorInterests();

			// if profile contain no authorInterest just skip
			if ( authorInterests == null || authorInterests.isEmpty() )
				continue;

			// a map for storing authorInterst based on language
			Map<String, List<AuthorInterest>> authorInterestLanguageMap = new HashMap<String, List<AuthorInterest>>();

			// split authorinterest based on language and put it on the map
			for ( AuthorInterest authorInterest : authorInterests )
			{
				if ( authorInterestLanguageMap.get( authorInterest.getLanguage() ) != null )
				{
					authorInterestLanguageMap.get( authorInterest.getLanguage() ).add( authorInterest );
				}
				else
				{
					List<AuthorInterest> authorInterestList = new ArrayList<AuthorInterest>();
					authorInterestList.add( authorInterest );
					authorInterestLanguageMap.put( authorInterest.getLanguage(), authorInterestList );
				}
			}

			// prepare calendar for extractind year from date
			Calendar calendar = Calendar.getInstance();

			// result author interest based on language
			List<Object> authorInterestResultLanguageList = new ArrayList<Object>();

			// sort authorinterest based on year
			for ( Map.Entry<String, List<AuthorInterest>> authorInterestLanguageIterator : authorInterestLanguageMap.entrySet() )
			{
				// result container
				Map<String, Object> authorInterestResultLanguageMap = new LinkedHashMap<String, Object>();
				// hashmap value
				String interestLanguage = authorInterestLanguageIterator.getKey();
				List<AuthorInterest> interestList = authorInterestLanguageIterator.getValue();

				// sort based on year
				Collections.sort( interestList, new AuthorInterestByDateComparator() );

				// term values based on year result container
				List<Object> authorInterestResultYearList = new ArrayList<Object>();

				// get interest year, term and value
				int indexYear = 0;
				boolean increaseIndex = true;
				for ( AuthorInterest authorInterest : interestList )
				{
					/*increaseIndex = true;
					// just skip if contain no term weights
					if ( authorInterest.getTermWeights() == null || authorInterest.getTermWeights().isEmpty() )
						continue;


					// get year
					calendar.setTime( authorInterest.getYear() );
					String year = Integer.toString( calendar.get( Calendar.YEAR ) );

					while ( !years.get( indexYear ).equals( year ) )
					{

						// empty result
						Map<String, Object> authorInterestResultYearMap = new LinkedHashMap<String, Object>();

						authorInterestResultYearMap.put( "year", years.get( indexYear ) );
						authorInterestResultYearMap.put( "termvalue", Collections.emptyList() );
						indexYear++;
						increaseIndex = false;

						// remove duplicated year
						if ( !authorInterestResultYearList.isEmpty() )
						{
							@SuppressWarnings( "unchecked" )
							Map<String, Object> prevAuthorInterestResultYearMap = (Map<String, Object>) authorInterestResultYearList.get( authorInterestResultYearList.size() - 1 );
							if ( prevAuthorInterestResultYearMap.get( "year" ).equals( years.get( indexYear - 1 ) ) )
								continue;
						}
						authorInterestResultYearList.add( authorInterestResultYearMap );

					}*/

					// put term and value
					for ( Map.Entry<Interest, Double> termWeightMap : authorInterest.getTermWeights().entrySet() )
					{
						// just remove not significant value
						if ( termWeightMap.getValue() < 0.4 || termValueResult.containsKey( termWeightMap.getKey().getId() ) )
							continue;

						List<Object> termWeightObjects = new ArrayList<Object>();
						termWeightObjects.add( termWeightMap.getKey().getId() );
						termWeightObjects.add( termWeightMap.getKey().getTerm() );
						termWeightObjects.add( termWeightMap.getValue() );
						termValueResult.put( termWeightMap.getKey().getId(), termWeightObjects );
					}

					// result container
					/*Map<String, Object> authorInterestResultYearMap = new LinkedHashMap<String, Object>();

					authorInterestResultYearMap.put( "year", year );
					authorInterestResultYearMap.put( "termvalue", termValueResult );
					authorInterestResultYearList.add( authorInterestResultYearMap );
					if ( increaseIndex )
						indexYear++;*/
				}

				// continue interest year which is missing
				/*for ( int i = indexYear + 1; i < years.size(); i++ )
				{
					Map<String, Object> authorInterestResultYearMap = new LinkedHashMap<String, Object>();

					authorInterestResultYearMap.put( "year", years.get( i ) );
					authorInterestResultYearMap.put( "termvalue", Collections.emptyList() );
					authorInterestResultYearList.add( authorInterestResultYearMap );
				}

				authorInterestResultLanguageMap.put( "language", interestLanguage );
				authorInterestResultLanguageMap.put( "interestyears", authorInterestResultYearList );
				if ( interestLanguage.equals( "english" ) )
					authorInterestResultLanguageList.add( 0, authorInterestResultLanguageMap );
				else
					authorInterestResultLanguageList.add( authorInterestResultLanguageMap );*/
			}

			// put profile map
			//authorInterestResultProfilesMap.put( "profile", interestProfileName );
			//authorInterestResultProfilesMap.put( "description", interestProfileDescription );
			authorInterestResultProfilesMap.put( "interestlanguages", termValueResult);//authorInterestResultLanguageList );
			authorInterestResult.add( termValueResult );
		}
		/*		List<Object> interests = new LinkedList<>(termValueResult.values());
		Collections.sort(interests, new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				// TODO Auto-generated method stub
				return 0;
			}

		});*/

		responseMap.put( "interest", new LinkedList<>( termValueResult.values() ) );

		return responseMap;
	}

	private Map<String, Object> getAutherInterestsById( String autherId ) {

		Map<String, Object> maps = new HashMap<>();
		try
		{
			maps = getAuthorInterestById( autherId );
		}
		catch ( Exception e )
		{
			return maps;
		}
		//System.out.println( "Interest: " + maps );
		List<Object> autherInterests = new LinkedList<>();
		List<String> existingInterest = new LinkedList<String>();

		if(maps.get("status").equals("Ok")){
			ArrayList<Object> arr = (ArrayList)maps.get("interest");

			if(arr != null) {
				for(int i=0; i<arr.size(); i++) {

					Map<String, Object> items = (Map<String, Object>) arr.get( i );
					List<Map<String, Object>> interestLang = 
							(List<Map<String, Object>>) items.get( "interestlanguages" );

					if( interestLang != null && !interestLang.isEmpty() ) {
						for ( Map<String, Object> interestLangItem : interestLang )
						{

							List<Map<String, Object>> interestsYears = 
									(List<Map<String, Object>>) interestLangItem.get( "interestyears" );

							if ( interestsYears != null && !interestsYears.isEmpty() )
								for ( Map<String, Object> interestsYearsItems : interestsYears )
								{

									List<List<Object>> termValue = 
											(List<List<Object>>) interestsYearsItems.get( "termvalue" );

									if ( termValue != null && !termValue.isEmpty() )
										for ( List<Object> termItem : termValue )
										{
											if ( !existingInterest.contains( termItem.get( 0 ) ) )
											{
												autherInterests.add( termItem );
												existingInterest.add( (String) termItem.get( 0 ) );
											}
										}
								}
						}
					}
				}
			}
		}
		maps.put( "interest", autherInterests );

		return maps;
	}


	public List<Object> authorSortedInterest( String authorID ) 
	{
		Map<String, Object> result = new HashMap<>();
		List<Object> interests = null;
		try
		{
			result = getAuthorInterestById( authorID );

			if ( result.get( "status" ).equals( "Ok" ) )
			{
				interests = ( LinkedList<Object> ) result.get( "interest" );
				/*Collections.sort( interests, new Comparator<Object>() {

					@Override
					public int compare( Object o1, Object o2 )
					{
						ArrayList<Object> items1 = ( ArrayList<Object> ) o1;
						ArrayList<Object> items2 = ( ArrayList<Object> ) o2;

						Double d1 = (Double) items1.get( 2 );
						Double d2 = (Double) items2.get( 2 );

						if ( d1 < d2 )
						{
							return -1;
						}
						else if ( d1 == d2 )
						{
							return 0;
						}

						return 1;
					}
				}.reversed() );*/
			}
		}
		catch (UnsupportedEncodingException | InterruptedException | URISyntaxException | ExecutionException e)
		{
		}

		return interests;
	}

	/**
	 * @param rID
	 * @throws SQLException
	 */
	private List<String> topInterestFinder(String rID) throws SQLException {
		//ArrayList<String> topScoreItems = new ArrayList<String>();
		ArrayList<String> topScoreItems = new ArrayList<String>();
		List<String> snData = new ArrayList<>();
		// 1.get the connection to db
		/*Connection myConn = DbService.ConnectToDB();
		// 2.create statement
		Statement myStmt = myConn.createStatement();
		// 3.execute query
		ResultSet myRs = myStmt
				.executeQuery("select authorid,result,score from results WHERE authorid IN ("
						+ rID + ")ORDER BY score DESC;");
		int i = 0;
		while (myRs.next()) {
			if (i < 5) {
				topScoreItems.add(interestIDTableMap.get(myRs
						.getString("result")));
				for (int j = 0; j < topScoreItems.size(); j++) {
					if (!interestIDTableMap.get(myRs.getString("result"))
							.equals(topScoreItems.get(j))) {
						System.out.println("Inserting: " + interestIDTableMap.get(myRs
								.getString("result"))
								+ ","
								+ topScoreItems.get(j));
						snDataArray.add(interestIDTableMap.get(myRs
								.getString("result"))
								+ ","
								+ topScoreItems.get(j));
					}
				}
			}
			i++;
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();*/
		List<Object> interests = authorSortedInterest( rID );

		int i = 0;
		if ( interests != null ) {
			for( Object ai : interests ) {
				if (i < 5) {

					// extracting the Interest ID;
					ArrayList<Object> items = ( ArrayList<Object> ) ai;
					String id = ( String ) items.get( 0 );

					topScoreItems.add(id);
					for (int j = 0; j < topScoreItems.size(); j++) {
						if (!id.equals(topScoreItems.get(j))) {
							snData.add(id
									+ ","
									+ topScoreItems.get(j));
						}
					}
				}
				else {
					break;
				}
				i++;
			}
		}
		return snData;
	}

	private class TopicInterestWorker implements Callable<List<String>>
	{
		private String[] authors;

		public TopicInterestWorker( String...authors )
		{
			this.authors = authors;
		}

		@Override
		public List<String> call() throws Exception
		{			
			List<String> topicInterest = new ArrayList<>();

			for ( String author : authors )
			{
				topInterestFinder( author );
			}

			return topicInterest;
		}
	}


	/**
	 * @param rID
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<Integer> coAuthorFinder(int rID)
			throws SQLException {
		Connection myConn = DbService.ConnectToDB();
		String userPub = UtilService.userPublicationsFinder(rID);
		Statement myStmt = myConn.createStatement();
		ArrayList<Integer> coAuthorIDs = new ArrayList<Integer>();
		// find user coAuthors
		ResultSet coAuthors = myStmt
				.executeQuery("SELECT DISTINCT authorid FROM `authorspublications` WHERE publicationid IN ("
						+ userPub + ");");
		while (coAuthors.next()) {
			if (rID != coAuthors.getInt("authorid")) {
				coAuthorIDs.add(coAuthors.getInt("authorid"));
			}
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return coAuthorIDs;
	}

	/**
	 * @param coAuthorIDs
	 * @throws SQLException
	 * 
	 */
	private JSONArray interestofCoFinder(String author, ArrayList<String> coAuthorIDs)
			throws SQLException {
		final int poolSize = 50/4;
		final int increment = coAuthorIDs.size() / poolSize;
		List<Future<List<String>>> resultList = new ArrayList<>();
		//ThreadPoolExecutor executor = Executors.newFixedThreadPool( poolSize + 2, 
		//		"TopicInterest-" ) ;//50, 100, "CoAuthors-", 1000 );

		/*ThreadPoolExecutor executor = new ThreadPoolExecutor( 4, 50, 10, TimeUnit.MINUTES, 
				new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy() {
			@Override
			public void rejectedExecution( Runnable r, ThreadPoolExecutor e )
			{
				super.rejectedExecution( r, e );
				System.out.println( "Thread stop execution: " + Thread.currentThread().getName() );
				e.execute( r );
			}
		} );
		TopicInterestWorker worker = new TopicInterestWorker( 
				coAuthorIDs.get( 0 ));//toArray( new String[] {} ) );
		Future<List<String>> result = executor.submit( worker );
		resultList.add( result );*/

		JSONArray coAuthorInterest = new JSONArray();

		/*String authors = coAuthorIDs.toString().replace( "[", "" ).replace( "]", "" )
				.replaceAll( ", ", "\',\'" );

		List<Object[]> author_interests = sessionFactory.getCurrentSession()
				.createSQLQuery( "CALL nugraha.GetAuthorsInterests(:authorIDs)" )
				.setParameter( "authorIDs", authors ).list();

		if ( author_interests != null && !author_interests.isEmpty() )
		{
			for ( Object[] author_interest : author_interests )
			{
				String authorId = String.valueOf( author_interest[0] );
				String interests = String.valueOf( author_interest[1] );

				ArrayList<String> topScoreItems = new ArrayList<String>();
				List<String> snData = new ArrayList<>();

				//collecting all the interests for each author
				String[] interestItems = interests.split( "," );

				//creating interest graph for each author
				for( String id : interestItems ) 
				{
					topScoreItems.add(id);
					for (int j = 0; j < topScoreItems.size(); j++) 
					{
						if (!id.equals(topScoreItems.get(j))) 
						{
							snData.add(id
									+ ","
									+ topScoreItems.get(j));
						}
					}
				}

				//inserting items to author list.
				snDataArray.addAll( snData );

				JSONObject authorInterest = new JSONObject();
				authorInterest.put( "author", authorId );
				authorInterest.put( "interests", snData );
				coAuthorInterest.put( authorInterest );
			}
		}*/

		List<Object> profiles = sessionFactory.getCurrentSession()
				.createSQLQuery( "CALL nugraha.InterestGraph(:authorID, :coauthors, :update)" )
				.setParameter( "authorID", author )
				.setParameter( "update", 0 )
				.setParameter( "coauthors", coAuthorIDs.toString().replace( "[", "" ).replace( "]", "" )
						.replaceAll( ", ", "\',\'" ) ).list();

		String interests = null;
		if ( profiles != null && !profiles.isEmpty() )
		{
			if ( profiles.get( 0 ) instanceof String  ) 
			{
				interests = String.valueOf( profiles.get( 0 ) );
			}
			else 
			{
				for ( Object value : profiles )
				{
					Object[] profile = (Object[]) value;
					if ( profile != null && profile.length > 1 )
						interests = String.valueOf( profile[1] );
				}
			}
		}

		if (interests != null) 
		{
			String[] authInterests = interests.split( ";" );
			if ( authInterests != null && authInterests.length > 0 )
			{
				for ( String authInterest : authInterests )
				{
					String[] authIntValues = authInterest.split( ":" );
					if ( authIntValues != null && authIntValues.length > 1 )
					{
						String cAuthor = authIntValues[0];
						String[] ints = authIntValues[1].split( "," );

						ArrayList<String> topScoreItems = new ArrayList<String>();
						ArrayList<String> list = new ArrayList<>();
						for( String id : ints ) {
							topScoreItems.add(id);
							for (int j = 0; j < topScoreItems.size(); j++) {
								if (!id.equals(topScoreItems.get(j))) {
									list.add(id
											+ ","
											+ topScoreItems.get(j));
								}
							}
						}

						JSONObject authorInterest = new JSONObject();
						authorInterest.put( "author", cAuthor );
						authorInterest.put( "interests", list );
						coAuthorInterest.put( authorInterest );
					}
				}
			}
		}

		if ( coAuthorInterest.length() <= 0 )
		{
			//EUMAMUS: can have cold start problem
			for ( int i = 0; i < coAuthorIDs.size(); i++ )
			{
				List<String> list = topInterestFinder( coAuthorIDs.get( i ) );
				snDataArray.addAll( list );

				JSONObject authorInterest = new JSONObject();
				authorInterest.put( "author", coAuthorIDs.get( i ) );
				authorInterest.put( "interests", list );
				coAuthorInterest.put( authorInterest );
			}
		}
		return coAuthorInterest;
		/*for ( int i = 0; i < coAuthorIDs.size(); i+=increment )
		{
			//topInterestFinder( coAuthorIDs.get( i ) );
			int index = i;
			for ( ; index < coAuthorIDs.size(); index++ )
			{
				if ( ( i + increment ) <= index )
				{
					break;
				}
			}
			index--;

			TopicInterestWorker worker = new TopicInterestWorker( 
					coAuthorIDs.subList( i, index ).toArray( new String[] {} ) );

			try
			{
				Future<List<String>> result = executor.submit( worker );
				resultList.add( result );
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}*/

		/*boolean isCompleted = false;
		while (!isCompleted) {
		for ( Future<List<String>> future : resultList )
		{
			try
			{
				if(future.isDone()){
				List<String> list = future.get();
				System.out.println( "Interest size: " + list.size() );
				snDataArray.addAll( list );
				}
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}
		System.out.println( "Active threads: " + executor.getActiveCount() + "  -  completed: " + executor.getCompletedTaskCount() );

		try
		{
			Thread.sleep( 2 * 60 * 1000 );
		}
		catch (InterruptedException e1)
		{
		}
		}*/
	}

	public ArrayList<String> getCoAuthors( String rID, GraphModel model, boolean alreadyFiltered ) 
	{
		Column groupCol = model.getNodeTable().getColumn( "group" );
		Graph graph = null;
		if ( !alreadyFiltered )
		{
			ProjectController pc = Lookup.getDefault().lookup(
					ProjectController.class);
			pc.newProject();
			Workspace workspace = pc.getCurrentWorkspace();
			workspace.add( model );
			FilterController filterController = new FilterControllerImpl();//Lookup.getDefault().lookup( FilterController.class );	

			//DynamicRangeFilter dFilter = new DynamicRangeFilter( model );
			//Query dQuery = filterController.createQuery( dFilter );

			AttributeRangeBuilder.AttributeRangeFilter.Node attreFilt = 
					new AttributeRangeBuilder.AttributeRangeFilter.Node( groupCol );
			attreFilt.init( model.getUndirectedGraph() );
			attreFilt.setRange( new Range( 2, Integer.MAX_VALUE ) );
			Query query = filterController.createQuery( attreFilt );

			//filterController.add( dQuery );
			//filterController.add( query );
			//filterController.setSubQuery( query, dQuery );

			GraphView view = filterController.filter( query );
			graph = model.getUndirectedGraph( view );
		}
		else
		{
			graph = model.getUndirectedGraph();
		}

		ArrayList<String> coAuthorIDs = new ArrayList<>();
		for ( Node node : graph.getNodes() )
		{
			if ( node != null ) 
			{
				if ( Integer.valueOf( String.valueOf( node.getAttribute( groupCol ) ) ) > 1 )
					coAuthorIDs.add( String.valueOf( node.getId() ) );
			}
		}

		// set coAuthors from JSONArray from previous step
		/*if ( coAuthors != null && coAuthors.length() > 0 ) 
		{
			JSONObject obj = coAuthors.optJSONObject( 0 );
			if ( obj != null )
			{
				if ( obj.has( "nodes" ) )
				{
					JSONArray list = obj.optJSONArray( "nodes" );
					if ( list != null ) 
					{
						for ( int index = 0; index < list.length(); index++ ) 
						{
							JSONObject item = list.optJSONObject( index );
							if ( item != null && !rID.equals( item.optString( "id" ) ) && item.optInt( "group", 0 ) != 0 ) 
							{
								coAuthorIDs.add( item.optString( "id" ) );
							}
						}
					}
				}
			}
		}*/

		return coAuthorIDs;
	}

	/**
	 * Create Interest Based file
	 * 
	 * @param rID
	 * @throws SQLException
	 * @throws IOException
	 */
	@Override
	public JSONArray interestSNFileCreator ( String rID, GraphModel model ) throws SQLException,
	IOException {

		ArrayList<String> coAuthorIDs = getCoAuthors( rID, model, false );
		// clean snDataArray in this step
		snDataArray.clear();



		// check if no co-Author inserted yet
		if ( coAuthorIDs == null || coAuthorIDs.size() <= 0 )
		{
			long time = System.currentTimeMillis();
			System.out.println( "CoAuthors collection started." );
			coAuthorIDs = new ArrayList<>( get3DCoAuthors( rID ) );//UtilC3d.find3dAllRIds(auth);
			System.out.println( "CoAuthors collection completed in: " + (System.currentTimeMillis() - time) );
		}

		System.out.println( "CoAuthors size: " + coAuthorIDs.size() );

		coAuthorIDs.add( rID );
		long time = System.currentTimeMillis();
		System.out.println( "Interest collection started." );
		JSONArray coAuthorInterests = interestofCoFinder(rID, coAuthorIDs);
		System.out.println( "Interest collection completed in: " + (System.currentTimeMillis() - time) );


		return coAuthorInterests;
	}

	public List<String> getCoAuthors ( String rID )
	{
		Author auth = persistenceStrategy.getAuthorDAO().getById( rID );
		Set<Publication> pubs = auth.getPublications();
		List<String> coAuthors = new LinkedList<>();

		for ( Publication publication : pubs )
		{
			List<Author> coAuth = publication.getCoAuthors();
			for ( Author author : coAuth )
			{
				String id = author.getId();

				if ( !coAuthors.contains( id ) )
				{
					coAuthors.add( id );
				}
			}
		}

		return coAuthors;
	}

	public void resetCoAuthors() 
	{
		if ( this.co3DAuthors != null )
		{
			this.co3DAuthors.clear();
		}
		this.co3DAuthors = null;
	}

	public JSONArray get1DGraph( String researcherID )
	{
		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();
		JSONArray arr = new JSONArray();

		List<String> coAuthors = getCoAuthors( researcherID );

		for ( String ids : coAuthors )
		{
			if ( !ids.equalsIgnoreCase( researcherID ) )
			{
				Author author = dao.getById( ids );
				authorNodes.put( addNode( ids, author.getName(), "Author", 0, _AUTHOR_NODE_CODE, _DEFAULT_SIZE ) );
				authorLinks.put( addAuthorLink( researcherID, ids ) );
			}
		}

		JSONObject nodes = new JSONObject();
		nodes.put( "nodes", authorNodes );
		nodes.put( "links", authorLinks );
		nodes.put( "count", authorNodes.length() );
		arr.put( nodes );

		return arr;
	}

	public JSONArray getNDCoAuthorsGraph ( String researcherID, JSONArray co1DCoAuthors, int dimentionNumber )
	{
		JSONObject obj = co1DCoAuthors.optJSONObject( 0 );
		List<String> authND = new LinkedList<>();
		Set<String> uniqueNodes = new LinkedHashSet<>();

		JSONArray arr = new JSONArray();
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();
		AuthorDAO dao = persistenceStrategy.getAuthorDAO();

		uniqueNodes.add( researcherID );

		List result = sessionFactory.getCurrentSession()
				.createSQLQuery( "CALL nugraha.CoAuthorGraph( :researcher )" )
				.setParameter( "researcher", researcherID ).list();

		if ( result != null && !result.isEmpty() )
		{
			String string = String.valueOf( result.get( 0 ) );
			String[] coAuthors = string.split( ";" );
			if ( coAuthors != null && coAuthors.length > 1 )
			{
				for ( int i = 0; i < coAuthors.length; i++ )
				{
					String[] ND = coAuthors[i].split( ":" );
					if ( ND != null && ND.length > 1 )
					{
						Integer group = Integer.valueOf( ND[0] );
						String[] links = ND[1].split( "," );

						System.out.println( "Group: " + group );
						if ( group <= dimentionNumber && group > 1 )
						{
							for ( int l = 0; links != null && l < links.length; l++ )
							{
								String[] link = links[l].split( "\\." );
								if ( link != null && link.length > 1 )
								{
									for ( int index = 0; index < link.length; index++ )
									{
										if ( !uniqueNodes.contains( link[index] ) && group > 1 )
										{
											Author author = dao.getById( link[index] );
											authorNodes.put( addNode( link[index], author.getName(), "Author", 
													(group == 3 ? (group+1) : (group-1)), _AUTHOR_NODE_CODE, _DEFAULT_SIZE ) );
											uniqueNodes.add( link[index] );
										}
									}
									//ND coAuthor Links
									authorLinks.put( addAuthorLink( link[0], link[1] ) );
								}
							}
						}
					}
				}
			}
		}
		else
		{
			//authorNodes.put( addNode( rID, researcher.getName(), "Author", 0, 0 ) );

			//1D coAuthors
			authND = insertCoAuthorGraph( 0, authorNodes, authorLinks, uniqueNodes, new ArrayList<>( Arrays.asList( new String[]{ researcherID } ) ) );
			//if ( authND.size() > 50 ) authND = authND.subList( 0, 50 );
			//2D coAuthors
			authorNodes = null; authorLinks = null;
			authorNodes = new JSONArray(); authorLinks = new JSONArray();
			authND = insertCoAuthorGraph( 1, authorNodes, authorLinks, uniqueNodes, authND );
			//if ( authND.size() > 50 ) authND = authND.subList( 0, 50 );
			//3D coAuthors
			if ( dimentionNumber > 2 )
				authND = insertCoAuthorGraph( 4, authorNodes, authorLinks, uniqueNodes, authND );
		}

		/*if ( obj != null )
		{
			arr = obj.optJSONArray( "nodes" );
			for ( int i = 0; i < arr.length(); i++ )
			{
				JSONObject node = arr.optJSONObject( i );
				if ( node != null )
				{
					String id = node.optString( "id", null );
					if ( id != null )
					{
						authND.add( id );
						uniqueNodes.add( id );
					}
				}
			}
		}

		//authND = insertCoAuthorGraph( 0, authorNodes, authorLinks, uniqueNodes, new ArrayList<>( Arrays.asList( new String[]{ researcherID } ) ) );
		authND = insertCoAuthorGraph( 1, authorNodes, authorLinks, uniqueNodes, authND );
		authND = insertCoAuthorGraph( 2, authorNodes, authorLinks, uniqueNodes, authND );*/

		JSONObject nodes = new JSONObject();
		nodes.put( "nodes", authorNodes );
		nodes.put( "links", authorLinks );
		nodes.put( "count", authorNodes.length() );
		arr.put( nodes );

		return arr;
	}

	@Override
	public Collection<String> get3DCoAuthors( String rID ) 
	{
		//ArrayList<String> Co3DAuthors = new ArrayList<>();

		Author auth = persistenceStrategy.getAuthorDAO().getById( rID );
		List<String> co1DAuthors = runCoAuthors( auth.getId() );//getCoAuthors( auth, auth.getPublications() );
		long time = System.currentTimeMillis();
		List<String> co2DAuthors = runCoAuthors( co1DAuthors.toArray( new String[]{} ) );
		//System.out.println( "Time required: " + (System.currentTimeMillis() - time) );
		List<String> co3DAuthors = runCoAuthors( co2DAuthors.toArray( new String[]{} ) );
		System.out.println( "Time required: " + (System.currentTimeMillis() - time) );

		Set<String> coAuthorsList = new LinkedHashSet<>();
		//coAuthorsList.addAll( co1DAuthors );
		coAuthorsList.addAll( co2DAuthors );
		coAuthorsList.addAll( co3DAuthors );

		/*for ( Author author : coAuthorsList )
		{
			Co3DAuthors.add( author.getId() );
		}*/

		return coAuthorsList;
	}

	public Collection<String> get3rdDCoAuthors( String rID ) 
	{
		//ArrayList<String> Co3DAuthors = new ArrayList<>();

		Author auth = persistenceStrategy.getAuthorDAO().getById( rID );
		List<String> co1DAuthors = getCoAuthors( auth, auth.getPublications() );
		long time = System.currentTimeMillis();
		List<String> co2DAuthors = runCoAuthors( co1DAuthors.toArray( new String[]{} ) );
		//System.out.println( "Time required: " + (System.currentTimeMillis() - time) );
		List<String> co3DAuthors = runCoAuthors( co2DAuthors.toArray( new String[]{} ) );
		//System.out.println( "Time required: " + (System.currentTimeMillis() - time) );

		Set<String> coAuthorsList = new LinkedHashSet<>();
		//coAuthorsList.addAll( co1DAuthors );
		coAuthorsList.addAll( co3DAuthors );

		/*for ( Author author : coAuthorsList )
		{
			Co3DAuthors.add( author.getId() );
		}*/

		return coAuthorsList;
	}

	public JSONArray get3DCoAuthorsGraph ( String rID )
	{
		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		JSONArray arr = new JSONArray();

		Set<String> uniqueNodes = new LinkedHashSet<>();
		Author researcher = dao.getById( rID );
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		uniqueNodes.add( rID );

		List result = sessionFactory.getCurrentSession()
				.createSQLQuery( "CALL nugraha.CoAuthorGraph( :researcher )" )
				.setParameter( "researcher", rID ).list();

		if ( result != null && !result.isEmpty() )
		{
			String string = String.valueOf( result.get( 0 ) );
			String[] coAuthors = string.split( ";" );
			if ( coAuthors != null && coAuthors.length > 1 )
			{
				for ( int i = 0; i < coAuthors.length; i++ )
				{
					String[] ND = coAuthors[i].split( ":" );
					if ( ND != null && ND.length > 1 )
					{
						Integer group = Integer.valueOf( ND[0] );
						String[] links = ND[1].split( "," );
						for ( int l = 0; links != null && l < links.length; l++ )
						{
							String[] link = links[l].split( "\\." );
							if ( link != null && link.length > 1 )
							{
								for ( int index = 0; index < link.length; index++ )
								{
									if ( !uniqueNodes.contains( link[index] ) )
									{
										Author author = dao.getById( link[index] );
										authorNodes.put( addNode( link[index], author.getName(), "Author", 
												( group == 3 ? (group+1) : (group-1) ), _AUTHOR_NODE_CODE, _DEFAULT_SIZE ) );
										uniqueNodes.add( link[index] );
									}
								}
								//ND coAuthor Links
								authorLinks.put( addAuthorLink( link[0], link[1] ) );
							}
						}
					}
				}
			}
		}
		else
		{
			//authorNodes.put( addNode( rID, researcher.getName(), "Author", 0, 0 ) );

			//1D coAuthors
			List<String> authND = insertCoAuthorGraph( 0, authorNodes, authorLinks, uniqueNodes, new ArrayList<>( Arrays.asList( new String[]{ rID } ) ) );
			//if ( authND.size() > 50 ) authND = authND.subList( 0, 50 );
			//2D coAuthors
			//authorNodes = null; authorLinks = null;
			//authorNodes = new JSONArray(); authorLinks = new JSONArray();
			authND = insertCoAuthorGraph( 1, authorNodes, authorLinks, uniqueNodes, authND );
			//if ( authND.size() > 50 ) authND = authND.subList( 0, 50 );
			//3D coAuthors
			authND = insertCoAuthorGraph( 4, authorNodes, authorLinks, uniqueNodes, authND );
		}

		JSONObject nodes = new JSONObject();
		nodes.put( "nodes", authorNodes );
		nodes.put( "links", authorLinks );
		nodes.put( "count", authorNodes.length() );
		arr.put( nodes );

		return arr;
	}

	public JSONObject getGraphNode( Node node, GraphModel graphModel )
	{
		if ( graphModel == null )
		{
			ProjectController pc = Lookup.getDefault().lookup(
					ProjectController.class);
			pc.newProject();
			Workspace workspace = pc.getCurrentWorkspace();

			// Get graph model and attribute model of current workspace
			graphModel = workspace.getLookup().getDefault()
					.lookup(GraphController.class).getGraphModel();
		}

		Table table = graphModel.getNodeTable();
		Column stepCol = table.getColumn( "stepNo" );
		if ( stepCol == null )
			stepCol = table.addColumn( "stepNo", Integer.class );
		Column groupCol = table.getColumn( "group" );
		if ( groupCol == null )
			groupCol = table.addColumn( "group", Integer.class );
		Column titleCol = table.getColumn( "title" );
		if ( titleCol == null )
			titleCol = table.addColumn( "title", String.class );
		Column typeCol = table.getColumn( "type" );
		if ( typeCol == null )
			typeCol = table.addColumn( "type", Integer.class );
		Column sizeCol = table.getColumn( "size" );
		if ( sizeCol == null )
			sizeCol = table.addColumn( "size", Integer.class );


		JSONObject obj = new JSONObject();
		obj.put( "id", node.getId() );
		obj.put( "details", node.getLabel() );
		obj.put( "stepNo", node.getAttribute( stepCol ) );
		obj.put( "title", node.getAttribute( titleCol ) );
		obj.put( "group", node.getAttribute( groupCol ) );
		obj.put( "type", node.getAttribute( typeCol ) );
		obj.put( "size", node.getAttribute( sizeCol ) );
		/*if ( node.getLayoutData() instanceof ForceVector ) {
			System.out.println( "Value ForceVector x,y: " + ( ( ForceVector ) node.getLayoutData() ).x() + ", " + ( ( ForceVector ) node.getLayoutData() ).y() );
		}
		else if ( node.getLayoutData() instanceof ForceAtlas2LayoutData )
			System.out.println( "Value ForceAtlas2LayoutData x,y: " + ( ( ForceAtlas2LayoutData ) node.getLayoutData() ).dx + ", " + 
		( ( ForceAtlas2LayoutData ) node.getLayoutData() ).dy + "  -  " + ( ( ForceAtlas2LayoutData ) node.getLayoutData() ).mass + "  -  " + 
		( ( ForceAtlas2LayoutData ) node.getLayoutData() ).old_dx + "  -  " + ( ( ForceAtlas2LayoutData ) node.getLayoutData() ).old_dy);
		else if ( node.getLayoutData() instanceof ForceVectorNodeLayoutData )
			System.out.println( "Value ForceVectorNodeLayoutData : " + ( ( ForceVectorNodeLayoutData ) node.getLayoutData() ).dx + "  -  "
					+ ( ( ForceVectorNodeLayoutData ) node.getLayoutData() ).dy + "  -  " + + ( ( ForceVectorNodeLayoutData ) node.getLayoutData() ).freeze );
		else {
			if ( node.getLayoutData() != null )
			System.out.println( "Layout class: " + node.getLayoutData().getClass().getName() );
			System.out.println( "X and Y: " + node.x() + "  -  " + node.y() );
		}
		System.out.println( "X and Y: " + node.x() + "  -  " + node.y() );*/
		obj.put( "x", node.x() );
		obj.put( "y", node.y() );

		return obj;
	}

	public JSONArray getStepGraph( GraphModel graphModel, int stepNo )
	{
		UndirectedGraph graph = graphModel.getUndirectedGraphVisible();
		JSONArray arr = new JSONArray();
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();
		//int iterations = (int) Math.pow(graph.getNodeCount(), 2);


		for ( Node node : graph.getNodes() )
		{
			authorNodes.put( getGraphNode( node, graphModel ) );
			//node.setFixed( true );
		}

		System.out.println( "Nodes: " + graphModel.getUndirectedGraphVisible().getNodeCount() );
		System.out.println( "Edges: " + graphModel.getUndirectedGraphVisible().getEdgeCount() );

		for ( Edge edge : graph.getEdges() )
		{
			authorLinks.put( addAuthorLink( ( ( String ) edge.getSource().getId() ), ( ( String ) edge.getTarget().getId() ) ) );
		}

		JSONObject nodes = new JSONObject();
		nodes.put( "nodes", authorNodes );
		nodes.put( "links", authorLinks );
		nodes.put( "count", authorNodes.length() );
		arr.put( nodes );

		return arr;
	}

	public JSONArray createInterestGraph( JSONArray interestArray )
	{
		JSONArray arr = new JSONArray();
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		final String spliter = ",";

		int id = 0;
		InterestDAO dao = persistenceStrategy.getInterestDAO();

		List<String> nodes = new LinkedList<>();
		for ( int i = 0; i < interestArray.length(); i++ )
		{
			JSONObject array_element = interestArray.optJSONObject( i );

			if ( array_element != null )
			{
				JSONArray interests = array_element.optJSONArray( "interests" );
				String author = array_element.optString( "author" );

				for ( int interest = 0; interest < interests.length(); interest++ )
				{
					String line = interests.optString( interest );
					String[] ns = line.split(spliter);
					for ( int index = 0; index < ns.length; index++ ) 
					{
						if( !nodes.contains( ns[ index ] ) ) 
						{
							String term = dao.getById( ns[index] ).getTerm();
							nodes.add( ns[ index ] );
							authorNodes.put( addNode( ns[ index ], term, "Interest", 0, _INTERST_NODE_CODE, _DEFAULT_SIZE ) );
							id++;
						}
					}
					authorLinks.put( addAuthorLink( ns[ 0 ], ns[ 1 ] ) );

					// connecting author with interest
					authorLinks.put( addAuthorLink( author, ns[0] ) );
					authorLinks.put( addAuthorLink( author, ns[1] ) );
				}
			}
		}

		JSONObject json = new JSONObject();
		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );
		json.put( "count", authorNodes.length() );
		arr.put( json );

		return arr;
	}

	private List<String> insertCoAuthorGraph ( int group, JSONArray authorNodes, JSONArray authorLinks, Set<String> uniqueNodes, 
			List<String> authors )
	{
		List<String> coAuthors = new LinkedList<String>();

		//ND coAuthors
		/*for ( Author author : authors )
		{
			String r1D = author.getId();

			Set<Publication> pubsND = author.getPublications();
			for ( Publication pubND : pubsND )
			{
				List<Author> authsND = pubND.getCoAuthors();
				for ( Author authND : authsND )
				{
					String rND = authND.getId();

					if ( !rND.equals( r1D ) )
					{
						if ( !uniqueNodes.contains( rND ) )
						{
							authorNodes.put( addAuthorNode( rND, authND.getName(), group ) );
							coAuthors.add( authND );
							uniqueNodes.add( rND );
						}

						//ND coAuthor Links
						authorLinks.put( addAuthorLink( r1D, rND ) );
					}

				}
			}
		}*/

		List<Object[]> co1DAuthors = sessionFactory.getCurrentSession()
				.createSQLQuery( "CALL nugraha.Get1DCoAuthors(:IDs)" )
				.setCacheMode( CacheMode.GET )
				.setParameter( "IDs", authors.toString().replace( "[", "" ).replace( "]", "" )
						.replaceAll( ", ", "\',\'" ) )
				.list();

		for ( Object[] objects : co1DAuthors )
		{
			if ( objects.length > 1 )
			{
				String authorId = String.valueOf( objects[0] ); 
				String coAuthorId = String.valueOf( objects[1] );
				String coAuthorName = String.valueOf( objects[2] );

				if ( !uniqueNodes.contains( coAuthorId ) )
				{
					authorNodes.put( addNode( coAuthorId, coAuthorName, "Author", group, _AUTHOR_NODE_CODE, _DEFAULT_SIZE ) );
					coAuthors.add( coAuthorId );
					uniqueNodes.add( coAuthorId );
				}

				//ND coAuthor Links
				authorLinks.put( addAuthorLink( authorId, coAuthorId ) );
			}
		}

		return coAuthors;
	}

	public JSONObject addNode ( String id, String authName, String title, int group, int type, int size )
	{
		JSONObject author = new JSONObject();
		author.put( "title", title );
		author.put( "details", authName );
		author.put( "group", group );
		author.put( "type", type );
		author.put( "size", size );
		author.put( "id", id );
		return author;
	}

	public JSONObject addAuthorLink ( String source, String target )
	{
		JSONObject link = new JSONObject();
		link.put( "source", source );
		link.put( "target", target );
		return link;
	}

	private List<String> runCoAuthors ( String...auth ) {
		Set<String> coAuthos = new LinkedHashSet<>();

		String query = "SELECT DISTINCT P.author FROM PublicationAuthor P WHERE P.publication IN ("
				+ " SELECT DISTINCT S.publication FROM PublicationAuthor S WHERE S.author IN ("
				+ " FROM Author WHERE id IN (:authors) " + ") )";

		String squery = "SELECT DISTINCT P.author_id FROM publication_author P WHERE P.publication_id IN ("
				+ " SELECT DISTINCT S.publication_id FROM publication_author S WHERE S.author_id IN ("
				+ " FROM author WHERE id IN (:authors) " + ") )";
		String authors = Arrays.asList( auth ).toString().replace( "[", "" ).replace( "]", "" )
				.replaceAll( ", ", "\',\'" );

		List<String> list = sessionFactory.getCurrentSession().createSQLQuery( "call nugraha.GetCoAuthors(:authorsIds)" )
				.setParameter( "authorsIds", authors ).list();
		return list;
		/*List<String> queryResult = sessionFactory.openSession().createSQLQuery( squery )
				.setParameterList( "authors", Arrays.asList( auth ) ).list();
		return new ArrayList<>( queryResult );*/
		/*List<Author> queryResult = sessionFactory.openSession().createQuery( query )
				.setParameterList( "authors", Arrays.asList( auth ) ).list();

		for ( Author author : queryResult )
		{
			coAuthos.add( author.getId() );
		}*/
		/*for ( int i = 0; i < auth.length; i+=increment )
		{
			int index = i;
			for ( ; index < auth.length; index++ )
			{
				if ( ( i + increment ) <= index )
				{
					break;
				}
			}
			index--;

			CoAuthorWorkerThread thread = new CoAuthorWorkerThread( Arrays.copyOfRange( auth, i, index ) );
			try
			{
				Future<List<String>> result = executor.submit( thread );//thread.getAsyncCoAuthorItem( thread );
				resultList.add( result );
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			Author author = persistenceStrategy.getAuthorDAO().getById( auth[i] );
			coAuthos.addAll( getCoAuthors( author, author.getPublications() ) );
		}
		System.out.println( "Measurement started: PoolSize: " + executor.getTaskCount() + " - active: " + 
				executor.getActiveCount() );
		boolean isCompleted = false;
		int count = 20, shouldCheckAfter = 0;

		while ( !isCompleted )
		{
			System.out.println( "Remaining: " + resultList.size() + "  -  Active Threads: " + executor.getActiveCount() );

			if ( resultList.isEmpty() || count <= 0 )
			{
				break;
			}

			List<Future<List<String>>> cloneList = new LinkedList<>( resultList );

			for ( Future<List<String>> future : cloneList )
			{
				try
				{
					if ( future.isDone() || shouldCheckAfter >= 3 )
					{
						List<String> list = future.get( );
						coAuthos.addAll( list );

						resultList.remove( future );
						shouldCheckAfter = 0;
					} 
					else if ( future.isCancelled() )
					{
						System.out.println( "Measurement Cancelled." );
						resultList.remove( future );
					}
				}
				catch (InterruptedException | ExecutionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			count--;
			shouldCheckAfter++;

			try
			{
				Thread.sleep( 30 * 1000 );
			}
			catch (InterruptedException e) { }
		}
		executor.shutdownNow();

		for ( Future<List<String>> future : resultList )
		{
			try
			{
				List<String> list = future.get( );
				coAuthos.addAll( list );
			}
			catch (InterruptedException | ExecutionException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/


		//return new ArrayList<>( coAuthos );
	}

	private List<Author> getCoAuthors( Author author ) {
		if ( co3DAuthors == null )
		{
			co3DAuthors = new LinkedList<>();
		}
		List<Author> coAuthors = new LinkedList<>();
		Set<Publication> listPubs = author.getPublications();
		if ( listPubs != null )
		{
			for ( Publication pub : listPubs )
			{
				List<Author> listAuth = pub.getCoAuthors();
				for ( Author a : listAuth ) {
					String cID = a.getId();
					if ( !cID.equals( author.getId() ) && !co3DAuthors.contains( cID ) )
					{
						coAuthors.add( a );
						co3DAuthors.add( cID );
					}
				}
			}
		}

		return coAuthors;
	}

	private List<String> getCoAuthors( Author author, Set<Publication> listPubs ) {
		if ( co3DAuthors == null )
		{
			co3DAuthors = new LinkedList<>();
		}
		List<String> coAuthors = new LinkedList<>();
		if ( listPubs != null )
		{
			for ( Publication pub : listPubs )
			{
				List<Author> listAuth = pub.getCoAuthors();
				for ( Author a : listAuth ) {
					String cID = a.getId();
					if ( !cID.equals( author.getId() ) && !co3DAuthors.contains( cID ) )
					{
						coAuthors.add( cID );
						co3DAuthors.add( cID );
					}
				}
			}
		}

		return coAuthors;
	}


	//this method is like UtilC3d.userPairsAdder();
	private List<String> getCoAuthors ( List<String> coAuthorIDs, String...authorIDs )
	{
		if ( coAuthorIDs == null )
		{
			coAuthorIDs = new LinkedList<>();
		}
		if ( co3DAuthors == null )
		{
			co3DAuthors = new LinkedList<>();
		}
		List<String> coAuthors = new LinkedList<>();

		/*if ( !co3DAuthors.contains( authorID ) )
		{
			AuthorDAO dao = persistenceStrategy.getAuthorDAO();
			Author author = dao.getById( authorID );

			Set<Publication> pubs = author.getPublications();
			for ( Publication publication : pubs )
			{
				List<Author> authors = publication.getCoAuthors();
				for ( Author coAuthor : authors )
				{
					String id = coAuthor.getId();
					if ( !id.equals( authorID ) )
					{
						coAuthorIDs.add( authorID + "," + coAuthor.getId() );
						coAuthors.add( coAuthor );
					}
				}
			}

			co3DAuthors.add( authorID );
		}*/

		List<String> authors = new ArrayList<>();
		if ( Collections.disjoint( co3DAuthors, Arrays.asList( authorIDs ) ) )
		{
			authors = Arrays.asList( authorIDs );
			co3DAuthors.addAll( Arrays.asList( authorIDs ) );
		}
		else if ( !co3DAuthors.containsAll( Arrays.asList( authorIDs ) ) )
		{
			for ( String id : authorIDs )
			{
				if ( !co3DAuthors.contains( id ) )
				{
					authors.add( id );
				}
				co3DAuthors.addAll( authors );
			}
		} else 
		{
			return coAuthors;
		}

		String calles = authors.toString()
				.replace( "[", "" ).replace( "]", "" )
				.replaceAll( ", ", "\',\'" );

		long time = System.currentTimeMillis();
		List<Object[]> co1DAuthors = sessionFactory.getCurrentSession()
				.createSQLQuery( "CALL nugraha.Get1DCoAuthors(:IDs)" )
				.setCacheMode( CacheMode.GET )
				.setParameter( "IDs", calles )
				.list();

		for ( Object[] objects : co1DAuthors )
		{
			if ( objects.length > 1 )
			{
				coAuthorIDs.add( String.valueOf( objects[0] ) 
						+ "," + String.valueOf( objects[1] ) );
				coAuthors.add( String.valueOf( objects[1] ) );
			}
		}

		return coAuthors;
	}

	public List<String> createCoAuthorGraph( String authorID, int ND )
	{
		boolean is3D = ND == 3;
		co3DAuthors = null;
		co3DAuthors = new LinkedList<>();
		List<String> coAuthorsGraph = new LinkedList<>();

		long time = System.currentTimeMillis();

		System.out.println( "CoAuthors calculation started." );
		//1D coAuthors are added in the graph.

		String viewName = "coAuthor" + ND + "ID" + authorID.replaceAll( "-", "" );

		String query = "SHOW TABLES LIKE \'" + viewName + "\'x";

		/*List list = sessionFactory.getCurrentSession().createSQLQuery( query ).list();

		if ( list == null || list.isEmpty() )
		{*/
		List<String> c1DAuthors = getCoAuthors( coAuthorsGraph, authorID );

		if ( ND > 2 )
		{
			coAuthorsGraph = null;
			coAuthorsGraph = new LinkedList<>();
		}

		for ( int i = 1; i < ND; i++ )
		{
			List<String> cNDAuthors = getCoAuthors( coAuthorsGraph, c1DAuthors.toArray( new String[]{} ) );
			c1DAuthors = null;
			c1DAuthors = new ArrayList<>( cNDAuthors );
		}
		//}

		/*List<Author> c1DAuthors = getCoAuthors( authorID, coAuthorsGraph );

		for ( Author c1DAuthor : c1DAuthors )
		{
			String id = c1DAuthor.getId();

			//2D coAuthors are added in the graph
			List<Author> c2DAuthors = getCoAuthors( id, coAuthorsGraph );

			if ( is3D )
			{
				for ( Author c2DAuthor : c2DAuthors )
				{
					String c2DID = c2DAuthor.getId();

					//3D coAuthors are added in the graph
					getCoAuthors( c2DID, coAuthorsGraph );
				}
			}
		}*/
		System.out.println( "CoAuthors calculation completed in: " + (System.currentTimeMillis() - time) );

		return coAuthorsGraph;
	}

	@Component
	@Scope("session")
	private class CoAuthorWorkerThread implements Callable<List<String>>
	{

		private String[] author = null;
		private Set<Publication> listPubs;

		private List<String> coAuthors = null;

		@Autowired( required = true )
		private SessionFactory session = null;

		private AuthorDAO authorDAO;

		@Autowired( required = true )
		public void setAuthorDAO( AuthorDAO authorDAO )
		{
			this.authorDAO = authorDAO;
		}

		public CoAuthorWorkerThread( String...author )
		{
			this.author = author;
			this.coAuthors = new LinkedList<>();
		}


		public List<String> getcoAuthors() 
		{
			return this.coAuthors;
		}

		public Future<List<String>> getAsyncCoAuthorItem( CoAuthorWorkerThread worker ) throws Exception 
		{
			return new AsyncResult<List<String>>( worker.call() );
		}

		@Override
		public List<String> call()
		{
			if( this.authorDAO == null ) 
			{
				if( this.session == null )
					authorDAO = new AuthorDAOHibernate( sessionFactory );
				else
					authorDAO = new AuthorDAOHibernate( this.session );
			}
			Thread t = Thread.currentThread();
			t.setPriority( Thread.MAX_PRIORITY );

			for ( String id : this.author ) {
				Author author = authorDAO.getById( id );
				coAuthors.addAll( getCoAuthors( author, author.getPublications() ) );
			}
			//System.out.println( "Thread No: " + t.getName() + " completed with size: " + coAuthors.size() );
			//System.out.println( "Ids: " + new ArrayList<String>(Arrays.asList( this.author )) );
			return coAuthors;
		}
	}

	/**
	 * It finds a researcher who has the highest rating of an Item
	 * 
	 * @param allCoIds
	 * @param Interest
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws SQLException
	 */
	static int topRofIFinder(ArrayList<Integer> allCoIds, String Interest)
			throws JSONException, IOException, SQLException {
		String rIDQuery = "";
		int rID = 0;
		for (int i = 0; i < allCoIds.size(); i++) {
			rIDQuery += "\"" + allCoIds.get(i) + "\"" + ",";
		}
		rIDQuery = UtilService.lastCharRemover(rIDQuery);
		Connection myConn = DbService.ConnectToDB();
		Statement myStmt = myConn.createStatement();
		ResultSet result = myStmt
				.executeQuery("select authorid,result,score from results WHERE result='"
						+ Interest
						+ "' AND authorid IN ("
						+ rIDQuery
						+ ") ORDER BY score DESC;");
		while (result.next()) {
			rID = result.getInt("authorid");
		}
		if (myStmt != null)
			myStmt.close();
		if (myConn != null)
			myConn.close();
		return rID;
	}

	public String topRofIFinderString( ArrayList<String> allCoIds, String Interest )
			throws JSONException, IOException, SQLException {
		String authors = allCoIds.toString().replace( "[", "" ).replace( "]", "" )
				.replaceAll( ", ", "\',\'" );

		List<String> list = sessionFactory.getCurrentSession().createSQLQuery( "call nugraha.GetTopResearcherIds(:authorsIds,:term)" )
				.setTimeout( 60 ).setParameter( "authorsIds", authors ).setParameter( "term", Interest ).list();
		return ((list == null || list.isEmpty()) ? null : list.get( 0 ) );
	}

	public List<String> topRofIFinderString( ArrayList<String> allCoIds, String Interest, int maxResults )
			throws JSONException, IOException, SQLException {
		String authors = allCoIds.toString().replace( "[", "" ).replace( "]", "" )
				.replaceAll( ", ", "\',\'" );

		List<String> list = sessionFactory.getCurrentSession().createSQLQuery( "call nugraha.GetTopResearcherIds(:authorsIds,:term)" )
				.setTimeout( 60 ).setParameter( "authorsIds", authors ).setParameter( "term", Interest ).list();

		if ( list != null && !list.isEmpty() )
		{
			if ( list.size() > maxResults )
				return list.subList( 0, maxResults );
			else
				return list;
		}

		return null;
	}

	public int getCommonInterests( String rID1, String rID2 )
	{
		int count = 0;
		List<String> intId1 = getInterestIds( rID1 );
		List<String> intId2 = getInterestIds( rID2 );

		for (int i = 0; i < intId1.size(); i++) {
			if (intId2.contains(intId1.get(i))) {
				count++;
			}
		}

		return count;
	}

	public double FindJaccardSimilarity( String rID1, String rID2 ) 
	{
		List<String> intId1 = getInterestIds( rID1 );
		List<String> intId2 = getInterestIds( rID2 );

		ArrayList<String> intersect = new ArrayList<String>();
		ArrayList<String> union = new ArrayList<String>();

		intersect.clear();
		intersect.addAll(intId1);
		intersect.retainAll(intId2);
		union.clear();
		union.addAll(intId1);
		union.addAll(intId2);
		double result = (double) intersect.size() / (double) union.size();
		return result;
	}

	private List<String> getInterestIds( String rID ) {
		try
		{
			Map<String, Object> result = getAuthorInterestById( rID );
			if ( result.get( "status" ).equals( "Ok" ) ) 
			{
				LinkedList<Object> interests = ( LinkedList<Object> ) result.get( "interest" );
				LinkedList<String> intId = new LinkedList<>();

				//inserting all interest ids from researcher 1
				for ( int i=0; i < interests.size(); i++ )
				{
					ArrayList<Object> items1 = ( ArrayList<Object> ) interests.get( i );
					intId.add( (String) items1.get( 0 ) );
				}

				return intId;
			}
		}
		catch (UnsupportedEncodingException | InterruptedException | URISyntaxException | ExecutionException e)
		{}

		return new LinkedList<>();
	}

	public JSONArray addCommonInterestListToJson( JSONArray FinalJsonArray,
			ArrayList<String> rIDs, String researcherID ) 
					throws JSONException {

		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		InterestDAO idao = persistenceStrategy.getInterestDAO();
		List<String> listR1 = getInterestIds(researcherID);
		for (int j = 0; j < rIDs.size(); j++) {
			JSONArray commonIListArray = new JSONArray();
			JSONObject obj3 = new JSONObject();
			JSONArray jsonArray2 = new JSONArray();
			// System.out.println("rID size is : "+ rIDs.size());
			List<String> listR2 = getInterestIds(rIDs.get(j));
			JSONObject obj = new JSONObject();
			obj.put( "rName", dao.getById( researcherID ).getName() );
			obj.put( "simRName", dao.getById( rIDs.get(j) ).getName() );
			for (int i = 0; i < listR1.size(); i++) {
				if (listR2.contains(listR1.get(i))) {
					JSONObject obj1 = new JSONObject();
					obj1.put( "iName", idao.getById( listR1.get(i) ).getTerm() );
					commonIListArray.put( obj1 );
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

	public JSONArray addPubsOfIToJson ( JSONArray cfCResult,
			ArrayList<String> rIDs, Map<String, Number> iNames, PublicationExtractionService service )
	{
		//System.out.println( "Names list: " + iNames.size() + " - " + rIDs.size() );

		JSONArray publicationsResult = new JSONArray();
		String query = "SELECT DISTINCT pub.id, pub.title, pub.authorText, pub.abstractText, pub.keywordText FROM publication pub " +
				"LEFT JOIN publication_author pub_auth ON pub.id=pub_auth.publication_id " +
				"WHERE (INSTR(pub.title, :term) OR INSTR(pub.abstractText, :term) OR INSTR(pub.keywordText, :term)) AND " +
				"pub_auth.author_id IN (:authorIds) ORDER BY pub_auth.position_ DESC";

		List<Future<JSONArray>> results = new LinkedList<>();
		List<String> interestItems = new LinkedList<>( iNames.keySet() );
		for (int i = 0; i < interestItems.size(); i++) 
		{
			/*List<Object[]> pubs = sessionFactory.getCurrentSession()
					//.createQuery( "FROM PublicationAuthor" )
					.createSQLQuery( query )
					.setParameter( "term", iNames.get( i ) )
					.setParameterList( "authorIds", rIDs ).list();*/
			try
			{

				results.add( service.getPublicationData( rIDs, interestItems.get( i ), ( ( Integer ) iNames.get( interestItems.get( i ) ) ) ) );
			}
			catch (InterruptedException e)
			{
				System.out.println( "String : " + e.getMessage() );
			}
			/*JSONArray pubOfIJsonArray = new JSONArray();
			JSONObject pubofIObj = new JSONObject();

			List<String> list = sessionFactory.getCurrentSession()
					.createSQLQuery( "call nugraha.GetTopResearcherIds(:authorsIds,:term)" )
					.setTimeout( 60 ).setParameter( "authorsIds", authors )
					.setParameter( "term", iNames.get( i ) ).list();

			List<Object[]> pubs = sessionFactory.getCurrentSession()
					//.createQuery( "FROM PublicationAuthor" )
					.createSQLQuery( query )
					.setParameter( "term", iNames.get( i ) )
					.setParameterList( "authorIds", rIDs ).list();

			//pubs = ( pubs.size() > 50 ? pubs.subList( 0, 50 ) : pubs );

			for ( Object[] publication : pubs )
			{
				//Publication publication = pubDao.getById( pubID.toString() );

				String id = String.valueOf( publication[0] );
				String title = String.valueOf( publication[1] );
				String author = String.valueOf( publication[2] );
				String abstrac = String.valueOf( publication[3] );
				String keyword = String.valueOf( publication[4] );

				JSONObject obj = new JSONObject();
				obj.put( "iName", iNames.get(i));
				obj.put( "pID", id); //publication.getId() );
				obj.put( "pTitle", title); //publication.getTitle() );
				obj.put( "pAuthors", author);//publication.getAuthors().toString().replace( "[", "" ).replace( "]", "" ));
				obj.put( "pAbstract", abstrac);//publication.getAbstractText() );
				obj.put( "pKeywords", keyword);//publication.getKeywordText() );
				pubOfIJsonArray.put( obj );

			}*/

			/*for (int j = 0; j < rIDs.size(); j++) 
			{
				Author author = persistenceStrategy.getAuthorDAO().getById( rIDs.get( j ) );
				Set<Publication> pubs = author.getPublications();
				//List<Publication> pubs = persistenceStrategy.getPublicationDAO().getAll();

				for ( Publication publication : pubs )
				{
					Set<PublicationTopic> topcis = publication.getPublicationTopics();
					boolean checker = false;
					for ( PublicationTopic publicationTopic : topcis )
					{
						checker = publicationTopic.getTermValues().keySet().contains( iNames.get( i ) );
						if ( checker )
						{
							break;
						}
					}
					boolean checker = ( publication.getTitle() != null && 
									publication.getTitle().toLowerCase().contains( iNames.get( i ) ) ) ||
							( publication.getAbstractText() != null &&
									publication.getAbstractText().toLowerCase().contains( iNames.get( i ) ) ) ||
							( publication.getKeywords() != null &&
									publication.getKeywords().contains( iNames.get( i ) ) );

					if ( checker && !(checkedPTitle.contains( publication.getTitle() )) )
					{
						//System.out.println( "Publication name: " + publication.getTitle() );
						checkedPTitle.add( publication.getTitle() );
						JSONObject obj = new JSONObject();
						obj.put( "iName", iNames.get(i));
						obj.put( "pID", publication.getId() );
						obj.put( "pTitle", publication.getTitle() );
						obj.put( "pAuthors", publication.getAuthors().toString().replace( "[", "" ).replace( "]", "" ));
						obj.put( "pAbstract", publication.getAbstractText() );
						obj.put( "pKeywords", publication.getKeywordText() );
						pubOfIJsonArray.put( obj );
					}
				}
			}*/
		}

		int index = 0;
		for ( Future<JSONArray> resultItem : results )
		{
			try
			{
				JSONArray pubOfIJsonArray = resultItem.get(1, TimeUnit.MINUTES);
				JSONObject pubofIObj = new JSONObject();
				pubofIObj.put("publicationsOfI" + (index++), pubOfIJsonArray);
				cfCResult.put(pubofIObj);
				publicationsResult.put( pubOfIJsonArray );
			}
			catch (InterruptedException | ExecutionException | TimeoutException e)
			{
				System.out.println( "Publication error: " + e.getMessage() );
			}
		}

		return publicationsResult;
	}

	/**
	 * // Create Json file for Degree
	 * 
	 * @param rDegree
	 * @param rIDName3
	 * @return
	 * @throws JSONException
	 */
	/*static JSONArray DegreeJsonCreator(Map<Integer, Integer> rDegree,
			Map<Integer, String> rIDName3) throws JSONException {
		JSONArray recBasedDegree = new JSONArray();
		for (Entry<Integer, Integer> entry : rDegree.entrySet()) {
			JSONObject obj = new JSONObject();
			obj.put("ID", entry.getKey());
			obj.put("itemName", rIDName3.get(entry.getKey()));
			obj.put("DegreeValue", entry.getValue());
			recBasedDegree.put(obj);
		}
		return recBasedDegree;
	}*/
	public static JSONArray DegreeJsonCreator(Map<String, String> rIDName,
			Map<String, Integer> rDegree) throws JSONException {
		JSONArray recBasedDegree = new JSONArray();
		for (Entry<String, Integer> entry : rDegree.entrySet()) {
			JSONObject obj = new JSONObject();
			obj.put("ID", entry.getKey());
			obj.put("itemName", rIDName.get(entry.getKey()));
			obj.put("DegreeValue", entry.getValue());
			recBasedDegree.put(obj);
		}
		// System.out.println(recBasedBetweenness);
		return recBasedDegree;
	}

	public JSONArray createDegreeGraph( JSONArray degreeJson ) 
	{
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		for ( int i = 0; i < degreeJson.length(); i++ )
		{
			JSONObject obj = degreeJson.optJSONObject( i );

			if ( obj != null )
			{
				String termID = obj.optString( "ID" );
				String term = obj.optString( "itemName" );
				int termDegree = obj.optInt( "DegreeValue" );

				authorNodes.put( addNode( termID, (term + "</br>Degree: " + termDegree), "Interest", termDegree, _INTERST_NODE_CODE, _DEFAULT_SIZE ) );
			}
		}

		JSONObject json = new JSONObject();
		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );
		json.put( "count", authorNodes.length() );

		JSONArray arr = new JSONArray();
		arr.put( json );
		return arr;
	}

	public JSONArray createTopNAuthorsGraph( JSONArray step3Authors )
	{
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		// inserting top authors
		for ( int i = 0; i < step3Authors.length(); i++ )
		{
			JSONObject obj = step3Authors.optJSONObject( i );
			if ( obj != null )
			{
				String authorId = obj.optString( "rID", "" );
				String authorName = obj.optString( "rName", "" );
				String termName = obj.optString( "ExpertIn", "" );
				//int interests = obj.optInt( "NofCommonInterest", 0 );
				long jFactor = obj.optLong( "SimInPercent", 0 );

				//inserting author node.
				authorNodes.put( addNode( authorId, (authorName + "</br>Degree: " + jFactor), "Author", ((int)jFactor), _AUTHOR_NODE_CODE, _DEFAULT_SIZE ) );
			}
		}

		JSONObject json = new JSONObject();
		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );
		json.put( "count", authorNodes.length() );

		JSONArray arr = new JSONArray();
		arr.put( json );
		return arr;
	}

	public JSONArray createTopInterestGraph( JSONArray topInterest )
	{
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		for ( int i = 0; i < topInterest.length(); i++ )
		{
			JSONObject obj = topInterest.optJSONObject( i );

			if ( obj != null )
			{
				String id = obj.optString( "ID" );
				String term = obj.optString( "itemName" );
				String authorId = obj.optString( "AuthorID" );
				double value = obj.optDouble( "DegreeValue", 1.0 );

				authorNodes.put( addNode( id, ( term + "</br>Degree: " + value ), "Interest", ( ( int ) value ), _INTERST_NODE_CODE, _DEFAULT_SIZE ) );
				authorLinks.put( addAuthorLink( authorId, id ) );
			}
		}

		JSONObject json = new JSONObject();
		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );
		json.put( "count", authorNodes.length() );

		JSONArray arr = new JSONArray();
		arr.put( json );
		return arr;
	}

	public JSONArray createTop10AuthorsGraph( JSONArray topAuthors, JSONArray topInterest ) 
	{
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		Map<String, String> interestMap = new LinkedMap();
		//getting top interests
		for ( int i = 0; i < topInterest.length(); i++ )
		{
			JSONObject obj = topInterest.optJSONObject( i );

			if ( obj != null )
			{
				String termID = obj.optString( "ID" );
				String term = obj.optString( "itemName" );
				//int termDegree = obj.optInt( "DegreeValue" );
				interestMap.put( term, termID );
			}
		}

		// inserting top authors
		for ( int i = 0; i < topAuthors.length(); i++ )
		{
			JSONObject obj = topAuthors.optJSONObject( i );
			if ( obj != null )
			{
				String authorId = obj.optString( "rID", "" );
				String authorName = obj.optString( "rName", "" );
				String termName = obj.optString( "ExpertIn", "" );
				//int interests = obj.optInt( "NofCommonInterest", 0 );
				double jFactor = obj.optDouble( "SimInPercent", 0.0 );

				//inserting author node.
				authorNodes.put( addNode( authorId, (authorName + "</br>Degree: " + jFactor), "Author", ((int)jFactor), _AUTHOR_NODE_CODE, _DEFAULT_SIZE ) );
				authorLinks.put( addAuthorLink( authorId, interestMap.get( termName ) ) );
				for ( String item : interestMap.keySet() )
				{
					if ( !item.equals( termName ) )
					{
						try
						{
							String id = topRofIFinderString( new ArrayList<String>(Arrays.asList( new String[]{authorId} )), item );
							if ( id != null )
							{
								authorLinks.put( addAuthorLink( id, interestMap.get( item ) ) );
							}
						}
						catch (JSONException | IOException | SQLException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}

		JSONObject json = new JSONObject();
		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );
		json.put( "count", authorNodes.length() );

		JSONArray arr = new JSONArray();
		arr.put( json );
		return arr;
	}

	public JSONArray createCoAuthorPublicationGraph( JSONArray publications, JSONArray topAuthors, JSONArray interestsDegree, JSONArray interestLinks )
	{
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		List<String> authors = new LinkedList<>();
		//getting top authors
		JSONObject graph = topAuthors.optJSONObject( 0 );
		if ( graph != null )
		{
			JSONArray nodes = graph.optJSONArray( "nodes" );
			JSONArray links = graph.optJSONArray( "links" );

			authorNodes = nodes;
			authorLinks = links;
		}

		graph = interestLinks.optJSONObject( 0 );
		if ( graph != null )
		{
			JSONArray links = graph.optJSONArray( "links" );

			for ( int i = 0; i < links.length(); i++ )
			{
				JSONObject obj = links.optJSONObject( i );
				authorLinks.put( obj );
			}
		}
		
		Map<String, String> interestMap = new LinkedMap();
		for ( int i = 0; i < interestsDegree.length(); i++ )
		{
			JSONObject obj = interestsDegree.optJSONObject( i );

			if ( obj != null )
			{
				String termID = obj.optString( "ID" );
				String term = obj.optString( "itemName" );
				int termDegree = obj.optInt( "DegreeValue" )+1;
				interestMap.put( term, termID );

				authorNodes.put( addNode( termID, (term + "</br>Degree: " + termDegree), "Interest", termDegree, _INTERST_NODE_CODE, _DEFAULT_SIZE ) );
			}
		}

		/*Map<String, List<String>> authorMap = new LinkedMap();
		for ( String term : interestMap.keySet() )
		{
			String termId = interestMap.get( term );
			for ( int i = 0; i < authorLinks.length(); i++ )
			{
				String intLink = null;
				JSONObject authorlink = authorLinks.optJSONObject( i );
				if ( termId.equals( authorlink.optString( "source", "" ) ) )
				{
					intLink = authorlink.optString( "target" );
				}
				else if ( termId.equals( authorlink.optString( "target", "" ) ) )
				{
					intLink = authorlink.optString( "source" );
				}

				if ( intLink != null )
				{
					List<String> authorsList = authorMap.get( term );
					if ( authorsList == null )
						authorsList = new LinkedList<>();
					authorsList.add( intLink );
					authorMap.put( term, authorsList );
				}
			}
		}*/

		for ( int i = 0; i < publications.length(); i++ )
		{
			JSONArray pubI = publications.optJSONArray( i );
			for ( int j = 0; j < pubI.length(); j++ )
			{
				JSONObject obj = pubI.optJSONObject( j );
				if ( obj != null )
				{
					String termName = obj.optString( "iName");
					String pubID = obj.optString( "pID"); 
					String pubTitle = obj.optString( "pTitle");
					String pubAuthor = obj.optString( "pAuthors");
					String pubAbstract = obj.optString( "pAbstract");
					String pubkeyword = obj.optString( "pKeywords");
					int termValue = obj.optInt( "iValue", 0 ) + 1;

					authorNodes.put( addNode( pubID, ( pubTitle + "</br>Degree: " + termValue), "Publication", termValue, _PUBLICATION_NODE_CODE, _DEFAULT_SIZE ) );
					authorLinks.put( addAuthorLink( pubID, interestMap.get( termName ) ) );

					String[] coAuthors = pubAuthor.split( "," );
					if ( coAuthors != null )
					{
						for ( String author : coAuthors )
						{
							authorLinks.put( addAuthorLink( pubID, author ) );
						}
					}
				}
			}
		}

		JSONObject json = new JSONObject();
		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );
		json.put( "count", authorNodes.length() );

		JSONArray arr = new JSONArray();
		arr.put( json );
		return arr;
	}
	
	public JSONArray createPublicationGraph( JSONArray publications, JSONArray topAuthors, JSONArray interestsDegree ) 
	{
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();

		List<String> authors = new LinkedList<>();
		//getting top authors
		JSONObject graph = topAuthors.optJSONObject( 0 );
		if ( graph != null )
		{
			JSONArray nodes = graph.optJSONArray( "nodes" );
			JSONArray links = graph.optJSONArray( "links" );

			//authorNodes = nodes;
			authorLinks = links;

			for ( int i = 0; i < nodes.length(); i++ )
			{
				JSONObject obj = nodes.optJSONObject( i );
				String id = obj.optString( "id" );
				authors.add( id );
			}
		}

		Map<String, String> interestMap = new LinkedMap();
		for ( int i = 0; i < interestsDegree.length(); i++ )
		{
			JSONObject obj = interestsDegree.optJSONObject( i );

			if ( obj != null )
			{
				String termID = obj.optString( "ID" );
				String term = obj.optString( "itemName" );
				int termDegree = obj.optInt( "DegreeValue" );
				interestMap.put( term, termID );

				authorNodes.put( addNode( termID, (term + "</br>Degree: " + termDegree), "Interest", termDegree, _INTERST_NODE_CODE, _DEFAULT_SIZE ) );
			}
		}

		Map<String, List<String>> authorMap = new LinkedMap();
		for ( String term : interestMap.keySet() )
		{
			String termId = interestMap.get( term );
			for ( int i = 0; i < authorLinks.length(); i++ )
			{
				String intLink = null;
				JSONObject authorlink = authorLinks.optJSONObject( i );
				if ( termId.equals( authorlink.optString( "source", "" ) ) )
				{
					intLink = authorlink.optString( "target" );
				}
				else if ( termId.equals( authorlink.optString( "target", "" ) ) )
				{
					intLink = authorlink.optString( "source" );
				}

				if ( intLink != null )
				{
					List<String> authorsList = authorMap.get( term );
					if ( authorsList == null )
						authorsList = new LinkedList<>();
					authorsList.add( intLink );
					authorMap.put( term, authorsList );
				}
			}
		}

		for ( int i = 0; i < publications.length(); i++ )
		{
			JSONArray pubI = publications.optJSONArray( i );
			for ( int j = 0; j < pubI.length(); j++ )
			{
				JSONObject obj = pubI.optJSONObject( j );
				if ( obj != null )
				{
					String termName = obj.optString( "iName");
					String pubID = obj.optString( "pID"); 
					String pubTitle = obj.optString( "pTitle");
					String pubAuthor = obj.optString( "pAuthors");
					String pubAbstract = obj.optString( "pAbstract");
					String pubkeyword = obj.optString( "pKeywords");
					int termValue = obj.optInt( "iValue", 1 );
					if( termValue == 0 ) termValue = 1;

					authorNodes.put( addNode( pubID, ( pubTitle + "</br>Degree: " + termValue), "Publication", termValue, _PUBLICATION_NODE_CODE, _DEFAULT_SIZE ) );
					authorLinks.put( addAuthorLink( pubID, interestMap.get( termName ) ) );

					String[] coAuthors = pubAuthor.split( "," );
					if ( coAuthors != null )
					{
						for ( String author : coAuthors )
						{
							authorLinks.put( addAuthorLink( pubID, author ) );
						}
					}
				}
			}
		}

		JSONObject json = new JSONObject();
		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );
		json.put( "count", authorNodes.length() );

		JSONArray arr = new JSONArray();
		arr.put( json );
		return arr;
	}

	public JSONArray DegreeJsonCreator( Map<String, Integer> rDegree,
			Map<String, String> rIDName3, String researcherID,
			int maxDegreeofaNode ) throws JSONException, SQLException,
	IOException, TasteException {
		JSONArray recBasedDegree = new JSONArray();
		for (Entry<String, Integer> entry : rDegree.entrySet()) {
			int NofcommonI = getCommonInterests( researcherID, entry.getKey() );
			double JaccardSim = FindJaccardSimilarity(researcherID,
					entry.getKey());
			// show double till only 2 decimal
			double NormDegree = ((double) entry.getValue() / maxDegreeofaNode);
			NormDegree = Math.round(NormDegree * 100);
			NormDegree = NormDegree / 100;
			JSONObject obj = new JSONObject();
			obj.put("rID", entry.getKey());
			obj.put("rName", rIDName3.get(entry.getKey()));
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
	 * @return
	 * @throws JSONException
	 */
	static JSONArray ClosenessJsonCreator(Map<Integer, Double> rIDCloseness,
			Map<Integer, String> rIDName) throws JSONException {
		JSONArray recBasedCloseness = new JSONArray();
		for (Entry<Integer, Double> entry : rIDCloseness.entrySet()) {
			JSONObject obj = new JSONObject();
			obj.put("ID", entry.getKey());
			obj.put("itemName", rIDName.get(entry.getKey()));
			obj.put("ClosenessValue", entry.getValue());
			recBasedCloseness.put(obj);
		}
		return recBasedCloseness;
	}

	/**
	 * @param rIDEigenvector
	 * @param rIDName
	 * @return
	 * @throws JSONException
	 */
	static JSONArray EigenvectorJsonCreator(
			Map<Integer, Double> rIDEigenvector, Map<Integer, String> rIDName)
					throws JSONException {
		JSONArray recBasedEigenvector = new JSONArray();
		for (Entry<Integer, Double> entry : rIDEigenvector.entrySet()) {
			JSONObject obj = new JSONObject();
			obj.put("ID", entry.getKey());
			obj.put("itemName", rIDName.get(entry.getKey()));
			obj.put("EigenvectorValue", entry.getValue());
			recBasedEigenvector.put(obj);
		}
		return recBasedEigenvector;
	}

	/**
	 * @param rIDName
	 * @param rIDBetweenness
	 * @return
	 * @throws JSONException
	 */
	public static JSONArray BetweennessJsonCreator(Map<String, String> rIDName,
			Map<String, Double> rIDBetweenness) throws JSONException {
		JSONArray recBasedBetweenness = new JSONArray();
		for (Entry<String, Double> entry : rIDBetweenness.entrySet()) {
			JSONObject obj = new JSONObject();
			obj.put("ID", entry.getKey());
			obj.put("itemName", rIDName.get(entry.getKey()));
			obj.put("BetweennessValue", entry.getValue());
			recBasedBetweenness.put(obj);
		}
		// System.out.println(recBasedBetweenness);
		return recBasedBetweenness;
	}
}
