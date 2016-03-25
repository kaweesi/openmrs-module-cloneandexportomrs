package org.openmrs.module.cloneandexportomrs.utils;

import java.io.File;

import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;

public class CloneAndExportOmrsUtils {
	
	public static String OPENMRS_DATA_DIR = OpenmrsUtil.getApplicationDataDirectory();
	
	public static String DATABASE_STORAGE_DIR = OPENMRS_DATA_DIR + File.separator + "cloneAndExportOmrs" + File.separator + "database";
	
	public static String DATA_DIR = OPENMRS_DATA_DIR + File.separator + "cloneAndExportOmrs";
	
	public static String TOMCAT_WEBAPPS_OPENMRS_DIR = System.getProperty("catalina.base") + File.separator + "webapps" + File.separator + WebConstants.WEBAPP_NAME;
	
	public static String OPENMRS_TOMCAT_STORAGE_DIR = OPENMRS_DATA_DIR + File.separator + "cloneAndExportOmrs" + File.separator + "tomcat";
	
	public static String CLONE_PREFIX = "-clone-";
	
	public static String FINAL_CLONE_PATH = System.getProperty("user.home") + File.separator + WebConstants.WEBAPP_NAME +"-clone.zip";
	
	public static String MY_DB_BACKUPFILE_NAME = "cloneandexportomrs-db.sql";

	public static String OPENMRS_DB_MODULE_BACKUP_FOLDER = OPENMRS_DATA_DIR + File.separator + "backup";
}
