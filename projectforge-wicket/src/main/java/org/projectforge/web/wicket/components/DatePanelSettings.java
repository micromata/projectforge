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

package org.projectforge.web.wicket.components;

import java.io.Serializable;
import java.util.Date;

import org.projectforge.web.calendar.CalendarPage;


/**
 * Fluent design pattern.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatePanelSettings implements Serializable
{
  private static final long serialVersionUID = 6001099642364749183L;

  protected boolean required;

  protected boolean selectPeriodMode;

  protected boolean selectStartStopTime;

  protected String tooltipI18nKey;

  protected String selectProperty;

  protected Class< ? extends Date> targetType = Date.class;

  protected Integer tabIndex;

  /**
   * Default is null.
   * @param tabIndex Use tabIndex as html tab index of date field (if visible), hours and minutes.
   * @return this
   */
  public DatePanelSettings withTabIndex(final Integer tabIndex)
  {
    this.tabIndex = tabIndex;
    return this;
  }

  public static DatePanelSettings get()
  {
    return new DatePanelSettings();
  }

  /**
   * Target type of the model (java.util.Date is default).
   * @param targetType
   * @return this
   */
  public DatePanelSettings withTargetType(final Class< ? extends Date> targetType)
  {
    this.targetType = targetType;
    return this;
  }

  /**
   * If true the user can select periods (weeks, months, hours).
   * @param selectPeriodMode
   * @return this
   * @see CalendarPage#setSelectPeriodMode(boolean)
   */
  public DatePanelSettings withSelectPeriodMode(final boolean selectPeriodMode)
  {
    this.selectPeriodMode = selectPeriodMode;
    return this;
  }

  /**
   * If true the user can select time stamps of time sheets.
   * @param selectStartStopTime
   * @return this
   * @see CalendarPage#setSelectStartStopTime(boolean)
   */
  public DatePanelSettings withSelectStartStopTime(final boolean selectStartStopTime)
  {
    this.selectStartStopTime = selectStartStopTime;
    return this;
  }

  /**
   * Customized tool tip to show for the calendar view select icon.
   * @param i18nKey
   * @return this
   */
  public DatePanelSettings withTooltip(final String i18nKey)
  {
    this.tooltipI18nKey = i18nKey;
    return this;
  }

  /**
   * If not given then the wicket id of the component will be used.
   * @param selectProperty
   * @return this
   */
  public DatePanelSettings withSelectProperty(final String selectProperty)
  {
    this.selectProperty = selectProperty;
    return this;
  }

  /**
   * Use this instead of setRequired(boolean) of the panel itself.
   * @param required
   */
  public DatePanelSettings withRequired(final boolean required)
  {
    this.required = required;
    return this;
  }
}
