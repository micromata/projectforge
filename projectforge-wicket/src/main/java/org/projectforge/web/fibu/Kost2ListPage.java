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
import java.util.Date;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.business.excel.ExportWorkbook;
import org.projectforge.business.excel.I18nExportColumn;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.export.MyXlsContentProvider;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@ListPage(editPage = Kost2EditPage.class)
public class Kost2ListPage extends AbstractListPage<Kost2ListForm, Kost2Dao, Kost2DO>
    implements IListPageColumnsCreator<Kost2DO>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Kost2ListPage.class);

  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private Kost2Dao kost2Dao;

  public Kost2ListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.kost2");
  }

  public Kost2ListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.kost2");
  }

  public Kost2ListPage(final PageParameters parameters, final ISelectCallerPage caller, final String selectProperty)
  {
    super(parameters, caller, selectProperty, "fibu.kost2");
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<Kost2DO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<Kost2DO, String>> columns = new ArrayList<IColumn<Kost2DO, String>>();
    final CellItemListener<Kost2DO> cellItemListener = new CellItemListener<Kost2DO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<Kost2DO>> item, final String componentId,
          final IModel<Kost2DO> rowModel)
      {
        final Kost2DO kost2 = rowModel.getObject();
        appendCssClasses(item, kost2.getId(), kost2.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("fibu.kost2")),
        getSortable("formattedNumber",
            sortable),
        "formattedNumber", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<Kost2DO>> item, final String componentId,
          final IModel<Kost2DO> rowModel)
      {
        final Kost2DO kost2 = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, Kost2EditPage.class, kost2.getId(), returnToPage,
              String.valueOf(kost2
                  .getFormattedNumber())));
          cellItemListener.populateItem(item, componentId, rowModel);
        } else {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, kost2.getId(),
              String.valueOf(kost2
                  .getFormattedNumber())));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("fibu.kost2.art")),
        getSortable("kost2Art.name",
            sortable),
        "kost2Art.name", cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("fibu.fakturiert")), getSortable(
            "kost2Art.fakturiert", sortable), "kost2Art.fakturiert", cellItemListener)
        {
          @Override
          public void populateItem(final Item<ICellPopulator<Kost2DO>> item, final String componentId,
              final IModel<Kost2DO> rowModel)
          {
            final Kost2DO kost2 = rowModel.getObject();
            final Component label = WicketUtils.createBooleanLabel(getRequestCycle(), componentId,
                kost2.getKost2Art() != null
                    && kost2.getKost2Art().isFakturiert() == true);
            item.add(label);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("fibu.kost2.workFraction")),
        getSortable(
            "workFraction", sortable),
        "workFraction", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("fibu.kunde")),
        getSortable("projekt.kunde.name",
            sortable),
        "projekt.kunde.name", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("fibu.projekt")),
        getSortable("projekt.name",
            sortable),
        "projekt.name", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("status")),
        getSortable("kostentraegerStatus",
            sortable),
        "kostentraegerStatus", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("description")),
        getSortable("description",
            sortable),
        "description", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<Kost2DO>(new Model<String>(getString("comment")),
        getSortable("comment", sortable),
        "comment", cellItemListener));
    return columns;
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "formattedNumber", SortOrder.ASCENDING);
    form.add(dataTable);
    {
      // Excel export
      final SubmitLink excelExportLink = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          exportExcel();
        }
      };
      final ContentMenuEntryPanel excelExportButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          excelExportLink,
          getString("exportAsXls")).setTooltip(getString("tooltip.export.excel"));
      addContentMenuEntry(excelExportButton);
    }
  }

  private enum Col
  {
    STATUS, KOST, ART, FAKTURIERT, PROJEKT, DESCRIPTION, COMMENT
  }

  protected void exportExcel()
  {
    log.info("Exporting kost2 list.");
    refresh();
    final List<Kost2DO> kost2List = getList();
    if (kost2List == null || kost2List.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final String filename = "ProjectForge-Kost2Export_" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".xls";
    final ExportWorkbook xls = new ExportWorkbook();
    final ContentProvider contentProvider = new MyXlsContentProvider(xls);
    xls.setContentProvider(contentProvider);
    final ExportSheet sheet = xls.addSheet(ThreadLocalUserContext.getLocalizedString("fibu.kost2.kost2s"));
    final ExportColumn[] cols = new ExportColumn[] { //
        new I18nExportColumn(Col.KOST, "fibu.kost2", MyXlsContentProvider.LENGTH_KOSTENTRAEGER),
        new I18nExportColumn(Col.ART, "fibu.kost2.art", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.FAKTURIERT, "fibu.fakturiert", 5),
        new I18nExportColumn(Col.PROJEKT, "fibu.projekt", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.STATUS, "status", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.DESCRIPTION, "description", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.COMMENT, "comment", MyXlsContentProvider.LENGTH_STD) };
    sheet.setColumns(cols);
    final PropertyMapping mapping = new PropertyMapping();
    for (final Kost2DO kost : kost2List) {
      mapping.add(Col.KOST, kost.getFormattedNumber());
      mapping.add(Col.ART, kost.getKost2Art().getName());
      mapping.add(Col.FAKTURIERT, kost.getKost2Art().isFakturiert() ? "X" : "");
      mapping.add(Col.PROJEKT, KostFormatter.formatProjekt(kost.getProjekt()));
      mapping.add(Col.STATUS, kost.getKostentraegerStatus());
      mapping.add(Col.DESCRIPTION, kost.getDescription());
      mapping.add(Col.COMMENT, kost.getComment());
      sheet.addRow(mapping.getMapping(), 0);
    }
    sheet.setZoom(3, 4); // 75%
    DownloadUtils.setDownloadTarget(xls.getAsByteArray(), filename);
  }

  @Override
  protected Kost2ListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new Kost2ListForm(this);
  }

  @Override
  public Kost2Dao getBaseDao()
  {
    return kost2Dao;
  }

  protected Kost2Dao getKost2Dao()
  {
    return kost2Dao;
  }
}
