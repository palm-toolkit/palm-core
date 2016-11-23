package de.rwth.i9.palm.recommendation.service;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Function;
import de.rwth.i9.palm.persistence.relational.GenericDAOHibernate;
/**
 * Master Thesis at The Learning Technologies Research Group (LuFG Informatik 9,
 * RWTH Aachen University), Year 2014 - 2015
 * 
 * Database Connection Information
 * 
 * @author Peyman Toreini
 * @version 1.1
 */

public class DbService {
	
	//private static Connection singleton = null;
	
	public static Connection ConnectToDB() throws SQLException {
		
		//if(singleton == null)
		/*Connection	singleton = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/sevtimov", "root", "rescu112");
		return singleton;*/
		return null;
		
		//SessionImpl sessionImpl = 
		//		(SessionImpl) sessionFactory.getCurrentSession();
		//return sessionImpl.connection();
	}
}