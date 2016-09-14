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

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.business.fibu.kost.BusinessAssessmentRow;
import org.projectforge.business.fibu.kost.BusinessAssessmentTable;
import org.projectforge.business.fibu.kost.reporting.Report;
import org.projectforge.business.fibu.kost.reporting.ReportStorage;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.utils.LabelValueBean;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.web.wicket.components.PlainLabel;

public class ReportObjectivesPanel extends Panel
{
  private static final long serialVersionUID = -77908336.4.0-SNAPSHOT2066719L;

  protected Priority priority = Priority.HIGH;

  ReportObjectivesPage parentPage;

  Report currentReport, rootReport;

  RepeatingView actionLinkRepeater, childHeadColRepeater, rowRepeater;

  WebMarkupContainer path;

  @SuppressWarnings("serial")
  public ReportObjectivesPanel(final String id, final ReportObjectivesPage parentPage)
  {
    super(id);
    this.parentPage = parentPage;
    add(new Label("title", new Model<String>() {
      @Override
      public String getObject()
      {
        return currentReport != null ? currentReport.getId() + " - " + currentReport.getTitle() + ": " + currentReport.getZeitraum() + " (beta)": "(beta)";
      }
    }));
    add(path = new WebMarkupContainer("path"));
    path.add(actionLinkRepeater = new RepeatingView("actionLinkRepeater"));
    path.add(new PlainLabel("reportId", new Model<String>() {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        return currentReport != null ? currentReport.getId() : "";
      }
    }));
    add(new PlainLabel("reportObjectiveId", new Model<String>() {
      @Override
      public String getObject()
      {
        return currentReport != null ? currentReport.getReportObjective().getId() : "";
      }
    }));
    add(childHeadColRepeater = new RepeatingView("childHeadColRepeater"));
    add(new SubmitLink("showAccountingRecordsLink") {
      @Override
      public void onSubmit()
      {
        setResponsePage(new AccountingRecordListPage(AccountingRecordListPage.getPageParameters(currentReport.getId())));
      }
    });
    add(rowRepeater = new RepeatingView("rowRepeater"));
  }

  /**
   * @see org.apache.wicket.Component#isVisible()
   */
  @Override
  public boolean isVisible()
  {
    return parentPage.getReportStorage() != null;
  }

  @SuppressWarnings("serial")
  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    final ReportStorage reportStorage = parentPage.getReportStorage();
    if (reportStorage == null) {
      currentReport = rootReport = null;
      // Nothing to be done.
      return;
    }
    currentReport = reportStorage.getCurrentReport();
    rootReport = reportStorage.getRoot();
    if (currentReport != rootReport) {
      path.setVisible(true);
      actionLinkRepeater.removeAll();
      for (final Report ancestorReport : currentReport.getPath()) {
        final WebMarkupContainer actionLinkContainer = new WebMarkupContainer(actionLinkRepeater.newChildId());
        actionLinkRepeater.add(actionLinkContainer);
        actionLinkContainer.add(createReportLink("actionLink", reportStorage, ancestorReport.getId()));
      }
    } else {
      path.setVisible(false);
    }
    final List<Report> childs = currentReport.getChilds();
    childHeadColRepeater.removeAll();
    if (CollectionUtils.isNotEmpty(childs) == true) {
      for (final Report childReport : childs) {
        final WebMarkupContainer item = new WebMarkupContainer(childHeadColRepeater.newChildId());
        childHeadColRepeater.add(item);
        if (childReport.hasChilds() == true) {
          item.add(createReportLink("actionLink", reportStorage, childReport.getId()));
          item.add(new Label("childId", "[invisible]").setVisible(false));
        } else {
          item.add(new Label("actionLink", "[invisible]").setVisible(false));
          item.add(new PlainLabel("childId", childReport.getId()));
        }
        item.add(new SubmitLink("showAccountingRecordsLink") {
          @Override
          public void onSubmit()
          {
            setResponsePage(new AccountingRecordListPage(AccountingRecordListPage.getPageParameters(childReport.getId())));
          }
        });
      }
    }
    rowRepeater.removeAll();
    int row = 0;
    final BusinessAssessmentTable businessAssessmentTable = currentReport.getChildBusinessAssessmentTable(true);
    final BusinessAssessment firstBusinessAssessment = businessAssessmentTable.getBusinessAssessmentList().get(0).getValue();
    for (final BusinessAssessmentRow firstBusinessAssessmentRow : firstBusinessAssessment.getRows()) { // First BusinessAssessment for
      // getting meta data of
      // BusinessAssessment.
      if (priority.ordinal() > firstBusinessAssessmentRow.getPriority().ordinal()) {
        // Don't show all business assessment rows (priority is here a kind of verbose level).
        continue;
      }
      final WebMarkupContainer rowContainer = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(rowContainer);
      rowContainer.add(AttributeModifier.replace("class", (row++ % 2 == 0) ? "even" : "odd"));
      rowContainer.add(new Label("zeileNo", firstBusinessAssessmentRow.getNo()));
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < firstBusinessAssessmentRow.getIndent(); i++) {
        buf.append("&nbsp;&nbsp;");
      }
      buf.append(HtmlHelper.escapeXml(firstBusinessAssessmentRow.getTitle()));
      rowContainer.add(new Label("description", buf.toString()).setEscapeModelStrings(false));
      final RepeatingView cellRepeater = new RepeatingView("cellRepeater");
      rowContainer.add(cellRepeater);
      int col = 0;
      for (final LabelValueBean<String, BusinessAssessment> lv : businessAssessmentTable.getBusinessAssessmentList()) {
        // So display the row for every BusinessAssessment:
        final String reportId = lv.getLabel();
        final BusinessAssessment businessAssessment = lv.getValue();
        final BusinessAssessmentRow businessAssessmentRow = businessAssessment.getRow(firstBusinessAssessmentRow.getId());
        final WebMarkupContainer item = new WebMarkupContainer(cellRepeater.newChildId());
        cellRepeater.add(item);
        buf = new StringBuffer();
        buf.append("text-align: right; white-space: nowrap;");
        if (col++ == 0) {
          buf.append(" font-weight: bold;");
        }
        final BigDecimal amount = businessAssessmentRow.getAmount();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
          buf.append(" color: red;");
        }
        item.add(AttributeModifier.replace("style", buf.toString()));
        item.add(new PlainLabel("bwaWert", NumberHelper.isNotZero(businessAssessmentRow.getAmount()) == true ? CurrencyFormatter
            .format(businessAssessmentRow.getAmount()) : ""));
        item.add(new SubmitLink("showAccountingRecordsLink") {
          @Override
          public void onSubmit()
          {
            setResponsePage(new AccountingRecordListPage(
                AccountingRecordListPage.getPageParameters(reportId, businessAssessmentRow.getNo())));
          }
        });
      }
    }
  }

  @SuppressWarnings("serial")
  private Component createReportLink(final String id, final ReportStorage reportStorage, final String reportId)
  {
    return new SubmitLink(id) {
      @Override
      public void onSubmit()
      {
        parentPage.getReportStorage().setCurrentReport(reportId);
      }
    }.add(new PlainLabel("label", reportId));
  }
}
