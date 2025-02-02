/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.wicket;

import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.IResourceStream;
import org.projectforge.common.MimeType;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

public class DownloadUtils {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DownloadUtils.class);

  public static void setCharacterEncoding(final Response response, final String encoding) {
    final Object cresp = response.getContainerResponse();
    if (cresp instanceof HttpServletResponse) {
      ((HttpServletResponse) cresp).setCharacterEncoding(encoding);
    } else {
      log.warn("Character encoding not supported for response of type: " + response.getClass());
    }
  }

  public static void setUTF8CharacterEncoding(final Response response) {
    setCharacterEncoding(response, "utf-8");
  }

  /**
   * Mime type etc. is done automatically.
   *
   * @param content  The content of the file to download.
   * @param filename
   */
  public static void setDownloadTarget(final byte[] content, final String filename) {
    setDownloadTarget(content, filename, (String) null);
  }

  /**
   * @param content     The content of the file to download.
   * @param filename
   * @param contentType For setting contentType manually.
   */
  public static void setDownloadTarget(final byte[] content, final String filename, final MimeType mimeType) {
    setDownloadTarget(content, filename, mimeType != null ? mimeType.getMimeTypeString() : (String) null);
  }

  /**
   * @param content     The content of the file to download.
   * @param filename
   * @param contentType For setting contentType manually.
   */
  public static void setDownloadTarget(final byte[] content, final String filename, final String contentType) {
    final ByteArrayResourceStream byteArrayResourceStream;
    if (contentType != null) {
      byteArrayResourceStream = new ByteArrayResourceStream(content, filename, contentType);
    } else {
      byteArrayResourceStream = new ByteArrayResourceStream(content, filename);
    }
    final ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(byteArrayResourceStream) {
      @Override
      public void respond(IRequestCycle requestCycle) {
        WebResponse response = (WebResponse) requestCycle.getResponse();
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        super.respond(requestCycle);
      }
    };
    handler.setFileName(filename).setContentDisposition(ContentDisposition.ATTACHMENT);
    handler.setCacheDuration(Duration.ZERO);
    RequestCycle.get().scheduleRequestHandlerAfterCurrent(handler);
    log.info("Starting download for file. filename:" + filename + ", content-type:" + byteArrayResourceStream.getContentType());
  }

  /**
   * Determines content type dependent on the file name suffix. Yet supported: application/pdf (*.pdf), application/vnd.ms-excel (*.xls),
   * image/jpeg (*.jpg, *.jpeg), image/svg+xml (*.svg), image/png (*.xml), application/xml (*.xml) and text (*.txt, *.csv).
   *
   * @param filename
   * @return
   */
  public static String getContentType(final String filename) {
    final MimeType mimeType = MimeType.getMimeType(filename);
    if (mimeType != null) {
      return mimeType.getMimeTypeString();
    }
    log.info("Unknown file type: " + filename);
    return "";
  }
}
