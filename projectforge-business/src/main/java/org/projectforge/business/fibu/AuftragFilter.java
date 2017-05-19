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

package org.projectforge.business.fibu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("AuftragFilter")
public class AuftragFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 3456000966109255447L;

  private int year;

  private PFUserDO user;

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

  /**
   * Year of invoices to filter. "<= 0" means showing all years.
   */
  public int getYear()
  {
    return year;
  }

  public void setYear(final int year)
  {
    this.year = year;
  }

  public PFUserDO getUser()
  {
    return user;
  }

  public void setUser(final PFUserDO user)
  {
    this.user = user;
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
    year = -1;
    user = null;
    auftragsStatuses.clear();
    auftragsPositionsArten.clear();
    setAuftragFakturiertFilterStatus(null);
    setAuftragsPositionsPaymentType(null);
    return this;
  }
}
