/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.fibu;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.scripting.ScriptDO;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for persisting the values of ReportScriptingAction.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class ReportScriptingStorage
{
  //private JasperReport jasperReport;

  private Map<String, String> fileMap;

  private String script;

  private ScriptDO.ScriptType type = null;

  private String lastAddedFile;

  /**
   * Compiled JasperReport design, if given.
   */
  //  public JasperReport getJasperReport()
  //  {
  //    return jasperReport;
  //  }

  // public void setJasperReport(JasperReport jasperReport, String filename)
  // {
  // this.jasperReport = jasperReport;
  // setFilename(filename, filename);
  // }

  public void setFilename(final String key, final String filename) {
    getFileMap().put(key, filename);
    lastAddedFile = key;
  }

  public File getFile(final String key) {
    final String filename = getFilename(key);
    if (filename == null) {
      return null;
    }
    return new File(filename);
  }

  public String getFilename(final String key) {
    return getFileMap().get(key);
  }

  public String getLastAddedFilename() {
    return lastAddedFile;
  }

  public String getGroovyScript()
  {
    return script;
  }

  public void setGroovyScript(final String groovyScript)
  {
    if (StringUtils.isNotEmpty(groovyScript)) {
      // Don't overwrite script with null in deserialization.
      this.script = groovyScript;
    }
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public ScriptDO.ScriptType getType() {
    return type;
  }

  public void setType(ScriptDO.ScriptType type) {
    this.type = type;
  }

  private Map<String, String> getFileMap()
  {
    if (fileMap == null) {
      fileMap = new HashMap<String, String>();
    }
    return fileMap;
  }
}
