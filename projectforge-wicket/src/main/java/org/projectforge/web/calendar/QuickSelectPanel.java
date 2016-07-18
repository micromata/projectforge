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

package org.projectforge.web.calendar;

import java.util.Date;

import org.apache.wicket.model.Model;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.components.DatePanel;


/**
 * This panel combines QuickSelectMonthPanel and QuickSelectWeekPanel.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class QuickSelectPanel extends AbstractSelectPanel<Date>
{
  private static final long serialVersionUID = 8334141505387689261L;

  private final DatePanel datePanel;

  private final ISelectCallerPage caller;

  /**
   * @param id
   * @param model Should contain begin of month as object.
   * @param caller
   * @param selectProperty Results int two select properties: selectProperty + ".month" and selectProperty + ".week".
   */
  public QuickSelectPanel(final String id, final ISelectCallerPage caller, final String selectProperty, final DatePanel datePanel)
  {
    super(id, new Model<Date>(), caller, selectProperty);
    this.caller = caller;
    this.datePanel = datePanel;
  }

  @Override
  @SuppressWarnings("serial")
  public QuickSelectPanel init()
  {
    super.init();
    final QuickSelectMonthPanel quickSelectMonthPanel = new QuickSelectMonthPanel("quickSelectMonth", new Model<Date>() {
      @Override
      public Date getObject()
      {
        return getInputDate();
      }
    }, caller, selectProperty + ".month");
    add(quickSelectMonthPanel);
    quickSelectMonthPanel.init();
    final QuickSelectWeekPanel quickSelectWeekPanel = new QuickSelectWeekPanel("quickSelectWeek", new Model<Date>() {
      @Override
      public Date getObject()
      {
        return getInputDate();
      }
    }, caller, selectProperty + ".week");
    add(quickSelectWeekPanel);
    quickSelectWeekPanel.init();
    return this;
  }

  public Date getInputDate() {
    datePanel.getDateField().validate(); // Update model from form field.
    final Date date = datePanel.getDateField().getConvertedInput();
    return date;
  }
}
