/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.scripting;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.apache.wicket.util.resource.IResourceStream;
import org.projectforge.business.scripting.ExportJson;
import org.projectforge.business.scripting.ExportZipArchive;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author kai
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public abstract class ScriptingHelper {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScriptingHelper.class);

  public static IResourceStream createResourceStreamWriter(ExportZipArchive exportZipArchive) {
    final IResourceStream iResourceStream = new AbstractResourceStreamWriter() {
      private static final long serialVersionUID = 7780552906708508709L;

      @Override
      public String getContentType() {
        return "application/zip";
      }

      @Override
      public void write(final OutputStream output) {
        exportZipArchive.write(output);
      }
    };
    return iResourceStream;
  }

  public static IResourceStream createResourceStreamWriter(ExportJson exportJson) {
    final IResourceStream iResourceStream = new AbstractResourceStreamWriter() {
      private static final long serialVersionUID = 7780552906708508709L;

      @Override
      public String getContentType() {
        return "application/json";
      }

      @Override
      public void write(final OutputStream output) {
        try {
          IOUtils.write(new Gson().toJson(exportJson.getResult()), output);
        } catch (IOException ex) {
          log.error("Exception encountered " + ex, ex);
        }
      }
    };
    return iResourceStream;
  }
}
