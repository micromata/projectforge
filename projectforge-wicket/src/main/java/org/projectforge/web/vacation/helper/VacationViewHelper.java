package org.projectforge.web.vacation.helper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.vacation.VacationEditPage;
import org.projectforge.web.vacation.VacationViewPageSortableDataProvider;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerLambdaColumn;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.Heading1Panel;
import org.projectforge.web.wicket.flowlayout.Heading3Panel;
import org.projectforge.web.wicket.flowlayout.LinkPanel;
import org.projectforge.web.wicket.flowlayout.TablePanel;
import org.projectforge.web.wicket.flowlayout.TextPanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VacationViewHelper
{
  @Autowired
  private VacationService vacationService;

  @Autowired
  private ConfigurationService configService;

  public void createVacationView(GridBuilder gridBuilder, EmployeeDO currentEmployee, boolean showAddButton, final WebPage returnToPage)
  {
    final Calendar now = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    Calendar endDatePreviousYearVacation = configService.getEndDateVacationFromLastYear();

    // leave account
    GridBuilder sectionLeftGridBuilder = gridBuilder.newSplitPanel(GridSize.COL25);
    DivPanel sectionLeft = sectionLeftGridBuilder.getPanel();
    sectionLeft.add(new Heading1Panel(sectionLeft.newChildId(), I18nHelper.getLocalizedMessage("menu.vacation.leaveaccount")));

    BigDecimal vacationdays = currentEmployee.getUrlaubstage() != null ? new BigDecimal(currentEmployee.getUrlaubstage()) : BigDecimal.ZERO;
    appendFieldset(sectionLeftGridBuilder, "vacation.annualleave", vacationdays.toString());

    BigDecimal vacationdaysPreviousYear = currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) != null
        ? currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) : BigDecimal.ZERO;
    appendFieldset(sectionLeftGridBuilder, "vacation.previousyearleave", vacationdaysPreviousYear.toString());

    BigDecimal subtotal1 = vacationdays.add(vacationdaysPreviousYear);
    appendFieldset(sectionLeftGridBuilder, "vacation.subtotal", subtotal1.toString());

    BigDecimal approvedVacationdays = vacationService.getApprovedVacationdaysForYear(currentEmployee, now.get(Calendar.YEAR));
    appendFieldset(sectionLeftGridBuilder, "vacation.approvedvacation", approvedVacationdays.toString());

    BigDecimal plannedVacation = vacationService.getPlannedVacationdaysForYear(currentEmployee, now.get(Calendar.YEAR));
    appendFieldset(sectionLeftGridBuilder, "vacation.plannedvacation", plannedVacation.toString());

    BigDecimal availableVacation = subtotal1.subtract(plannedVacation).subtract(approvedVacationdays);
    appendFieldset(sectionLeftGridBuilder, "vacation.availablevacation", availableVacation.toString());

    //middel
    GridBuilder sectionMiddleGridBuilder = gridBuilder.newSplitPanel(GridSize.COL25);
    DivPanel sectionMiddle = sectionMiddleGridBuilder.getPanel();
    sectionMiddle.add(new Heading1Panel(sectionMiddle.newChildId(), I18nHelper.getLocalizedMessage("menu.vacation.lastyear")));

    BigDecimal vacationdaysPreviousYearUsed =
        currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) != null ?
            currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) : BigDecimal.ZERO;
    appendFieldset(sectionMiddleGridBuilder, "vacation.previousyearleaveused", vacationdaysPreviousYearUsed.toString());

    BigDecimal vacationdaysPreviousYearUnused = vacationdaysPreviousYear.subtract(vacationdaysPreviousYearUsed);
    String endDatePreviousYearVacationString =
        endDatePreviousYearVacation.get(Calendar.DAY_OF_MONTH) + "." + (endDatePreviousYearVacation.get(Calendar.MONTH) + 1) + ".";
    appendFieldset(sectionMiddleGridBuilder, "vacation.previousyearleaveunused", vacationdaysPreviousYearUnused.toString(),
        endDatePreviousYearVacationString);

    // special leave
    GridBuilder sectionRightGridBuilder = gridBuilder.newSplitPanel(GridSize.COL33);
    DivPanel sectionRight = sectionRightGridBuilder.getPanel();
    sectionRight.add(new Heading1Panel(sectionRight.newChildId(), I18nHelper.getLocalizedMessage("vacation.isSpecial")));
    appendFieldset(sectionRightGridBuilder, "vacation.isSpecialPlaned",
        String.valueOf(vacationService.getSpecialVacationCount(currentEmployee, now.get(Calendar.YEAR), VacationStatus.IN_PROGRESS)));

    appendFieldset(sectionRightGridBuilder, "vacation.isSpecialApproved",
        String.valueOf(vacationService.getSpecialVacationCount(currentEmployee, now.get(Calendar.YEAR), VacationStatus.APPROVED)));

    // bottom list
    GridBuilder sectionBottomGridBuilder = gridBuilder.newSplitPanel(GridSize.COL100);
    DivPanel sectionBottom = sectionBottomGridBuilder.getPanel();
    sectionBottom.add(new Heading3Panel(sectionBottom.newChildId(),
        I18nHelper.getLocalizedMessage("vacation.title.list") + " " + now.get(Calendar.YEAR)));
    if (showAddButton) {
      LinkPanel addLink = new LinkPanel(sectionBottom.newChildId(), I18nHelper.getLocalizedMessage("add"), VacationEditPage.class, returnToPage);
      addLink.addLinkAttribute("class", "btn btn-sm btn-success bottom-xs-gap");
      sectionBottom.add(addLink);
    }
    TablePanel tablePanel = new TablePanel(sectionBottom.newChildId());
    sectionBottom.add(tablePanel);
    final DataTable<VacationDO, String> dataTable = createDataTable(createColumns(returnToPage), "startDate", SortOrder.ASCENDING,
        currentEmployee);
    tablePanel.add(dataTable);
  }

  private DataTable<VacationDO, String> createDataTable(final List<IColumn<VacationDO, String>> columns,
      final String sortProperty, final SortOrder sortOrder, final EmployeeDO employee)
  {
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    return new DefaultDataTable<VacationDO, String>(TablePanel.TABLE_ID, columns,
        createSortableDataProvider(sortParam, employee), 50);
  }

  private ISortableDataProvider<VacationDO, String> createSortableDataProvider(final SortParam<String> sortParam,
      EmployeeDO employee)
  {
    return new VacationViewPageSortableDataProvider<VacationDO>(sortParam, vacationService, employee);
  }

  private List<IColumn<VacationDO, String>> createColumns(WebPage returnToPage)
  {
    final List<IColumn<VacationDO, String>> columns = new ArrayList<IColumn<VacationDO, String>>();

    final CellItemListener<VacationDO> cellItemListener = new CellItemListener<VacationDO>()
    {
      private static final long serialVersionUID = 1L;

      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        //Nothing to do here
      }
    };
    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "startDate", "startDate", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO vacation = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, VacationEditPage.class, vacation.getId(),
            returnToPage, DateTimeFormatter.instance().getFormattedDate(vacation.getStartDate())));
        cellItemListener.populateItem(item, componentId, rowModel);
        final Item<?> row = (item.findParent(Item.class));
        WicketUtils.addRowClick(row);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<>(VacationDO.class, "endDate", "endDate", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(VacationDO.class, "status", "status", cellItemListener));
    columns.add(new CellItemListenerLambdaColumn<>(new ResourceModel("vacation.workingdays"),
        rowModel -> vacationService.getVacationDays(rowModel.getObject()),
        cellItemListener)
    );

    columns.add(new CellItemListenerPropertyColumn<VacationDO>(VacationDO.class, "isSpecial", "isSpecial", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<VacationDO>> item, final String componentId,
          final IModel<VacationDO> rowModel)
      {
        final VacationDO vacation = rowModel.getObject();
        if (vacation.getIsSpecial() != null && vacation.getIsSpecial() == Boolean.TRUE) {
          item.add(new TextPanel(componentId, I18nHelper.getLocalizedMessage("yes")));
        } else {
          item.add(new TextPanel(componentId, I18nHelper.getLocalizedMessage("no")));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    return columns;
  }

  private boolean appendFieldset(GridBuilder gridBuilder, final String label, final String value, final String... labelParameters)
  {
    if (StringUtils.isBlank(value) == true) {
      return false;
    }
    final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage(label, (Object[]) labelParameters)).suppressLabelForWarning();
    DivTextPanel divTextPanel = new DivTextPanel(fs.newChildId(), value);
    WebMarkupContainer fieldset = fs.getFieldset();
    fieldset.add(AttributeAppender.append("class", "vacationPanel"));
    if (label.contains("vacation.subtotal") || label.contains("vacation.availablevacation")) {
      WebMarkupContainer fieldsetLabel = (WebMarkupContainer) fieldset.get("label");
      WebMarkupContainer fieldsetControls = (WebMarkupContainer) fieldset.get("controls");
      fieldsetLabel.add(AttributeModifier.replace("class", "control-label-bold"));
      fieldsetControls.add(AttributeModifier.replace("class", "controls-bold"));
    }
    fs.add(divTextPanel);
    return true;
  }

}
