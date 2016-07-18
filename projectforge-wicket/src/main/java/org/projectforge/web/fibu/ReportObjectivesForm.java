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

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

public class ReportObjectivesForm extends AbstractStandardForm<ReportObjectivesFilter, ReportObjectivesPage>
{
  private static final long serialVersionUID = -2262096357903710703L;

  private static final String KEY_REPORT_FILTER = "ReportObjectivesForm:filter";

  protected FileUploadField fileUploadField;

  protected ReportObjectivesFilter filter;

  private ReportObjectivesPanel reportObjectivesPanel;

  private DatePanel fromDatePanel, toDatePanel;

  private final FormComponent< ? >[] dependentFormComponents = new FormComponent[2];

  public ReportObjectivesForm(final ReportObjectivesPage parentPage)
  {
    super(parentPage);
    initUpload(Bytes.megabytes(10));
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    add(new IFormValidator() {
      @Override
      public FormComponent< ? >[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(final Form< ? > form)
      {
        final Date fromDate = fromDatePanel.getConvertedInput();
        final Date toDate = toDatePanel.getConvertedInput();
        if (toDate != null && fromDate != null && fromDate.after(toDate) == true) {
          toDatePanel.getDateField().error(getString("fibu.buchungssatz.error.invalidTimeperiod"));
        }
      }
    });
    filter = getFilter();
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = new FieldsetPanel(gridBuilder.getPanel(), getString("file"), "*.xml") {
        @Override
        public boolean isVisible()
        {
          return parentPage.getReportStorage() == null;
        }
      };
      fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
      fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("import")) {
        @Override
        public final void onSubmit()
        {
          parentPage.importReportObjectivs();
        }
      }, getString("import"), SingleButtonPanel.NORMAL));
    }
    {
      final FieldsetPanel fs = new FieldsetPanel(gridBuilder.getPanel(), getString("timePeriod")) {
        @Override
        public boolean isVisible()
        {
          return reportObjectivesPanel.isVisible();
        }
      };
      fs.add(fromDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(filter, "fromDate"), DatePanelSettings.get()
          .withRequired(true)));
      dependentFormComponents[0] = fromDatePanel;
      fs.add(new DivTextPanel(fs.newChildId(), " - "));
      fs.add(toDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(filter, "toDate"), DatePanelSettings.get()));
      dependentFormComponents[1] = toDatePanel;
      final Button createReportButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("createReport")) {
        @Override
        public final void onSubmit()
        {
          parentPage.createReport();
        }
      };
      setDefaultButton(createReportButton);
      fs.add(new SingleButtonPanel(fs.newChildId(), createReportButton, getString("fibu.kost.reporting.createReport"),
          SingleButtonPanel.DEFAULT_SUBMIT));
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("clear")) {
        @Override
        public final void onSubmit()
        {
          parentPage.clear();
        }
      }, getString("fibu.kost.reporting.clearStorage"), SingleButtonPanel.RESET));
    }
    final DivPanel panel = gridBuilder.getPanel();
    panel.add(reportObjectivesPanel = new ReportObjectivesPanel(panel.newChildId(), parentPage));
  }

  protected ReportObjectivesFilter getFilter()
  {
    if (filter != null) {
      return filter;
    }
    filter = (ReportObjectivesFilter) parentPage.getUserPrefEntry(KEY_REPORT_FILTER);
    if (filter != null) {
      return filter;
    }
    filter = new ReportObjectivesFilter();
    final DateHolder day = new DateHolder();
    day.setBeginOfYear();
    filter.setFromDate(day.getDate());
    day.setEndOfYear();
    filter.setToDate(day.getDate());
    parentPage.putUserPrefEntry(KEY_REPORT_FILTER, filter, true);
    return filter;
  }
}
