/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.liquidityplanning;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.fibu.*;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * The controller of the list page. Most functionality such as search etc. is done by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@ListPage(editPage = LiquidityEntryEditPage.class)
public class LiquidityEntryListPage
    extends AbstractListPage<LiquidityEntryListForm, LiquidityEntryDao, LiquidityEntryDO> implements
    IListPageColumnsCreator<LiquidityEntryDO>
{
  private static final long serialVersionUID = 9158903150132480532L;

  @SpringBean
  private LiquidityEntryDao liquidityEntryDao;

  @SpringBean
  private RechnungDao rechnungDao;

  @SpringBean
  private EingangsrechnungDao eingangsrechnungDao;

  @SpringBean
  private LiquidityForecast forecast;

  private LiquidityEntriesStatistics statistics;

  public LiquidityEntryListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.liquidityplanning.entry");
  }

  LiquidityEntriesStatistics getStatistics()
  {
    if (statistics == null) {
      statistics = liquidityEntryDao.buildStatistics(getList());
    }
    return statistics;
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<LiquidityEntryDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<LiquidityEntryDO, String>> columns = new ArrayList<>();
    final Date today = new DayHolder().getDate();
    final CellItemListener<LiquidityEntryDO> cellItemListener = new CellItemListener<LiquidityEntryDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<LiquidityEntryDO>> item, final String componentId,
          final IModel<LiquidityEntryDO> rowModel)
      {
        final LiquidityEntryDO liquidityEntry = rowModel.getObject();
        appendCssClasses(item, liquidityEntry.getId(), liquidityEntry.isDeleted());
        if (liquidityEntry.isDeleted()) {
          // Do nothing further
        } else {
          if (!liquidityEntry.getPaid()) {
            if (liquidityEntry.getDateOfPayment() == null || liquidityEntry.getDateOfPayment().before(today)) {
              appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
            } else {
              appendCssClasses(item, RowCssClass.BLUE);
            }
          }
        }
      }
    };

    columns.add(new CellItemListenerPropertyColumn<LiquidityEntryDO>(LiquidityEntryDO.class,
        getSortable("dateOfPayment", sortable),
        "dateOfPayment", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<LiquidityEntryDO>> item, final String componentId,
          final IModel<LiquidityEntryDO> rowModel)
      {
        final LiquidityEntryDO liquidityEntry = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, LiquidityEntryEditPage.class, liquidityEntry.getId(),
            returnToPage,
            DateTimeFormatter.instance().getFormattedDate(liquidityEntry.getDateOfPayment())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(
        new CurrencyPropertyColumn<>(LiquidityEntryDO.class, getSortable("amount", sortable), "amount",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<LiquidityEntryDO>(LiquidityEntryDO.class,
        getSortable("paid", sortable), "paid",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<LiquidityEntryDO>> item, final String componentId,
          final IModel<LiquidityEntryDO> rowModel)
      {
        final LiquidityEntryDO entry = rowModel.getObject();
        if (entry.getPaid()) {
          item.add(new IconPanel(componentId, IconType.ACCEPT));
        } else {
          item.add(createInvisibleDummyComponent(componentId));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<>(LiquidityEntryDO.class,
        getSortable("subject", sortable), "subject",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(LiquidityEntryDO.class,
        getSortable("comment", sortable), "comment",
        cellItemListener));

    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "dateOfPayment", SortOrder.ASCENDING);
    form.add(dataTable);
    @SuppressWarnings("serial")
    final ContentMenuEntryPanel liquidityForecastButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Object>("link")
        {
          @Override
          public void onClick()
          {
            final LiquidityForecastPage page = new LiquidityForecastPage(new PageParameters())
                .setForecast(getForecast());
            page.setReturnToPage(LiquidityEntryListPage.this);
            setResponsePage(page);
          }

        }, getString("plugins.liquidityplanning.forecast"));
    addContentMenuEntry(liquidityForecastButton);
    addExcelExport("liquidity", getString("plugins.liquidityplanning.entry.title.heading"));
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#createExcelExporter(java.lang.String)
   */
  @Override
  protected DOListExcelExporter createExcelExporter(final String filenameIdentifier)
  {
    return new DOListExcelExporter("liquidity")
    {
      /**
       * @see org.projectforge.export.DOListExcelExporter#putFieldFormat(ContentProvider,
       *      java.lang.reflect.Field, org.projectforge.common.anots.PropertyInfo, ExportColumn)
       */
      @Override
      public void putFieldFormat(final ContentProvider sheetProvider, final Field field, final PropertyInfo propInfo,
          final ExportColumn exportColumn)
      {
        super.putFieldFormat(sheetProvider, field, propInfo, exportColumn);
        if ("dateOfPayment".equals(field.getName())) {
          exportColumn.setWidth(12);
        } else if ("paid".equals(field.getName())) {
          exportColumn.setWidth(8);
        } else if ("subject".equals(field.getName())) {
          exportColumn.setWidth(40);
        } else if ("comment".equals(field.getName())) {
          exportColumn.setWidth(80);
        }
      }

      /**
       * @see org.projectforge.export.DOListExcelExporter#onBeforeExcelDownload(org.projectforge.export.MyExcelExporter)
       */
      @Override
      public void onBeforeDownload()
      {
        final InvoicesExcelExport invoicesExport = new InvoicesExcelExport();
        forecast = getForecast();
        final LiquidityForecastCashFlow cashFlow = new LiquidityForecastCashFlow(forecast);
        cashFlow.addAsExcelSheet(this, getString("plugins.liquidityplanning.forecast.cashflow"));
        final ExportSheet sheet = addSheet(getString("filter.all"));
        addList(sheet, forecast.getEntries());
        sheet.getPoiSheet().setAutoFilter(org.apache.poi.ss.util.CellRangeAddress.valueOf("A1:F1"));
        invoicesExport.addDebitorInvoicesSheet(this, getString("fibu.rechnungen"), forecast.getInvoices());
        invoicesExport.addCreditorInvoicesSheet(this, getString("fibu.eingangsrechnungen"),
            forecast.getCreditorInvoices());
      }
    };
  }

  /**
   * Calculates expected dates of payments inside the last year (-365 days).
   */
  private LiquidityForecast getForecast()
  {
    // Consider only invoices of the last year:
    final java.sql.Date fromDate = new DayHolder().add(Calendar.DAY_OF_YEAR, -365).getSQLDate();
    {
      final List<RechnungDO> paidInvoices = rechnungDao.getList(new RechnungFilter().setShowBezahlt().setFromDate(fromDate));
      forecast.calculateExpectedTimeOfPayments(paidInvoices);

      final List<RechnungDO> invoices = rechnungDao.getList(new RechnungFilter().setShowUnbezahlt());
      forecast.setInvoices(invoices);
    }
    {
      final List<EingangsrechnungDO> paidInvoices = eingangsrechnungDao.getList(new RechnungFilter().setShowBezahlt().setFromDate(fromDate));
      forecast.calculateExpectedTimeOfCreditorPayments(paidInvoices);

      final List<EingangsrechnungDO> creditorInvoices = eingangsrechnungDao.getList(new RechnungFilter().setListType(RechnungFilter.FILTER_UNBEZAHLT));
      forecast.setCreditorInvoices(creditorInvoices);
    }

    final List<LiquidityEntryDO> list = liquidityEntryDao.getList(new LiquidityFilter().setPaymentStatus(PaymentStatus.UNPAID));
    forecast.set(list);

    forecast.build();
    return forecast;
  }

  /**
   * Forces the statistics to be reloaded.
   *
   * @see org.projectforge.web.wicket.AbstractListPage#refresh()
   */
  @Override
  public void refresh()
  {
    super.refresh();
    this.statistics = null;
  }

  @Override
  protected LiquidityEntryListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new LiquidityEntryListForm(this);
  }

  @Override
  public LiquidityEntryDao getBaseDao()
  {
    return liquidityEntryDao;
  }

  protected LiquidityEntryDao getLiquidityEntryDao()
  {
    return liquidityEntryDao;
  }
}
