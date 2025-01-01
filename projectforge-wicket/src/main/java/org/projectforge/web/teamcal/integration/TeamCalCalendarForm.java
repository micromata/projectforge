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

package org.projectforge.web.teamcal.integration;

import net.fortuna.ical4j.model.component.VEvent;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.ical.ICalParser;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter;
import org.projectforge.business.teamcal.filter.TemplateEntry;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.calendar.CalendarForm;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.calendar.CalendarPageSupport;
import org.projectforge.web.dialog.ModalMessageDialog;
import org.projectforge.web.teamcal.admin.TeamCalListPage;
import org.projectforge.web.teamcal.dialog.TeamCalFilterDialog;
import org.projectforge.web.teamcal.event.TeamEventEditForm;
import org.projectforge.web.teamcal.event.TeamEventEditPage;
import org.projectforge.web.teamcal.event.TeamEventListPage;
import org.projectforge.web.teamcal.event.importics.DropIcsPanel;
import org.projectforge.web.teamcal.event.importics.TeamCalImportPage;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class TeamCalCalendarForm extends CalendarForm
{
  private static final long serialVersionUID = -5838203593605203398L;

  private static final String MAGIC_FILTER_ENTRY = "__MAGIC-FILTER-ENTRY__";

  private ModalMessageDialog errorDialog;

  @SuppressWarnings("unused")
  private TemplateEntry activeTemplate;

  private DropDownChoicePanel<TemplateEntry> templateChoice;

  /**
   * @param parentPage
   */
  public TeamCalCalendarForm(final CalendarPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.calendar.CalendarForm#createCalendarPageSupport()
   */
  @Override
  protected CalendarPageSupport createCalendarPageSupport()
  {
    return new CalendarPageSupport(parentPage).setShowOptions(false).setShowTimsheetsSelectors(false);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    {
      final IconButtonPanel icsExportButtonPanel = new IconButtonPanel(buttonGroupPanel.newChildId(), IconType.LIST,
          new ResourceModel(
              "plugins.teamcal.calendar.listAndIcsExport.tooltip"))
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.IconButtonPanel#onSubmit()
         */
        @Override
        protected void onSubmit()
        {
          setResponsePage(TeamCalListPage.class);
        }
      };
      icsExportButtonPanel.setDefaultFormProcessing(false);
      buttonGroupPanel.addButton(icsExportButtonPanel);
    }
    {
      final IconButtonPanel searchButtonPanel = new IconButtonPanel(buttonGroupPanel.newChildId(), IconType.SEARCH,
          new ResourceModel(
              "search"))
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.IconButtonPanel#onSubmit()
         */
        @Override
        protected void onSubmit()
        {
          final Set<Long> visibleCalsSet = ((TeamCalCalendarFilter) filter).getActiveVisibleCalendarIds();
          final String calendars = StringHelper.objectColToString(visibleCalsSet, ",");
          final TeamEventListPage teamEventListPage = new TeamEventListPage(
              new PageParameters().add(TeamEventListPage.PARAM_CALENDARS,
                  calendars));
          setResponsePage(teamEventListPage);
        }
      };
      searchButtonPanel.setDefaultFormProcessing(false);
      buttonGroupPanel.addButton(searchButtonPanel);
    }

    final TeamCalFilterDialog dialog = new TeamCalFilterDialog(parentPage.newModalDialogId(),
        (TeamCalCalendarFilter) filter)
    {
      /**
       * @see org.projectforge.web.teamcal.dialog.TeamCalFilterDialog#onClose(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onClose(final AjaxRequestTarget target)
      {
        activeTemplate = ((TeamCalCalendarFilter) filter).getActiveTemplateEntry();
        target.add(templateChoice.getDropDownChoice());
      }
    };
    parentPage.add(dialog);
    dialog.init();
    final List<TemplateEntry> values = ((TeamCalCalendarFilter) filter).getTemplateEntries();
    final LabelValueChoiceRenderer<TemplateEntry> templateNamesChoiceRenderer = new LabelValueChoiceRenderer<TemplateEntry>();
    for (final TemplateEntry entry : values) {
      templateNamesChoiceRenderer.addValue(entry, entry.getName());
    }
    templateNamesChoiceRenderer.addValue(new TemplateEntry().setName(MAGIC_FILTER_ENTRY),
        getString("plugins.teamcal.calendar.filter.newEntry"));
    final IModel<TemplateEntry> activeModel = new PropertyModel<TemplateEntry>(this, "activeTemplate");
    this.activeTemplate = ((TeamCalCalendarFilter) filter).getActiveTemplateEntry();
    templateChoice = new DropDownChoicePanel<TemplateEntry>(fieldset.newChildId(), activeModel,
        templateNamesChoiceRenderer.getValues(),
        templateNamesChoiceRenderer, false);
    fieldset.add(templateChoice);
    templateChoice.setTooltip(getString("plugins.teamcal.calendar.filter.choose"));
    templateChoice.getDropDownChoice().setOutputMarkupId(true);

    templateChoice.getDropDownChoice().add(new AjaxFormComponentUpdatingBehavior("change")
    {
      private static final long serialVersionUID = 8999698636114154230L;

      @Override
      protected void onUpdate(AjaxRequestTarget target)
      {
        final TemplateEntry selectedEntry = activeModel.getObject();
        if (MAGIC_FILTER_ENTRY.equals(selectedEntry.getName()) == false) {
          ((TeamCalCalendarFilter) filter).setActiveTemplateEntry(selectedEntry);
          filter.setSelectedCalendar(TemplateEntry.calcCalendarStringForCalendar(selectedEntry.getDefaultCalendarId()));
          setResponsePage(getParentPage().getClass());
        } else {
          final String newTemplateName = ((TeamCalCalendarFilter) filter)
              .getNewTemplateName(getString("plugins.teamcal.calendar.filterDialog.newTemplateName"));
          if (newTemplateName == null) {
            // New filter 9 is already reached.
            activeTemplate = ((TeamCalCalendarFilter) filter).getActiveTemplateEntry();
            target.add(templateChoice.getDropDownChoice());
            return;
          }
          dialog.openNew(target, newTemplateName);
          // Redraw the content:
          dialog.redraw().addContent(target);
        }

      }
    });

    final IconButtonPanel calendarButtonPanel = new AjaxIconButtonPanel(buttonGroupPanel.newChildId(), IconType.EDIT,
        new ResourceModel("plugins.teamcal.calendar.filter.edit"))
    {
      /**
       * @see org.projectforge.web.wicket.flowlayout.AjaxIconButtonPanel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        dialog.open(target);
        // Redraw the content:
        dialog.redraw().addContent(target);
      }
    };
    calendarButtonPanel.setDefaultFormProcessing(false);
    buttonGroupPanel.addButton(calendarButtonPanel);

    fieldset.add(new DropIcsPanel(fieldset.newChildId())
    {
      @Override
      protected void onStringImport(final AjaxRequestTarget target, final String fileName, final String content)
      {
        ICalParser parser = new ICalParser();

        List<TeamEventDO> events = parser.parse(content);

        if (events.isEmpty()) {
          errorDialog.setMessage(getString("plugins.teamcal.import.ics.noEventsGiven")).open(target);
          return;
        }

        if (events.size() > 1) {
          // Can't import multiple entries, redirect to import page:
          redirectToImportPage(parser.getVEvents(), activeModel.getObject());
          return;
        }

        // Here we have just one event.
        final TeamEventDO event = parser.getTeamEvents().get(0);
        final TemplateEntry activeTemplateEntry = ((TeamCalCalendarFilter) filter).getActiveTemplateEntry();
        // check id/external id. If not yet given, create new entry and ask for calendar to add: Redirect to TeamEventEditPage.

        if (event.getUid() != null && activeTemplateEntry != null) {
          final TeamEventDO dbEvent = WicketSupport.get(TeamEventDao.class).getByUid(activeTemplateEntry.getDefaultCalendarId(), event.getUid(), false);

          if (dbEvent != null) {
            if (ThreadLocalUserContext.getLoggedInUserId().equals(dbEvent.getCreator().getId()) || dbEvent.getDeleted()) {
              event.setId(dbEvent.getId());
              event.setCreated(dbEvent.getCreated());
              event.setCreator(dbEvent.getCreator());
              event.setDeleted(dbEvent.getDeleted());
            } else {
              // Can't import event with existing uid in selected calendar, redirect to import page:
              redirectToImportPage(parser.getVEvents(), activeModel.getObject());
              return;
            }
          }
        }

        // set calendar
        if (activeTemplateEntry != null && activeTemplateEntry.getDefaultCalendarId() != null) {
          WicketSupport.get(TeamEventDao.class).setCalendar(event, activeTemplateEntry.getDefaultCalendarId());
        }

        // fix attendees
        WicketSupport.get(TeamEventService.class).fixAttendees(event);

        final Set<TeamEventAttendeeDO> originAssignedAttendees = new HashSet<>();
        event.getAttendees().forEach(attendee -> {
          attendee.setId(null);
          originAssignedAttendees.add(attendee);
        });
        event.setAttendees(new HashSet<>());
        final TeamEventEditPage editPage = new TeamEventEditPage(new PageParameters(), event);
        final TeamEventEditForm form = editPage.getForm();
        originAssignedAttendees.forEach(attendee -> {
          if (attendee.getAddress() != null) {
            form.getAttendeeWicketProvider().initSortedAttendees();
            form.getAttendeeWicketProvider().getSortedAttendees().forEach(sortedAttendee -> {
              if (sortedAttendee.getAddress() != null && sortedAttendee.getAddress().getId().equals(attendee.getAddress().getId())) {
                sortedAttendee.setId(form.getAttendeeWicketProvider().getAndDecreaseInternalNewAttendeeSequence());
                form.getAssignAttendeesListHelper().assignItem(sortedAttendee);
              }
            });
          } else {
            attendee.setId(form.getAttendeeWicketProvider().getAndDecreaseInternalNewAttendeeSequence());
            form.getAttendeeWicketProvider().getCustomAttendees().add(attendee);
            form.getAssignAttendeesListHelper().assignItem(attendee);
          }
        });
        setResponsePage(editPage);
      }
    }.setTooltip(getString("plugins.teamcal.dropIcsPanel.tooltip")));
  }

  private void redirectToImportPage(final List<VEvent> events, final TemplateEntry activeTemplate)
  {
    final PageParameters parameters = new PageParameters();
    if (activeTemplate != null && activeTemplate.getDefaultCalendarId() != null) {
      parameters.add(TeamCalImportPage.PARAM_KEY_TEAM_CAL_ID, activeTemplate.getDefaultCalendarId());
    }
    final TeamCalImportPage importPage = new TeamCalImportPage(parameters);
    importPage.setReturnToPage(parentPage);
    importPage.setEventsToImport(events);
    setResponsePage(importPage);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    errorDialog = new ModalMessageDialog(parentPage.newModalDialogId(), new ResourceModel("plugins.teamcal.import.ics.error"));
    errorDialog.setType(DivType.ALERT_ERROR);
    parentPage.add(errorDialog);
    errorDialog.init();
    super.onInitialize();
  }

  /**
   * @see org.projectforge.web.calendar.CalendarForm#getRefreshIconTooltip()
   */
  @Override
  protected String getRefreshIconTooltip()
  {
    return getString("plugins.teamcal.calendar.refresh.tooltip");
  }

  @Override
  public TeamCalCalendarFilter getFilter()
  {
    return (TeamCalCalendarFilter) filter;
  }

  @Override
  protected void setFilter(final ICalendarFilter filter)
  {
    this.filter = filter;
  }

  /**
   * @return the selectedCalendars
   */
  public Set<Long> getSelectedCalendars()
  {
    return ((TeamCalCalendarFilter) filter).getActiveVisibleCalendarIds();
  }

}
