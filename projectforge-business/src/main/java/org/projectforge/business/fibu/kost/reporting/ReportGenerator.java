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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.reporting.Buchungssatz;
import org.projectforge.reporting.impl.BuchungssatzImpl;

public class ReportGenerator
{
  private String jasperReportId;

  private Map<String, Object> parameters = new HashMap<String, Object>();

  private ReportOutputType outputType = ReportOutputType.PDF;

  private Collection< ? > beanCollection;

  /**
   * If given then the jasper report with this id will be used for generating the report. If not given then the default report will be used.
   * @return
   */
  public String getJasperReportId()
  {
    return jasperReportId;
  }

  public void setJasperReportId(final String jasperReportId)
  {
    this.jasperReportId = jasperReportId;
  }

  /**
   * Adds all lines of the given bwa as parameters.
   * @see ReportBwaImpl#putBwaWerte(Map, BusinessAssessment)
   */
  public void addBusinessAssessment(final BusinessAssessment businessAssessment)
  {
    BusinessAssessment.putBusinessAssessmentRows(parameters, businessAssessment);
  }

  /**
   * Creates a new business assessment from the given buchungsSaetze and adds all lines of the resulting BusinessAssessment as parameters.
   * @see ReportBwaImpl#putBwaWerte(Map, BusinessAssessment)
   */
  public BusinessAssessment addBusinessAssessment(final List<BuchungssatzDO> buchungsSaetze)
  {
    final BusinessAssessment businessAssessment = new BusinessAssessment(AccountingConfig.getInstance().getBusinessAssessmentConfig(), buchungsSaetze);
    addBusinessAssessment(businessAssessment);
    return businessAssessment;
  }

  /**
   * Adds a parameter which is accessible under the name from inside the JasperReport.
   */
  public void addParameter(final String name, final Object value)
  {
    parameters.put(name, value);
  }

  public Object getParameter(final String name)
  {
    return parameters.get(name);
  }

  /**
   * Attention: Overwrites any existing parameter!
   * @param parameters
   */
  public void setParameters(final Map<String, Object> parameters)
  {
    this.parameters = parameters;
  }

  public Map<String, Object> getParameters()
  {
    return parameters;
  }

  /**
   * The bean collection used by the JasperReport.
   * @param beanCollection
   */
  public Collection< ? > getBeanCollection()
  {
    return beanCollection;
  }

  /**
   * Converts any collection of BuchungssatzDO into list of Buchungssatz.
   * @param beanCollection
   */
  public void setBeanCollection(final Collection< ? > beanCollection)
  {
    if (CollectionUtils.isEmpty(beanCollection) == true) {
      this.beanCollection = beanCollection;
      return;
    }
    final Iterator< ? > it = beanCollection.iterator();
    if (it.next() instanceof BuchungssatzDO == true) {
      final List<Buchungssatz> list = new ArrayList<Buchungssatz>();
      @SuppressWarnings("unchecked")
      final
      Collection<BuchungssatzDO> col = (Collection<BuchungssatzDO>)beanCollection;
      for (final BuchungssatzDO buchungssatzDO : col) {
        final Buchungssatz satz = new BuchungssatzImpl(buchungssatzDO);
        list.add(satz);
      }
      this.beanCollection = list;
    } else {
      this.beanCollection = beanCollection;
    }
  }

  public ReportOutputType getOutputType()
  {
    return outputType;
  }

  public void setOutputType(final ReportOutputType outputType)
  {
    this.outputType = outputType;
  }

  public void setOutputType(final String outputType)
  {
    this.outputType = ReportOutputType.getType(outputType);
  }
}
