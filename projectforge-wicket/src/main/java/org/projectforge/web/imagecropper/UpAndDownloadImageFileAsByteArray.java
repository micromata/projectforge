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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.lf5.util.StreamUtils;
import org.projectforge.web.core.ResponseUtils;

/**
 * Up-/ und Download des Bildes als ByteArray um das Bild auf dem lokalen Rechner zu speichern. <br/>
 * 
 * Servlet Funktion: Up-/ und Download der Daten als ByteArray. <br/>
 * 
 * Wird benötigt um Bild auf lokalem Rechner zu speichern.
 */
@WebServlet("/secure/UpAndDownloadImageFileAsByteArray")
public class UpAndDownloadImageFileAsByteArray extends HttpServlet
{
  private static final long serialVersionUID = 1244641220486001809L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(UpAndDownloadImageFileAsByteArray.class);

  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    log.debug("Start doPost");
    try {
      // Inputstream
      final InputStream is = request.getInputStream();
      final byte[] buffer = StreamUtils.getBytes(is);
      String filename;
      String croppedname;
      if (request.getParameter("croppedname") != null) {
        croppedname = request.getParameter("croppedname");
      } else {
        croppedname = "cropped";
      }
      if ((request.getParameter("filename") != null) && (request.getParameter("filetype") != null)) {
        filename = croppedname + request.getParameter("filename") + "." + request.getParameter("filetype");
      } else {
        filename = "image.png";
      }
      log.info("Filename: " + filename);
      ResponseUtils.streamToOut(filename, buffer, response, request.getSession().getServletContext(), true);
    } catch (Exception ex) {
      log.warn("Failure reading the request");
      log.warn(ex.getMessage(), ex);
    }
  }
}
