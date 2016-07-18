/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.imagecropper;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Servlet Funktion: Temporärer Upload eines Images in das Verzeichnis tempimages. Wird benötigt um Bild vom lokalen
 * Rechner über den Server in den Flexclient zu laden
 * 
 */
@WebServlet("/secure/UploadImageFileTemporary")
public class UploadImageFileTemporary extends HttpServlet
{
  private static final long serialVersionUID = -6889184720152393862L;

  private static final long MAX_SUPPORTED_FILE_SIZE = 10000000; // 10 Megabyte

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UploadImageFileTemporary.class);

  /**
   * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    log.debug("Start doPost");
    final PFUserDO user = UserFilter.getUser(request);
    if (user == null) {
      log.warn("Calling of UploadImageFileTemp without logged in user.");
      return;
    }
    // check if the sent request is of the type multi part
    final boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    if (isMultipart == false) {
      log.warn("The request is not of the type multipart");
      return;
    }
    try { // Parse the request
      final FileItem imgFile = getImgFileItem(request); // get the file item of the multipart request
      // everything ok so far so process the file uploaded
      if (imgFile == null || imgFile.getSize() == 0) {
        log.warn("No file was uploaded, aborting!");
        return;
      }
      if (imgFile.getSize() > MAX_SUPPORTED_FILE_SIZE) {
        log.warn("Maximum file size exceded for file '" + imgFile.getName() + "': " + imgFile.getSize());
        return;
      }
      final File tmpImageFile = ImageCropperUtils.getTempFile(user); // Temporary file
      log.info("Writing tmp file: " + tmpImageFile.getAbsolutePath());
      try {
        // Write new File
        imgFile.write(tmpImageFile);
      } catch (Exception e) {
        log.error("Could not write " + tmpImageFile.getAbsolutePath(), e);
      }
    } catch (FileUploadException ex) {
      log.warn("Failure reading the multipart request");
      log.warn(ex.getMessage(), ex);
    }
    final ServletOutputStream out = response.getOutputStream();
    out.println("text/html");
  }

  /**
   * This function gets the fileItem from the form
   * 
   * @param items the list of fields sent to this servlet
   * @return the imgItem if not found it returns null
   */
  private FileItem getImgFileItem(final HttpServletRequest request) throws FileUploadException
  {
    // Create a factory for disk-based file items
    final FileItemFactory factory = new DiskFileItemFactory();
    // Create a new file upload handler
    final ServletFileUpload upload = new ServletFileUpload(factory);
    final List<?> items = upload.parseRequest(request); // get the items sent by the form
    FileItem fileItem = null;
    final Iterator<?> iter = items.iterator();
    // iterate over the items and if the required field is found break the loop
    while (iter.hasNext()) {
      fileItem = (FileItem) iter.next();
      if (fileItem.isFormField() == false) {
        break;
      }
    }
    return fileItem;
  }
}
