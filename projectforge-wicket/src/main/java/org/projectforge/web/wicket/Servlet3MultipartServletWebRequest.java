/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.util.runtime.RuntimeIOException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.*;
import java.util.*;

/**
 * Compat to servlet 3.0 api.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class Servlet3MultipartServletWebRequest extends MultipartServletWebRequest
{
  private static final Logger LOG = LoggerFactory.getLogger(Servlet3MultipartServletWebRequest.class);

  private long maxSize;
  private String upload;

  private Map<String, List<FileItem>> files = new HashMap<>();

  public Servlet3MultipartServletWebRequest(HttpServletRequest request, String filterPrefix, Bytes maxSize,
      String upload) throws FileUploadException
  {
    super(request, filterPrefix);
    this.maxSize = maxSize.bytes();
    this.upload = upload;
  }

  @Override
  public void parseFileParts() throws FileUploadException
  {

    try {
      Collection<Part> parts = getContainerRequest().getParts();
      for (Part part : parts) {
        if (StringUtils.isBlank(part.getContentType()) == true) {
          continue;
        }
        parsePart(part);
      }
    } catch (IOException | ServletException e) {
      throw new RuntimeException("Cannot parse Parts: " + e.getMessage(), e);
    }

  }

  /**
   * File item based on Part.
   *
   * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
   */
  public static class ParFileItem implements FileItem
  {
    private Part part;
    private byte[] bytes;
    private boolean formField;
    private FileItemHeaders headers;

    public ParFileItem(Part part)
    {
      this.part = part;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
      return part.getInputStream();
    }

    @Override
    public String getContentType()
    {
      return part.getContentType();
    }

    @Override
    public String getName()
    {
      return part.getSubmittedFileName();
    }

    @Override
    public boolean isInMemory()
    {
      return true;
    }

    @Override
    public long getSize()
    {
      return part.getSize();
    }

    @Override
    public byte[] get()
    {
      if (bytes != null) {
        return bytes;
      }
      try {
        bytes = IOUtils.toByteArray(getInputStream());
      } catch (IOException e) {
        throw new RuntimeIOException(e);
      }
      return bytes;
    }

    @Override
    public String getString(String encoding) throws UnsupportedEncodingException
    {
      return new String(get(), encoding);
    }

    @Override
    public String getString()
    {
      return new String(get());
    }

    @Override
    public void write(File file) throws IOException
    {
      LOG.warn("Unsupported operation write");
    }

    @Override
    public void delete()
    {
      LOG.warn("Unsupported operation delete");

    }

    @Override
    public String getFieldName()
    {
      return part.getName();
    }

    @Override
    public void setFieldName(String name)
    {
      LOG.warn("Unsupported operation setFieldName");
    }

    @Override
    public boolean isFormField()
    {
      return formField;
    }

    @Override
    public void setFormField(boolean state)
    {
      this.formField = state;
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
      return null;
    }

    @Override
    public FileItemHeaders getHeaders()
    {
      // TODO sn migration check
      return this.headers;
    }

    @Override
    public void setHeaders(final FileItemHeaders headers)
    {
      // TODO sn migration check
      this.headers = headers;
    }
  }

  protected void parsePart(Part part)
  {
    if (part.getSize() > maxSize) {
      throw new RuntimeException("Upload size too large. Max: " + maxSize + "; size:" + part.getSize());
    }
    FileItem fileItem = new ParFileItem(part);
    Map<String, List<FileItem>> files = getFiles();
    List<FileItem> list = files.get(part.getName());
    if (list == null) {
      list = new ArrayList<>();
      files.put(part.getName(), list);
    }
    list.add(fileItem);

  }

  @Override
  public Map<String, List<FileItem>> getFiles()
  {
    return files;
  }

  @Override
  public List<FileItem> getFile(String fieldName)
  {
    return files.get(fieldName);
  }
}
