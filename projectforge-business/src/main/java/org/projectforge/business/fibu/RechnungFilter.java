/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.persistence.api.BaseSearchFilter;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class RechnungFilter extends BaseSearchFilter implements Serializable
{
  public static final String FILTER_ALL = "all";

  public static final String FILTER_BEZAHLT = "bezahlt";

  public static final String FILTER_UEBERFAELLIG = "ueberfaellig";

  public static final String FILTER_UNBEZAHLT = "unbezahlt";

  private static final long serialVersionUID = 3078373853576678481L;

  protected LocalDate fromDate, toDate;

  protected String listType = FILTER_ALL;

  private boolean showKostZuweisungStatus;

  public RechnungFilter()
  {
  }

  public RechnungFilter(final BaseSearchFilter filter)
  {
    super(filter);

    if (filter instanceof RechnungFilter) {
      RechnungFilter obj = ((RechnungFilter) filter);

      this.listType = obj.getListType();
      this.fromDate = obj.getFromDate();
      this.toDate = obj.getToDate();
      this.showKostZuweisungStatus = obj.isShowKostZuweisungStatus();
    }
  }

  /**
   * Zeige Fehlbeträge in der Liste.
   */
  public boolean isShowKostZuweisungStatus()
  {
    return showKostZuweisungStatus;
  }

  public void setShowKostZuweisungStatus(final boolean showKostZuweisungStatus)
  {
    this.showKostZuweisungStatus = showKostZuweisungStatus;
  }

  /**
   * Standard means to consider options: current, departed, uninteresting, personaIngrata, ...
   *
   * @return
   */
  public boolean isShowAll()
  {
    return FILTER_ALL.equals(listType);
  }

  public RechnungFilter setShowAll()
  {
    listType = FILTER_ALL;
    return this;
  }

  public RechnungFilter setShowUnbezahlt()
  {
    listType = FILTER_UNBEZAHLT;
    return this;
  }

  public boolean isShowUnbezahlt()
  {
    return FILTER_UNBEZAHLT.equals(listType);
  }

  public RechnungFilter setShowBezahlt()
  {
    listType = FILTER_BEZAHLT;
    return this;
  }

  public boolean isShowBezahlt()
  {
    return FILTER_BEZAHLT.equals(listType);
  }

  public RechnungFilter setShowUeberFaellig()
  {
    listType = FILTER_UEBERFAELLIG;
    return this;
  }

  public boolean isShowUeberFaellig()
  {
    return FILTER_UEBERFAELLIG.equals(listType);
  }

  public String getListType()
  {
    return this.listType;
  }

  /**
   * @param listType
   * @return this for chaining.
   */
  public RechnungFilter setListType(final String listType)
  {
    this.listType = listType;
    return this;
  }

  public LocalDate getFromDate()
  {
    return fromDate;
  }

  /**
   * @param fromDate the fromDate to set
   * @return this for chaining.
   */
  public RechnungFilter setFromDate(final LocalDate fromDate)
  {
    this.fromDate = fromDate;
    return this;
  }

  public LocalDate getToDate()
  {
    return toDate;
  }

  /**
   * @param toDate the toDate to set
   * @return this for chaining.
   */
  public RechnungFilter setToDate(final LocalDate toDate)
  {
    this.toDate = toDate;
    return this;
  }

  @Override
  public RechnungFilter reset()
  {
    super.reset();
    fromDate = null;
    toDate = null;
    setShowAll();
    setSearchString("");
    showKostZuweisungStatus = false;
    return this;
  }

}
