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

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class CalendarPageSupport implements Serializable
{
  private static final long serialVersionUID = 8223888624685052575L;

  private AccessChecker accessChecker;

  private final PFUserDO user;

  private final ISelectCallerPage parentPage;

  private boolean showTimsheetsSelectors = true, showOptions = true;

  private UserSelectPanel userSelectPanel;

  public CalendarPageSupport(final ISelectCallerPage parentPage, AccessChecker accessChecker)
  {
    this.parentPage = parentPage;
    this.user = ThreadLocalUserContext.getUser();
    this.accessChecker = accessChecker;
  }

  public UserSelectPanel addUserSelectPanel(final FieldsetPanel fieldset, final IModel<PFUserDO> model,
      final boolean autosubmit)
  {
    if (showTimsheetsSelectors == false || isOtherTimesheetsUsersAllowed() == false) {
      return null;
    }
    userSelectPanel = new UserSelectPanel(fieldset.newChildId(), model, parentPage, "userId");
    fieldset.add(userSelectPanel);
    userSelectPanel.withAutoSubmit(autosubmit).setLabel(new Model<String>(fieldset.getString("user"))).init();
    return userSelectPanel;
  }

  public void addOptions(final DivPanel checkBoxDivPanel, final boolean autoSubmit,
      final ICalendarFilter filter)
  {
    if (accessChecker.isRestrictedUser(user) == true || showOptions == false) {
      return;
    }
    if (isOtherTimesheetsUsersAllowed() == false) {
      addCheckBox(checkBoxDivPanel, filter, "showTimesheets", "calendar.option.timesheeets", null, autoSubmit);
    }
    addCheckBox(checkBoxDivPanel, filter, "showBreaks", "calendar.option.showBreaks",
        "calendar.option.showBreaks.tooltip", autoSubmit);
    addCheckBox(checkBoxDivPanel, filter, "showPlanning", "calendar.option.planning",
        "calendar.option.planning.tooltip", autoSubmit);
    if (Configuration.getInstance().isAddressManagementConfigured() == true) {
      addCheckBox(checkBoxDivPanel, filter, "showBirthdays", "calendar.option.birthdays", null, autoSubmit);
    }
    addCheckBox(checkBoxDivPanel, filter, "showStatistics", "calendar.option.statistics",
        "calendar.option.statistics.tooltip", autoSubmit);
  }

  @SuppressWarnings("serial")
  private void addCheckBox(final DivPanel checkBoxDivPanel, final ICalendarFilter filter, final String property,
      final String labelKey,
      final String tooltipKey, final boolean autoSubmit)
  {
    final CheckBoxButton checkBoxButton = new CheckBoxButton(checkBoxDivPanel.newChildId(),
        new PropertyModel<Boolean>(filter, property), checkBoxDivPanel.getString(labelKey), autoSubmit ? new FormComponentUpdatingBehavior() : null);
    if (autoSubmit == false) {
      checkBoxButton.getCheckBox().add(new OnChangeAjaxBehavior()
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          // Do nothing (the model object is updated).
        }
      });
    }
    if (tooltipKey != null) {
      checkBoxButton.setTooltip(checkBoxDivPanel.getString(tooltipKey));
    }
    checkBoxDivPanel.add(checkBoxButton);
  }

  /**
   * Has the logged-in user the permission to see time-sheets of other users?
   *
   * @return
   */
  public boolean isOtherTimesheetsUsersAllowed()
  {
    return accessChecker.hasLoggedInUserAccessToTimesheetsOfOtherUsers();
  }

  /**
   * @param showOptions the showOptions to set
   * @return this for chaining.
   */
  public CalendarPageSupport setShowOptions(final boolean showOptions)
  {
    this.showOptions = showOptions;
    return this;
  }

  /**
   * @param showTimsheetsSelectors the showTimsheetsSelectors to set
   * @return this for chaining.
   */
  public CalendarPageSupport setShowTimsheetsSelectors(final boolean showTimsheetsSelectors)
  {
    this.showTimsheetsSelectors = showTimsheetsSelectors;
    return this;
  }

  /**
   * @return the userSelectPanel
   */
  public UserSelectPanel getUserSelectPanel()
  {
    return userSelectPanel;
  }
}
