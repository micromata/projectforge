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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportGeneratorList
{
  private List<ReportGenerator> reports = new ArrayList<ReportGenerator>();
  
  public void addReportGenerator(ReportGenerator generator) {
    reports.add(generator);
  }
  
  /**
   * Creates and adds a new ReportGenerator.
   * @return The created and added ReportGenerator.
   */
  public ReportGenerator addReport() {
    ReportGenerator generator = new ReportGenerator();
    reports.add(generator);
    return generator;
  }

  /**
   * Creates and adds a new ReportGenerator with the given parameters.
   * @param parameters
   * @return The created and added ReportGenerator.
   */
  public ReportGenerator addReport(Map<String, Object> parameters) {
    ReportGenerator generator = addReport();
    generator.setParameters(parameters);
    return generator;
  }

  /**
   * Creates and adds a new ReportGenerator with the given parameters and jasperReportId.
   * @param jasperReportId
   * @param parameters
   * @return The created and added ReportGenerator.
   */
  public ReportGenerator addReport(String jasperReportId, Map<String, Object> parameters) {
    ReportGenerator generator = addReport(parameters);
    generator.setJasperReportId(jasperReportId);
    return generator;
  }

  /**
   * Creates and adds a new ReportGenerator with the given jasperReportId.
   * @param jasperReportId
   * @return The created and added ReportGenerator.
   */
  public ReportGenerator addReport(String jasperReportId) {
    ReportGenerator generator = new ReportGenerator();
    generator.setJasperReportId(jasperReportId);
    return generator;
  }
  
  public List<ReportGenerator> getReports()
  {
    return reports;
  }
}
