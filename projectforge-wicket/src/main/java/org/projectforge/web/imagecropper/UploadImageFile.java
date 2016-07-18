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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Funktion: Upload eines Images in das Verzeichnis images. Wird benötigt um Bild vom FlexClient auf den Server
 * zu laden.
 * 
 */
@WebServlet("/secure/UploadImageFile")
public class UploadImageFile extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet
{
  private static final long serialVersionUID = 8127583022457169845L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UploadImageFile.class);

  /**
   * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    log.debug("Start doPost");
    log.info("Upload of images not yet implemented. First needed with upload of address pictures.");
    return;
    /*
     * try { final InputStream is = request.getInputStream(); byte[] buffer = StreamUtils.getBytes(is); String file; if
     * ((request.getParameter("filename") != null) && (request.getParameter("filetype") != null)) { file = "cropped" +
     * request.getParameter("filename") + "." + request.getParameter("filetype"); } else { file = "image.png"; } } catch
     * (Exception ex) { log.warn("Failure reading the request"); log.warn(ex.getMessage(), ex); }
     */
  }
}
