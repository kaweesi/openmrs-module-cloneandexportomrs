package org.openmrs.module.cloneandexportomrs;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import org.openmrs.api.context.Context;
import org.openmrs.module.cloneandexportomrs.utils.CloneAndExportOmrsUtils;
import org.openmrs.module.cloneandexportomrs.utils.DbDump;
import org.openmrs.module.cloneandexportomrs.utils.Zip;
import org.openmrs.web.WebConstants;

public class DumpDatabase {
	private Properties props;
	private String databaseFilename;
	private String folder;

	public void execute() {
		if (Context.isAuthenticated()) {
			Context.openSession();
			// create file name with timestamp
			databaseFilename = WebConstants.WEBAPP_NAME + "_"
					+ new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime()) + ".sql";
			// do backup without process notification (no controller class
			// passed)
			handleBackup(databaseFilename);
			Context.closeSession();
		}
	}

	private void handleBackup(final String filename) {
		// set jdbc connection properties
		props = new Properties();
		props.setProperty("driver.class", "com.mysql.jdbc.Driver");
		props.setProperty("driver.url", Context.getRuntimeProperties().getProperty("connection.url"));
		props.setProperty("user", Context.getRuntimeProperties().getProperty("connection.username"));
		props.setProperty("password", Context.getRuntimeProperties().getProperty("connection.password"));

		folder = CloneAndExportOmrsUtils.DATABASE_STORAGE_DIR;

		if (!(new File(folder).exists())) {
			(new File(folder)).mkdirs();
		}

		// if no problems occured with creating or finding the backup folder...
		props.setProperty("filename", filename);
		props.setProperty("folder", folder);

		try {
			String filenameInThread = filename;
			DbDump.dumpDB(props);

			// zip sql file
			Zip.zip(folder + File.separator, filenameInThread);
			// remove sql file after zipping it
			try {
				File f = new File(folder + File.separator + filenameInThread);
				f.delete();
			} catch (SecurityException e) {
				// log.error("Could not delete raw sql file.",e);
			}
		} catch (Exception e) {
			System.err.println("Unable to backup database: " + e);
			e.printStackTrace();
			// log.error("Unable to backup database: ", e);
		}
	}
}
