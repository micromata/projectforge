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

package org.projectforge.web.fibu;

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

  private String groovyScript;

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
    return groovyScript;
  }

  public void setGroovyScript(final String groovyScript)
  {
    this.groovyScript = groovyScript;
  }

  private Map<String, String> getFileMap()
  {
    if (fileMap == null) {
      fileMap = new HashMap<String, String>();
    }
    return fileMap;
  }
}
