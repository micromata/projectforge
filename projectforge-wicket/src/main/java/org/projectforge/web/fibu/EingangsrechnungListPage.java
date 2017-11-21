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

<<<<<<< HEAD
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
=======
import org.apache.commons.lang3.StringEscapeUtils;
>>>>>>> develop
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
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExcelExporter;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.I18nExportColumn;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EingangsrechnungDao;
import org.projectforge.business.fibu.EingangsrechnungsStatistik;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.RechnungFilter;
import org.projectforge.business.fibu.kost.KostZuweisungExport;
import org.projectforge.business.fibu.kost.reporting.SEPATransferGenerator;
import org.projectforge.business.fibu.kost.reporting.SEPATransferResult;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.CurrencyPropertyColumn;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.RowCssClass;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;

@ListPage(editPage = EingangsrechnungEditPage.class)
public class EingangsrechnungListPage
    extends AbstractListPage<EingangsrechnungListForm, EingangsrechnungDao, EingangsrechnungDO> implements
    IListPageColumnsCreator<EingangsrechnungDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EingangsrechnungListPage.class);

  private static final long serialVersionUID = 4417254962066648504L;

  @SpringBean
  private EingangsrechnungDao eingangsrechnungDao;

  @SpringBean
  private KostZuweisungExport kostZuweisungExport;

  @SpringBean
  private KontoCache kontoCache;

  @SpringBean
  private SEPATransferGenerator SEPATransferGenerator;

  private EingangsrechnungsStatistik eingangsrechnungsStatistik;

  private ContentMenuEntryPanel exportKostzuweisungButton;

  EingangsrechnungsStatistik getEingangsrechnungsStatistik()
  {
    if (eingangsrechnungsStatistik == null) {
      eingangsrechnungsStatistik = eingangsrechnungDao.buildStatistik(getList());
    }
    return eingangsrechnungsStatistik;
  }

  public EingangsrechnungListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.eingangsrechnung");
  }

  public EingangsrechnungListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.eingangsrechnung");
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
    this.eingangsrechnungsStatistik = null;
  }

  @Override
  public List<IColumn<EingangsrechnungDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    return createColumns(returnToPage, sortable, false);
  }

  public List<IColumn<EingangsrechnungDO, String>> createColumns(final WebPage returnToPage, final boolean sortable, final boolean isMassUpdateMode)
  {
    final List<IColumn<EingangsrechnungDO, String>> columns = new ArrayList<>();
    final CellItemListener<EingangsrechnungDO> cellItemListener = new CellItemListener<EingangsrechnungDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<EingangsrechnungDO>> item, final String componentId,
          final IModel<EingangsrechnungDO> rowModel)
      {
        final EingangsrechnungDO eingangsrechnung = rowModel.getObject();
        appendCssClasses(item, eingangsrechnung.getId(), eingangsrechnung.isDeleted());
        if (eingangsrechnung.isDeleted() == true) {
          // Do nothing further
        } else if (eingangsrechnung.isUeberfaellig() == true) {
          appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
        } else if (eingangsrechnung.isBezahlt() == false) {
          appendCssClasses(item, RowCssClass.BLUE);
        }
      }
    };
    if (isMassUpdateMode == true) {
      columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>("", null, "selected", cellItemListener)
      {
        @Override
        public void populateItem(final Item<ICellPopulator<EingangsrechnungDO>> item, final String componentId,
            final IModel<EingangsrechnungDO> rowModel)
        {
          final EingangsrechnungDO incomingInvoice = rowModel.getObject();
          final CheckBoxPanel checkBoxPanel = new CheckBoxPanel(componentId, new SelectItemModel(incomingInvoice.getId()),
              null);
          item.add(checkBoxPanel);
          cellItemListener.populateItem(item, componentId, rowModel);
          addRowClick(item, isMassUpdateMode);
        }
      });
    }
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(
        new Model<String>(getString("fibu.common.creditor")), getSortable(
        "kreditor", sortable),
        "kreditor", cellItemListener)
    {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final EingangsrechnungDO eingangsrechnung = (EingangsrechnungDO) rowModel.getObject();
        String kreditor = StringEscapeUtils.escapeHtml4(eingangsrechnung.getKreditor());
        if (form.getSearchFilter().isShowKostZuweisungStatus() == true) {
          final BigDecimal fehlBetrag = eingangsrechnung.getKostZuweisungFehlbetrag();
          if (NumberHelper.isNotZero(fehlBetrag) == true) {
            kreditor += " *** " + CurrencyFormatter.format(fehlBetrag) + " ***";
          }
        }
        final Label kreditorLabel = new Label(ListSelectActionPanel.LABEL_ID, kreditor);
        kreditorLabel.setEscapeModelStrings(false);
        item.add(new ListSelectActionPanel(componentId, rowModel, EingangsrechnungEditPage.class,
            eingangsrechnung.getId(), returnToPage,
            kreditorLabel));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item, isMassUpdateMode);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(new Model<String>(getString("fibu.konto")), getSortable("konto", sortable),
        "konto",
        cellItemListener)
    {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final EingangsrechnungDO rechnung = (EingangsrechnungDO) rowModel.getObject();
        final KontoDO konto = kontoCache.getKonto(rechnung.getKontoId());
        item.add(new Label(componentId, konto != null ? konto.formatKonto() : ""));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(getString("fibu.common.reference"),
        getSortable("referenz", sortable), "referenz", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(getString("fibu.rechnung.betreff"),
        getSortable("betreff", sortable), "betreff", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(getString("fibu.rechnung.datum.short"),
        getSortable("datum",
            sortable),
        "datum", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(getString("fibu.rechnung.faelligkeit.short"),
        getSortable(
            "faelligkeit", sortable),
        "faelligkeit", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(getString("fibu.rechnung.bezahlDatum.short"),
        getSortable(
            "bezahlDatum", sortable),
        "bezahlDatum", cellItemListener));
    columns.add(new CurrencyPropertyColumn<EingangsrechnungDO>(getString("fibu.common.netto"),
        getSortable("netSum", sortable), "netSum",
        cellItemListener));
    columns.add(new CurrencyPropertyColumn<EingangsrechnungDO>(getString("fibu.common.brutto"),
        getSortable("grossSum", sortable),
        "grossSum", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(new Model<String>(getString("comment")),
        getSortable("bemerkung",
            sortable),
        "bemerkung", cellItemListener));
    return columns;
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    addExcelExport(getString("fibu.common.creditor"), getString("fibu.eingangsrechnungen"));
    if (Configuration.getInstance().isCostConfigured() == true) {
      exportKostzuweisungButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Object>("link")
          {
            @Override
            public void onClick()
            {
              exportExcelWithCostAssignments();
            }

          }, getString("fibu.rechnung.kostExcelExport")).setTooltip(getString("fibu.rechnung.kostExcelExport.tootlip"));
      addContentMenuEntry(exportKostzuweisungButton);
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
       * @see ExcelExporter#onBeforeSettingColumns(java.util.List)
       */
      @Override
      protected List<ExportColumn> onBeforeSettingColumns(final ContentProvider sheetProvider,
          final List<ExportColumn> columns)
      {
        final List<ExportColumn> sortedColumns = reorderColumns(columns, "kreditor", "konto", "kontoBezeichnung",
            "betreff", "datum",
            "faelligkeit", "bezahlDatum", "zahlBetrag");
        I18nExportColumn col = new I18nExportColumn("kontoBezeichnung", "fibu.konto.bezeichnung",
            MyXlsContentProvider.LENGTH_STD);
        sortedColumns.add(2, col);
        col = new I18nExportColumn("netSum", "fibu.common.netto");
        putCurrencyFormat(sheetProvider, col);
        sortedColumns.add(7, col);
        col = new I18nExportColumn("grossSum", "fibu.common.brutto");
        putCurrencyFormat(sheetProvider, col);
        sortedColumns.add(8, col);
        return sortedColumns;
      }

      /**
       * @see ExcelExporter#addMapping(PropertyMapping, java.lang.Object,
       *      java.lang.reflect.Field)
       */
      @Override
      public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
      {
        if ("konto".equals(field.getName()) == true) {
          Integer kontoNummer = null;
          final Integer kontoId = ((EingangsrechnungDO) entry).getKontoId();
          if (kontoId != null) {
            final KontoDO konto = kontoCache.getKonto(kontoId);
            if (konto != null) {
              kontoNummer = konto.getNummer();
            }
          }
          mapping.add(field.getName(), kontoNummer != null ? kontoNummer : "");
        } else {
          super.addMapping(mapping, entry, field);
        }
      }

      /**
       * @see ExcelExporter#addMappings(PropertyMapping, java.lang.Object)
       */
      @Override
      protected void addMappings(final PropertyMapping mapping, final Object entry)
      {
        final EingangsrechnungDO invoice = (EingangsrechnungDO) entry;
        String kontoBezeichnung = null;
        final Integer kontoId = ((EingangsrechnungDO) entry).getKontoId();
        if (kontoId != null) {
          final KontoDO konto = kontoCache.getKonto(kontoId);
          if (konto != null) {
            kontoBezeichnung = konto.getBezeichnung();
          }
        }
        mapping.add("kontoBezeichnung", kontoBezeichnung != null ? kontoBezeichnung : "");
        mapping.add("grossSum", invoice.getGrossSum());
        mapping.add("netSum", invoice.getNetSum());
      }
    };
  }

  @Override
  public boolean isSupportsMassUpdate()
  {
    return true;
  }

  @Override
  protected String getMassUpdateLabel()
  {
    return getString("fibu.rechnung.transferExport");
  }

  @Override
  public void setMassUpdateMode(final boolean mode)
  {
    super.setMassUpdateMode(mode);
    exportExcelButton.setVisible(!mode);
    exportKostzuweisungButton.setVisible(!mode);
  }

  @Override
  public void onNextSubmit()
  {
    if (CollectionUtils.isEmpty(this.selectedItems) == true) {
      form.addError("validation.error.nothingToExport");
      return;
    }
    final List<EingangsrechnungDO> list = eingangsrechnungDao.internalLoad(this.selectedItems);
    exportInvoicesAsXML(list);
  }

  @Override
  protected void createDataTable()
  {
    dataTable = createDataTable(createColumns(this, true, isMassUpdateMode()), "datum", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  private void exportInvoicesAsXML(final List<EingangsrechnungDO> invoices)
  {
    if (invoices == null || invoices.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }

    this.form.getFeedbackMessages().clear();

    final String filename = String.format("transfer-%s.xml", DateHelper.getTimestampAsFilenameSuffix(new Date()));
    try {
      SEPATransferResult result = this.SEPATransferGenerator.format(invoices);

      if (result.isSuccessful() == false) {
        if (result.getErrors().isEmpty()) {
          // unknown error
          log.error("Oups, xml has zero size. Filename: " + filename);
          this.form.addError("fibu.rechnung.transferExport.error");
          return;
        }

        List<String> brokenInvoices = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.instance();

        // check invoice
        for (EingangsrechnungDO invoice : result.getErrors().keySet()) {
          if (invoice.getReferenz() != null) {
            brokenInvoices.add("[" + invoice.getKreditor() + ", " + invoice.getReferenz() + ", " + formatter.getFormattedDate(invoice.getDatum()) + "]");
          } else {
            brokenInvoices.add("[" + invoice.getKreditor() + ", " + formatter.getFormattedDate(invoice.getDatum()) + "]");
          }
        }

        String brokenInvoicesStr = String.join("; ", brokenInvoices);
        this.form.addError("fibu.rechnung.transferExport.error.entries", brokenInvoicesStr);

        return;
      }
      DownloadUtils.setDownloadTarget(result.getXml(), filename);
    } catch (UserException e) {
      this.form.addError("error", e.getParams()[0]);
    }
  }

  protected void exportExcelWithCostAssignments()
  {
    refresh();
    final RechnungFilter filter = new RechnungFilter();
    final RechnungFilter src = form.getSearchFilter();
    filter.setFromDate(src.getFromDate());
    filter.setToDate(src.getToDate());
    final List<EingangsrechnungDO> rechnungen = eingangsrechnungDao.getList(filter);
    if (rechnungen == null || rechnungen.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final String filename = "ProjectForge-"
        + getString("fibu.common.creditor")
        + "-"
        + getString("menu.fibu.kost")
        + "_"
        + DateHelper.getDateAsFilenameSuffix(new Date())
        + ".xls";
    final byte[] xls = kostZuweisungExport.exportRechnungen(rechnungen, getString("fibu.common.creditor"),
        kontoCache);
    if (xls == null || xls.length == 0) {
      log.error("Oups, xls has zero size. Filename: " + filename);
      return;
    }
    DownloadUtils.setDownloadTarget(xls, filename);
  }

  @Override
  protected EingangsrechnungListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new EingangsrechnungListForm(this);
  }

  @Override
  public EingangsrechnungDao getBaseDao()
  {
    return eingangsrechnungDao;
  }
}
