/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.scripting.ScriptExecutionResult;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.export.ExportJson;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;

import java.util.Date;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public abstract class AbstractScriptingPage extends AbstractStandardFormPage
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractScriptingPage.class);

  protected ScriptExecutionResult scriptExecutionResult;

  public AbstractScriptingPage(final PageParameters parameters)
  {
    super(parameters);
  }

  protected void jsonExport()
  {
    try {
      final ExportJson exportJson = (ExportJson) scriptExecutionResult.getResult();
      final StringBuilder sb = new StringBuilder();
      sb.append(exportJson.getJsonName()).append("_");
      sb.append(DateHelper.getTimestampAsFilenameSuffix(new Date())).append(".json");
      final String filename = sb.toString();
      DownloadUtils.setDownloadTarget(filename, exportJson.createResourceStreamWriter());
    } catch (final Exception ex) {
      error(getLocalizedMessage("error", ex.getMessage()));
      log.error(ex.getMessage(), ex);
    }
  }
}
