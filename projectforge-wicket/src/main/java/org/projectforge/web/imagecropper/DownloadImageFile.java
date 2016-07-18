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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.core.ResponseUtils;

/**
 * Servlet Funktion: Download der Daten als Image (croppedimage) Wird benötigt um Bild auf lokalem Rechner zu speichern.
 * Dieses Servlet wird angesprochen, nachdem der User zu Beginn ein Bild von seinem lokalen System ausgewählt hat und es
 * über UploadImageFileTemporary hochgeladen wurde.
 * 
 */
@WebServlet("/secure/DownloadImageFile")
public class DownloadImageFile extends HttpServlet
{
  private static final long serialVersionUID = 7778310216427808799L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DownloadImageFile.class);

  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    log.debug("Start doPost");
    final PFUserDO user = UserFilter.getUser(request);
    if (user == null) {
      log.warn("Calling of UploadImageFileTemp without logged in user.");
      return;
    }
    try {
      String filename = "image";
      // request.getParameter("filedirectory") != null) {
      if (request.getParameter("filename") != null) {
        filename = request.getParameter("filename");
      }

      final File imageFile = ImageCropperUtils.getTempFile(user);
      log.info("Reading temporary file: " + imageFile.getAbsolutePath());
      final byte[] puffer = FileUtils.readFileToByteArray(imageFile);
      ResponseUtils.streamToOut(filename, puffer, response, request.getSession().getServletContext(), true);
      log.info("Deleting temporary file: " + imageFile.getAbsolutePath());
      imageFile.delete();
    } catch (Exception ex) {
      log.warn("Failure reading the request");
      log.warn(ex.getMessage(), ex);
    }

  }
}
