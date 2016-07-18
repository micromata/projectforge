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

package org.projectforge.web.export;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.apache.wicket.util.resource.IResourceStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class ExportJson
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ExportJson.class);

  private String jsonName;

  private Object result;

  public ExportJson(final Object result)
  {
    this.jsonName = "output";
    this.result = result;
  }

  public ExportJson(final String jsonName, final Object result)
  {
    this.jsonName = jsonName;
    this.result = result;
  }

  public String getJsonName()
  {
    return jsonName;
  }

  public IResourceStream createResourceStreamWriter()
  {
    final IResourceStream iResourceStream = new AbstractResourceStreamWriter() {
      private static final long serialVersionUID = 7780552906708508709L;

      @Override
      public String getContentType()
      {
        return "application/json";
      }

      @Override
      public void write(final OutputStream output)
      {
        try {
          IOUtils.write(new Gson().toJson(result), output);
        } catch (IOException ex) {
          log.error("Exception encountered " + ex, ex);
        }
      }
    };
    return iResourceStream;
  }
}
