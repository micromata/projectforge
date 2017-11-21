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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BuchungssatzDao;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.business.fibu.kost.BusinessAssessmentRow;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.reporting.Report;
import org.projectforge.business.fibu.kost.reporting.ReportStorage;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.CurrencyPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.springframework.util.CollectionUtils;

@ListPage(editPage = AccountingRecordEditPage.class)
public class AccountingRecordListPage
    extends AbstractListPage<AccountingRecordListForm, BuchungssatzDao, BuchungssatzDO> implements
    IListPageColumnsCreator<BuchungssatzDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AccountingRecordListPage.class);

  private static final long serialVersionUID = -34213362189153025L;

  private static final String PARAM_KEY_REPORT_ID = "reportId";

  private static final String PARAM_KEY_BUSINESS_ASSESSMENT_ROW_ID = "businessAssessmentRowId";

  @SpringBean
  private BuchungssatzDao buchungssatzDao;

  protected BusinessAssessment businessAssessment;

  protected String reportId;

  private String businessAssessmentRowId;

  private Report report;

  /**
   * Gets the page parameters for calling the list page only for displaying accounting records of the given report.
   * 
   * @param reportId The id of the report of the ReportStorage of ReportObjectivesPage.
   */
  public static PageParameters getPageParameters(final String reportId)
  {
    return getPageParameters(reportId, null);
  }

  /**
   * Gets the page parameters for calling the list page only for displaying accounting records of the given report.
   * 
   * @param reportId The id of the report of the ReportStorage of ReportObjectivesPage.
   * @param businessAssessmentRowId Display only records concerning the given row of the business assessment.
   */
  public static PageParameters getPageParameters(final String reportId, final String businessAssessmentRowNo)
  {
    final PageParameters params = new PageParameters();
    params.add(PARAM_KEY_REPORT_ID, reportId);
    if (businessAssessmentRowNo != null) {
      params.add(PARAM_KEY_BUSINESS_ASSESSMENT_ROW_ID, businessAssessmentRowNo);
    }
    return params;
  }

  public AccountingRecordListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.buchungssatz");
    checkAccess();
  }

  private void checkAccess()
  {
    accessChecker.checkLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE);
    accessChecker.checkRestrictedOrDemoUser();
  }

  @Override
  protected void setup()
  {
    reportId = WicketUtils.getAsString(getPageParameters(), PARAM_KEY_REPORT_ID);
    businessAssessmentRowId = WicketUtils.getAsString(getPageParameters(), PARAM_KEY_BUSINESS_ASSESSMENT_ROW_ID);
    if (reportId != null) {
      storeFilter = false;
    }
  }

  @Override
  protected void init()
  {
    final List<IColumn<BuchungssatzDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "formattedSatzNummer", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<BuchungssatzDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<BuchungssatzDO, String>> columns = new ArrayList<IColumn<BuchungssatzDO, String>>();
    final CellItemListener<BuchungssatzDO> cellItemListener = new CellItemListener<BuchungssatzDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<BuchungssatzDO>> item, final String componentId,
          final IModel<BuchungssatzDO> rowModel)
      {
        final BuchungssatzDO satz = rowModel.getObject();
        appendCssClasses(item, satz.getId(), satz.isDeleted());
      }
    };
    columns.add(
        new CellItemListenerPropertyColumn<BuchungssatzDO>(new Model<String>(getString("fibu.buchungssatz.satznr")),
            "formattedSatzNummer", "formattedSatzNummer", cellItemListener)
        {
          @SuppressWarnings({ "unchecked", "rawtypes" })
          @Override
          public void populateItem(final Item item, final String componentId, final IModel rowModel)
          {
            final BuchungssatzDO satz = (BuchungssatzDO) rowModel.getObject();
            item.add(new ListSelectActionPanel(componentId, rowModel, AccountingRecordEditPage.class, satz.getId(),
                AccountingRecordListPage.this, String.valueOf(satz.getFormattedSatzNummer())));
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item);
          }
        });
    columns.add(new CurrencyPropertyColumn<BuchungssatzDO>(getString("fibu.common.betrag"), "betrag", "betrag",
        cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<BuchungssatzDO>(getString("fibu.buchungssatz.beleg"), "beleg", "beleg",
            cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<BuchungssatzDO>(new Model<String>(getString("fibu.kost1")), getSortable(
            "kost1.shortDisplayName", sortable), "kost1.shortDisplayName", cellItemListener)
        {
          @Override
          public String getTooltip(final BuchungssatzDO satz)
          {
            final Kost1DO kost1 = satz != null ? satz.getKost1() : null;
            if (kost1 == null) {
              return null;
            } else {
              return KostFormatter.formatToolTip(kost1);
            }
          }
        });
    columns
        .add(new CellItemListenerPropertyColumn<BuchungssatzDO>(new Model<String>(getString("fibu.kost2")), getSortable(
            "kost2.shortDisplayName", sortable), "kost2.shortDisplayName", cellItemListener)
        {
          @Override
          public String getTooltip(final BuchungssatzDO satz)
          {
            final Kost2DO kost2 = satz != null ? satz.getKost2() : null;
            if (kost2 == null) {
              return null;
            } else {
              return KostFormatter.formatToolTip(kost2);
            }
          }
        });
    columns.add(new CellItemListenerPropertyColumn<BuchungssatzDO>(
        new Model<String>(getString("fibu.buchungssatz.konto")), getSortable(
            "konto.shortDisplayName", sortable),
        "konto.shortDisplayName", cellItemListener)
    {
      @Override
      public String getTooltip(final BuchungssatzDO satz)
      {
        final KontoDO konto = satz != null ? satz.getKonto() : null;
        if (konto == null) {
          return null;
        } else {
          return konto.getBezeichnung();
        }
      }
    });
    columns.add(
        new CellItemListenerPropertyColumn<BuchungssatzDO>(new Model<String>(getString("fibu.buchungssatz.gegenKonto")),
            getSortable("gegenKonto.shortDisplayName", sortable), "gegenKonto.shortDisplayName", cellItemListener)
        {
          @Override
          public String getTooltip(final BuchungssatzDO satz)
          {
            final KontoDO gegenKonto = satz != null ? satz.getGegenKonto() : null;
            if (gegenKonto == null) {
              return null;
            } else {
              return gegenKonto.getBezeichnung();
            }
          }
        });
    columns.add(new CellItemListenerPropertyColumn<BuchungssatzDO>(getString("finance.accountingRecord.dc"), "sh", "sh",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<BuchungssatzDO>(getString("fibu.buchungssatz.text"), "text", "text",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<BuchungssatzDO>(getString("comment"), "comment", "comment",
        cellItemListener));
    return columns;
  }

  @Override
  public void refresh()
  {
    super.refresh();
    form.refresh();
  }

  @Override
  protected AccountingRecordListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new AccountingRecordListForm(this);
  }

  @Override
  public BuchungssatzDao getBaseDao()
  {
    return buchungssatzDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#buildList()
   */
  @Override
  protected List<BuchungssatzDO> buildList()
  {
    List<BuchungssatzDO> list = null;
    if (StringUtils.isNotEmpty(reportId) == true) {
      final ReportStorage reportStorage = (ReportStorage) getUserPrefEntry(ReportObjectivesPage.KEY_REPORT_STORAGE);
      if (reportStorage != null) {
        report = reportStorage.findById(this.reportId);
        if (report != null) {
          if (this.businessAssessmentRowId != null) {
            final BusinessAssessmentRow row = report.getBusinessAssessment().getRow(businessAssessmentRowId);
            if (row != null) {
              list = row.getAccountRecords();
            } else {
              log.info("Business assessment row "
                  + businessAssessmentRowId
                  + " not found for report with id '"
                  + reportId
                  + "' in existing ReportStorage.");
            }
          } else {
            list = report.getBuchungssaetze();
          }
        } else {
          log.info("Report with id '" + reportId + "' not found in existing ReportStorage.");
        }
      } else {
        log.info("Report with id '" + reportId + "' not found. ReportStorage does not exist.");
      }
    } else {
      list = super.buildList();
    }
    if (CollectionUtils.isEmpty(list) == true) {
      this.businessAssessment = null;
    } else {
      this.businessAssessment = new BusinessAssessment(AccountingConfig.getInstance().getBusinessAssessmentConfig());
      this.businessAssessment.setAccountRecords(list);
    }
    return list;
  }

  /**
   * @return the businessAssessment
   */
  BusinessAssessment getBusinessAssessment()
  {
    return businessAssessment;
  }
}
