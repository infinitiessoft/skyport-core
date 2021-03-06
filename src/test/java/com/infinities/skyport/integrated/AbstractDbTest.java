/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport.integrated;

import java.io.InputStream;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.HibernateException;
import org.hibernate.internal.SessionImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.infinities.skyport.jpa.EntityManagerFactoryBuilder;
import com.infinities.skyport.jpa.EntityManagerHelper;
import com.infinities.skyport.jpa.JpaProperties;

public abstract class AbstractDbTest {

	// private static EntityManagerFactory entityManagerFactory;
	private static IDatabaseConnection connection;
	private static IDataSet dataset;


	// protected static EntityManager entityManager;

	@BeforeClass
	public static void initEntityManager() throws HibernateException, DatabaseUnitException {
		JpaProperties.PERSISTENCE_UNIT_NAME = "com.infinities.skyport.jpa.test";
		JpaProperties.JPA_PROPERTIES_FILE = null;
		FlatXmlDataSetBuilder flatXmlDataSetBuilder = new FlatXmlDataSetBuilder();
		flatXmlDataSetBuilder.setColumnSensing(true);
		InputStream dataSet = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-data.xml");
		dataset = flatXmlDataSetBuilder.build(dataSet);
	}

	@AfterClass
	public static void closeEntityManager() {
		EntityManagerFactoryBuilder.shutdown();
	}

	/**
	 * Will clean the dataBase before each test
	 * 
	 * @throws SQLException
	 * @throws DatabaseUnitException
	 */
	@Before
	public void cleanDB() throws DatabaseUnitException, SQLException {
		EntityManager entityManager = EntityManagerHelper.getEntityManager();
		connection = new DatabaseConnection(((SessionImpl) (entityManager.getDelegate())).connection());
		connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
		try {
			DatabaseOperation.INSERT.execute(connection, dataset);
		} finally {
			connection.close();
			entityManager.close();
		}
	}

}
