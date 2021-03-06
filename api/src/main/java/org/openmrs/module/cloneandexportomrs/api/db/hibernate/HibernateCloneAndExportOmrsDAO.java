/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.cloneandexportomrs.api.db.hibernate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.cloneandexportomrs.DumpDatabase;
import org.openmrs.module.cloneandexportomrs.api.db.CloneAndExportOmrsDAO;
import org.openmrs.module.cloneandexportomrs.utils.CloneAndExportOmrsUtils;
import org.openmrs.module.cloneandexportomrs.utils.Zip;
import org.openmrs.web.WebConstants;

/**
 * It is a default implementation of {@link CloneAndExportOmrsDAO}.
 */
public class HibernateCloneAndExportOmrsDAO implements CloneAndExportOmrsDAO {
	protected final Log log = LogFactory.getLog(this.getClass());

	private SessionFactory sessionFactory;

	/**
	 * @param sessionFactory
	 *            the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * @return the sessionFactory
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	private boolean deleteCloneAndExportOmrsDirectory(String dir) {
		File omrsDbClone = new File(dir);

		if (omrsDbClone.exists() && omrsDbClone.isDirectory()) {
			try {
				FileUtils.deleteDirectory(omrsDbClone);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		} else
			return false;
	}

	@Override
	public String prepareCurrentOpenMRSDataDirectoryToExport() {
		prepareOpenMRSTomcatDataAndWarToExport();
		File omrsClone = new File(CloneAndExportOmrsUtils.OPENMRS_DATA_DIR);
		DumpDatabase dump = new DumpDatabase();

		if (omrsClone.exists() && omrsClone.isDirectory()) {
			File dbBackup = new File(CloneAndExportOmrsUtils.OPENMRS_DB_MODULE_BACKUP_FOLDER);
			if (dbBackup.exists() && dbBackup.isDirectory() && dbBackup.list().length > 0) {
				// Do nothing, don't run db backup since we have one already
			} else {
				// TODO manually backup
				// dump.execute();
			}
			File finalF = getOpenMRSDataZip(CloneAndExportOmrsUtils.OPENMRS_DATA_DIR,
					CloneAndExportOmrsUtils.FINAL_CLONE_PATH);

			deleteCloneAndExportOmrsDirectory(CloneAndExportOmrsUtils.DATA_DIR);

			return finalF.getAbsolutePath();
		}

		return null;
	}

	private void prepareOpenMRSTomcatDataAndWarToExport() {
		// File omrsTomcatWeb = new
		// File(CloneAndExportOmrsUtils.TOMCAT_WEBAPPS_OPENMRS_DIR);
		File omrsTomcatWebWar = new File(CloneAndExportOmrsUtils.TOMCAT_WEBAPPS_OPENMRS_DIR + ".war");
		// File openmrsTomcatDest = new
		// File(CloneAndExportOmrsUtils.OPENMRS_TOMCAT_STORAGE_DIR +
		// File.separator + WebConstants.WEBAPP_NAME);
		File openmrsWarTomcatDest = new File(CloneAndExportOmrsUtils.OPENMRS_TOMCAT_STORAGE_DIR + File.separator
				+ WebConstants.WEBAPP_NAME + ".war");

		try {
			/*
			 * if (omrsTomcatWeb.exists() && omrsTomcatWeb.isDirectory()) {
			 * FileUtils.copyDirectory(omrsTomcatWeb, openmrsTomcatDest); }
			 */
			if (omrsTomcatWebWar.exists() && !omrsTomcatWebWar.isDirectory()) {
				FileUtils.copyFile(omrsTomcatWebWar, openmrsWarTomcatDest);

				Zip.zip(CloneAndExportOmrsUtils.OPENMRS_TOMCAT_STORAGE_DIR + File.separator,
						WebConstants.WEBAPP_NAME + ".war");
				try {
					File f = new File(CloneAndExportOmrsUtils.OPENMRS_TOMCAT_STORAGE_DIR + File.separator
							+ WebConstants.WEBAPP_NAME + ".war");
					f.delete();
				} catch (SecurityException e) {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File getOpenMRSDataZip(String sourceDirectory, String zipFile) {
		File dirObj = new File(sourceDirectory);
		ZipOutputStream out;

		try {
			out = new ZipOutputStream(new FileOutputStream(zipFile));

			System.out.println("Creating : " + zipFile);
			addDirectoriesAndFilesToZip(dirObj, out, sourceDirectory);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return StringUtils.isBlank(zipFile) && !(new File(zipFile).exists()) ? null : new File(zipFile);
	}

	private void addDirectoriesAndFilesToZip(File dirObj, ZipOutputStream out, String sourceDirectory) {
		byte[] tmpBuf = new byte[1024];

		if (dirObj.isDirectory()) {
			File[] files = dirObj.listFiles();
			for (Integer i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					addDirectoriesAndFilesToZip(files[i], out, sourceDirectory);
					continue;
				}
				try {
					addFileToZip(out, sourceDirectory, files[i], tmpBuf);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			try {
				addFileToZip(out, sourceDirectory, dirObj, tmpBuf);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void addFileToZip(ZipOutputStream out, String sourceDirectory, File file, byte[] tmpBuf)
			throws FileNotFoundException, IOException {
		FileInputStream in = new FileInputStream(file.getAbsolutePath());
		String entryPath = (new File(sourceDirectory)).toURI().relativize(file.toURI()).getPath();
		// System.out.println("Adding: " + entryPath);
		out.putNextEntry(new ZipEntry(entryPath));
		int len;
		while ((len = in.read(tmpBuf)) > 0) {
			out.write(tmpBuf, 0, len);
		}
		out.closeEntry();
		in.close();
	}

	/**
	 * Depends on the presence of the Database backup module
	 * 
	 * @return
	 */
	@Override
	public String downloadDbBackUp() {
		String pathToZip = null;
		String backUpFolder = CloneAndExportOmrsUtils.OPENMRS_DB_MODULE_BACKUP_FOLDER;
		File backUpDir = new File(backUpFolder);

		Arrays.sort(backUpDir.listFiles(), new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				if (((File) o1).lastModified() > ((File) o2).lastModified()) {
					return -1;
				} else if (((File) o1).lastModified() < ((File) o2).lastModified()) {
					return +1;
				} else {
					return 0;
				}
			}

		});
		if (backUpDir.exists() && backUpDir.isDirectory()) {
			if (myBackUpExists(backUpDir.list())) {
				File realFile = new File(backUpFolder + File.separator + CloneAndExportOmrsUtils.MY_DB_BACKUPFILE_NAME);

				if (realFile.exists() && realFile.isFile() && realFile.length() > 0) {
					pathToZip = realFile.getAbsolutePath();
				}
			} else {
				for (String file : backUpDir.list()) {
					if (file.startsWith("openmrs.backup.") && file.endsWith(".sql.zip")) {
						File realFile = new File(backUpFolder + File.separator + file);

						if (realFile.length() > 0) {
							pathToZip = realFile.getAbsolutePath();
						}
					}
				}
			}
		}

		return pathToZip;
	}

	private boolean myBackUpExists(String[] list) {
		boolean exists = false;

		for (int i = 0; i < list.length; i++) {
			if (list[i].equals(CloneAndExportOmrsUtils.MY_DB_BACKUPFILE_NAME)) {
				exists = true;
				break;
			}
		}
		return exists;
	}

	@Override
	public void dumpDbUsingTerminal() {
		String user = Context.getRuntimeProperties().getProperty("connection.username");
		String pswd = Context.getRuntimeProperties().getProperty("connection.password");
		String db = null;
		Properties props = new Properties();
		String connUrl = Context.getRuntimeProperties().getProperty("connection.url");

		props.put("user", user);
		props.put("password", pswd);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			db = DriverManager.getConnection(connUrl, props).getCatalog();
			System.out.println("Getting Ready to dump: " + db);
		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		File f = new File(CloneAndExportOmrsUtils.OPENMRS_DB_MODULE_BACKUP_FOLDER);

		if (!f.exists() || !f.isDirectory()) {
			f.mkdirs();
		}

		// TODO instead of depending on user adding mysql to PATH, find where
		// MySQL bin as and run this command using full path to it
		String mysqlHome = getMySQLBinFolder(connUrl, user, pswd);
		String dumpCommand = mysqlHome + "mysqldump -u\"" + user + "\" -p\"" + pswd + "\" " + db + " > "
				+ CloneAndExportOmrsUtils.OPENMRS_DB_MODULE_BACKUP_FOLDER + File.separator
				+ CloneAndExportOmrsUtils.MY_DB_BACKUPFILE_NAME;

		try {
			if (StringUtils.isNotBlank(db)) {
				String[] cmdarray = null;
				String osName = System.getProperty("os.name").toLowerCase();

				if (osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") > 0) {
					String[] cmds = { "/bin/sh", "-c", dumpCommand };
					cmdarray = cmds;
				} else if (osName.indexOf("win") >= 0) {
					String[] cmds = { "cmd.exe", "/c", dumpCommand };
					cmdarray = cmds;
				} else if (osName.indexOf("mac") >= 0) {
					String[] cmds = { "/bin/bash", "-c", dumpCommand };
					cmdarray = cmds;
				}

				Runtime.getRuntime().exec(cmdarray);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getMySQLBinFolder(String url, String user, String pass) {
		String mysqlFolderPath = "";

		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(url, user, pass);
			stmt = conn.createStatement();

			String sql = "select @@basedir";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String str = rs.getString("@@basedir");
				if (str.indexOf("XAMPP") > 0)
					mysqlFolderPath = rs.getString("@@basedir") + File.separator + "bin" + File.separator;
				break;
			}
			rs.close();
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					conn.close();
			} catch (SQLException se) {
			}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}
		return mysqlFolderPath;
	}
}