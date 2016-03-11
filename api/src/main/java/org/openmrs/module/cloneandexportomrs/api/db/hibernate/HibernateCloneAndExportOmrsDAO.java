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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.openmrs.module.cloneandexportomrs.DumpDatabase;
import org.openmrs.module.cloneandexportomrs.api.db.CloneAndExportOmrsDAO;
import org.openmrs.module.cloneandexportomrs.utils.CloneAndExportOmrsUtils;
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
			omrsDbClone.delete();
			return true;
		} else
			return false;
	}

	@Override
	public String prepareCurrentOpenMRSDataDirectoryToExport() {
		prepareOpenMRSTomcatDataAndWarToExport();
		File omrsClone = new File(CloneAndExportOmrsUtils.OPENMRS_DATA_DIR);
		DumpDatabase dump = new DumpDatabase();
		
		dump.execute();
		if (omrsClone.exists() && omrsClone.isDirectory()) {
			File finalF = getOpenMRSDataZip(CloneAndExportOmrsUtils.OPENMRS_DATA_DIR, CloneAndExportOmrsUtils.FINAL_CLONE_PATH);
			
			deleteCloneAndExportOmrsDirectory(omrsClone.getAbsolutePath());
			
			return finalF.getAbsolutePath();
		}
		
		return null;
	}

	private void prepareOpenMRSTomcatDataAndWarToExport() {
		File omrsTomcatWeb = new File(CloneAndExportOmrsUtils.TOMCAT_WEBAPPS_OPENMRS_DIR);
		File omrsTomcatWebWar = new File(CloneAndExportOmrsUtils.TOMCAT_WEBAPPS_OPENMRS_DIR + ".war");
		File openmrsTomcatDest = new File(CloneAndExportOmrsUtils.OPENMRS_TOMCAT_STORAGE_DIR + File.separator + WebConstants.WEBAPP_NAME);
		File openmrsWarTomcatDest = new File(CloneAndExportOmrsUtils.OPENMRS_TOMCAT_STORAGE_DIR + File.separator + WebConstants.WEBAPP_NAME + ".war");

		try {
			if (omrsTomcatWeb.exists() && omrsTomcatWeb.isDirectory()) {
				FileUtils.copyDirectory(omrsTomcatWeb, openmrsTomcatDest);
			}
	
			if (omrsTomcatWebWar.exists() && !omrsTomcatWebWar.isDirectory()) {
				FileUtils.copyFile(omrsTomcatWebWar, openmrsWarTomcatDest);
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
		//System.out.println("Adding: " + entryPath);
		out.putNextEntry(new ZipEntry(entryPath));
		int len;
		while ((len = in.read(tmpBuf)) > 0) {
			out.write(tmpBuf, 0, len);
		}
		out.closeEntry();
		in.close();
	}
}