/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.task;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.IConverter;
import org.hibernate.Hibernate;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.gantt.GanttObjectType;
import org.projectforge.business.gantt.GanttRelationType;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.common.StringHelper;
import org.projectforge.common.i18n.Priority;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.fibu.Kost2ListPage;
import org.projectforge.web.fibu.Kost2SelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.converter.IntegerPercentConverter;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TaskEditForm extends AbstractEditForm<TaskDO, TaskEditPage>
{
  private static final long serialVersionUID = -3784956996856970327L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskEditForm.class);

  public static final BigDecimal MAX_DURATION_DAYS = new BigDecimal(10000);

  protected MaxLengthTextField kost2BlackWhiteTextField;

  private DropDownChoice<Boolean> kost2listTypeChoice;

  private DropDownChoice<TimesheetBookingStatus> timesheetBookingStatusChoice;

  @SuppressWarnings("unused")
  private Integer kost2Id;

  private ProjektDO projekt;

  private DivTextPanel projektKostLabel;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent[2];

  public TaskEditForm(final TaskEditPage parentPage, final TaskDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @SuppressWarnings("unchecked")
      @Override
      public void validate(final Form<?> form)
      {
        final MinMaxNumberField<BigDecimal> durationField = (MinMaxNumberField<BigDecimal>) dependentFormComponents[0];
        final LocalDatePanel endDate = (LocalDatePanel) dependentFormComponents[1];
        if (durationField.getConvertedInput() != null && endDate.getDateField().getConvertedInput() != null) {
          error(getString("gantt.error.durationAndEndDateAreMutuallyExclusive"));
        }
      }
    });
    gridBuilder.newGridPanel();
    {
      // Parent task
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.parentTask"));
      final TaskSelectPanel parentTaskSelectPanel = new TaskSelectPanel(fs,
          new PropertyModel<>(data, "parentTask"), parentPage,
          "parentTaskId");
      fs.add(parentTaskSelectPanel);
      fs.getFieldset().setOutputMarkupId(true);
      parentTaskSelectPanel.init();
      if (!TaskTree.getInstance().isRootNode(data)) {
        parentTaskSelectPanel.setRequired(true);
      } else {
        fs.setVisible(false);
      }
      parentTaskSelectPanel.setRequired(true);
    }
    {
      // Title
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.title"));
      final MaxLengthTextField title = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "title"));
      WicketUtils.setStrong(title);
      fs.add(title);
      if (isNew()) {
        WicketUtils.setFocus(title);
      }
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Status drop down box:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<TaskStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<>(fs,
          TaskStatus.values());
      final DropDownChoice<TaskStatus> statusChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
          new PropertyModel<>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      fs.add(statusChoice);
    }
    {
      // Assigned user:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.assignedUser"));
      PFUserDO responsibleUser = data.getResponsibleUser();
      if (!Hibernate.isInitialized(responsibleUser)) {
        responsibleUser = UserGroupCache.getInstance().getUser(responsibleUser.getId());
        data.setResponsibleUser(responsibleUser);
      }
      final UserSelectPanel responsibleUserSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data,
              "responsibleUser"),
          parentPage, "responsibleUserId");
      fs.add(responsibleUserSelectPanel);
      responsibleUserSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Priority drop down box:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("priority"));
      final LabelValueChoiceRenderer<Priority> priorityChoiceRenderer = new LabelValueChoiceRenderer<>(fs,
          Priority.values());
      final DropDownChoice<Priority> priorityChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
          new PropertyModel<>(
              data, "priority"),
          priorityChoiceRenderer.getValues(), priorityChoiceRenderer);
      priorityChoice.setNullValid(true);
      fs.add(priorityChoice);
    }
    {
      // Max hours:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.maxHours"));
      final MinMaxNumberField<Integer> maxNumberField = new MinMaxNumberField<>(InputPanel.WICKET_ID,
          new PropertyModel<>(
              data, "maxHours"),
          0, 9999);
      WicketUtils.setSize(maxNumberField, 6);
      fs.add(maxNumberField);
      if (!isNew() && TaskTree.getInstance().hasOrderPositions(data.getId(), true)) {
        WicketUtils.setWarningTooltip(maxNumberField);
        WicketUtils.addTooltip(maxNumberField, getString("task.edit.maxHoursIngoredDueToAssignedOrders"));
      }
    }
    gridBuilder.newGridPanel();
    {
      // Short description:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("shortDescription"));
      final IModel<String> model = new PropertyModel<>(data, "shortDescription");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, model));
      fs.addJIRAField(model);
    }
    {
      // Reference
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task.reference"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "reference")));
    }

    // ///////////////////////////////
    // GANTT
    // ///////////////////////////////
    gridBuilder.newGridPanel();
    {
      final ToggleContainerPanel extendedSettingsPanel = new ToggleContainerPanel(gridBuilder.getPanel().newChildId())
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#wantsOnStatusChangedNotification()
         */
        @Override
        protected boolean wantsOnStatusChangedNotification()
        {
          return true;
        }
      };
      extendedSettingsPanel.setHeading(getString("task.gantt.settings"));
      gridBuilder.getPanel().add(extendedSettingsPanel);
      extendedSettingsPanel.setClosed();
      final GridBuilder innerGridBuilder = extendedSettingsPanel.createGridBuilder();
      innerGridBuilder.newSplitPanel(GridSize.COL50);
      {
        // Gantt object type:
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("gantt.objectType"));
        final LabelValueChoiceRenderer<GanttObjectType> objectTypeChoiceRenderer = new LabelValueChoiceRenderer<>(
            fs,
            GanttObjectType.values());
        final DropDownChoice<GanttObjectType> objectTypeChoice = new DropDownChoice<>(
            fs.getDropDownChoiceId(),
            new PropertyModel<>(data, "ganttObjectType"), objectTypeChoiceRenderer.getValues(),
            objectTypeChoiceRenderer);
        objectTypeChoice.setNullValid(true);
        fs.add(objectTypeChoice);
      }
      {
        // Gantt: start date
        final FieldProperties<LocalDate> props = getStartDateProperties();
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("gantt.startDate"));
        LocalDatePanel components = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
        fs.add(components);
      }
      {
        // Gantt: end date
        final FieldProperties<LocalDate> props = getEndDateProperties();
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("gantt.endDate"));
        LocalDatePanel components = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
        fs.add(components);
        dependentFormComponents[1] = components;
      }

      innerGridBuilder.newSplitPanel(GridSize.COL50);
      {
        // Progress
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("task.progress")).setUnit("%");
        final MinMaxNumberField<Integer> progressField = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
            new PropertyModel<>(
                data, "progress"),
            0, 100) {
          @SuppressWarnings({"unchecked", "rawtypes"})
          @Override
          public IConverter getConverter(final Class type) {
            return new IntegerPercentConverter(0);
          }
        };
        WicketUtils.setSize(progressField, 3);
        fs.add(progressField);
      }
      {
        // Gantt: duration
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("gantt.duration")).suppressLabelForWarning();
        final MinMaxNumberField<BigDecimal> durationField = new MinMaxNumberField<>(InputPanel.WICKET_ID,
            new PropertyModel<>(data, "duration"), BigDecimal.ZERO, TaskEditForm.MAX_DURATION_DAYS);
        WicketUtils.setSize(durationField, 6);
        fs.add(durationField);
        dependentFormComponents[0] = durationField;
      }
      {
        // Gantt: predecessor offset
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("gantt.predecessorOffset"))
            .setUnit(getString("days"));
        final MinMaxNumberField<Integer> ganttPredecessorField = new MinMaxNumberField<>(InputPanel.WICKET_ID,
            new PropertyModel<>(data, "ganttPredecessorOffset"), Integer.MIN_VALUE, Integer.MAX_VALUE);
        WicketUtils.setSize(ganttPredecessorField, 6);
        fs.add(ganttPredecessorField);
      }
      innerGridBuilder.newGridPanel();
      {
        // Gantt relation type:
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("gantt.relationType"));
        final LabelValueChoiceRenderer<GanttRelationType> relationTypeChoiceRenderer = new LabelValueChoiceRenderer<>(
            fs,
            GanttRelationType.values());
        final DropDownChoice<GanttRelationType> relationTypeChoice = new DropDownChoice<>(
            fs.getDropDownChoiceId(),
            new PropertyModel<>(data, "ganttRelationType"), relationTypeChoiceRenderer.getValues(),
            relationTypeChoiceRenderer);
        relationTypeChoice.setNullValid(true);
        fs.add(relationTypeChoice);
      }
      {
        // Gantt: predecessor
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("gantt.predecessor"));
        final TaskSelectPanel ganttPredecessorSelectPanel = new TaskSelectPanel(fs,
            new PropertyModel<>(data, "ganttPredecessor"),
            parentPage, "ganttPredecessorId");
        fs.add(ganttPredecessorSelectPanel);
        ganttPredecessorSelectPanel.setShowFavorites(true);
        ganttPredecessorSelectPanel.init();
      }
    }

    // ///////////////////////////////
    // FINANCE ADMINISTRATION
    // ///////////////////////////////
    gridBuilder.newGridPanel();
    {
      final ToggleContainerPanel extendedSettingsPanel = new ToggleContainerPanel(gridBuilder.getPanel().newChildId())
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#wantsOnStatusChangedNotification()
         */
        @Override
        protected boolean wantsOnStatusChangedNotification()
        {
          return true;
        }
      };
      extendedSettingsPanel.setHeading(getString("financeAdministration"));
      gridBuilder.getPanel().add(extendedSettingsPanel);
      extendedSettingsPanel.setClosed();
      final GridBuilder innerGridBuilder = extendedSettingsPanel.createGridBuilder();
      innerGridBuilder.newSplitPanel(GridSize.COL50);

      if (Configuration.getInstance().isCostConfigured()) {
        // Cost 2 settings
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("fibu.kost2"));
        this.projektKostLabel = new DivTextPanel(fs.newChildId(), "");
        WicketUtils.addTooltip(projektKostLabel.getLabel(), new Model<String>() {
          @Override
          public String getObject() {
            final List<Kost2DO> kost2DOs = TaskTree.getInstance().getKost2List(projekt, data, data.getKost2BlackWhiteItems(),
                data.getKost2IsBlackList());
            final String[] kost2s = TaskListPage.getKost2s(kost2DOs);
            if (kost2s == null || kost2s.length == 0) {
              return " - (-)";
            }
            return StringHelper.listToString("\n", kost2s);
          }
        });
        fs.add(projektKostLabel);
        final PropertyModel<String> model = new PropertyModel<>(data, "kost2BlackWhiteList");
        kost2BlackWhiteTextField = new MaxLengthTextField(InputPanel.WICKET_ID, model);
        WicketUtils.setSize(kost2BlackWhiteTextField, 10);
        fs.add(kost2BlackWhiteTextField);
        final LabelValueChoiceRenderer<Boolean> kost2listTypeChoiceRenderer = new LabelValueChoiceRenderer<Boolean>() //
            .addValue(Boolean.FALSE, getString("task.kost2list.whiteList")) //
            .addValue(Boolean.TRUE, getString("task.kost2list.blackList"));
        kost2listTypeChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
            new PropertyModel<>(data, "kost2IsBlackList"),
            kost2listTypeChoiceRenderer.getValues(), kost2listTypeChoiceRenderer);
        kost2listTypeChoice.setNullValid(false);
        fs.add(kost2listTypeChoice);
        final Kost2SelectPanel kost2SelectPanel = new Kost2SelectPanel(fs.newChildId(),
            new PropertyModel<>(this, "kost2Id"),
            parentPage, "kost2Id")
        {
          @Override
          protected void beforeSelectPage(final PageParameters parameters)
          {
            super.beforeSelectPage(parameters);
            if (projekt != null) {
              parameters.add(Kost2ListPage.PARAMETER_KEY_STORE_FILTER, false);
              parameters.add(Kost2ListPage.PARAMETER_KEY_SEARCH_STRING, "nummer:" + projekt.getKost() + ".*");
            }
          }
        };
        fs.add(kost2SelectPanel);
        kost2SelectPanel.init();
      }
      {
        // Time sheet booking status drop down box:
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("task.timesheetBooking"));
        final LabelValueChoiceRenderer<TimesheetBookingStatus> timesheetBookingStatusChoiceRenderer = new LabelValueChoiceRenderer<>(
            fs, TimesheetBookingStatus.values());
        timesheetBookingStatusChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
            new PropertyModel<>(data, "timesheetBookingStatus"),
            timesheetBookingStatusChoiceRenderer.getValues(),
            timesheetBookingStatusChoiceRenderer);
        timesheetBookingStatusChoice.setNullValid(false);
        fs.add(timesheetBookingStatusChoice);
      }
      innerGridBuilder.newSplitPanel(GridSize.COL50);
      {
        // Protection of privacy:
        innerGridBuilder.newFieldset(getString("task.protectionOfPrivacy"))
            .addCheckBox(new PropertyModel<>(data, "protectionOfPrivacy"), null)
            .setTooltip(getString("task.protectionOfPrivacy.tooltip"));
      }
      {
        // Protection until
        final FieldsetPanel fs = innerGridBuilder.newFieldset(getString("task.protectTimesheetsUntil"));
        final FieldProperties<LocalDate> props = getProtectionProperties();
        LocalDatePanel components = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
        fs.add(components);
        if (!UserGroupCache.getInstance().isUserMemberOfFinanceGroup()) {
          components.setEnabled(false);
        }
      }
    }

    gridBuilder.newGridPanel();
    {
      // Description:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      final IModel<String> model = new PropertyModel<>(data, "description");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, model), true);
      fs.addJIRAField(model);
    }
  }

  private FieldProperties<LocalDate> getStartDateProperties() {
    return new FieldProperties<>("gantt.startDate", new PropertyModel<>(data, "startDate"));
  }

  private FieldProperties<LocalDate> getEndDateProperties() {
    return new FieldProperties<>("gantt.endDate", new PropertyModel<>(data, "endDate"));
  }

  private FieldProperties<LocalDate> getProtectionProperties() {
    return new FieldProperties<>("task.protectTimesheetsUntil", new PropertyModel<>(data, "protectTimesheetsUntil"));
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#onBeforeRender()
   */
  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    final TaskDO task = isNew() ? data.getParentTask() : data;
    final boolean hasKost2AndTimesheetBookingAccess = ((TaskDao) getBaseDao())
        .hasAccessForKost2AndTimesheetBookingStatus(
            ThreadLocalUserContext.getLoggedInUser(), task);
    if (Configuration.getInstance().isCostConfigured() && task != null) {
      // Cost 2 settings
      final ProjektDO projekt = TaskTree.getInstance().getProjekt(task.getId());
      if (this.projekt == projekt) {
        return;
      }
      this.projekt = projekt;
      if (projekt != null) {
        this.projektKostLabel.setText(projekt.getKost() + ".*");
      } else {
        this.projektKostLabel.setText("");
      }
      kost2listTypeChoice.setEnabled(hasKost2AndTimesheetBookingAccess);
      kost2BlackWhiteTextField.setEnabled(hasKost2AndTimesheetBookingAccess);
    }
    timesheetBookingStatusChoice.setEnabled(hasKost2AndTimesheetBookingAccess);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
