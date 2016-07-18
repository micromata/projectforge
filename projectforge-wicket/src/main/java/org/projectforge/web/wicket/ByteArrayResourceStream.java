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

package org.projectforge.web.wicket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

/**
 * Needed for download files generated of byte arrays.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ByteArrayResourceStream extends AbstractResourceStream
{
  private static final long serialVersionUID = 102937904470626593L;

  private String contentType;

  private final byte[] content;

  /**
   * @param content
   * @param filename Only needed for determine the mime type.
   */
  public ByteArrayResourceStream(final byte[] content, final String filename)
  {
    this.content = content;
    contentType = DownloadUtils.getContentType(filename);
  }

  /**
   * @param content
   * @param filename Only needed for determine the mime type.
   */
  public ByteArrayResourceStream(final byte[] content, final String filename, final String contentType)
  {
    this.content = content;
    this.contentType = contentType;
  }

  /**
   * @param contentType Mime type.
   */
  public void setContentType(final String contentType)
  {
    this.contentType = contentType;
  }

  public void close() throws IOException
  {
    // ByteArrayInputStream.close() has no effect.
  }

  @Override
  public String getContentType()
  {
    return contentType;
  }

  public InputStream getInputStream() throws ResourceStreamNotFoundException
  {
    return new ByteArrayInputStream(content);
  }

  @Override
  public Bytes length()
  {
    return Bytes.bytes(content.length);
  }
}
