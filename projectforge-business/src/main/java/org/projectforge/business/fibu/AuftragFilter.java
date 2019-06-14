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

package org.projectforge.business.fibu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("AuftragFilter")
public class AuftragFilter extends BaseSearchFilter implements Serializable, SearchFilterWithPeriodOfPerformance
{
  private static final long serialVersionUID = 3456000966109255447L;

  private PFUserDO user;

  private Date startDate;

  private Date endDate;

  private Date periodOfPerformanceStartDate;

  private Date periodOfPerformanceEndDate;

  private final Collection<AuftragsStatus> auftragsStatuses = new ArrayList<>();

  private final Collection<AuftragsPositionsArt> auftragsPositionsArten = new ArrayList<>();

  private AuftragFakturiertFilterStatus auftragFakturiertFilterStatus;

  private AuftragsPositionsPaymentType auftragsPositionsPaymentType;

  public AuftragFilter()
  {
  }

  public AuftragFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public PFUserDO getUser()
  {
    return user;
  }

  public void setUser(final PFUserDO user)
  {
    this.user = user;
  }

  public Date getStartDate()
  {
    return startDate;
  }

  public void setStartDate(final Date startDate)
  {
    this.startDate = startDate;
  }

  public Date getEndDate()
  {
    return endDate;
  }

  public void setEndDate(final Date endDate)
  {
    this.endDate = endDate;
  }

  @Override
  public Date getPeriodOfPerformanceStartDate()
  {
    return periodOfPerformanceStartDate;
  }

  public void setPeriodOfPerformanceStartDate(final Date periodOfPerformanceStartDate)
  {
    this.periodOfPerformanceStartDate = periodOfPerformanceStartDate;
  }

  @Override
  public Date getPeriodOfPerformanceEndDate()
  {
    return periodOfPerformanceEndDate;
  }

  public void setPeriodOfPerformanceEndDate(final Date periodOfPerformanceEndDate)
  {
    this.periodOfPerformanceEndDate = periodOfPerformanceEndDate;
  }

  /**
   * empty collection represents all.
   */
  public Collection<AuftragsStatus> getAuftragsStatuses()
  {
    return auftragsStatuses;
  }

  /**
   * empty collection represents all.
   */
  public Collection<AuftragsPositionsArt> getAuftragsPositionsArten()
  {
    return auftragsPositionsArten;
  }

  public AuftragFakturiertFilterStatus getAuftragFakturiertFilterStatus()
  {
    if (auftragFakturiertFilterStatus == null) {
      auftragFakturiertFilterStatus = AuftragFakturiertFilterStatus.ALL;
    }
    return auftragFakturiertFilterStatus;
  }

  public void setAuftragFakturiertFilterStatus(final AuftragFakturiertFilterStatus auftragFakturiertFilterStatus)
  {
    this.auftragFakturiertFilterStatus = auftragFakturiertFilterStatus;
  }

  /**
   * null represents all.
   */
  public AuftragsPositionsPaymentType getAuftragsPositionsPaymentType()
  {
    return auftragsPositionsPaymentType;
  }

  public void setAuftragsPositionsPaymentType(final AuftragsPositionsPaymentType auftragsPositionsPaymentType)
  {
    this.auftragsPositionsPaymentType = auftragsPositionsPaymentType;
  }

  @Override
  public AuftragFilter reset()
  {
    searchString = "";
    startDate = null;
    endDate = null;
    periodOfPerformanceStartDate = null;
    periodOfPerformanceEndDate = null;
    user = null;
    auftragsStatuses.clear();
    auftragsPositionsArten.clear();
    setAuftragFakturiertFilterStatus(null);
    setAuftragsPositionsPaymentType(null);
    return this;
  }
}
