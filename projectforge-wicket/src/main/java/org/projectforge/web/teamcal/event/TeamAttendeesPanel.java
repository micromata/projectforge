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

package org.projectforge.web.teamcal.event;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.teamcal.event.model.TeamAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.components.AjaxMaxLengthEditableLabel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TeamAttendeesPanel extends Panel
{
  private static final long serialVersionUID = 5951744897882589488L;

  private final Set<TeamEventAttendeeDO> attendees;

  private RepeatingView attendeesRepeater;

  private WebMarkupContainer mainContainer;

  private LabelValueChoiceRenderer<TeamAttendeeStatus> statusChoiceRenderer;

  /**
   * @param id
   */
  public TeamAttendeesPanel(final String id, final Set<TeamEventAttendeeDO> attendees)
  {
    super(id);
    this.attendees = attendees;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    statusChoiceRenderer = new LabelValueChoiceRenderer<TeamAttendeeStatus>(this, TeamAttendeeStatus.values());
    mainContainer = new WebMarkupContainer("main");
    add(mainContainer.setOutputMarkupId(true));
    attendeesRepeater = new RepeatingView("liRepeater");
    mainContainer.add(attendeesRepeater);
    rebuildAttendees();
    final WebMarkupContainer item = new WebMarkupContainer("liAddNewAttendee");
    mainContainer.add(item);
    item.add(new AttendeeEditableLabel("editableLabel", Model.of(new TeamEventAttendeeDO()), true));
    item.add(new Label("status", "invisible").setVisible(false));
    attendeesRepeater.setVisible(true);
  }

  @SuppressWarnings("serial")
  class AttendeeEditableLabel extends AjaxMaxLengthEditableLabel
  {
    private IModel<TeamEventAttendeeDO> attendeeModel;

    private boolean lastEntry;

    AttendeeEditableLabel(final String id, final IModel<TeamEventAttendeeDO> attendeeModel, final boolean lastEntry)
    {
      super("editableLabel", new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          if (lastEntry == true) {
            return TeamAttendeesPanel.this.getString("plugins.teamcal.event.addNewAttendee");
          }
          final TeamEventAttendeeDO attendee = attendeeModel.getObject();
          if (attendee.getAddressId() != null) {
            final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry()
                .getUserGroupCache();
            final PFUserDO user = userGroupCache.getUser(attendee.getAddressId());
            return user != null ? user.getFullname() : attendee.getUrl();
          }
          return attendee.getUrl();
        }

        /**
         * @see org.apache.wicket.model.Model#setObject(java.io.Serializable)
         */
        @Override
        public void setObject(final String object)
        {
          final TeamEventAttendeeDO attendee = attendeeModel.getObject();
          if (StringUtils.isBlank(object) == true) {
            attendee.setUrl(null);
            attendee.setAddress(null);
            return;
          }
          final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
          //          final AddressDO address = userGroupCache.getUserByFullname(object);
          //          if (user != null) {
          //            attendee.setAddress(address);
          //            attendee.setUrl(null);
          //          } else {
          //            attendee.setUrl(object);
          //            attendee.setUser(null);
          //          }
        }
      }, TeamEventAttendeeDO.URL_MAX_LENGTH);
      this.attendeeModel = attendeeModel;
      this.lastEntry = lastEntry;
      setType(String.class);
    }

    /**
     * @return the attendeeModel
     */
    TeamEventAttendeeDO getAttendee()
    {
      return attendeeModel.getObject();
    }

    /**
     * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onEdit(org.apache.wicket.ajax.AjaxRequestTarget)
     */
    @Override
    public void onEdit(final AjaxRequestTarget target)
    {
      super.onEdit(target);
    }

    /**
     * @see org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
     */
    @Override
    protected void onSubmit(final AjaxRequestTarget target)
    {
      final TeamEventAttendeeDO attendee = attendeeModel.getObject();
      if (lastEntry == true) {
        final TeamEventAttendeeDO clone = new TeamEventAttendeeDO();
        clone.setUrl(attendee.getUrl()).setAddress(attendee.getAddress());
        addAttendee(clone);
        rebuildAttendees();
        target.add(mainContainer);
      } else if (attendee.getAddressId() == null && StringUtils.isBlank(attendee.getUrl()) == true) {
        final Iterator<TeamEventAttendeeDO> it = attendees.iterator();
        while (it.hasNext() == true) {
          if (it.next() == attendeeModel.getObject()) {
            it.remove();
          }
        }
        rebuildAttendees();
        target.add(mainContainer);
      }
      super.onSubmit(target);
    }

    @Override
    protected FormComponent<String> newEditor(final MarkupContainer parent, final String componentId,
        final IModel<String> model)
    {
      final FormComponent<String> form = super.newEditor(parent, componentId, model);
      return form;
    }
  }

  private void rebuildAttendees()
  {
    attendeesRepeater.removeAll();
    for (final TeamEventAttendeeDO attendee : attendees) {
      final WebMarkupContainer item = new WebMarkupContainer(attendeesRepeater.newChildId());
      attendeesRepeater.add(item);
      item.add(new AttendeeEditableLabel("editableLabel", Model.of(attendee), false));
      final DropDownChoice<TeamAttendeeStatus> statusChoice = new DropDownChoice<TeamAttendeeStatus>("status",
          new PropertyModel<TeamAttendeeStatus>(attendee, "status"), statusChoiceRenderer.getValues(),
          statusChoiceRenderer);
      statusChoice.setEnabled(false);
      item.add(statusChoice);
    }
  }

  private void addAttendee(final TeamEventAttendeeDO attendee)
  {
    short number = 1;
    for (final TeamEventAttendeeDO pos : attendees) {
      if (pos.getNumber() >= number) {
        number = pos.getNumber();
        number++;
      }
    }
    attendee.setNumber(number);
    this.attendees.add(attendee);
  }
}
