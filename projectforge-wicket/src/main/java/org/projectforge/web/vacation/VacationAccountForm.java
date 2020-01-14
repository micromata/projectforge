/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.vacation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeStatus;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.vacation.model.LeaveAccountEntryDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.repository.RemainingLeaveDao;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.business.vacation.service.VacationStats;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.fibu.EmployeeSelectPanel;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VacationAccountForm extends AbstractStandardForm<VacationAccountForm, VacationAccountPage> {
  @SpringBean
  private VacationService vacationService;

  @SpringBean
  private ConfigurationService configService;

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private RemainingLeaveDao remainingLeaveDao;

  private EmployeeDO employee;

  final int year = Year.now().getValue();
  VacationStats currentYearStats;
  VacationStats previousYearStats;

  public VacationAccountForm(final VacationAccountPage parentPage) {
    super(parentPage);
  }

  @Override
  protected void onSubmit() {
    super.onSubmit();
    Integer employeeId = null;
    if (employee != null) {
      employeeId = employee.getId();
    }
    // Force reload of page.
    setResponsePage(VacationAccountPage.class, new PageParameters().add("employeeId", employeeId));
  }

  @Override
  @SuppressWarnings("serial")
  protected void init() {
    super.init();
    boolean showAddButton = false;
    boolean hrAccess = vacationService.hasLoggedInUserHRVacationAccess();

    if (employee == null) {
      employee = employeeService.getEmployeeByUserId(getUserId());
    }
    if (employee == null) {
      throw new IllegalStateException("Emmployee with user id " + getUserId() + " not found.");
    }
    currentYearStats = vacationService.getVacationStats(VacationAccountForm.this.employee, year);
    previousYearStats = vacationService.getVacationStats(VacationAccountForm.this.employee, year - 1);
    if (hrAccess || Objects.equals(employee.getUserId(), getUserId())) {
      showAddButton = true;
    }
    if (hrAccess) {
      gridBuilder.newSplitPanel(GridSize.COL33);
      // Employee
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.employee"));
      final EmployeeSelectPanel employeeSelectPanel = new EmployeeSelectPanel(fs.newChildId(), new PropertyModel<EmployeeDO>(this,
              "employee"), parentPage, "employeeId").withAutoSubmit(true);
      fs.add(employeeSelectPanel);
      employeeSelectPanel.setFocus().setRequired(true);
      employeeSelectPanel.init();
      gridBuilder.newSplitPanel(GridSize.COL66);
      gridBuilder.newSplitPanel(GridSize.COL100);
    }

    LocalDate endDatePreviousYearVacation = configService.getEndDateVacationFromLastYear();

    // leave account
    GridBuilder sectionLeftGridBuilder = gridBuilder.newSplitPanel(GridSize.COL33);
    DivPanel sectionLeft = sectionLeftGridBuilder.getPanel();
    sectionLeft.add(new Heading1Panel(sectionLeft.newChildId(), I18nHelper.getLocalizedMessage("menu.vacation.leaveaccount")));

    appendFieldset(sectionLeftGridBuilder, "vacation.annualleave", currentYearStats.getVacationDaysInYearFromContract(), false);

    //student leave
    if (EmployeeStatus.STUD_ABSCHLUSSARBEIT.equals(employeeService.getEmployeeStatus(employee)) ||
            EmployeeStatus.STUDENTISCHE_HILFSKRAFT.equals(employeeService.getEmployeeStatus(employee))) {
      appendFieldset(sectionLeftGridBuilder, "vacation.countPerDay", employeeService.getStudentVacationCountPerDay(employee), false);
    }

    appendFieldset(sectionLeftGridBuilder, "vacation.previousyearleave", currentYearStats.getRemainingLeaveFromPreviousYear(), false);

    appendFieldset(sectionLeftGridBuilder, "vacation.subtotal", currentYearStats.getTotalLeaveIncludingCarry(), true);

    if (currentYearStats.getLeaveAccountEntriesSum().compareTo(BigDecimal.ZERO) != 0) {
      appendFieldset(sectionLeftGridBuilder, "menu.vacation.leaveAccountEntry", currentYearStats.getLeaveAccountEntriesSum(), true);
    }

    appendFieldset(sectionLeftGridBuilder, "vacation.approvedvacation", currentYearStats.getVacationDaysApproved(), false);

    appendFieldset(sectionLeftGridBuilder, "vacation.plannedvacation", currentYearStats.getVacationDaysInProgress(), false);

    appendFieldset(sectionLeftGridBuilder, "vacation.availablevacation", currentYearStats.getVacationDaysLeftInYear(), true);

    //middle
    GridBuilder sectionMiddleGridBuilder = gridBuilder.newSplitPanel(GridSize.COL33);
    DivPanel sectionMiddle = sectionMiddleGridBuilder.getPanel();
    BigDecimal specialDaysApproved = currentYearStats.getSpecialVacationDaysApproved();
    BigDecimal specialDaysInProgress = currentYearStats.getSpecialVacationDaysInProgress();
    if (specialDaysApproved != null && specialDaysApproved.compareTo(BigDecimal.ZERO) != 0 ||
            specialDaysInProgress != null && specialDaysInProgress.compareTo(BigDecimal.ZERO) != 0) {
      sectionMiddle.add(new Heading1Panel(sectionMiddle.newChildId(), I18nHelper.getLocalizedMessage("vacation.isSpecial")));
      appendFieldset(sectionMiddleGridBuilder, "vacation.isSpecialPlaned", specialDaysInProgress, false);
      appendFieldset(sectionMiddleGridBuilder, "vacation.isSpecialApproved", specialDaysApproved, false);
    }
    sectionMiddle.add(new Heading1Panel(sectionMiddle.newChildId(), I18nHelper.getLocalizedMessage("vacation.previousyearleave")));
    appendFieldset(sectionLeftGridBuilder, "vacation.previousyearleaveused", currentYearStats.getRemainingLeaveFromPreviousYearAllocated(), false);
    String endDatePreviousYearVacationString = endDatePreviousYearVacation.getDayOfMonth() + "." + endDatePreviousYearVacation.getMonthValue() + "." + year;
    appendFieldset(sectionLeftGridBuilder, "vacation.previousyearleaveunused", currentYearStats.getRemainingLeaveFromPreviousYearUnused(), false,
            endDatePreviousYearVacationString);

    // right
    GridBuilder sectionRightGridBuilder = gridBuilder.newSplitPanel(GridSize.COL33);
    DivPanel sectionRight = sectionRightGridBuilder.getPanel();

    sectionRight.add(new Heading1Panel(sectionRight.newChildId(), I18nHelper.getLocalizedMessage("vacation.leaveOfYear", String.valueOf(year - 1))));
    appendFieldset(sectionRightGridBuilder, "vacation.annualleave", previousYearStats.getVacationDaysInYearFromContract(), false,
            String.valueOf(year - 2));
    appendFieldset(sectionRightGridBuilder, "vacation.remainingLeaveFromYear", previousYearStats.getRemainingLeaveFromPreviousYear(), false,
            String.valueOf(year - 2));
    appendFieldset(sectionRightGridBuilder, "vacation.remainingLeaveFromYearUnused", previousYearStats.getRemainingLeaveFromPreviousYearUnused(), false,
            String.valueOf(year - 2));
    appendFieldset(sectionRightGridBuilder, "vacation.approvedVacationInYear", previousYearStats.getVacationDaysApproved(), false,
            String.valueOf(year - 1));
    appendFieldset(sectionRightGridBuilder, "vacation.approvedSpecialVacationInYear", previousYearStats.getSpecialVacationDaysApproved(), false,
            String.valueOf(year - 1));
    if (previousYearStats.getLeaveAccountEntriesSum().compareTo(BigDecimal.ZERO) != 0) {
      appendFieldset(sectionLeftGridBuilder, "menu.vacation.leaveAccountEntry", previousYearStats.getLeaveAccountEntriesSum(), true);
    }

    // bottom list
    GridBuilder sectionBottomGridBuilder = gridBuilder.newSplitPanel(GridSize.COL100);
    DivPanel sectionBottom = sectionBottomGridBuilder.getPanel();
    if (showAddButton) {
      final PageParameters pageParameters = new PageParameters();
      pageParameters.add("employeeId", employee.getId());
      LinkPanel addLink = new LinkPanel(sectionBottom.newChildId(), I18nHelper.getLocalizedMessage("add"), VacationEditPage.class, VacationAccountPage.class, pageParameters);
      addLink.addLinkAttribute("class", "btn btn-sm btn-success bottom-xs-gap");
      sectionBottom.add(addLink);
    }
    if (hrAccess) {
      ButtonPanel recalculateButton = new ButtonPanel(sectionBottom.newChildId(), I18nHelper.getLocalizedMessage("vacation.recalculateRemainingLeave"), new Button(ButtonPanel.BUTTON_ID) {
        @Override
        public void onSubmit() {
          super.onSubmit();
          // Force recalculation of remaining leave (carry from previous year):
          remainingLeaveDao.internalMarkAsDeleted(employee.getId(), year);
          final PageParameters pageParameters = new PageParameters();
          pageParameters.add("employeeId", employee.getId());
          setResponsePage(VacationAccountPage.class, pageParameters);
        }
      });
      recalculateButton.add(new AttributeAppender("class", "btn btn-sm btn-success bottom-xs-gap"));
      sectionBottom.add(recalculateButton);
    }
    addLeaveTable(sectionBottom, employee, currentYearStats);
    addLeaveTable(sectionBottom, employee, previousYearStats);
  }

  private void addLeaveTable(DivPanel sectionBottom, EmployeeDO currentEmployee, VacationStats stats) {
    sectionBottom.add(new Heading3Panel(sectionBottom.newChildId(),
            I18nHelper.getLocalizedMessage("vacation.title.list") + " " + stats.getYear()));
    TablePanel tablePanel = new TablePanel(sectionBottom.newChildId());
    sectionBottom.add(tablePanel);
    final DataTable<VacationDO, String> dataTable = createDataTable(createColumns(), "startDate", SortOrder.ASCENDING,
            currentEmployee, year);
    tablePanel.add(dataTable);
    if (CollectionUtils.isNotEmpty(stats.getLeaveAccountEntries())) {
      sectionBottom.add(new Heading1Panel(sectionBottom.newChildId(), I18nHelper.getLocalizedMessage("vacation.leaveAccountEntry.title.list")));
      for (LeaveAccountEntryDO entry : stats.getLeaveAccountEntries()) {
        sectionBottom.add(new DivTextPanel(sectionBottom.newChildId(), "" + entry.getDate() + ": " + VacationStats.format(entry.getAmount()) + " " + entry.getDescription()));
      }
    }
  }

  private DataTable<VacationDO, String> createDataTable(final List<IColumn<VacationDO, String>> columns,
                                                        final String sortProperty, final SortOrder sortOrder, final EmployeeDO employee, int year) {
    final SortParam<String> sortParam = sortProperty != null
            ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    return new DefaultDataTable<VacationDO, String>(TablePanel.TABLE_ID, columns,
            createSortableDataProvider(sortParam, employee, year), 1000);
  }

  private ISortableDataProvider<VacationDO, String> createSortableDataProvider(final SortParam<String> sortParam,
                                                                               EmployeeDO employee, int year) {
    return new VacationViewPageSortableDataProvider<VacationDO>(sortParam, vacationService, employee, year);
  }

  private List<IColumn<VacationDO, String>> createColumns() {
    final List<IColumn<VacationDO, String>> columns = new ArrayList<IColumn<VacationDO, String>>();

    final CellItemListener<VacationDO> cellItemListener = new CellItemListener<VacationDO>() {
      private static final long serialVersionUID = 1L;

      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
                               final IModel<VacationDO> rowModel) {
        //Nothing to do here
      }
    };
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "startDate", "startDate", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
                               final IModel<VacationDO> rowModel) {
        final VacationDO vacation = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, VacationEditPage.class, vacation.getId(),
                VacationAccountPage.class, DateTimeFormatter.instance().getFormattedDate(vacation.getStartDate()), "employeeId", String.valueOf(employee.getId())));
        cellItemListener.populateItem(item, componentId, rowModel);
        final Item<?> row = (item.findParent(Item.class));
        WicketUtils.addRowClick(row);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "endDate", "endDate", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
                               final IModel<VacationDO> rowModel) {
        final VacationDO vacation = rowModel.getObject();
        item.add(new TextPanel(componentId, DateTimeFormatter.instance().getFormattedDate(vacation.getEndDate())));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<>(VacationDO.class, "status", "status", cellItemListener));
    columns.add(new CellItemListenerLambdaColumn<>(new ResourceModel("vacation.workingdays"),
            rowModel -> vacationService.getVacationDays(rowModel.getObject().getStartDate(), rowModel.getObject().getEndDate(), rowModel.getObject().getHalfDayBegin()),
            cellItemListener)
    );

    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "special", "special", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
                               final IModel<VacationDO> rowModel) {
        final VacationDO vacation = rowModel.getObject();
        if (vacation.getSpecial() != null && vacation.getSpecial() == Boolean.TRUE) {
          item.add(new TextPanel(componentId, I18nHelper.getLocalizedMessage("yes")));
        } else {
          item.add(new TextPanel(componentId, I18nHelper.getLocalizedMessage("no")));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "manager", "manager", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
                               final IModel<VacationDO> rowModel) {
        final VacationDO vacation = rowModel.getObject();
        String userString = "";
        if (vacation.getManager() != null) {
          userString = vacation.getManager().getUser().getFullname();
        }
        item.add(new TextPanel(componentId, userString));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "replacement", "replacement", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
                               final IModel<VacationDO> rowModel) {
        final VacationDO vacation = rowModel.getObject();
        String userString = "";
        if (vacation.getReplacement() != null) {
          userString = vacation.getReplacement().getUser().getFullname();
        }
        item.add(new TextPanel(componentId, userString));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<>(VacationDO.class, "comment", "comment", cellItemListener));
    return columns;
  }

  private boolean appendFieldset(GridBuilder gridBuilder, final String label, final BigDecimal value, final boolean bold, final String... labelParameters) {
    final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage(label, (Object[]) labelParameters)).suppressLabelForWarning();
    DivTextPanel divTextPanel = new DivTextPanel(fs.newChildId(), VacationStats.format(value));
    return appendFieldset(label, fs, bold, divTextPanel);
  }

  private boolean appendFieldset(GridBuilder gridBuilder, final String label, final String value, final boolean bold, final String... labelParameters) {
    final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage(label, (Object[]) labelParameters)).suppressLabelForWarning();
    DivTextPanel divTextPanel = new DivTextPanel(fs.newChildId(), value);
    return appendFieldset(label, fs, bold, divTextPanel);
  }

  private boolean appendFieldset(final String label, final FieldsetPanel fs, final boolean bold, final DivTextPanel divTextPanel) {
    WebMarkupContainer fieldset = fs.getFieldset();
    fieldset.add(AttributeAppender.append("class", "vacationPanel"));
    if (bold) {
      WebMarkupContainer fieldsetLabel = (WebMarkupContainer) fieldset.get("label");
      WebMarkupContainer fieldsetControls = (WebMarkupContainer) fieldset.get("controls");
      fieldsetLabel.add(AttributeModifier.replace("style", "font-weight: bold;"));
      fieldsetControls.add(AttributeModifier.replace("style", "font-weight: bold;"));
    }
    fs.add(divTextPanel);
    return true;
  }

  public EmployeeDO getEmployee() {
    return employee;
  }

  public void setEmployee(EmployeeDO employee) {
    this.employee = employee;
  }
}
