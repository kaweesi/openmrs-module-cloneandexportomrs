package org.openmrs.module.cloneandexportomrs.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zip {
static final int BUFFER = 2048;
	
	/**
	 * Compresses a given file that resides under path foldername filename.
	 * The compressed file will be stored in the same folder, the extension
	 * .zip added to it's filename.
	 * 
	 * @param folder Folder where the original file resides
	 * @param filename File name of the original uncompressed file
	 */
	public static void zip(String folder, String filename) {
		try {
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(folder + filename + ".zip");
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			byte data[] = new byte[BUFFER];			
			String files[] = { filename };
			
			for (int i = 0; i < files.length; i++) {
				System.out.println("Adding: " + files[i]);
				FileInputStream fi = new FileInputStream(folder + files[i]);
				origin = new BufferedInputStream(fi, BUFFER);
				ZipEntry entry = new ZipEntry(files[i]);
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
			}
			out.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
