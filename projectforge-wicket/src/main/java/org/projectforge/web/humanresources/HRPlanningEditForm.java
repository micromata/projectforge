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

package org.projectforge.web.humanresources;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.hibernate.Hibernate;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.humanresources.*;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.fibu.NewProjektSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mario Groß (m.gross@micromata.de)
 */
public class HRPlanningEditForm extends AbstractEditForm<HRPlanningDO, HRPlanningEditPage>
{
  private static final long serialVersionUID = 3150725003240437752L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRPlanningEditForm.class);

  private boolean showDeletedOnly;

  private RepeatingView entriesRepeater;

  private HRPlanningDO predecessor;

  private boolean predecessorUpdToDate;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent[2];

  private final List<FormComponent<?>> dependentEntryFormComponents = new ArrayList<FormComponent<?>>();

  private FormComponent<?>[] dependentEntryFormComponentsArray;

  protected List<NewProjektSelectPanel> projektSelectPanels = new ArrayList<NewProjektSelectPanel>();

  public HRPlanningEditForm(final HRPlanningEditPage parentPage, final HRPlanningDO data)
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

      @Override
      public void validate(final Form<?> form)
      {
        if (WicketSupport.get(HRPlanningDao.class).doesEntryAlreadyExist(data.getId(), data.getUserId(), data.getWeek()) == true) {
          error(getString("hr.planning.entry.error.entryDoesAlreadyExistForUserAndWeekOfYear"));
        }
      }
    });
    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        if (dependentEntryFormComponentsArray == null) {
          dependentEntryFormComponentsArray = new FormComponent[dependentEntryFormComponents.size()];
          dependentEntryFormComponentsArray = dependentEntryFormComponents.toArray(dependentEntryFormComponentsArray);
        }
        return dependentEntryFormComponentsArray;
      }

      @Override
      public void validate(final Form<?> form)
      {
        for (int i = 0; i < getDependentFormComponents().length - 1; i += 2) {
          @SuppressWarnings("unchecked")
          final DropDownChoice<HRPlanningEntryStatus> statusChoice = (DropDownChoice<HRPlanningEntryStatus>) dependentEntryFormComponentsArray[i];
          final HRPlanningEntryStatus status = statusChoice.getConvertedInput();
          final NewProjektSelectPanel projektSelectPanel = (NewProjektSelectPanel) dependentEntryFormComponentsArray[i + 1];
          final ProjektDO projekt = projektSelectPanel.getModelObject();
          if (projekt == null && status == null) {
            projektSelectPanel.error(getString("hr.planning.entry.error.statusOrProjektRequired"));
          } else if (projekt != null && status != null) {
            projektSelectPanel.error(getString("hr.planning.entry.error.statusAndProjektNotAllowed"));
          }
        }
      }
    });
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user"));
      Hibernate.initialize(data.getUser());
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(), new PropertyModel<PFUserDO>(data, "user"), parentPage,
          "userId");
      fs.add(dependentFormComponents[0] = userSelectPanel);
      userSelectPanel.setRequired(true);
      userSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Start Date
      final FieldProperties<LocalDate> props = getWeekProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timesheet.startTime"));
      LocalDatePanel weekDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      weekDatePanel.setRequired(true);
      weekDatePanel.add((IValidator<Date>) iValidatable -> {
         PFDay day = PFDay.fromOrNull(iValidatable.getValue());
        if (day != null) {
          day = day.getBeginOfWeek();
          data.setWeek(day.getLocalDate());
        }
        weekDatePanel.markModelAsChanged();
      });

      fs.add(dependentFormComponents[1] = weekDatePanel);
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("calendarWeek"))
      {
        @Override
        public final void onSubmit()
        {
        }
      }.setDefaultFormProcessing(false), new Model<String>()
      {
        @Override
        public String getObject()
        {
          if (data.getWeek() != null) {
            return getString("calendar.weekOfYearShortLabel") + " " + DateTimeFormatter.formatWeekOfYear(data.getWeek());
          } else {
            return getString("calendar.weekOfYearShortLabel");
          }
        }

      }, SingleButtonPanel.NORMAL).setTooltip(getString("recalculate")));
      final DivPanel checkBoxDiv = new DivPanel(fs.newChildId(), DivType.BTN_GROUP)
      {
        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          return data.hasDeletedEntries();
        }
      };
      fs.add(checkBoxDiv);
      checkBoxDiv.add(new CheckBoxButton(checkBoxDiv.newChildId(), new PropertyModel<Boolean>(this, "showDeletedOnly"),
          getString("onlyDeleted"), new FormComponentUpdatingBehavior()
      {
        @Override
        public void onUpdate()
        {
          refresh();
        }
      }));
      if (isNew() == true) {
        fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("predecessor"))
        {
          @Override
          public final void onSubmit()
          {
            if (getPredecessor() != null && predecessor.getEntries() != null) {
              final Iterator<HRPlanningEntryDO> it = getData().getEntries().iterator();
              while (it.hasNext() == true) {
                if (it.next().isEmpty() == true) {
                  it.remove();
                }
              }
              for (final HRPlanningEntryDO entry : predecessor.getEntries()) {
                getData().addEntry(entry.newClone());
              }
            }
            predecessor = null;
            refresh();
          }
        }.setDefaultFormProcessing(false), getString("hr.planning.entry.copyFromPredecessor"))
        {
          /**
           * @see org.apache.wicket.Component#isVisible()
           */
          @Override
          public boolean isVisible()
          {
            return getPredecessor() != null;
          }
        });
      }
    }
    gridBuilder.newGridPanel();
    entriesRepeater = gridBuilder.newRepeatingView();
    refresh();
    if (getBaseDao().hasInsertAccess(getUser()) == true && showDeletedOnly == false) {
      final DivPanel panel = gridBuilder.newGridPanel().getPanel();
      final Button addPositionButton = new Button(SingleButtonPanel.WICKET_ID)
      {
        @Override
        public final void onSubmit()
        {
          getData().addEntry(new HRPlanningEntryDO());
          refresh();
        }
      };
      final SingleButtonPanel addPositionButtonPanel = new SingleButtonPanel(panel.newChildId(), addPositionButton, getString("add"));
      addPositionButtonPanel.setTooltip(getString("hr.planning.tooltip.addEntry"));
      panel.add(addPositionButtonPanel);
    }
    WicketUtils.addShowDeleteRowQuestionDialog(this, WicketSupport.get(HRPlanningEntryDao.class));
  }

  private FieldProperties<LocalDate> getWeekProperties() {
    return new FieldProperties<>("week", new PropertyModel<>(super.data, "week"));
  }

  @SuppressWarnings("serial")
  void refresh()
  {
    if (hasError() == true) {
      // Do nothing.
      return;
    }
    if (data.hasDeletedEntries() == false) {
      this.showDeletedOnly = false;
    }
    if (isNew() == true) {
      this.predecessorUpdToDate = false;
    }
    entriesRepeater.removeAll();
    if (CollectionUtils.isEmpty(data.getEntries()) == true) {
      // Ensure that at least one entry is available:
      data.addEntry(new HRPlanningEntryDO());
    }
    projektSelectPanels.clear();
    int idx = -1;
    dependentEntryFormComponents.clear();
    dependentEntryFormComponentsArray = null;
    int uiId = -1;
    for (final HRPlanningEntryDO entry : data.getEntries()) {
      ++idx;
      ++uiId;
      if (entry.getDeleted() != showDeletedOnly) {
        // Don't show deleted/undeleted entries.
        --uiId;
        continue;
      }
      final ToggleContainerPanel positionsPanel = new ToggleContainerPanel(entriesRepeater.newChildId());
      positionsPanel.getContainer().setOutputMarkupId(true);
      entriesRepeater.add(positionsPanel);
      String heading = escapeHtml(entry.getProjektNameOrStatus());
      if (StringUtils.isBlank(heading) == true) {
        heading = "???";
      }
      final BigDecimal totalHours = entry.getTotalHours();
      if (NumberHelper.isNotZero(totalHours) == true) {
        heading += ": " + NumberHelper.formatFraction2(totalHours);
      }
      positionsPanel.setHeading(new HtmlCodePanel(ToggleContainerPanel.HEADING_TEXT_ID, heading));
      final DivPanel content = new DivPanel(ToggleContainerPanel.CONTENT_ID);
      positionsPanel.add(content);
      final GridBuilder posGridBuilder = new GridBuilder(content, content.newChildId());
      {
        // DropDownChoice status / project
        final FieldsetPanel fs = posGridBuilder.newFieldset(WicketUtils.createMultipleFieldsetLabel(getString("status"),
            getString("fibu.projekt")));
        final LabelValueChoiceRenderer<HRPlanningEntryStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<HRPlanningEntryStatus>(
            fs, HRPlanningEntryStatus.values());
        final DropDownChoice<HRPlanningEntryStatus> statusChoice = new DropDownChoice<HRPlanningEntryStatus>(fs.getDropDownChoiceId(),
            new PropertyModel<HRPlanningEntryStatus>(entry, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
        statusChoice.setNullValid(true).setRequired(false).setEnabled(!entry.getDeleted());
        fs.add(statusChoice);
        dependentEntryFormComponents.add(statusChoice);
        final NewProjektSelectPanel projektSelectPanel = new NewProjektSelectPanel(fs.newChildId(),
            new PropertyModel<ProjektDO>(entry, "projekt"), parentPage, "projektId:" + idx + ":" + uiId);
        projektSelectPanel.setRequired(false).setEnabled(!entry.getDeleted());
        fs.add(projektSelectPanel);
        projektSelectPanel.init();
        dependentEntryFormComponents.add(projektSelectPanel);
        projektSelectPanels.add(projektSelectPanel);

        final Button button = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("deleteUndelete"))
        {
          @Override
          public final void onSubmit()
          {
            if (entry.getDeleted() == true) {
              // Undelete
              entry.setDeleted(false);
            } else {
              getData().deleteEntry(entry);
            }
            refresh();
          }
        };
        final String buttonLabel, classNames;
        if (entry.getDeleted() == true) {
          buttonLabel = getString("undelete");
          classNames = SingleButtonPanel.NORMAL;
        } else {
          buttonLabel = getString("delete");
          classNames = SingleButtonPanel.DELETE;
          if (entry.getId() != null) {
            button.add(AttributeModifier.prepend("onclick", "if (showDeleteQuestionDialog() == false) return false;"));
          }
        }
        button.setDefaultFormProcessing(false);
        fs.add(new SingleButtonPanel(fs.newChildId(), button, buttonLabel, classNames)
        {

        });
      }
      posGridBuilder.newSplitPanel(GridSize.COL50);
      {
        // DropDownChoice Priority
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("hr.planning.priority"));
        final LabelValueChoiceRenderer<Priority> priorityChoiceRenderer = new LabelValueChoiceRenderer<Priority>(fs, Priority.values());
        final DropDownChoice<Priority> priorityChoice = new DropDownChoice<Priority>(fs.getDropDownChoiceId(), new PropertyModel<Priority>(
            entry, "priority"), priorityChoiceRenderer.getValues(), priorityChoiceRenderer);
        priorityChoice.setNullValid(true).setEnabled(!entry.getDeleted());
        fs.add(priorityChoice);
      }
      posGridBuilder.newSplitPanel(GridSize.COL50);
      {
        // DropDownChoice probability
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("hr.planning.probability"));
        final LabelValueChoiceRenderer<Integer> probabilityChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
        probabilityChoiceRenderer.addValue(25, "25%");
        probabilityChoiceRenderer.addValue(50, "50%");
        probabilityChoiceRenderer.addValue(75, "75%");
        probabilityChoiceRenderer.addValue(95, "95%");
        probabilityChoiceRenderer.addValue(100, "100%");
        final DropDownChoice<Integer> probabilityChoice = new DropDownChoice<Integer>(fs.getDropDownChoiceId(), new PropertyModel<Integer>(
            entry, "probability"), probabilityChoiceRenderer.getValues(), probabilityChoiceRenderer);
        probabilityChoice.setNullValid(true).setEnabled(!entry.getDeleted());
        fs.add(probabilityChoice);
      }
      posGridBuilder.newSplitPanel(GridSize.COL50);
      {
        // Hours
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("hours")).suppressLabelForWarning();
        final HRPlanningEditTablePanel table = new HRPlanningEditTablePanel(fs.newChildId());
        fs.add(table);
        table.init(entry);
      }
      posGridBuilder.newSplitPanel(GridSize.COL50);
      {
        // Description
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("hr.planning.description"));
        final IModel<String> model = new PropertyModel<String>(entry, "description");
        final MaxLengthTextArea description = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, model);
        if (entry.getDeleted() == true) {
          description.setEnabled(false);
        }
        fs.add(description);
        fs.add(new JiraIssuesPanel(fs.newChildId(), entry.getDescription()));
        fs.addJIRAField(model);
      }
    }
  }

  private HRPlanningDO getPredecessor()
  {
    if (predecessorUpdToDate == false) {
      predecessor = null;
      final Long userId = data.getUserId();
      if (userId != null) {
        // Get the entry from the predecessor week:
        PFDay dh = PFDay.from(getData().getWeek());
        dh = dh.minusWeeks(1);
        predecessor = WicketSupport.get(HRPlanningDao.class).getEntry(userId, dh.getLocalDate());
      }
      predecessorUpdToDate = true;
    }
    return predecessor;
  }

  public boolean isShowDeletedOnly()
  {
    return showDeletedOnly;
  }

  public void setShowDeletedOnly(final boolean showDeletedOnly)
  {
    this.showDeletedOnly = showDeletedOnly;
  }

  void setData(final HRPlanningDO planning)
  {
    data = planning;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
