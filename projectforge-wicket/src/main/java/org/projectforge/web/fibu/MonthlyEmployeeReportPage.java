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

package org.projectforge.web.fibu;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.MonthlyEmployeeReport;
import org.projectforge.business.fibu.MonthlyEmployeeReport.Kost2Row;
import org.projectforge.business.fibu.MonthlyEmployeeReportDao;
import org.projectforge.business.fibu.MonthlyEmployeeReportEntry;
import org.projectforge.business.fibu.MonthlyEmployeeReportWeek;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.renderer.PdfRenderer;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.timesheet.TimesheetListPage;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.TextStyle;

public class MonthlyEmployeeReportPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -136398850032685654L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MonthlyEmployeeReportPage.class);

  private static final String USER_PREF_KEY_FILTER = "monthlyEmployeeReportFilter";

  @SpringBean
  private UserDao userDao;

  private final MonthlyEmployeeReportForm form;

  private MonthlyEmployeeReport report;

  private WebMarkupContainer table;

  @SpringBean
  private MonthlyEmployeeReportDao monthlyEmployeeReportDao;

  @SpringBean
  private PdfRenderer pdfRenderer;

  @SpringBean
  private Kost1Dao kost1Dao;

  @SpringBean
  private DateTimeFormatter dateTimeFormatter;

  private final GridBuilder gridBuilder;

  @SuppressWarnings("serial")
  public MonthlyEmployeeReportPage(final PageParameters parameters)
  {
    super(parameters);
    final boolean costConfigured = Configuration.getInstance().isCostConfigured();
    form = new MonthlyEmployeeReportForm(this);
    if (form.filter == null) {
      form.filter = (MonthlyEmployeeReportFilter) getUserPrefEntry(MonthlyEmployeeReportFilter.class,
          USER_PREF_KEY_FILTER);
    }
    if (form.filter == null) {
      form.filter = new MonthlyEmployeeReportFilter();
      putUserPrefEntry(USER_PREF_KEY_FILTER, form.filter, true);
    }
    if (form.filter.getUser() == null) {
      form.filter.setUser(getUser());
    }
    body.add(form);
    form.init();
    {
      final ContentMenuEntryPanel exportAsPdf = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new SubmitLink("link", form)
          {
            @Override
            public void onSubmit()
            {
              exportAsPdf();
            }
          }, getString("exportAsPdf"));
      addContentMenuEntry(exportAsPdf);
    }
    gridBuilder = form.newGridBuilder(body, "fields");
    final GridSize gridSize = costConfigured == true ? GridSize.COL33 : GridSize.COL50;
    gridBuilder.newSplitPanel(gridSize);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timesheet.user")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          final PFUserDO user = form.filter.getUser();
          return user != null ? user.getFullname() : "";
        }
      }));
    }
    if (costConfigured == true) {
      gridBuilder.newSplitPanel(gridSize);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost1")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          if (report == null) {
            return "";
          }
          final Kost1DO kost1 = kost1Dao.internalGetById(report.getKost1Id());
          return kost1 != null ? KostFormatter.format(kost1) : "";
        }
      }));
    }
    gridBuilder.newSplitPanel(gridSize);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.common.workingDays")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return report != null ? String.valueOf(report.getNumberOfWorkingDays()) : "";
        }
      }));
    }
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = new FieldsetPanel(gridBuilder.getPanel(), getString("fibu.common.workingDays"),
          getString("fibu.monthlyEmployeeReport.withoutTimesheets"))
      {
        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          return report != null && StringUtils.isNotBlank(report.getFormattedUnbookedDays());
        }
      }.suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          return report.getFormattedUnbookedDays();
        }
      }, TextStyle.RED));
    }
  }

  @Override
  public void onBeforeRender()
  {
    if (table != null) {
      body.remove(table);
    }
    body.add(table = new WebMarkupContainer("table"));
    report = monthlyEmployeeReportDao.getReport(form.filter.getYear(), form.filter.getMonth(), form.filter.getUser());
    if (report == null) {
      table.setVisible(false);
    } else {
      addReport();
    }
    super.onBeforeRender();
  }

  private void addReport()
  {
    final RepeatingView headcolRepeater = new RepeatingView("headcolRepeater");
    table.add(headcolRepeater);
    if (MapUtils.isEmpty(report.getKost2Rows()) == false) {
      headcolRepeater.add(new Label(headcolRepeater.newChildId(), getString("fibu.kost2")));
      headcolRepeater.add(new Label(headcolRepeater.newChildId(), getString("fibu.kunde")));
      headcolRepeater.add(new Label(headcolRepeater.newChildId(), getString("fibu.projekt")));
      headcolRepeater.add(new Label(headcolRepeater.newChildId(), getString("fibu.kost2.art")));
    } else {
      // No kost 2 entries, so only task as head is useful.
      headcolRepeater.add(
          new Label(headcolRepeater.newChildId(), getString("task")).add(AttributeModifier.replace("colspan", "4")));
    }
    final RepeatingView headcolWeekRepeater = new RepeatingView("headcolWeekRepeater");
    table.add(headcolWeekRepeater);
    for (final MonthlyEmployeeReportWeek week : report.getWeeks()) {
      headcolWeekRepeater.add(new Label(headcolWeekRepeater.newChildId(), week.getFormattedFromDayOfMonth()
          + ".-"
          + week.getFormattedToDayOfMonth()
          + "."));
    }
    final RepeatingView rowRepeater = new RepeatingView("rowRepeater");
    table.add(rowRepeater);
    int rowCounter = 0;
    for (final Map.Entry<String, Kost2Row> rowEntry : report.getKost2Rows().entrySet()) {
      final WebMarkupContainer row = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(row);
      if (rowCounter++ % 2 == 0) {
        row.add(AttributeModifier.replace("class", "even"));
      } else {
        row.add(AttributeModifier.replace("class", "odd"));
      }
      final Kost2Row kost2Row = rowEntry.getValue();
      final Kost2DO cost2 = kost2Row.getKost2();
      addLabelCols(row, cost2, null, "kost2.nummer:" + cost2.getFormattedNumber(), report.getUser(),
          report.getFromDate().getTime(), report
              .getToDate().getTime());
      final RepeatingView colWeekRepeater = new RepeatingView("colWeekRepeater");
      row.add(colWeekRepeater);
      for (final MonthlyEmployeeReportWeek week : report.getWeeks()) {
        final MonthlyEmployeeReportEntry entry = week.getKost2Entries().get(kost2Row.getKost2().getId());
        colWeekRepeater.add(new Label(colWeekRepeater.newChildId(), entry != null ? entry.getFormattedDuration() : ""));
      }
      row.add(new Label("sum", report.getKost2Durations().get(cost2.getId()).getFormattedDuration()));
    }

    for (final Map.Entry<String, TaskDO> rowEntry : report.getTaskEntries().entrySet()) {
      final WebMarkupContainer row = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(row);
      if (rowCounter++ % 2 == 0) {
        row.add(AttributeModifier.replace("class", "even"));
      } else {
        row.add(AttributeModifier.replace("class", "odd"));
      }
      final TaskDO task = rowEntry.getValue();
      addLabelCols(row, null, task, null, report.getUser(), report.getFromDate().getTime(),
          report.getToDate().getTime());
      final RepeatingView colWeekRepeater = new RepeatingView("colWeekRepeater");
      row.add(colWeekRepeater);
      for (final MonthlyEmployeeReportWeek week : report.getWeeks()) {
        final MonthlyEmployeeReportEntry entry = week.getTaskEntries().get(task.getId());
        colWeekRepeater.add(new Label(colWeekRepeater.newChildId(), entry != null ? entry.getFormattedDuration() : ""));
      }
      row.add(new Label("sum", report.getTaskDurations().get(task.getId()).getFormattedDuration()));
    }
    {
      // Sum row.
      final WebMarkupContainer row = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(row);
      if (rowCounter++ % 2 == 0) {
        row.add(AttributeModifier.replace("class", "even"));
      } else {
        row.add(AttributeModifier.replace("class", "odd"));
      }
      addLabelCols(row, null, null, null, report.getUser(), report.getFromDate().getTime(),
          report.getToDate().getTime()).add(
          AttributeModifier.replace("style", "text-align: right;"));
      final RepeatingView colWeekRepeater = new RepeatingView("colWeekRepeater");
      row.add(colWeekRepeater);
      for (final MonthlyEmployeeReportWeek week : report.getWeeks()) {
        colWeekRepeater.add(new Label(colWeekRepeater.newChildId(), week.getFormattedTotalDuration()));
      }
      row.add(new Label("sum", report.getFormattedTotalNetDuration()).add(AttributeModifier.replace("style",
          "font-weight: bold; color:red; text-align: right;")));
    }
    if (report.getTotalGrossDuration() != report.getTotalNetDuration()) {
      // Net sum row.
      final WebMarkupContainer row = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(row);
      if (rowCounter++ % 2 == 0) {
        row.add(AttributeModifier.replace("class", "even"));
      } else {
        row.add(AttributeModifier.replace("class", "odd"));
      }
      final Component comp = new WebMarkupContainer("cost2").setVisible(false);
      row.add(comp);
      row.add(new Label("customer", "").setVisible(false));
      row.add(new Label("project", "").setVisible(false));
      final Label title = addCostType(row, getString("fibu.monthlyEmployeeReport.totalSum"));
      WicketUtils.addTooltip(title, new ResourceModel("fibu.monthlyEmployeeReport.totalSum.tooltip"));
      final WebMarkupContainer tdContainer = title.findParent(WebMarkupContainer.class);
      tdContainer.add(AttributeModifier.replace("colspan", "4"));
      tdContainer.add(AttributeModifier.replace("style", "font-weight: bold; text-align: right;"));
      final RepeatingView colWeekRepeater = new RepeatingView("colWeekRepeater");
      row.add(colWeekRepeater);
      for (@SuppressWarnings("unused")
      final MonthlyEmployeeReportWeek week : report.getWeeks()) {
        colWeekRepeater.add(new Label(colWeekRepeater.newChildId(), ""));
      }
      row.add(new Label("sum", report.getFormattedTotalGrossDuration()).add(AttributeModifier.replace("style",
          "font-weight: bold; text-align: right;")));
    }
  }

  @SuppressWarnings("serial")
  private WebMarkupContainer addLabelCols(final WebMarkupContainer row, final Kost2DO cost2, final TaskDO task,
      final String searchString,
      final PFUserDO user, final long startTime, final long stopTime)
  {
    final WebMarkupContainer result = new WebMarkupContainer("cost2");
    row.add(result);
    final Link<String> link = new Link<String>("link")
    {
      @Override
      public void onClick()
      {
        final PageParameters params = new PageParameters();
        params.add("userId", user.getId());
        if (task != null) {
          params.add("taskId", task.getId());
        }
        params.add("startTime", startTime);
        params.add("stopTime", stopTime);
        params.add("storeFilter", false);
        if (searchString != null) {
          params.add("searchString", searchString);
        }
        setResponsePage(new TimesheetListPage(params));
      }
    };
    result.add(link);
    WicketUtils.addRowClick(row);
    if (cost2 != null) {
      final ProjektDO project = cost2.getProjekt();
      final KundeDO customer = project != null ? project.getKunde() : null;
      final Kost2ArtDO costType = cost2.getKost2Art();
      link.add(new Label("label", KostFormatter.format(cost2)));
      if (project != null) {
        row.add(new Label("customer", customer != null ? customer.getName() : ""));
        row.add(new Label("project", project.getName()));
      } else {
        row.add(new Label("customer", cost2.getDescription()).add(AttributeModifier.replace("colspan", "2")));
        row.add(new Label("project", "").setVisible(false));
      }
      addCostType(row, costType.getName());
    } else {
      if (task != null) {
        // Entries for one task (not cost2).
        link.add(new Label("label", WicketTaskFormatter.getTaskPath(task.getId(), true, OutputType.PLAIN)));
      } else {
        link.add(new Label("label", getString("sum")));
      }
      result.add(AttributeModifier.replace("colspan", "4"));
      row.add(new Label("customer", "").setVisible(false));
      row.add(new Label("project", "").setVisible(false));
      addCostType(row, null);
    }
    return result;
  }

  private Label addCostType(final WebMarkupContainer row, final String content)
  {
    final WebMarkupContainer costTypeContainer = new WebMarkupContainer("costType");
    row.add(costTypeContainer);
    if (content == null) {
      costTypeContainer.setVisible(false);
      return null;
    } else {
      final Label label = new Label("content", content);
      costTypeContainer.add(label);
      return label;
    }
  }

  protected void exportAsPdf()
  {
    log.info(
        "Monthly employee report for " + form.filter.getUser().getFullname() + ": " + form.filter.getFormattedMonth());
    final StringBuffer buf = new StringBuffer();
    buf.append(getString("menu.monthlyEmployeeReport.fileprefix")).append("_");
    final PFUserDO employee = userDao.getById(form.filter.getUserId());
    buf.append(employee.getLastname()).append("_").append(form.filter.getYear()).append("-")
        .append(form.filter.getFormattedMonth())
        .append(".pdf");
    final String filename = buf.toString();

    // get the sheets of the given format
    final String styleSheet = "fo-styles/monthlyEmployeeReport-template-fo.xsl";
    final String xmlData = "fo-styles/monthlyEmployeeReport2pdf.xml";

    report = monthlyEmployeeReportDao.getReport(form.filter.getYear(), form.filter.getMonth(), employee);
    final Map<String, Object> data = new HashMap<String, Object>();
    data.put("systemDate", dateTimeFormatter.getFormattedDateTime(new Date()));
    data.put("title", getString("menu.monthlyEmployeeReport"));
    data.put("employeeLabel", getString("timesheet.user"));
    data.put("employee", employee.getFullname());
    data.put("monthLabel", getString("calendar.month"));
    data.put("year", form.filter.getYear());
    data.put("month", form.filter.getFormattedMonth());
    data.put("workingDaysLabel", getString("fibu.common.workingDays"));
    data.put("workingDays", report.getNumberOfWorkingDays());
    data.put("kost1Label", getString("fibu.kost1"));
    final Kost1DO kost1 = kost1Dao.internalGetById(report.getKost1Id());
    data.put("kost1", kost1 != null ? kost1.getFormattedNumber() : "--");
    data.put("kost2Label", getString("fibu.kost2"));
    data.put("kundeLabel", getString("fibu.kunde"));
    data.put("projektLabel", getString("fibu.projekt"));
    data.put("kost2ArtLabel", getString("fibu.kost2.art"));
    data.put("sumLabel", getString("sum"));
    data.put("netSumLabel", getString("sum"));
    data.put("totalSumLabel", getString("fibu.monthlyEmployeeReport.totalSum"));
    data.put("report", report);
    data.put("signatureEmployeeLabel", getString("timesheet.signatureEmployee") + ": " + employee.getFullname());
    data.put("signatureProjectLeaderLabel", getString("timesheet.signatureProjectLeader"));
    data.put("unbookedWorkingDaysLabel", getString("fibu.monthlyEmployeeReport.withoutTimesheets"));
    // render the PDF with fop
    final byte[] ba = pdfRenderer.render(styleSheet, xmlData, data);
    DownloadUtils.setDownloadTarget(ba, filename);
  }

  @Override
  protected String getTitle()
  {
    return getString("menu.monthlyEmployeeReport");
  }

  @Override
  public void cancelSelection(final String property)
  {
    log.error("cancelSelection not supported. Property was '" + property + "'.");
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("user".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      form.filter.setUser(getTenantRegistry().getUserGroupCache().getUser(id));
    } else if ("quickSelect".equals(property) == true) {
      final Date date = (Date) selectedValue;
      form.setDate(date);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    log.error("unselect not supported. Property was '" + property + "'.");
  }
}
