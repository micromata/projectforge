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

package org.projectforge.web.gantt;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.gantt.GanttAccess;
import org.projectforge.business.gantt.GanttChartDO;
import org.projectforge.business.gantt.GanttChartSettings;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class GanttChartEditForm extends AbstractEditForm<GanttChartDO, GanttChartEditPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GanttChartEditForm.class);

  private static final long serialVersionUID = 3199820655287750358L;

  static final String EXPORT_JPG = "JPG";

  static final String EXPORT_MS_PROJECT_XML = "MSP-XML";

  static final String EXPORT_MS_PROJECT_MPX = "MPX";

  static final String EXPORT_PDF = "PDF";

  static final String EXPORT_PNG = "PNG";

  static final String EXPORT_PROJECTFORGE = "PROJECTFORGE";

  static final String EXPORT_SVG = "SVG";

  GanttChartEditTreeTablePanel ganttChartEditTreeTablePanel;

  private Button redrawButton;

  private String exportFormat;

  DivPanel imagePanel;

  public GanttChartEditForm(final GanttChartEditPage parentPage, final GanttChartDO data)
  {
    super(parentPage, data);
    if (isNew() == true) {
      if (data.getOwner() == null) {
        data.setOwner(ThreadLocalUserContext.getUser());
      }
      if (StringUtils.isEmpty(data.getName()) == true) {
        data.setName("MyChart");
      }
    }
    if (data.getReadAccess() == null) {
      data.setReadAccess(GanttAccess.OWNER);
    }
    if (data.getWriteAccess() == null) {
      data.setWriteAccess(GanttAccess.OWNER);
    }
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task"));
      final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new PropertyModel<TaskDO>(data, "task"), parentPage, "taskId")
      {
        @Override
        protected void selectTask(final TaskDO task)
        {
          super.selectTask(task);
          parentPage.refresh(); // Task was changed. Therefore update the kost2 list.
        }
      };
      fs.add(taskSelectPanel);
      taskSelectPanel.init();
      taskSelectPanel.setRequired(true);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.name"));
      final RequiredMaxLengthTextField name = new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "name"));
      WicketUtils.setStrong(name);
      fs.add(name);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.owner"));
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(), new PropertyModel<PFUserDO>(data, "owner"), parentPage,
          "ownerId");
      fs.add(userSelectPanel);
      userSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("title"));
      final RequiredMaxLengthTextField title = new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(getSettings(),
          "title"), 100);
      WicketUtils.setStrong(title);
      fs.add(title);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // read-access:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("access.read"));
      final LabelValueChoiceRenderer<GanttAccess> readAccessChoiceRenderer = new LabelValueChoiceRenderer<GanttAccess>(this,
          GanttAccess.values());
      final DropDownChoice<GanttAccess> readAccessChoice = new DropDownChoice<GanttAccess>(fs.getDropDownChoiceId(),
          new PropertyModel<GanttAccess>(getData(), "readAccess"), readAccessChoiceRenderer.getValues(), readAccessChoiceRenderer);
      readAccessChoice.setNullValid(false);
      fs.add(readAccessChoice);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Width
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.settings.width"));
      fs.add(new MinMaxNumberField<Integer>(fs.getTextFieldId(), new PropertyModel<Integer>(data.getStyle(), "width"), 100, 10000));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // write-access:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("access.write"));
      final LabelValueChoiceRenderer<GanttAccess> writeAccessChoiceRenderer = new LabelValueChoiceRenderer<GanttAccess>(this,
          GanttAccess.values());
      final DropDownChoice<GanttAccess> writeAccessChoice = new DropDownChoice<GanttAccess>(fs.getDropDownChoiceId(),
          new PropertyModel<GanttAccess>(getData(), "writeAccess"), writeAccessChoiceRenderer.getValues(), writeAccessChoiceRenderer);
      writeAccessChoice.setNullValid(false);
      fs.add(writeAccessChoice);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Total label width:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("gantt.settings.totalLabelWidth"));
      fs.add(new MinMaxNumberField<Double>(fs.getTextFieldId(), new PropertyModel<Double>(data.getStyle(), "totalLabelWidth"), 10.0,
          10000.0));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Options
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.options")).suppressLabelForWarning();
      final DivPanel checkBoxPanel = fs.addNewCheckBoxButtonDiv();
      checkBoxPanel.addCheckBoxButton(new PropertyModel<Boolean>(data.getStyle(), "relativeTimeValues"),
          getString("gantt.style.relativeTimeValues"));
      checkBoxPanel.addCheckBoxButton(new PropertyModel<Boolean>(data.getStyle(), "showToday"), getString("gantt.style.showToday"));
      checkBoxPanel.addCheckBoxButton(new PropertyModel<Boolean>(data.getStyle(), "showCompletion"), getString("gantt.style.showCompletion"));
      checkBoxPanel.add(new CheckBoxButton(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(getSettings(), "showOnlyVisibles"),
          getString("gantt.settings.showOnlyVisibles"), new FormComponentUpdatingBehavior()));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Time period
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod")).suppressLabelForWarning();
      final DatePanel fromDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(getSettings(), "fromDate"), DatePanelSettings
          .get().withSelectProperty("fromDate"));
      fs.add(fromDatePanel);
      fs.add(new DivTextPanel(fs.newChildId(), "-"));
      final DatePanel toDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(getSettings(), "toDate"), DatePanelSettings
          .get().withSelectProperty("toDate"));
      fs.add(toDatePanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("export"));
      final LabelValueChoiceRenderer<String> exportFormatChoiceRenderer = new LabelValueChoiceRenderer<String>();
      exportFormatChoiceRenderer.addValue(EXPORT_JPG, getString("gantt.export.jpg"));
      exportFormatChoiceRenderer.addValue(EXPORT_MS_PROJECT_MPX, getString("gantt.export.msproject.mpx"));
      exportFormatChoiceRenderer.addValue(EXPORT_MS_PROJECT_XML, getString("gantt.export.msproject.xml"));
      exportFormatChoiceRenderer.addValue(EXPORT_PDF, getString("gantt.export.pdf"));
      exportFormatChoiceRenderer.addValue(EXPORT_PNG, getString("gantt.export.png"));
      exportFormatChoiceRenderer.addValue(EXPORT_PROJECTFORGE, getString("gantt.export.projectforge"));
      exportFormatChoiceRenderer.addValue(EXPORT_SVG, getString("gantt.export.svg"));
      final DropDownChoice<String> exportFormatChoice = new DropDownChoice<String>(fs.getDropDownChoiceId(), new PropertyModel<String>(
          this, "exportFormat"), exportFormatChoiceRenderer.getValues(), exportFormatChoiceRenderer);
      exportFormatChoice.setNullValid(false);
      fs.add(exportFormatChoice);
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("export"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.export(exportFormat);
        }
      }, getString("export"), SingleButtonPanel.NORMAL));
    }
    gridBuilder.newGridPanel();
    {
      final DivPanel panel = gridBuilder.getPanel();
      ganttChartEditTreeTablePanel = new GanttChartEditTreeTablePanel(panel.newChildId(), this, getData());
      panel.add(ganttChartEditTreeTablePanel);
      ganttChartEditTreeTablePanel.init();
      ganttChartEditTreeTablePanel.setOpenNodes(getSettings().getOpenNodes());
      gridBuilder.getPanel().add(imagePanel = new DivPanel(panel.newChildId()));
    }
    {
      // Redraw:
      redrawButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("redraw"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.refresh();
        }
      };
      redrawButton.setDefaultFormProcessing(false);
      WicketUtils.addTooltip(redrawButton, getString("gantt.tooltip.returnKeyCallsRedraw"));
      final SingleButtonPanel redrawButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), redrawButton, getString("redraw"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(3, redrawButtonPanel);
    }
    if (isNew() == false && data.isDeleted() == false) {
      // Clone:
      final Button cloneButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("clone"))
      {
        @Override
        public final void onSubmit()
        {
          getData().setId(null);
          this.setVisible(false);
          updateButtonVisibility();
        }
      };
      cloneButton.setDefaultFormProcessing(false);
      final SingleButtonPanel cloneButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cloneButton, getString("clone"),
          SingleButtonPanel.NORMAL);
      actionButtons.add(3, cloneButtonPanel);
    }

    // final SubmitLink addPositionButton = new SubmitLink("addActivity") {
    // @Override
    // public void onSubmit()
    // {
    // final GanttTaskImpl root = (GanttTaskImpl) parentPage.ganttChartData.getRootObject();
    // final Integer nextId = root.getNextId();
    // root.addChild(new GanttTaskImpl(nextId).setVisible(true).setTitle(getString("untitled")));
    // final GanttChartEditTreeTablePanel tablePanel = parentPage.ganttChartEditTreeTablePanel;
    // final Set<Serializable> openNodes = tablePanel.getOpenNodes();
    // tablePanel.refreshTreeTable();
    // tablePanel.setOpenNodes(openNodes);
    // parentPage.refresh();
    // };
    // };
    // add(addPositionButton);
    // addPositionButton.add(WicketUtils.getAddRowImage("addImage", getResponse(), getString("gantt.action.newActivity")));

    // new AjaxEditableLabel("text1");
  }

  @Override
  public void updateButtonVisibility()
  {
    super.updateButtonVisibility();
    setDefaultButton(redrawButton);
  }

  @Override
  protected void markDefaultButtons()
  {
    // Avoid re-marking.
  }

  /**
   * @return the exportFormat
   */
  public String getExportFormat()
  {

    if (exportFormat == null) {
      exportFormat = (String) parentPage.getUserPrefEntry(this.getClass().getName() + ":exportFormat");
    }
    if (exportFormat == null) {
      exportFormat = EXPORT_PDF;
    }

    return exportFormat;
  }

  /**
   * @param exportFormat the exportFormat to set
   */
  public void setExportFormat(final String exportFormat)
  {
    this.exportFormat = exportFormat;
    parentPage.putUserPrefEntry(this.getClass().getName() + ":exportFormat", this.exportFormat, true);
  }

  GanttChartSettings getSettings()
  {
    return getData().getSettings();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
