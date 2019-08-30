/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.kost.reporting;

import java.util.HashSet;
import java.util.Set;

public class ReportStorage
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReportStorage.class);

  private Report root;

  private Report currentReport;
  
  private String fileName;

  private Set<String> openReports = new HashSet<String>();
  
  public ReportStorage(Report root)
  {
    this.root = root;
    this.currentReport = root;
  }

  public Report getRoot()
  {
    return root;
  }
  
  public Report getCurrentReport()
  {
    return currentReport;
  }

  public void setCurrentReport(String id)
  {
    Report report = root.findById(id);
    if (report != null) {
      this.currentReport = report;
    } else {
      log.error("Report with id '" + id + "' not found.");
    }
  }
  
  public Report findById(String id) {
    return root.findById(id);
  }

  /**
   * @return true, wenn die Childreports des current Reports angezeigt werden sollen.
   */
  public boolean isOpen()
  {
    return this.openReports.contains(currentReport.getId());
  }

  /** Set report only as open, if the report has child reports. */
  public void setOpen(boolean opened)
  {
    if (currentReport.hasChildren() == false) {
      log.info("Try to open a report without children, ignoring this operation for " + currentReport.getId());
      return;
    }
    if (opened == true) {
      this.openReports.add(currentReport.getId());
    } else {
      if (this.openReports.contains(currentReport.getId()) == true) {
        this.openReports.remove(currentReport.getId());
      }
    }
  }
  
  /**
   * The name of the file where the report objectives were defined.
   */
  public String getFileName()
  {
    return fileName;
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }
}
