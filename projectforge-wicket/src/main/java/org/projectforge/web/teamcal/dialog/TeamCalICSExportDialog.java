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

package org.projectforge.web.teamcal.dialog;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.service.TeamCalServiceImpl;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.calendar.AbstractICSExportDialog;
import org.projectforge.web.wicket.I18nParamMap;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivType;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class TeamCalICSExportDialog extends AbstractICSExportDialog
{
  private static final long serialVersionUID = -3840971062603541903L;

  private TeamCalDO teamCal;

  @SpringBean
  private TeamCalServiceImpl teamCalService;

  private boolean exportReminders;

  private String calendarTitle = "-";

  /**
   * @param id
   */
  @SuppressWarnings("serial")
  public TeamCalICSExportDialog(final String id)
  {
    super(id, null);
    setTitle(new Model<String>()
    {
      @Override
      public String getObject()
      {
        return getLocalizer().getString("plugins.teamcal.download", TeamCalICSExportDialog.this,
            new I18nParamMap().put("calendar", calendarTitle));
      }
    });
  }

  /**
   * @param calendarTitle the calendarTitle to set
   * @return this for chaining.
   */
  public TeamCalICSExportDialog setCalendarTitle(final AjaxRequestTarget target, final String calendarTitle)
  {
    this.calendarTitle = calendarTitle;
    addTitleLabel(target);
    return this;
  }

  public void redraw(final TeamCalDO teamCal)
  {
    this.teamCal = teamCal;
    super.redraw();
  }

  /**
   * @see org.projectforge.web.calendar.AbstractICSExportDialog#addFormFields()
   */
  @SuppressWarnings("serial")
  @Override
  protected void addFormFields()
  {
    if (addReminders(ThreadLocalUserContext.getUser())) {
      exportReminders = true;
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.options")).suppressLabelForWarning();
      final DivPanel checkBoxesPanel = new DivPanel(fs.newChildId(), DivType.BTN_GROUP);
      fs.add(checkBoxesPanel);
      final CheckBox checkBox = new CheckBox(CheckBoxPanel.WICKET_ID,
          new PropertyModel<Boolean>(this, "exportReminders"));
      checkBox.add(new OnChangeAjaxBehavior()
      {
        @Override
        protected void onUpdate(AjaxRequestTarget target)
        {
          target.add(checkBox);
          target.add(urlTextArea);
        }
      });
      checkBoxesPanel.add(new CheckBoxButton(checkBoxesPanel.newChildId(), checkBox,
          getString("plugins.teamcal.export.reminder.checkbox"))
          .setTooltip(getString("plugins.teamcal.export.reminder.checkbox.tooltip")));
    }
  }

  private boolean addReminders(PFUserDO user)
  {
    // Export reminders for owners as default.
    if (teamCal.getOwnerId() != null && teamCal.getOwnerId().equals(user.getId()) == true) {
      return true;
    }
    // Export reminders for full access users.
    if (StringUtils.isBlank(teamCal.getFullAccessUserIds()) == false) {
      List<String> fullAccessUserIds = Arrays.asList(teamCal.getFullAccessUserIds().split(","));
      if (fullAccessUserIds.contains(String.valueOf(user.getId()))) {
        return true;
      }
    }
    // Export reminders for read only users.
    if (StringUtils.isBlank(teamCal.getReadonlyAccessUserIds()) == false) {
      List<String> readonlyAccessUserIds = Arrays.asList(teamCal.getReadonlyAccessUserIds().split(","));
      if (readonlyAccessUserIds.contains(String.valueOf(user.getId()))) {
        return true;
      }
    }
    Collection<Integer> userGroupsIds = getUserGroupCache().getUserGroups(user);
    List<String> fullAccessGroupIds =
        StringUtils.isBlank(teamCal.getFullAccessGroupIds()) == false ? Arrays.asList(teamCal.getFullAccessGroupIds().split(",")) :
            Collections.emptyList();
    List<String> readonlyAccessGroupIds = StringUtils.isBlank(teamCal.getReadonlyAccessGroupIds()) == false ?
        Arrays.asList(teamCal.getReadonlyAccessGroupIds().split(",")) :
        Collections.emptyList();
    for (Integer groupId : userGroupsIds) {
      // Export reminders for full access group.
      if (fullAccessGroupIds.contains(String.valueOf(groupId))) {
        return true;
      }
      // Export reminders for read only group.
      if (readonlyAccessGroupIds.contains(String.valueOf(groupId))) {
        return true;
      }
    }
    return false;
  }

  private UserGroupCache getUserGroupCache()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
  }

  /**
   * @see org.projectforge.web.calendar.AbstractICSExportDialog#getUrl()
   */
  @Override
  protected String getUrl()
  {
    return teamCalService.getUrl(teamCal.getId(),
        "&" + teamCalService.PARAM_EXPORT_REMINDER + "=" + exportReminders);
  }

}
