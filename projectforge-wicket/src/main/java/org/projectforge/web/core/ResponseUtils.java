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

package org.projectforge.web.core;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/**
 * @author Wolfgang Jung (w.jung@micromata.de)
 *
 */
public class ResponseUtils
{
  private static final Logger log = Logger.getLogger(ResponseUtils.class);
  
  /**
   * Startet im Browser den Download einer Datei. Anhand des Dateinamen wird automatisch der Content-Type ermittelt.
   *
   * @param filename Virtueller Name der Datei. Bei Pfadangaben werden diese abgeschnitten.
   * @param content Inhalt der Datei
   * @param response
   * @param ctx der Servletcontext
   * @param attach Download as Attachment
   * @throws IOException
   */
  public static void streamToOut(String filename, byte[] content, HttpServletResponse response, ServletContext ctx, boolean attach)
      throws IOException
  {
    prepareDownload(filename, response, ctx, attach);
    response.setContentLength(content.length);
    response.getOutputStream().write(content);
    response.getOutputStream().flush();
  }
  
  /**
   * Prepares download of a file. The content type will be detected automatically by the file name.
   *
   * @param filename Virtual file name. Any path infos will be truncated.
   * @param response
   * @param ctx der Servletcontext
   * @param attach Download as Attachment
   */
  public static void prepareDownload(String filename, HttpServletResponse response, ServletContext ctx, boolean attach)
  {
    String mimeType = null;
    try {
      mimeType = ctx.getMimeType(filename);
    } catch (Exception ex) {
      log.info("Exception while getting mime-type (using application/binary): " + ex);
    }
    if (mimeType == null) {
      response.setContentType("application/binary");
    } else {
      response.setContentType(mimeType);
    }
    log.debug("Using content-type " + mimeType);
    final String filenameWithoutPath = FilenameUtils.getName(filename);
    if (attach == true) {
      response.setHeader("Content-disposition", "attachment; filename=\"" + filenameWithoutPath + "\"");
    } else {
      response.setHeader("Content-disposition", "inline; filename=\"" + filenameWithoutPath + "\"");
    }
  }
}
