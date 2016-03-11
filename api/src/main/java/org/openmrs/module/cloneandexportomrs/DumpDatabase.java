package org.openmrs.module.cloneandexportomrs;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;

import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.cloneandexportomrs.utils.CloneAndExportOmrsUtils;
import org.openmrs.module.cloneandexportomrs.utils.DbDump;
import org.openmrs.module.cloneandexportomrs.utils.Zip;
import org.openmrs.notification.Alert;
import org.openmrs.web.WebConstants;

public class DumpDatabase {
	private Properties props;
	private String databaseFilename;
	private String folder;
	private UserContext ctx;

	public void execute() {
		if (Context.isAuthenticated()) {
			Context.openSession();
			// create file name with timestamp
			databaseFilename = WebConstants.WEBAPP_NAME + "_."
					+ new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(Calendar.getInstance().getTime()) + ".sql";
			// do backup without process notification (no controller class
			// passed)
			handleBackup(databaseFilename, false, null);
			Context.closeSession();
		}
	}

	private void handleBackup(final String filename, final boolean showProgress, final Class showProgressToClass) {
		// set jdbc connection properties
		props = new Properties();
		props.setProperty("driver.class", "com.mysql.jdbc.Driver");
		props.setProperty("driver.url", Context.getRuntimeProperties().getProperty("connection.url"));
		props.setProperty("user", Context.getRuntimeProperties().getProperty("connection.username"));
		props.setProperty("password", Context.getRuntimeProperties().getProperty("connection.password"));

		folder = CloneAndExportOmrsUtils.DATABASE_STORAGE_DIR;

		if(!(new File(folder).exists())) {
			(new File(folder)).mkdirs();
		}

		// if no problems occured with creating or finding the backup folder...
		props.setProperty("filename", filename);
		props.setProperty("folder", folder);

		// make ctx available for the thread
		ctx = Context.getUserContext();

		new Thread(new Runnable() {

			public void run() {
				try {
					UserContext ctxInThread = ctx;
					String filenameInThread = filename;
					// DbDump.dumpDB(props, false, null);

					DbDump.dumpDB(props, showProgress, showProgressToClass);

					// BackupFormController.getProgressInfo().put(filenameInThread,
					// "Zipping file...");
					if (showProgress) {
						try {
							Map<String, String> info = (Map<String, String>) showProgressToClass
									.getMethod("getProgressInfo", new Class[] {})
									.invoke(showProgressToClass, new Object[] {});
							System.out.println("*** info " + info);
							info.put(filenameInThread, "Zipping file...");
							showProgressToClass.getMethod("setProgressInfo", new Class[] { Map.class })
									.invoke(showProgressToClass, info);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// zip sql file
					Zip.zip(folder, filenameInThread);

					// remove sql file after zipping it
					try {
						File f = new File(folder + filenameInThread);
						f.delete();
					} catch (SecurityException e) {
						// log.error("Could not delete raw sql file.",e);
					}

					// BackupFormController.getProgressInfo().put(filenameInThread,
					// "Backup complete.");
					if (showProgress) {
						try {
							Map<String, String> info = (Map<String, String>) showProgressToClass
									.getMethod("getProgressInfo", null).invoke(showProgressToClass, new Object[] {});
							System.out.println("*** info " + info);
							info.put(filenameInThread, "Backup complete.");
							showProgressToClass.getMethod("setProgressInfo", new Class[] { Map.class })
									.invoke(showProgressToClass, info);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					Context.setUserContext(ctxInThread);
					Alert alert = new Alert("The backup file is ready at: " + folder + filenameInThread + ".zip",
							Context.getUserContext().getAuthenticatedUser());
					Context.getAlertService().saveAlert(alert);

				} catch (Exception e) {
					System.err.println("Unable to backup database: " + e);
					e.printStackTrace();
					// log.error("Unable to backup database: ", e);
				}
			}
		}).start();
	}
}
