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

package org.projectforge.web.teamcal.dialog;

import de.micromata.wicket.ajax.AjaxCallback;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.Constants;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.right.TeamEventRight;
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter;
import org.projectforge.business.teamcal.filter.TemplateEntry;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.calendar.CalendarPageSupport;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.AjaxMaxLengthEditableLabel;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.Heading3Panel;
import org.projectforge.web.wicket.flowlayout.SelectPanel;
import org.wicketstuff.select2.Select2MultiChoice;

import java.util.*;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
public class TeamCalFilterDialog extends ModalDialog
{
  private static final long serialVersionUID = 8687197318833240410L;

  private final List<TeamCalDO> selectedCalendars;

  private AjaxMaxLengthEditableLabel templateName;

  // Adaption (fake) to display "Time Sheets" as selection option
  private final TeamCalDO timesheetsCalendar = new TeamCalDO();

  private final TeamCalCalendarFilter filter;

  private TeamCalCalendarFilter backupFilter;

  private TeamCalFilterDialogCalendarColorPanel calendarColorPanel;

  private Select<Long> defaultCalendarSelect;

  private Select2MultiChoice<TeamCalDO> teamCalsChoice;

  private WebMarkupContainer optionsControls;

  private FieldsetPanel optionsFieldset;

  private WebMarkupContainer timesheetUserControls;

  private FieldsetPanel timesheetUserFieldset;

  private final transient TeamEventRight teamEventRight;

  private CalendarPageSupport calendarPageSupport;

  /**
   * @param id
   * @param filter
   */
  public TeamCalFilterDialog(final String id, final TeamCalCalendarFilter filter)
  {
    super(id);
    this.filter = filter;
    setTitle(new ResourceModel("plugins.teamcal.calendar.filterDialog.title"));
    setBigWindow().setShowCancelButton().wantsNotificationOnClose().setEscapeKeyEnabled(false);
    selectedCalendars = new LinkedList<TeamCalDO>();
    teamEventRight = (TeamEventRight) WicketSupport.get(UserRightService.class).getRight(UserRightId.PLUGIN_CALENDAR_EVENT);
  }

  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    response.render(CssHeaderItem.forUrl("scripts/spectrum/spectrum.css"));
    response.render(JavaScriptReferenceHeaderItem.forUrl("scripts/spectrum/spectrum.js"));
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#handleCloseEvent(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected void handleCloseEvent(final AjaxRequestTarget target)
  {
    myClose(target);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#onCancelButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected void onCancelButtonSubmit(final AjaxRequestTarget target)
  {
    // Restore values (valid at opening time of this dialog):
    filter.copyValuesFrom(backupFilter);
    close(target);
    onClose(target);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#onCloseButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected boolean onCloseButtonSubmit(final AjaxRequestTarget target)
  {
    myClose(target);
    onClose(target);
    return true;
  }

  protected void onClose(final AjaxRequestTarget target)
  {
  }

  @SuppressWarnings("serial")
  @Override
  public void init()
  {
    init(new Form<String>(getFormId()));
    calendarPageSupport = new CalendarPageSupport((ISelectCallerPage) getPage());
    timesheetsCalendar.setTitle(getString("plugins.teamcal.timeSheetCalendar"));
    timesheetsCalendar.setId(Constants.TIMESHEET_CALENDAR_ID);
    // confirm
    setCloseButtonTooltip(null, new ResourceModel("plugins.teamcal.calendar.filterDialog.closeButton.tooltip"));
    insertNewAjaxActionButton(new AjaxCallback()
    {
      @Override
      public void callback(final AjaxRequestTarget target)
      {
        filter.remove(filter.getActiveTemplateEntry());
        close(target);
        onClose(target);
      }
    }, 1, getString("delete"), SingleButtonPanel.DELETE);
  }

  public TeamCalFilterDialog redraw()
  {
    clearContent();
    gridBuilder.newSplitPanel(GridSize.COL50);
    filter.sort();
    addFilterFieldset();
    gridBuilder.newSplitPanel(GridSize.COL50);
    addDefaultCalenderSelection();
    if (calendarPageSupport.isOtherTimesheetsUsersAllowed() == true) {
      gridBuilder.newSplitPanel(GridSize.COL50);
      timesheetUserFieldset = gridBuilder.newFieldset(getString("timesheet.timesheets"));
      timesheetUserControls = timesheetUserFieldset.getControlsDiv();
      timesheetUserControls.setOutputMarkupId(true);
      redrawTimesheetsUserControls();
      gridBuilder.newSplitPanel(GridSize.COL50);
    } else {
      gridBuilder.newGridPanel();
    }
    optionsFieldset = gridBuilder.newFieldset(getString("label.options")).suppressLabelForWarning();
    optionsControls = optionsFieldset.getControlsDiv();
    optionsControls.setOutputMarkupId(true);
    redrawOptionControls();
    gridBuilder.newGridPanel();
    addTeamCalsChoiceFieldset();
    final DivPanel panel = gridBuilder.getPanel();
    panel.add(new Heading3Panel(panel.newChildId(), getString("plugins.teamcal.selectColor")));
    panel.add(calendarColorPanel = new TeamCalFilterDialogCalendarColorPanel(panel.newChildId()));
    calendarColorPanel.redraw(filter.getActiveTemplateEntry(), selectedCalendars);
    return this;
  }

  @SuppressWarnings("serial")
  private void redrawTimesheetsUserControls()
  {
    if (timesheetUserFieldset != null) {
      timesheetUserFieldset.removeAllFields();
    }
    final UserSelectPanel timesheetUserSelectPanel = calendarPageSupport.addUserSelectPanel(timesheetUserFieldset,
        new PropertyModel<PFUserDO>(this, "timesheetsUser"), false);
    if (timesheetUserSelectPanel != null) {
      timesheetUserSelectPanel.getFormComponent().add(new OnChangeAjaxBehavior()
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          final PFUserDO user = (PFUserDO) timesheetUserSelectPanel.getFormComponent().getModelObject();
          setTimesheetsUser(user);
        }
      });
    }
  }

  private void redrawOptionControls()
  {
    optionsFieldset.removeAllFields();
    final DivPanel checkBoxPanel = optionsFieldset.addNewCheckBoxButtonDiv();
    calendarPageSupport.addOptions(checkBoxPanel, false, filter);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#open(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  public TeamCalFilterDialog open(final AjaxRequestTarget target)
  {
    backupFilter = new TeamCalCalendarFilter().copyValuesFrom(filter);
    super.open(target);
    return this;
  }

  /**
   * Opens the dialog for adding a new entry.
   */
  public TeamCalFilterDialog openNew(final AjaxRequestTarget target, final String newTemplateName)
  {
    backupFilter = new TeamCalCalendarFilter().copyValuesFrom(filter);
    final TemplateEntry newTemplate = new TemplateEntry();
    newTemplate.setName(newTemplateName);
    filter.add(newTemplate);
    super.open(target);
    return this;
  }

  private void myClose(final AjaxRequestTarget target)
  {
    if (filter.isModified(backupFilter) == false) {
      // Do nothing.
      return;
    }
    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    if (activeTemplateEntry != null) {
      activeTemplateEntry.setDirty();
    }
    filter.setSelectedCalendar(TemplateEntry.calcCalendarStringForCalendar(activeTemplateEntry.getDefaultCalendarId()));
    setResponsePage(getPage().getClass(), getPage().getPageParameters());
  }

  @SuppressWarnings("serial")
  private void addFilterFieldset()
  {
    final FieldsetPanel fs = gridBuilder.newFieldset((String) null).setLabelSide(false).suppressLabelForWarning();
    templateName = new AjaxMaxLengthEditableLabel(fs.getAjaxEditableLabelId(), new Model<String>()
    {
      @Override
      public String getObject()
      {
        return filter.getActiveTemplateEntry().getName();
      }

      @Override
      public void setObject(final String value)
      {
        filter.getActiveTemplateEntry().setName(value);
      }
    }, 40)
    {
      /**
       * @see org.apache.wicket.Component#onInitialize()
       */
      @Override
      protected void onInitialize()
      {
        super.onInitialize();
        getEditor().add(AttributeModifier.append("style", "width: 10em;"));
        WicketUtils.setStrong(getLabel());
        WicketUtils.setFontSizeLarge(getLabel());
        WicketUtils.addEditableLabelDefaultTooltip(getLabel());
      }
    };
    templateName.setType(String.class).setOutputMarkupId(true);
    fs.add(templateName);

    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    selectedCalendars.clear();
    if (activeTemplateEntry != null) {
      selectedCalendars.addAll(activeTemplateEntry.getCalendars());
    }
  }

  private void addTeamCalsChoiceFieldset()
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.selectCalendar")).setLabelSide(false);
    // TEAMCAL CHOICE FIELD
    final TeamCalChoiceProvider teamProvider = new TeamCalChoiceProvider();
    teamCalsChoice = new Select2MultiChoice<TeamCalDO>(fs.getSelect2MultiChoiceId(),
        new PropertyModel<Collection<TeamCalDO>>(
            TeamCalFilterDialog.this, "selectedCalendars"),
        teamProvider);
    teamCalsChoice.setOutputMarkupId(true);
    teamCalsChoice.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
        final Set<Long> oldCalIds = activeTemplateEntry.getCalendarIds();
        final List<Long> newIds = new LinkedList<Long>();
        // add new keys
        for (final TeamCalDO calendar : selectedCalendars) {
          if (oldCalIds.contains(calendar.getId()) == false) {
            activeTemplateEntry.addNewCalendarProperties(filter, calendar.getId());
          }
          newIds.add(calendar.getId());
        }
        // delete removed keys
        for (final Long key : oldCalIds) {
          if (newIds.contains(key) == false) {
            activeTemplateEntry.removeCalendarProperties(key);
          }
        }
        updateComponents(target);
      }
    });
    fs.add(teamCalsChoice);
  }

  @SuppressWarnings("serial")
  private void addDefaultCalenderSelection()
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.defaultCalendar"));
    fs.addHelpIcon(getString("plugins.teamcal.defaultCalendar.tooltip"));
    final IOptionRenderer<Long> renderer = new IOptionRenderer<Long>()
    {

      @Override
      public String getDisplayValue(final Long object)
      {
        if (Constants.isTimesheetCalendarId(object)) {
          return timesheetsCalendar.getTitle();
        }
        return WicketSupport.get(TeamCalDao.class).find(object).getTitle();
      }

      @Override
      public IModel<Long> getModel(final Long value)
      {
        return Model.of(value);
      }
    };

    // TEAMCAL SELECT
    defaultCalendarSelect = new Select<Long>(fs.getSelectId(), new PropertyModel<Long>(filter,
        "activeTemplateEntry.defaultCalendarId"))
    {

      /**
       * @see org.apache.wicket.Component#onBeforeRender()
       */
      @Override
      protected void onBeforeRender()
      {
        super.onBeforeRender();
        final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
        final List<TeamCalDO> result = new ArrayList<TeamCalDO>();
        if (activeTemplateEntry != null) {
          for (final TeamCalDO cal : activeTemplateEntry.getCalendars()) {
            if (teamEventRight.hasUpdateAccess(ThreadLocalUserContext.getLoggedInUser(), cal) == true) {
              // User is allowed to insert events to this calendar:
              result.add(cal);
            }
          }
        }
        final List<Long> filteredList = new ArrayList<Long>();
        filteredList.add(Constants.TIMESHEET_CALENDAR_ID);
        if (result != null) {
          final Iterator<TeamCalDO> it = result.iterator();
          while (it.hasNext()) {
            final TeamCalDO teamCal = it.next();
            filteredList.add(teamCal.getId());
          }
        }
        final SelectOptions<Long> options = new SelectOptions<Long>(SelectPanel.OPTIONS_WICKET_ID, filteredList,
            renderer);
        this.addOrReplace(options);
      }
    };
    defaultCalendarSelect.setOutputMarkupId(true);
    defaultCalendarSelect.add(new OnChangeAjaxBehavior()
    {

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        final Long value = defaultCalendarSelect.getModelObject();
        filter.getActiveTemplateEntry().setDefaultCalendarId(value);
      }
    });
    fs.add(defaultCalendarSelect);
  }

  private void updateComponents(final AjaxRequestTarget target)
  {
    selectedCalendars.clear();
    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    selectedCalendars.addAll(activeTemplateEntry.getCalendars());
    redrawTimesheetsUserControls();
    redrawOptionControls();
    calendarColorPanel.redraw(activeTemplateEntry, selectedCalendars);
    teamCalsChoice.modelChanged();
    target.add(teamCalsChoice, calendarColorPanel.main, templateName, defaultCalendarSelect, optionsControls);
    if (timesheetUserControls != null) {
      target.add(timesheetUserControls);
    }
  }

  public PFUserDO getTimesheetsUser()
  {
    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    if (activeTemplateEntry == null) {
      return null;
    }
    final Long userId = activeTemplateEntry.getTimesheetUserId();
    return userId != null ? UserGroupCache.getInstance().getUser(userId) : null;
  }

  public void setTimesheetsUser(final PFUserDO user)
  {
    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    if (activeTemplateEntry == null) {
      return;
    }
    if (user == null) {
      activeTemplateEntry.setTimesheetUserId(null);
    } else {
      activeTemplateEntry.setTimesheetUserId(user.getId());
    }
  }
}
