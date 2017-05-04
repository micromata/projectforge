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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.AuftragsPositionVO;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KundeFormatter;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungFilter;
import org.projectforge.business.fibu.RechnungsStatistik;
import org.projectforge.business.fibu.kost.KostZuweisungExport;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.excel.ContentProvider;
import org.projectforge.excel.ExportColumn;
import org.projectforge.excel.I18nExportColumn;
import org.projectforge.excel.PropertyMapping;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.AbstractUnsecureBasePage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.CurrencyPropertyColumn;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.RowCssClass;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@ListPage(editPage = RechnungEditPage.class)
public class RechnungListPage extends AbstractListPage<RechnungListForm, RechnungDao, RechnungDO> implements
    IListPageColumnsCreator<RechnungDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RechnungListPage.class);

  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private RechnungDao rechnungDao;

  @SpringBean
  KostZuweisungExport kostZuweisungExport;

  @SpringBean
  KontoCache kontoCache;

  private RechnungsStatistik rechnungsStatistik;

  RechnungsStatistik getRechnungsStatistik()
  {
    if (rechnungsStatistik == null) {
      rechnungsStatistik = rechnungDao.buildStatistik(getList());
    }
    return rechnungsStatistik;
  }

  public RechnungListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.rechnung");
  }

  public RechnungListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.rechnung");
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
    this.rechnungsStatistik = null;
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<RechnungDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<RechnungDO, String>> columns = new ArrayList<IColumn<RechnungDO, String>>();
    final CellItemListener<RechnungDO> cellItemListener = new CellItemListener<RechnungDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<RechnungDO>> item, final String componentId,
          final IModel<RechnungDO> rowModel)
      {
        final RechnungDO rechnung = rowModel.getObject();
        if (rechnung.getStatus() == null) {
          // Should not occur:
          return;
        }
        appendCssClasses(item, rechnung.getId(), rechnung.isDeleted());
        if (rechnung.isDeleted() == true) {
          // Do nothing further
        } else if (rechnung.isUeberfaellig() == true) {
          appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
        } else if (rechnung.isBezahlt() == false) {
          appendCssClasses(item, RowCssClass.BLUE);
        }
      }
    };
    columns.add(new CellItemListenerPropertyColumn<RechnungDO>(
        new Model<String>(getString("fibu.rechnung.nummer.short")), getSortable(
        "nummer", sortable),
        "nummer", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<RechnungDO>> item, final String componentId,
          final IModel<RechnungDO> rowModel)
      {
        final RechnungDO rechnung = rowModel.getObject();
        String nummer = String.valueOf(rechnung.getNummer());
        if (form.getSearchFilter().isShowKostZuweisungStatus() == true) {
          final BigDecimal fehlBetrag = rechnung.getKostZuweisungFehlbetrag();
          if (NumberHelper.isNotZero(fehlBetrag) == true) {
            nummer += " *** " + CurrencyFormatter.format(fehlBetrag) + " ***";
          }
        }
        final Label nummerLabel = new Label(ListSelectActionPanel.LABEL_ID, nummer);
        nummerLabel.setEscapeModelStrings(false);
        item.add(new ListSelectActionPanel(componentId, rowModel, RechnungEditPage.class, rechnung.getId(),
            returnToPage, nummerLabel));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(
        new CellItemListenerPropertyColumn<RechnungDO>(getString("fibu.kunde"), getSortable("kundeAsString", sortable),
            "kundeAsString", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<RechnungDO>(getString("fibu.projekt"), getSortable("projekt.name", sortable),
            "projekt.name", cellItemListener));
    if (kontoCache.isEmpty() == false) {
      columns.add(new CellItemListenerPropertyColumn<RechnungDO>(RechnungDO.class, null, "konto", cellItemListener)
      {
        /**
         * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
         *      java.lang.String, org.apache.wicket.model.IModel)
         */
        @Override
        public void populateItem(final Item<ICellPopulator<RechnungDO>> item, final String componentId,
            final IModel<RechnungDO> rowModel)
        {
          final RechnungDO invoice = rowModel.getObject();
          final KontoDO konto = kontoCache.getKonto(invoice);
          item.add(new Label(componentId, konto != null ? konto.formatKonto() : ""));
          cellItemListener.populateItem(item, componentId, rowModel);
        }
      });
    }
    columns.add(
        new CellItemListenerPropertyColumn<RechnungDO>(RechnungDO.class, getSortable("betreff", sortable), "betreff",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<RechnungDO>(getString("fibu.rechnung.datum.short"),
        getSortable("datum", sortable),
        "datum", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<RechnungDO>(getString("fibu.rechnung.faelligkeit.short"),
        getSortable("faelligkeit",
            sortable),
        "faelligkeit", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<RechnungDO>(getString("fibu.rechnung.bezahlDatum.short"),
        getSortable("bezahlDatum",
            sortable),
        "bezahlDatum", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<>(getString("fibu.periodOfPerformance.from"),
        getSortable("periodOfPerformanceBegin", sortable), "periodOfPerformanceBegin", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<>(getString("fibu.periodOfPerformance.to"),
        getSortable("periodOfPerformanceEnd", sortable), "periodOfPerformanceEnd", cellItemListener));

    columns.add(new CurrencyPropertyColumn<RechnungDO>(getString("fibu.common.netto"), getSortable("netSum", sortable),
        "netSum",
        cellItemListener));
    columns.add(new CurrencyPropertyColumn<RechnungDO>(getString("fibu.common.brutto"),
        getSortable("grossSum", sortable), "grossSum",
        cellItemListener));
    // columns.add(new CurrencyPropertyColumn<RechnungDO>(getString("fibu.rechnung.zahlBetrag.short"), getSortable("zahlBetrag", sortable),
    // "zahlBetrag",
    // cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<RechnungDO>(getString("fibu.auftrag.auftraege"), null, null,
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<RechnungDO>> item, final String componentId,
          final IModel<RechnungDO> rowModel)
      {
        final Set<AuftragsPositionVO> orderPositions = rowModel.getObject().getAuftragsPositionVOs();
        if (CollectionUtils.isEmpty(orderPositions) == true) {
          item.add(AbstractUnsecureBasePage.createInvisibleDummyComponent(componentId));
        } else {
          final OrderPositionsPanel panel = new OrderPositionsPanel(componentId)
          {
            @Override
            protected void onBeforeRender()
            {
              super.onBeforeRender();
              // Lazy initialization because getString(...) of OrderPositionsPanel fails if panel.init(orderPositions) is called directly
              // after instantiation.
              init(orderPositions);
            }
          };
          item.add(panel);
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<RechnungDO>(RechnungDO.class, getSortable("bemerkung", sortable),
        "bemerkung",
        cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<RechnungDO>(RechnungDO.class, getSortable("status", sortable), "status",
            cellItemListener));
    return columns;
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "nummer", SortOrder.DESCENDING);
    form.add(dataTable);
    addExcelExport(getString("fibu.common.debitor"), getString("fibu.rechnungen"));
    if (Configuration.getInstance().isCostConfigured() == true) {
      final ContentMenuEntryPanel exportExcelButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Object>("link")
          {
            @Override
            public void onClick()
            {
              exportExcelWithCostAssignments();
            }
          }, getString("fibu.rechnung.kostExcelExport")).setTooltip(getString("fibu.rechnung.kostExcelExport.tootlip"));
      addContentMenuEntry(exportExcelButton);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#createExcelExporter(java.lang.String)
   */
  @Override
  protected DOListExcelExporter createExcelExporter(final String filenameIdentifier)
  {
    return new DOListExcelExporter(filenameIdentifier)
    {
      /**
       * @see org.projectforge.excel.ExcelExporter#onBeforeSettingColumns(java.util.List)
       */
      @Override
      protected List<ExportColumn> onBeforeSettingColumns(final ContentProvider sheetProvider,
          final List<ExportColumn> columns)
      {
        final List<ExportColumn> sortedColumns = reorderColumns(columns, "nummer", "kunde", "projekt", "konto",
            "betreff", "datum",
            "faelligkeit", "bezahlDatum", "zahlBetrag");
        I18nExportColumn col = new I18nExportColumn("kontoBezeichnung", "fibu.konto.bezeichnung",
            MyXlsContentProvider.LENGTH_STD);
        sortedColumns.add(4, col);
        col = new I18nExportColumn("netSum", "fibu.common.netto");
        putCurrencyFormat(sheetProvider, col);
        sortedColumns.add(9, col);
        col = new I18nExportColumn("grossSum", "fibu.common.brutto");
        putCurrencyFormat(sheetProvider, col);
        sortedColumns.add(10, col);
        return removeColumns(sortedColumns, "kundeText");
      }

      /**
       * @see org.projectforge.excel.ExcelExporter#addMapping(org.projectforge.excel.PropertyMapping, java.lang.Object,
       *      java.lang.reflect.Field)
       */
      @Override
      public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
      {
        if ("kunde".equals(field.getName()) == true) {
          final RechnungDO rechnung = (RechnungDO) entry;
          mapping.add(field.getName(),
              KundeFormatter.formatKundeAsString(rechnung.getKunde(), rechnung.getKundeText()));
        } else if ("konto".equals(field.getName()) == true) {
          Integer kontoNummer = null;
          final KontoDO konto = kontoCache.getKonto((RechnungDO) entry);
          if (konto != null) {
            kontoNummer = konto.getNummer();
          }
          mapping.add(field.getName(), kontoNummer != null ? kontoNummer : "");
        } else {
          super.addMapping(mapping, entry, field);
        }
      }

      /**
       * @see org.projectforge.excel.ExcelExporter#addMappings(org.projectforge.excel.PropertyMapping, java.lang.Object)
       */
      @Override
      protected void addMappings(final PropertyMapping mapping, final Object entry)
      {
        final RechnungDO invoice = (RechnungDO) entry;
        String kontoBezeichnung = null;
        final KontoDO konto = kontoCache.getKonto(invoice);
        if (konto != null) {
          kontoBezeichnung = konto.getBezeichnung();
        }
        mapping.add("kontoBezeichnung", kontoBezeichnung != null ? kontoBezeichnung : "");
        mapping.add("grossSum", invoice.getGrossSum());
        mapping.add("netSum", invoice.getNetSum());
      }
    };
  }

  protected void exportExcelWithCostAssignments()
  {
    refresh();
    final RechnungFilter filter = new RechnungFilter();
    final RechnungFilter src = form.getSearchFilter();
    filter.setYear(src.getYear());
    filter.setMonth(src.getMonth());
    final List<RechnungDO> rechnungen = rechnungDao.getList(filter);
    if (rechnungen == null || rechnungen.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final String filename = "ProjectForge-"
        + getString("fibu.common.debitor")
        + "-"
        + getString("menu.fibu.kost")
        + "_"
        + DateHelper.getDateAsFilenameSuffix(new Date())
        + ".xls";
    final byte[] xls = kostZuweisungExport.exportRechnungen(rechnungen, getString("fibu.common.debitor"),
        kontoCache);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  @Override
  protected RechnungListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new RechnungListForm(this);
  }

  @Override
  public RechnungDao getBaseDao()
  {
    return rechnungDao;
  }
}
