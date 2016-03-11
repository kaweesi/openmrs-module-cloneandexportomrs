package org.openmrs.module.cloneandexportomrs.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DbDump {
	/** Logger for this class and subclasses */
	protected final static Log log = LogFactory.getLog(DbDump.class);
	
	private static final String fileEncoding = "UTF8";
	
    /** Dump the whole database to an SQL string */
    public static void dumpDB(Properties props, boolean showProgress, Class showProgressToClass) throws Exception {
    	String filename = props.getProperty("filename");
    	String folder= props.getProperty("folder");
        String driverClassName = props.getProperty("driver.class");
        String driverURL = props.getProperty("driver.url");
        DatabaseMetaData dbMetaData = null;
        Connection dbConn = null;

        Class.forName(driverClassName);
        dbConn = DriverManager.getConnection(driverURL, props);
        dbMetaData = dbConn.getMetaData();
        
        FileOutputStream fos = new FileOutputStream(folder + filename);        
        OutputStreamWriter result = new OutputStreamWriter(fos, fileEncoding);            
        
        String catalog = props.getProperty("catalog");
        String schema = props.getProperty("schemaPattern");
        
        ResultSet rs = dbMetaData.getTables(catalog, schema, null, null);
        int progressCnt = 0;

        result.write( "/*\n" + 
        		" * DB jdbc url: " + driverURL + "\n" +
        		" * Database product & version: " + dbMetaData.getDatabaseProductName() + " " + dbMetaData.getDatabaseProductVersion() + "\n" +
        		" */"
        		);                                   
        
        result.write("\nSET FOREIGN_KEY_CHECKS=0;\n");
        
        List<String> tableVector = new Vector<String>();
        int progressTotal = 0;
        while(rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            progressTotal++;
            tableVector.add(tableName);               
        }
        rs.beforeFirst();
        
        if (! rs.next()) {
            log.error("Unable to find any tables matching: catalog="+catalog+" schema=" + schema + " tables=" + tableVector.toArray().toString());
            rs.close();
        } else {
            do {
                String tableName = rs.getString("TABLE_NAME");                    
                String tableType = rs.getString("TABLE_TYPE");
                
                if (tableVector.contains(tableName)) {

                	progressCnt++;
                	//BackupFormController.getProgressInfo().put(filename, "Backing up table " + progressCnt + " of " + progressTotal + " (" + tableName + ")...");

                    if (showProgress) {
                        Map<String,String> info = (Map<String,String>)showProgressToClass.getMethod("getProgressInfo", null).invoke(showProgressToClass);
                        info.put(filename, "Backing up table " + progressCnt + " of " + progressTotal + " (" + tableName + ")...");
                        showProgressToClass.getMethod("setProgressInfo", new Class[]{Map.class}).invoke(showProgressToClass, info);
                    }

                    if ("TABLE".equalsIgnoreCase(tableType)) {

                    	result.write( "\n\n-- Structure for table `" + tableName + "`\n" );
                    	result.write( "DROP TABLE IF EXISTS `"+tableName+"`;\n" );
                    	
                    	PreparedStatement tableStmt = dbConn.prepareStatement("SHOW CREATE TABLE "+ tableName +";");
                    	ResultSet tablesRs = tableStmt.executeQuery();
                    	while (tablesRs.next()) {
                    		result.write(tablesRs.getString("Create Table") + ";\n\n");	
                    	}
                    	tablesRs.close();
                    	tableStmt.close();                    	

                        dumpTable(dbConn, result, tableName);
                        System.gc();
                    }
                }
            } while (rs.next());
            rs.close();
        }
        
        result.write("\nSET FOREIGN_KEY_CHECKS=1;\n");
        
        result.flush();
        result.close();
        
        dbConn.close();       
    }

    /** dump this particular table to the string buffer */
	private static void dumpTable(Connection dbConn, OutputStreamWriter result, String tableName) {
        try {
            // create table sql
            PreparedStatement stmt = dbConn.prepareStatement("SELECT * FROM "+tableName);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // data inserts
            result.write( "\n\n-- Data for table '"+tableName+"'\n" );
            while (rs.next()) {
                result.write( "INSERT INTO "+tableName+" VALUES (" );
                for (int i=0; i<columnCount; i++) {
                    if (i > 0) {
                        result.write(", ");
                    }
                    Object value = rs.getObject(i+1);
                    if (value == null) {
                        result.write("NULL");
                    } else {
                        String outputValue = value.toString();
                        outputValue = outputValue.replaceAll("\'","\\\\'");
                        result.write( "'"+outputValue+"'" );
                    }
                }
                result.write(");\n");
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            log.error("Unable to dump table "+tableName+".  "+e);
        } catch (IOException e) {
            log.error("Unable to dump table "+tableName+".  "+e);
        }
    }
}
