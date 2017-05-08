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

  // TODO CT: delete fibu.auftrag.filter.type.* i18n keys?
  public static final String FILTER_ALL = "all";

  public static final String FILTER_AKQUISE = "akquise";

  public static final String FILTER_BEAUFTRAGT = "beauftragt";

  public static final String FILTER_BEAUFTRAGT_NOCH_NICHT_VOLLSTAENDIG_FAKTURIERT = "beauftragtNochNichtVollstaendigFakturiert";

  public static final String FILTER_ABGESCHLOSSEN_NF = "abgeschlossenNichtFakturiert";

  public static final String FILTER_VOLLSTAENDIG_FAKTURIERT = "vollstaendigFakturiert";

  public static final String FILTER_NOCH_NICHT_VOLLSTAENDIG_FAKTURIERT = "nochNichtVollstaendigFakturiert";

  public static final String FILTER_ABGELEHNT = "abgelehnt";

  public static final String FILTER_ERSETZT = "ersetzt";

  protected int year;

  protected PFUserDO user;

  protected String listType = FILTER_ALL;

  private Collection<AuftragsStatus> auftragsStatuses;

  private Collection<AuftragsPositionsArt> auftragsPositionsArten;

  protected AuftragsPositionsPaymentType auftragsPositionsPaymentType;

  public static final String[] LIST = { FILTER_ALL, FILTER_AKQUISE, FILTER_BEAUFTRAGT, FILTER_NOCH_NICHT_VOLLSTAENDIG_FAKTURIERT,
      FILTER_BEAUFTRAGT_NOCH_NICHT_VOLLSTAENDIG_FAKTURIERT,
      FILTER_ABGESCHLOSSEN_NF, FILTER_VOLLSTAENDIG_FAKTURIERT, FILTER_ABGELEHNT, FILTER_ERSETZT };

  public AuftragFilter()
  {
  }

  public AuftragFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public boolean isShowAll()
  {
    return FILTER_ALL.equals(listType);
  }

  public boolean isShowAkquise()
  {
    return FILTER_AKQUISE.equals(listType);
  }

  public boolean isShowBeauftragt()
  {
    return FILTER_BEAUFTRAGT.equals(listType);
  }

  public boolean isShowBeauftragtNochNichtVollstaendigFakturiert()
  {
    return FILTER_BEAUFTRAGT_NOCH_NICHT_VOLLSTAENDIG_FAKTURIERT.equals(listType);
  }

  public boolean isShowNochNichtVollstaendigFakturiert()
  {
    return FILTER_NOCH_NICHT_VOLLSTAENDIG_FAKTURIERT.equals(listType);
  }

  public boolean isShowVollstaendigFakturiert()
  {
    return FILTER_VOLLSTAENDIG_FAKTURIERT.equals(listType);
  }

  public boolean isShowAbgeschlossenNichtFakturiert()
  {
    return FILTER_ABGESCHLOSSEN_NF.equals(listType);
  }

  public boolean isShowAbgelehnt()
  {
    return FILTER_ABGELEHNT.equals(listType);
  }

  public boolean isShowErsetzt()
  {
    return FILTER_ERSETZT.equals(listType);
  }

  public String getListType()
  {
    return this.listType;
  }

  public void setListType(String listType)
  {
    if (listType == null) {
      listType = FILTER_ALL;
      return;
    }
    this.listType = listType;
  }

  /**
   * Year of invoices to filter. "<= 0" means showing all years.
   *
   * @return
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
   * null / empty collection represents all.
   */
  public Collection<AuftragsStatus> getAuftragsStatuses()
  {
    return auftragsStatuses;
  }

  public void setAuftragsStatuses(final Collection<AuftragsStatus> auftragsStatuses)
  {
    this.auftragsStatuses = auftragsStatuses;
  }

  /**
   * null / empty collection represents all.
   */
  public Collection<AuftragsPositionsArt> getAuftragsPositionsArten()
  {
    return auftragsPositionsArten;
  }

  public void setAuftragsPositionsArten(final Collection<AuftragsPositionsArt> auftragsPositionsArten)
  {
    this.auftragsPositionsArten = auftragsPositionsArten;
  }

  /**
   * null represents all.
   *
   * @return
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
    year = -1;
    searchString = "";
    setAuftragsStatuses(null);
    setAuftragsPositionsArten(null);
    setListType(AuftragFilter.FILTER_ALL);
    return this;
  }
}
