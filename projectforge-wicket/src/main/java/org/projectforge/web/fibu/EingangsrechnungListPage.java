/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.text.StringEscapeUtils;
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
import org.projectforge.business.excel.*;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.kost.KostZuweisungExport;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.rest.fibu.EingangsrechnungPagesRest;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ListPage(editPage = EingangsrechnungEditPage.class)
public class EingangsrechnungListPage
        extends AbstractListPage<EingangsrechnungListForm, EingangsrechnungDao, EingangsrechnungDO> implements
        IListPageColumnsCreator<EingangsrechnungDO> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EingangsrechnungListPage.class);

    private static final long serialVersionUID = 4417254962066648504L;

    private EingangsrechnungsStatistik eingangsrechnungsStatistik;

    private ContentMenuEntryPanel exportKostzuweisungButton;

    EingangsrechnungsStatistik getEingangsrechnungsStatistik() {
        if (eingangsrechnungsStatistik == null) {
            eingangsrechnungsStatistik = WicketSupport.get(EingangsrechnungDao.class).buildStatistik(getList());
        }
        return eingangsrechnungsStatistik;
    }

    public EingangsrechnungListPage(final PageParameters parameters) {
        super(parameters, "fibu.eingangsrechnung");
    }

    public EingangsrechnungListPage(final ISelectCallerPage caller, final String selectProperty) {
        super(caller, selectProperty, "fibu.eingangsrechnung");
    }

    /**
     * Forces the statistics to be reloaded.
     *
     * @see org.projectforge.web.wicket.AbstractListPage#refresh()
     */
    @Override
    public void refresh() {
        super.refresh();
        this.eingangsrechnungsStatistik = null;
    }

    @Override
    public List<IColumn<EingangsrechnungDO, String>> createColumns(final WebPage returnToPage, final boolean sortable) {
        return createColumns(returnToPage, sortable, false);
    }

    public List<IColumn<EingangsrechnungDO, String>> createColumns(final WebPage returnToPage, final boolean sortable, final boolean isMassUpdateMode) {
        final List<IColumn<EingangsrechnungDO, String>> columns = new ArrayList<>();
        final CellItemListener<EingangsrechnungDO> cellItemListener = new CellItemListener<EingangsrechnungDO>() {
            @Override
            public void populateItem(final Item<ICellPopulator<EingangsrechnungDO>> item, final String componentId,
                                     final IModel<EingangsrechnungDO> rowModel) {
                final EingangsrechnungDO eingangsrechnung = rowModel.getObject();
                appendCssClasses(item, eingangsrechnung.getId(), eingangsrechnung.getDeleted());
                if (eingangsrechnung.getDeleted() == true) {
                    // Do nothing further
                } else {
                    if (eingangsrechnung.getInfo().isUeberfaellig()) {
                        appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
                    } else if (!eingangsrechnung.getInfo().isBezahlt()) {
                        appendCssClasses(item, RowCssClass.BLUE);
                    }
                }
            }
        };
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(
                new Model<String>(

                        getString("fibu.common.creditor")),

                getSortable(
                        "kreditor", sortable),
                "kreditor", cellItemListener) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public void populateItem(final Item item, final String componentId, final IModel rowModel) {
                final EingangsrechnungDO eingangsrechnung = (EingangsrechnungDO) rowModel.getObject();
                String kreditor = StringEscapeUtils.escapeHtml4(eingangsrechnung.getKreditor());
                if (form.getSearchFilter().isShowKostZuweisungStatus() == true) {
                    final BigDecimal fehlBetrag = eingangsrechnung.getInfo().getKostZuweisungenFehlbetrag();
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
                addRowClick(item);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(new Model<String>(

                getString("fibu.konto")),

                getSortable("konto.nummer", sortable),
                "konto",
                cellItemListener) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public void populateItem(final Item item, final String componentId, final IModel rowModel) {
                final EingangsrechnungDO rechnung = (EingangsrechnungDO) rowModel.getObject();
                final KontoDO konto = WicketSupport.get(KontoCache.class).getKontoIfNotInitialized(rechnung.getKonto());
                item.add(new Label(componentId, konto != null ? konto.formatKonto() : ""));
                cellItemListener.populateItem(item, componentId, rowModel);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(

                getString("fibu.common.reference"),

                getSortable("referenz", sortable), "referenz", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(

                getString("fibu.rechnung.betreff"),

                getSortable("betreff", sortable), "betreff", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(

                getString("fibu.rechnung.datum.short"),

                getSortable("datum",
                        sortable),
                "datum", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(

                getString("fibu.rechnung.faelligkeit.short"),

                getSortable(
                        "info.faelligkeitOrDiscountMaturity", sortable),
                "info.faelligkeitOrDiscountMaturity", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(

                getString("fibu.rechnung.bezahlDatum.short"),

                getSortable(
                        "bezahlDatum", sortable),
                "bezahlDatum", cellItemListener));
        columns.add(new CurrencyPropertyColumn<EingangsrechnungDO>(
                getString("fibu.common.netto"),
                getSortable("info.netSum", sortable), "info.netSum",
                cellItemListener));
        columns.add(new CurrencyPropertyColumn<EingangsrechnungDO>(

                getString("fibu.common.brutto"),

                getSortable("info.grossSum", sortable),
                "info.grossSum", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<EingangsrechnungDO>(new Model<String>(

                getString("comment")),

                getSortable("bemerkung",
                        sortable),
                "bemerkung", cellItemListener));
        return columns;
    }

    @SuppressWarnings("serial")
    @Override
    protected void init() {
        dataTable = createDataTable(createColumns(this, true), "datum", SortOrder.DESCENDING);
        form.add(dataTable);
        addExcelExport(getString("fibu.common.creditor"), getString("fibu.eingangsrechnungen"));
        if (Configuration.getInstance().isCostConfigured() == true) {
            exportKostzuweisungButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
                    new Link<Object>("link") {
                        @Override
                        public void onClick() {
                            exportExcelWithCostAssignments();
                        }

                    }, getString("fibu.rechnung.kostExcelExport")).setTooltip(getString("fibu.rechnung.kostExcelExport.tootlip"));
            addContentMenuEntry(exportKostzuweisungButton);
        }
        addNewMassSelect(EingangsrechnungPagesRest.class);
    }

    /**
     * @see org.projectforge.web.wicket.AbstractListPage#createExcelExporter(java.lang.String)
     */
    @Override
    protected DOListExcelExporter createExcelExporter(final String filenameIdentifier) {
        return new DOListExcelExporter(filenameIdentifier) {
            @Override
            protected List<ExportColumn> onBeforeSettingColumns(final ContentProvider sheetProvider,
                                                                final List<ExportColumn> columns) {
                final List<ExportColumn> sortedColumns = reorderColumns(columns, "kreditor", "konto", "kontoBezeichnung",
                        "betreff", "datum",
                        "faelligkeitOrDiscountMaturity", "bezahlDatum", "zahlBetrag");
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
            public void addMapping(final PropertyMapping mapping, final Object entry, final Field field) {
                if ("konto".equals(field.getName()) == true) {
                    EingangsrechnungDO invoice = (EingangsrechnungDO) entry;
                    KontoDO konto = WicketSupport.get(KontoCache.class).getKontoIfNotInitialized(invoice.getKonto());
                    Integer kontoNummer = konto != null ? konto.getNummer() : null;
                    mapping.add(field.getName(), kontoNummer != null ? kontoNummer : "");
                } else {
                    super.addMapping(mapping, entry, field);
                }
            }

            /**
             * @see ExcelExporter#addMappings(PropertyMapping, java.lang.Object)
             */
            @Override
            protected void addMappings(final PropertyMapping mapping, final Object entry) {
                final EingangsrechnungDO invoice = (EingangsrechnungDO) entry;
                KontoDO konto = WicketSupport.get(KontoCache.class).getKontoIfNotInitialized(invoice.getKonto());
                String kontoBezeichnung = (konto != null) ? konto.getBezeichnung() : null;
                mapping.add("kontoBezeichnung", kontoBezeichnung != null ? kontoBezeichnung : "");
                mapping.add("grossSum", invoice.getInfo().getGrossSum());
                mapping.add("netSum", invoice.getInfo().getNetSum());
            }
        };
    }

    protected void exportExcelWithCostAssignments() {
        refresh();
        final RechnungFilter filter = new RechnungFilter();
        final RechnungFilter src = form.getSearchFilter();
        filter.setFromDate(src.getFromDate());
        filter.setToDate(src.getToDate());
        final List<EingangsrechnungDO> rechnungen = WicketSupport.get(EingangsrechnungDao.class).select(filter);
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
        final byte[] xls = WicketSupport.get(KostZuweisungExport.class).exportRechnungen(rechnungen, getString("fibu.common.creditor"));
        if (xls == null || xls.length == 0) {
            log.error("Oups, xls has zero size. Filename: " + filename);
            return;
        }
        DownloadUtils.setDownloadTarget(xls, filename);
    }

    @Override
    protected EingangsrechnungListForm newListForm(final AbstractListPage<?, ?, ?> parentPage) {
        return new EingangsrechnungListForm(this);
    }

    @Override
    public EingangsrechnungDao getBaseDao() {
        return WicketSupport.get(EingangsrechnungDao.class);
    }
}
