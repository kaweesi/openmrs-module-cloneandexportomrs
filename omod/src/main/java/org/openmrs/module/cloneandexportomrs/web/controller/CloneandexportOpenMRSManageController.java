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
package org.openmrs.module.cloneandexportomrs.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.cloneandexportomrs.api.CloneAndExportOmrsService;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The main controller.
 */
@Controller
public class  CloneandexportOpenMRSManageController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@RequestMapping(value = "/module/cloneandexportomrs/clone", method = RequestMethod.GET)
	public void clone(ModelMap model) {
		
	}
	
	@RequestMapping(value = "/module/cloneandexportomrs/links", method = RequestMethod.GET)
	public void links(ModelMap model) {
		
	}
	
	@RequestMapping(value = "/module/cloneandexportomrs/downloadDb", method = RequestMethod.GET)
	public void downloadDb(ModelMap model) {
		
	}
	
	@RequestMapping(value = "/module/cloneandexportomrs/downloadDb", method = RequestMethod.POST)
	public void downloadDbPost(ModelMap model, HttpServletResponse response) {
		String clone = Context.getService(CloneAndExportOmrsService.class).downloadDbBackUp();
		
		try {
			exportZipFile(response, 4096, clone);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/module/cloneandexportomrs/clone", method = RequestMethod.POST)
	public void clonePost(ModelMap model, HttpServletResponse response) {
		String clone = Context.getService(CloneAndExportOmrsService.class).prepareCurrentOpenMRSDataDirectoryToExport();
		
		try {
			exportZipFile(response, 4096, clone);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * fullPath must be a temporally stored file path since it's deleted after
	 * being exported
	 */
	private void exportZipFile(HttpServletResponse response, int BUFFER_SIZE, String fullPath)
			throws FileNotFoundException, IOException {
		if (fullPath != null) {
			File downloadFile = new File(fullPath);
			FileInputStream inputStream = new FileInputStream(downloadFile);
			String mimeType = "application/octet-stream";

			System.out.println("MIME type: " + mimeType);
			response.setContentType(mimeType);
			response.setContentLength((int) downloadFile.length());

			String headerKey = "Content-Disposition";
			String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());

			response.setHeader(headerKey, headerValue);

			OutputStream outStream = response.getOutputStream();
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;

			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, bytesRead);
			}
			inputStream.close();
			outStream.close();
			(new File(fullPath)).delete();
		}
	}
	
	@RequestMapping(value = "/module/cloneandexportomrs/dumpDb", method = RequestMethod.GET)
	public void dumpDBonTerminalGet(ModelMap model) {
	}
	
	@RequestMapping(value = "/module/cloneandexportomrs/dumpDb", method = RequestMethod.POST)
	public void dumpDBonTerminal(ModelMap model, HttpServletResponse response, HttpServletRequest request) {
		try {
			Context.getService(CloneAndExportOmrsService.class).dumpDbUsingTerminal();
			
			response.sendRedirect("dumpDb.form");
			request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Successfully dumped, go to <a href='downloadDb.form'>downloadDb.form</a> to download it");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, e.getMessage());
		}
	}
}
