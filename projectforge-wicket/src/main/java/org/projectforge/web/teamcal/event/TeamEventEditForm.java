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

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.AttendeeComparator;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeOne;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeTwo;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventRecurrenceData;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.event.right.TeamEventRight;
import org.projectforge.business.teamcal.service.TeamCalServiceImpl;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.AttendeeWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.DateTimePanel;
import org.projectforge.web.wicket.components.DateTimePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.LabelPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.projectforge.web.wicket.flowlayout.ToggleContainerPanel;
import org.wicketstuff.select2.Select2MultiChoice;

/**
 * Form to edit team events.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (K.Reinhard@micromata.de)
 */
public class TeamEventEditForm extends AbstractEditForm<TeamEventDO, TeamEventEditPage>
{
  private static final long serialVersionUID = -8378262684943803495L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventEditForm.class);

  @SpringBean
  private transient TeamCalDao teamCalDao;

  @SpringBean
  private transient TeamEventDao teamEventDao;

  @SpringBean
  private transient AccessChecker accessChecker;

  @SpringBean
  private transient TeamEventService teamEventService;

  private DateTimePanel startDateTimePanel;

  private DateTimePanel endDateTimePanel;

  private boolean access;

  private FieldsetPanel endDateField;

  private FieldsetPanel startDateField;

  protected TeamEventRecurrenceData recurrenceData;

  private DivPanel customizedCheckBoxButton;

  private WebMarkupContainer recurrencePanel;

  private FieldsetPanel recurrenceFieldset, recurrenceUntilDateFieldset, recurrenceIntervalFieldset, recurrenceExDateFieldset;
  private FieldsetPanel recurrenceWeekIntervalFieldset, recurrenceMonthIntervalFieldset, recurrenceYearIntervalFieldset;

  private DropDownChoice<RecurrenceFrequencyModeOne> modeOneDropDownChoiceMonth, modeOneDropDownChoiceYear;
  private DropDownChoice<RecurrenceFrequencyModeTwo> modeTwoDropDownChoiceMonth, modeTwoDropDownChoiceYear;

  private final transient TeamEventRight right;

  private final FormComponent<?>[] dependentFormComponents = new FormComponent[6];

  // Needed by autocompletion for location fields.
  private TeamCalDO[] calendarsWithFullAccess;

  protected FileUploadField fileUploadField;

  protected MultiChoiceListHelper<TeamEventAttendeeDO> assignAttendeesListHelper;

  protected AttendeeWicketProvider attendeeWicketProvider;

  /**
   * @param parentPage
   * @param data
   */
  public TeamEventEditForm(final TeamEventEditPage parentPage, final TeamEventDO data)
  {
    super(parentPage, data);
    right = new TeamEventRight(accessChecker);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#init()
   */
  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();

    recurrenceData = data.getRecurrenceData(ThreadLocalUserContext.getTimeZone());

    gridBuilder.newSplitPanel(GridSize.COL50);
    final TeamCalDO teamCal = data.getCalendar();
    // setting access view
    if (isNew() == true || teamCal == null || teamCal.getOwner() == null) {
      access = true;
    } else {
      if (right.hasUpdateAccess(getUser(), data, data) == true) {
        access = true;
      } else {
        access = false;
        if (right.hasMinimalAccess(data, getUserId()) == true) {
          final TeamEventDO newTeamEventDO = new TeamEventDO();
          newTeamEventDO.setId(data.getId());
          newTeamEventDO.setStartDate(data.getStartDate());
          newTeamEventDO.setEndDate(data.getEndDate());
          data = newTeamEventDO;
          access = false;
        }
      }
    }

    // add teamCal drop down
    initTeamCalPicker(gridBuilder.newFieldset(getString("plugins.teamcal.event.teamCal")));
    {
      // SUBJECT
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("plugins.teamcal.event.subject"));
      final MaxLengthTextField subjectField = new MaxLengthTextField(fieldSet.getTextFieldId(),
          new PropertyModel<>(data, "subject"));
      subjectField.setRequired(true);
      fieldSet.add(subjectField);
      if (access == false) {
        fieldSet.setEnabled(false);
      } else {
        WicketUtils.setFocus(subjectField);
      }
    }
    {
      // LOCATION
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("plugins.teamcal.event.location"));
      final PFAutoCompleteMaxLengthTextField locationTextField = new PFAutoCompleteMaxLengthTextField(
          fieldSet.getTextFieldId(),
          new PropertyModel<>(data, "location"))
      {
        @Override
        protected List<String> getChoices(final String input)
        {
          return teamEventDao.getLocationAutocompletion(input, calendarsWithFullAccess);
        }
      };
      locationTextField.withMatchContains(true).withMinChars(3);
      fieldSet.add(locationTextField);
      if (access == false)
        fieldSet.setEnabled(false);
    }
    {
      // ATTENDEE
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("plugins.teamcal.attendees"));
      List<TeamEventAttendeeDO> fullAttendeeList = teamEventService.getAddressesAndUserAsAttendee();
      assignAttendeesListHelper = new MultiChoiceListHelper<TeamEventAttendeeDO>()
          .setComparator(new AttendeeComparator()).setFullList(fullAttendeeList);
      if (data.getAttendees() != null) {
        for (final TeamEventAttendeeDO attendee : data.getAttendees()) {
          assignAttendeesListHelper.addOriginalAssignedItem(attendee).assignItem(attendee);
        }
      }
      attendeeWicketProvider = new AttendeeWicketProvider(data, teamEventService);

      final Select2MultiChoice<TeamEventAttendeeDO> attendees = new Select2MultiChoice<>(fieldSet.getSelect2MultiChoiceId(),
          new PropertyModel<>(this.assignAttendeesListHelper, "assignedItems"), attendeeWicketProvider);
      attendees.setMarkupId("attendees").setOutputMarkupId(true);
      attendees.add(new TeamEventAttendeeValidator());
      attendees.getSettings().setCloseOnSelect(true);
      fieldSet.add(attendees);
      if (access == false) {
        fieldSet.setEnabled(false);
      }
    }
    {
      // NOTE
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("plugins.teamcal.event.note"));
      final MaxLengthTextArea noteField = new MaxLengthTextArea(fieldSet.getTextAreaId(), new PropertyModel<>(data, "note"));
      fieldSet.add(noteField).setAutogrow();
      if (access == false)
        fieldSet.setEnabled(false);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    // add date panel
    initDatePanel();
    {
      // ALL DAY CHECKBOX
      final FieldsetPanel fieldSet = gridBuilder.newFieldset("").suppressLabelForWarning();
      final DivPanel divPanel = fieldSet.addNewCheckBoxButtonDiv();
      final CheckBoxButton checkBox = new CheckBoxButton(divPanel.newChildId(), new PropertyModel<Boolean>(data, "allDay"),
          getString("plugins.teamcal.event.allDay"));
      checkBox.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          if (data.isAllDay() == false) {
            setDateDropChoiceVisible(true);
          } else {
            setDateDropChoiceVisible(false);
          }
          target.add(startDateTimePanel.getTimeContainer(), endDateTimePanel.getTimeContainer());
        }
      });
      setDateDropChoiceVisible(data.isAllDay() == false);
      divPanel.add(checkBox);
      fieldSet.add(divPanel);
      if (access == false)
        fieldSet.setEnabled(false);

      // ///////////////////////////////
      // Reminder
      // ///////////////////////////////
      final FieldsetPanel reminderPanel = gridBuilder.newFieldset(getString("plugins.teamcal.event.reminder.title"));
      reminderPanel.add(new TeamEventReminderComponent(reminderPanel.newChildId(), Model.of(data), reminderPanel));
      reminderPanel.addHelpIcon(getString("plugins.teamcal.event.reminder.tooltip"));
    }

    // ///////////////////////////////
    // Recurrence
    // ///////////////////////////////
    gridBuilder.newSplitPanel(GridSize.COL50);
    gridBuilder.newFormHeading(getString("plugins.teamcal.event.recurrence"));
    {
      // Recurrence interval type:
      recurrenceFieldset = gridBuilder.newFieldset(getString("plugins.teamcal.event.recurrence"));
      recurrencePanel = gridBuilder.getPanel().getDiv();
      recurrencePanel.setOutputMarkupId(true);
      final RecurrenceFrequency[] supportedFrequencies = TeamCalServiceImpl.getSupportedRecurrenceFrequencies();
      final LabelValueChoiceRenderer<RecurrenceFrequency> frequencyChoiceRenderer = new LabelValueChoiceRenderer<>(
          recurrenceFieldset, supportedFrequencies);
      final DropDownChoice<RecurrenceFrequency> frequencyChoice = new DropDownChoice<>(
          recurrenceFieldset.getDropDownChoiceId(), new PropertyModel<>(recurrenceData, "frequency"),
          frequencyChoiceRenderer.getValues(), frequencyChoiceRenderer);
      frequencyChoice.setNullValid(false);
      recurrenceFieldset.add(frequencyChoice);
      recurrenceFieldset.getFieldset().setOutputMarkupId(true);
      frequencyChoice.add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          setRecurrenceComponentsVisibility(target);
        }
      });
      customizedCheckBoxButton = recurrenceFieldset.addNewCheckBoxButtonDiv();
      final CheckBoxButton checkBox = new CheckBoxButton(customizedCheckBoxButton.newChildId(),
          new PropertyModel<>(recurrenceData, "customized"), getString("plugins.teamcal.event.recurrence.customized"));
      checkBox.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          setRecurrenceComponentsVisibility(target);
        }
      });
      customizedCheckBoxButton.add(checkBox);
    }
    {
      //IntervallSet
      recurrenceIntervalFieldset = gridBuilder.newFieldset("");
      DivTextPanel panel = new DivTextPanel(recurrenceIntervalFieldset.newChildId(), HtmlHelper.escapeHtml(
          getString("plugins.teamcal.event.recurrence.customized.all"), false) + "&nbsp;");
      panel.getLabel().setEscapeModelStrings(false);
      recurrenceIntervalFieldset.add(panel);
      final MinMaxNumberField<Integer> intervalNumberField = new MinMaxNumberField<>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(recurrenceData, "interval"), 0, 1000);
      WicketUtils.setSize(intervalNumberField, 2);
      recurrenceIntervalFieldset.add(intervalNumberField);
      panel = new DivTextPanel(recurrenceIntervalFieldset.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          final RecurrenceFrequency interval = recurrenceData.getFrequency();
          final String unitI18nKey = getString(interval.getUnitI18nKey());
          if (unitI18nKey != null) {
            return "&nbsp;" + HtmlHelper.escapeHtml(unitI18nKey, false);
          }
          return "";
        }
      });
      panel.getLabel().setEscapeModelStrings(false);
      recurrenceIntervalFieldset.add(panel);
      recurrenceIntervalFieldset.getFieldset().setOutputMarkupId(true);
    }
    //ToDO i18nKey
    {
      //WeekSet
      recurrenceWeekIntervalFieldset = gridBuilder.newFieldset("");
      DivTextPanel panel = new DivTextPanel(recurrenceWeekIntervalFieldset.newChildId(), HtmlHelper.escapeHtml("Am :", false));
      panel.getLabel().setEscapeModelStrings(false);
      recurrenceWeekIntervalFieldset.add(panel);
      recurrenceWeekIntervalFieldset.getFieldset().setOutputMarkupId(true);
      addNewDayButtons();
    }
    {
      //MonthSet
      recurrenceMonthIntervalFieldset = gridBuilder.newFieldset("");
      final DivPanel newRadioBoxButtonDiv1 = recurrenceMonthIntervalFieldset.addNewRadioBoxButtonDiv();

      final CheckBoxButton checkBox1 = new CheckBoxButton(newRadioBoxButtonDiv1.newChildId(),
          new PropertyModel<>(recurrenceData, "monthDays"), "Jeden: ");
      checkBox1.setOutputMarkupId(true);

      checkBox1.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          if (checkBox1.getCheckBox().getConvertedInput()) {
            recurrenceData.setMonthMode(false);
          }
        }
      });
      newRadioBoxButtonDiv1.add(checkBox1);
      recurrenceMonthIntervalFieldset.getFieldset().setOutputMarkupId(true);

      addDayMonthButtons();

      DivPanel newRadioBoxButtonDiv2 = recurrenceMonthIntervalFieldset.addNewRadioBoxButtonDiv();
      final CheckBoxButton checkBox2 = new CheckBoxButton(newRadioBoxButtonDiv2.newChildId(),
          new PropertyModel<>(recurrenceData, "monthMode"), "Am: ");
      checkBox2.setOutputMarkupId(true);
      checkBox2.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          if (checkBox2.getCheckBox().getConvertedInput()) {
            modeOneDropDownChoiceMonth.setEnabled(true);
            modeTwoDropDownChoiceMonth.setEnabled(true);
            recurrenceData.setMonthDays(false);
          } else {
            modeOneDropDownChoiceMonth.setEnabled(false);
            modeTwoDropDownChoiceMonth.setEnabled(false);
          }
          target.add(modeOneDropDownChoiceMonth, modeTwoDropDownChoiceMonth);
        }
      });
      newRadioBoxButtonDiv2.add(checkBox2);

      //Selecet für (erster, zweiter, dritter, vierter, letzter)
      final RecurrenceFrequencyModeOne[] supportedFrequenciesModeOne = TeamCalServiceImpl.getSupportedRecurrenceFrequenciesModeOne();
      final LabelValueChoiceRenderer<RecurrenceFrequencyModeOne> frequencyModeOneChoiceRenderer = new LabelValueChoiceRenderer<>(
          recurrenceMonthIntervalFieldset, supportedFrequenciesModeOne);
      modeOneDropDownChoiceMonth = new DropDownChoice<>(
          recurrenceMonthIntervalFieldset.getDropDownChoiceId(), new PropertyModel<>(recurrenceData, "modeOneMonth"),
          frequencyModeOneChoiceRenderer.getValues(), frequencyModeOneChoiceRenderer);
      modeOneDropDownChoiceMonth.setNullValid(false).setOutputMarkupId(true);
      recurrenceMonthIntervalFieldset.add(modeOneDropDownChoiceMonth);
      recurrenceMonthIntervalFieldset.getFieldset().setOutputMarkupId(true);

      //Select für (Wochentage, Tag, Wochentag, Wochenende)
      final RecurrenceFrequencyModeTwo[] supportedFrequenciesModeTwo = TeamCalServiceImpl.getSupportedRecurrenceFrequenciesModeTwo();
      final LabelValueChoiceRenderer<RecurrenceFrequencyModeTwo> frequencyModeTwoChoiceRenderer = new LabelValueChoiceRenderer<>(
          recurrenceMonthIntervalFieldset, supportedFrequenciesModeTwo);
      modeTwoDropDownChoiceMonth = new DropDownChoice<>(
          recurrenceMonthIntervalFieldset.getDropDownChoiceId(), new PropertyModel<>(recurrenceData, "modeTwoMonth"),
          frequencyModeTwoChoiceRenderer.getValues(), frequencyModeTwoChoiceRenderer);
      modeTwoDropDownChoiceMonth.setNullValid(false).setOutputMarkupId(true);
      recurrenceMonthIntervalFieldset.add(modeTwoDropDownChoiceMonth);
      recurrenceMonthIntervalFieldset.getFieldset().setOutputMarkupId(true);

      DivTextPanel panel = new DivTextPanel(recurrenceMonthIntervalFieldset.newChildId(), HtmlHelper.escapeHtml("des Monats", false));
      panel.getLabel().setEscapeModelStrings(false);
      recurrenceMonthIntervalFieldset.add(panel);
    }
    {
      //YearSet
      recurrenceYearIntervalFieldset = gridBuilder.newFieldset("");
      DivTextPanel panel = new DivTextPanel(recurrenceYearIntervalFieldset.newChildId(), HtmlHelper.escapeHtml("Im :", false));
      panel.getLabel().setEscapeModelStrings(false);
      recurrenceYearIntervalFieldset.add(panel);
      recurrenceYearIntervalFieldset.getFieldset().setOutputMarkupId(true);

      addMonthYearButtons();

      DivPanel components = recurrenceYearIntervalFieldset.addNewCheckBoxButtonDiv();
      final CheckBoxButton checkBox = new CheckBoxButton(components.newChildId(),
          new PropertyModel<>(recurrenceData, "yearMode"), "Am");
      checkBox.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          if (checkBox.getCheckBox().getConvertedInput()) {
            modeOneDropDownChoiceYear.setEnabled(true);
            modeTwoDropDownChoiceYear.setEnabled(true);
          } else {
            modeOneDropDownChoiceYear.setEnabled(false);
            modeTwoDropDownChoiceYear.setEnabled(false);
          }
          target.add(modeOneDropDownChoiceYear, modeTwoDropDownChoiceYear);
        }
      });
      components.add(checkBox);
      //Selecet für (erster, zweiter, dritter, vierter, letzter)
      final RecurrenceFrequencyModeOne[] supportedFrequenciesModeOne = TeamCalServiceImpl.getSupportedRecurrenceFrequenciesModeOne();
      final LabelValueChoiceRenderer<RecurrenceFrequencyModeOne> frequencyModeOneChoiceRenderer = new LabelValueChoiceRenderer<>(
          recurrenceYearIntervalFieldset, supportedFrequenciesModeOne);
      modeOneDropDownChoiceYear = new DropDownChoice<>(
          recurrenceYearIntervalFieldset.getDropDownChoiceId(), new PropertyModel<>(recurrenceData, "modeOneYear"),
          frequencyModeOneChoiceRenderer.getValues(), frequencyModeOneChoiceRenderer);
      modeOneDropDownChoiceYear.setNullValid(false).setOutputMarkupId(true);
      recurrenceYearIntervalFieldset.add(modeOneDropDownChoiceYear);
      recurrenceYearIntervalFieldset.getFieldset().setOutputMarkupId(true);
      modeOneDropDownChoiceYear.setEnabled(false);

      //Select für (Wochentage, Tag, Wochentag, Wochenende)
      final RecurrenceFrequencyModeTwo[] supportedFrequenciesModeTwo = TeamCalServiceImpl.getSupportedRecurrenceFrequenciesModeTwo();
      final LabelValueChoiceRenderer<RecurrenceFrequencyModeTwo> frequencyModeTwoChoiceRenderer = new LabelValueChoiceRenderer<>(
          recurrenceYearIntervalFieldset, supportedFrequenciesModeTwo);
      modeTwoDropDownChoiceYear = new DropDownChoice<>(
          recurrenceYearIntervalFieldset.getDropDownChoiceId(), new PropertyModel<>(recurrenceData, "modeTwoYear"),
          frequencyModeTwoChoiceRenderer.getValues(), frequencyModeTwoChoiceRenderer);
      modeTwoDropDownChoiceYear.setNullValid(false).setOutputMarkupId(true);
      recurrenceYearIntervalFieldset.add(modeTwoDropDownChoiceYear);
      modeTwoDropDownChoiceYear.setEnabled(false);

      recurrenceYearIntervalFieldset.getFieldset().setOutputMarkupId(true);
      panel = new DivTextPanel(recurrenceYearIntervalFieldset.newChildId(), HtmlHelper.escapeHtml("des Jahres", false));
      panel.getLabel().setEscapeModelStrings(false);
      recurrenceYearIntervalFieldset.add(panel);
    }
    {
      // Until. Only visible if recurrenceData.interval != NONE.
      recurrenceUntilDateFieldset = gridBuilder.newFieldset(getString("plugins.teamcal.event.recurrence.until"));
      final DatePanel untilDatePanel = new DatePanel(recurrenceUntilDateFieldset.newChildId(), new PropertyModel<Date>(recurrenceData,
          "until"), DatePanelSettings.get().withTimeZone(DateHelper.UTC));

      recurrenceUntilDateFieldset.add(untilDatePanel);
      recurrenceUntilDateFieldset.getFieldset().setOutputMarkupId(true);
    }

    gridBuilder.newGridPanel();
    {
      final ToggleContainerPanel extendedSettingsPanel = new ToggleContainerPanel(gridBuilder.getPanel().newChildId());
      extendedSettingsPanel.setHeading(getString("plugins.teamcal.event.expertSettings"));
      gridBuilder.getPanel().add(extendedSettingsPanel);
      extendedSettingsPanel.setClosed();
      final GridBuilder innerGridBuilder = extendedSettingsPanel.createGridBuilder();
      {
        // Until. Only visible if recurrenceData.interval != NONE.
        recurrenceExDateFieldset = innerGridBuilder.newFieldset(getString("plugins.teamcal.event.recurrence.exDate"));
        recurrenceExDateFieldset
            .add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data,
                "recurrenceExDate"), 4000));
        recurrenceExDateFieldset.getFieldset().setOutputMarkupId(true);
        recurrenceExDateFieldset.addHelpIcon(getString("plugins.teamcal.event.recurrence.exDate.tooltip"));
      }
      {
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("plugins.teamcal.event.uid"));
        fs.add(new LabelPanel(fs.getTextFieldId(), new PropertyModel<>(data, "uid")));
      }
      {
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("plugins.teamcal.event.ownership"));
        fs.addCheckBox(new PropertyModel<>(data, "ownership"), null);
        fs.addHelpIcon(getString("plugins.teamcal.event.ownership.tooltip"));
      }
    }

    gridBuilder.newGridPanel();
    if (parentPage.getRecurrencyChangeType() != null) {
      final FieldsetPanel fs = gridBuilder.newFieldset((String) null).setLabelSide(false).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), getString("plugins.teamcal.event.recurrence.change.text")
          + " "
          + getString(parentPage.getRecurrencyChangeType().getI18nKey())
          + "."));
    }

    setRecurrenceComponentsVisibility(null);

    addCloneButton();

    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(final Form<?> form)
      {
        final DateHolder startDate = new DateHolder(startDateTimePanel.getConvertedInput());
        final DateHolder endDate = new DateHolder(endDateTimePanel.getConvertedInput());
        data.setStartDate(startDate.getTimestamp());
        data.setEndDate(endDate.getTimestamp());
        if (data.getDuration() < 60000) {
          // Duration is less than 60 seconds.
          error(getString("plugins.teamcal.event.duration.error"));
        }
      }
    });
  }

  private void addDayMonthButtons()
  {
    for (int i = 0; i < 31; i++) {
      DivPanel newCheckBoxButtonDiv = recurrenceMonthIntervalFieldset.addNewCheckBoxButtonDiv();
      final CheckBoxButton checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(),
          new PropertyModel<>(recurrenceData, "monthdays[" + i + "]"), "Tag" + i);
      newCheckBoxButtonDiv.add(checkBox);
    }
  }

  private void addMonthYearButtons()
  {
    for (int i = 0; i < 12; i++) {
      DivPanel newCheckBoxButtonDiv = recurrenceYearIntervalFieldset.addNewCheckBoxButtonDiv();
      final CheckBoxButton checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(),
          new PropertyModel<>(recurrenceData, "months[" + i + "]"), "Monat" + i);
      newCheckBoxButtonDiv.add(checkBox);
    }
  }

  private void addNewDayButtons()
  {
    DivPanel newCheckBoxButtonDiv = recurrenceWeekIntervalFieldset.addNewCheckBoxButtonDiv();
    CheckBoxButton checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(), new PropertyModel<>(recurrenceData, "weekdays[0]"), "Montag");
    newCheckBoxButtonDiv.add(checkBox);
    newCheckBoxButtonDiv = recurrenceWeekIntervalFieldset.addNewCheckBoxButtonDiv();
    checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(), new PropertyModel<>(recurrenceData, "weekdays[1]"), "Dienstag");
    newCheckBoxButtonDiv.add(checkBox);
    newCheckBoxButtonDiv = recurrenceWeekIntervalFieldset.addNewCheckBoxButtonDiv();
    checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(), new PropertyModel<>(recurrenceData, "weekdays[2]"), "Mitwoch");
    newCheckBoxButtonDiv.add(checkBox);
    newCheckBoxButtonDiv = recurrenceWeekIntervalFieldset.addNewCheckBoxButtonDiv();
    checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(), new PropertyModel<>(recurrenceData, "weekdays[3]"), "Donnerstag");
    newCheckBoxButtonDiv.add(checkBox);
    newCheckBoxButtonDiv = recurrenceWeekIntervalFieldset.addNewCheckBoxButtonDiv();
    checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(), new PropertyModel<>(recurrenceData, "weekdays[4]"), "Freitag");
    newCheckBoxButtonDiv.add(checkBox);
    newCheckBoxButtonDiv = recurrenceWeekIntervalFieldset.addNewCheckBoxButtonDiv();
    checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(), new PropertyModel<>(recurrenceData, "weekdays[5]"), "Samstag");
    newCheckBoxButtonDiv.add(checkBox);
    newCheckBoxButtonDiv = recurrenceWeekIntervalFieldset.addNewCheckBoxButtonDiv();
    checkBox = new CheckBoxButton(newCheckBoxButtonDiv.newChildId(), new PropertyModel<>(recurrenceData, "weekdays[6]"), "Sonntag");
    newCheckBoxButtonDiv.add(checkBox);
  }

  private void setRecurrenceComponentsVisibility(final AjaxRequestTarget target)
  {
    if (recurrenceData.getFrequency() == RecurrenceFrequency.NONE) {
      customizedCheckBoxButton.setVisible(false);
      recurrenceUntilDateFieldset.setVisible(false);
      recurrenceExDateFieldset.setVisible(false);
      recurrenceIntervalFieldset.setVisible(false);
      recurrenceWeekIntervalFieldset.setVisible(false);
      recurrenceMonthIntervalFieldset.setVisible(false);
      recurrenceYearIntervalFieldset.setVisible(false);
    } else {
      customizedCheckBoxButton.setVisible(true);
      recurrenceUntilDateFieldset.setVisible(true);
      recurrenceExDateFieldset.setVisible(true);
      recurrenceIntervalFieldset.setVisible(recurrenceData.isCustomized());

      recurrenceWeekIntervalFieldset.setVisible(false);
      recurrenceMonthIntervalFieldset.setVisible(false);
      recurrenceYearIntervalFieldset.setVisible(false);
      if (recurrenceData.getFrequency() == RecurrenceFrequency.WEEKLY) {
        recurrenceWeekIntervalFieldset.setVisible(recurrenceData.isCustomized());
      } else if (recurrenceData.getFrequency() == RecurrenceFrequency.MONTHLY) {
        recurrenceMonthIntervalFieldset.setVisible(recurrenceData.isCustomized());
      } else if (recurrenceData.getFrequency() == RecurrenceFrequency.YEARLY) {
        recurrenceYearIntervalFieldset.setVisible(recurrenceData.isCustomized());
      }
    }
    if (target != null) {
      target.add(recurrencePanel);
    }
  }

  private void setDateDropChoiceVisible(final boolean visible)
  {
    startDateTimePanel.getHourOfDayDropDownChoice().setVisible(visible);
    startDateTimePanel.getMinuteDropDownChoice().setVisible(visible);
    endDateTimePanel.getHourOfDayDropDownChoice().setVisible(visible);
    endDateTimePanel.getMinuteDropDownChoice().setVisible(visible);
  }

  /**
   * if has access: create drop down with teamCals else create label
   *
   * @param fieldSet
   */
  private void initTeamCalPicker(final FieldsetPanel fieldSet)
  {
    if (access == false) {
      final TeamCalDO calendar = data.getCalendar();
      final Label teamCalTitle = new Label(fieldSet.newChildId(),
          calendar != null ? new PropertyModel<String>(data.getCalendar(), "title")
              : "");
      fieldSet.add(teamCalTitle);
    } else {
      final List<TeamCalDO> list = teamCalDao.getAllCalendarsWithFullAccess();
      calendarsWithFullAccess = list.toArray(new TeamCalDO[0]);
      final LabelValueChoiceRenderer<TeamCalDO> calChoiceRenderer = new LabelValueChoiceRenderer<TeamCalDO>();
      for (final TeamCalDO cal : list) {
        calChoiceRenderer.addValue(cal, cal.getTitle());
      }
      final DropDownChoice<TeamCalDO> calDropDownChoice = new DropDownChoice<>(fieldSet.getDropDownChoiceId(),
          new PropertyModel<TeamCalDO>(data, "calendar"), calChoiceRenderer.getValues(), calChoiceRenderer);
      calDropDownChoice.setNullValid(false);
      calDropDownChoice.setRequired(true);
      fieldSet.add(calDropDownChoice);
    }
  }

  /**
   * create date panel
   */
  private void initDatePanel()
  {
    startDateField = gridBuilder.newFieldset(getString("plugins.teamcal.event.beginDate"));
    startDateField.getFieldset().setOutputMarkupPlaceholderTag(true);
    startDateField.getFieldset().setOutputMarkupId(true);

    startDateField.getFieldset().setOutputMarkupId(true);
    startDateTimePanel = new DateTimePanel(startDateField.newChildId(), new PropertyModel<Date>(data, "startDate"),
        (DateTimePanelSettings) DateTimePanelSettings.get().withSelectStartStopTime(true)
            .withTargetType(java.sql.Timestamp.class)
            .withRequired(true),
        DatePrecision.MINUTE_5);
    startDateTimePanel.getDateField().setOutputMarkupId(true);
    startDateTimePanel.getTimeContainer().setOutputMarkupId(true);

    startDateField.add(startDateTimePanel);
    dateFieldToolTip(startDateTimePanel);
    dependentFormComponents[0] = startDateTimePanel;
    dependentFormComponents[1] = startDateTimePanel.getHourOfDayDropDownChoice();
    dependentFormComponents[2] = startDateTimePanel.getMinuteDropDownChoice();

    endDateField = gridBuilder.newFieldset(getString("plugins.teamcal.event.endDate"));
    endDateField.getFieldset().setOutputMarkupPlaceholderTag(true);
    endDateField.getFieldset().setOutputMarkupId(true);

    endDateField.getFieldset().setOutputMarkupId(true);
    endDateTimePanel = new DateTimePanel(endDateField.newChildId(), new PropertyModel<Date>(data, "endDate"),
        (DateTimePanelSettings) DateTimePanelSettings.get().withSelectStartStopTime(true)
            .withTargetType(java.sql.Timestamp.class)
            .withRequired(true),
        DatePrecision.MINUTE_5);
    endDateTimePanel.getDateField().setOutputMarkupId(true);
    endDateTimePanel.getTimeContainer().setOutputMarkupId(true);
    endDateTimePanel.getDateField().add(new AjaxFormComponentUpdatingBehavior("change")
    {

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        // do nothing, just update
      }
    });

    endDateField.add(endDateTimePanel);
    dateFieldToolTip(endDateTimePanel);
    dependentFormComponents[3] = endDateTimePanel;
    dependentFormComponents[4] = endDateTimePanel.getHourOfDayDropDownChoice();
    dependentFormComponents[5] = endDateTimePanel.getMinuteDropDownChoice();

    startDateTimePanel.getDateField().add(new AjaxFormComponentUpdatingBehavior("change")
    {
      private static final long serialVersionUID = 4577664688930645961L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        final long selectedDate = startDateTimePanel.getDateField().getModelObject().getTime();
        target.appendJavaScript("$(function() { $('#"
            + endDateTimePanel.getDateField().getMarkupId()
            + "').datepicker('option', 'minDate', new Date("
            + selectedDate
            + ")); });");
      }
    });
    if (access == false) {
      endDateField.setEnabled(false);
      startDateField.setEnabled(false);
    }
  }

  /**
   * add tooltip to datefield.
   */
  private void dateFieldToolTip(final DateTimePanel component)
  {
    WicketUtils.addTooltip(component.getDateField(), new Model<String>()
    {
      private static final long serialVersionUID = 3878115580425103805L;

      @Override
      public String getObject()
      {
        final StringBuffer buf = new StringBuffer();
        if (data.getStartDate() != null) {
          buf.append(DateHelper.TECHNICAL_ISO_UTC.get().format(data.getStartDate()));
          if (data.getEndDate() != null) {
            buf.append(" - ");
          }
        }
        if (data.getEndDate() != null) {
          buf.append(DateHelper.TECHNICAL_ISO_UTC.get().format(data.getEndDate()));
        }
        return buf.toString();
      }
    });
  }

  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  void setData(final TeamEventDO data)
  {
    this.data = data;
  }

  public MultiChoiceListHelper<TeamEventAttendeeDO> getAssignAttendeesListHelper()
  {
    return assignAttendeesListHelper;
  }

  public AttendeeWicketProvider getAttendeeWicketProvider()
  {
    return attendeeWicketProvider;
  }
}
