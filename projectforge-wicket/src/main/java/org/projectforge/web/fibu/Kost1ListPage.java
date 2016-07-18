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
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.excel.ContentProvider;
import org.projectforge.excel.ExportColumn;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
import org.projectforge.excel.I18nExportColumn;
import org.projectforge.excel.PropertyMapping;
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
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@ListPage(editPage = Kost1EditPage.class)
public class Kost1ListPage extends AbstractListPage<Kost1ListForm, Kost1Dao, Kost1DO>
    implements IListPageColumnsCreator<Kost1DO>
{
  private static final long serialVersionUID = 2432908214495492575L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Kost1ListPage.class);

  @SpringBean
  private Kost1Dao kost1Dao;

  public Kost1ListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.kost1");
  }

  public Kost1ListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.kost1");
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<Kost1DO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<Kost1DO, String>> columns = new ArrayList<IColumn<Kost1DO, String>>();
    final CellItemListener<Kost1DO> cellItemListener = new CellItemListener<Kost1DO>()
    {
      public void populateItem(final Item<ICellPopulator<Kost1DO>> item, final String componentId,
          final IModel<Kost1DO> rowModel)
      {
        final Kost1DO kost1 = rowModel.getObject();
        appendCssClasses(item, kost1.getId(), kost1.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<Kost1DO>(new Model<String>(getString("fibu.kost1")),
        getSortable("formattedNumber",
            sortable),
        "formattedNumber", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<Kost1DO>> item, final String componentId,
          final IModel<Kost1DO> rowModel)
      {
        final Kost1DO kost1 = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, Kost1EditPage.class, kost1.getId(), returnToPage,
              String.valueOf(kost1
                  .getFormattedNumber())));
          cellItemListener.populateItem(item, componentId, rowModel);
        } else {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, kost1.getId(),
              String.valueOf(kost1
                  .getFormattedNumber())));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<Kost1DO>(new Model<String>(getString("description")),
        getSortable("description",
            sortable),
        "description", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<Kost1DO>(new Model<String>(getString("status")),
        getSortable("kostentraegerStatus",
            sortable),
        "kostentraegerStatus", cellItemListener));
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
    STATUS, KOST, DESCRIPTION;
  }

  protected void exportExcel()
  {
    log.info("Exporting kost1 list.");
    refresh();
    final List<Kost1DO> kost1List = getList();
    if (kost1List == null || kost1List.size() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final String filename = "ProjectForge-Kost1Export_" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".xls";
    final ExportWorkbook xls = new ExportWorkbook();
    final ContentProvider contentProvider = new MyXlsContentProvider(xls);
    xls.setContentProvider(contentProvider);
    final ExportSheet sheet = xls.addSheet(ThreadLocalUserContext.getLocalizedString("fibu.kost1.kost1s"));
    final ExportColumn[] cols = new ExportColumn[] { //
        new I18nExportColumn(Col.KOST, "fibu.kost1", MyXlsContentProvider.LENGTH_KOSTENTRAEGER),
        new I18nExportColumn(Col.DESCRIPTION, "description", MyXlsContentProvider.LENGTH_STD),
        new I18nExportColumn(Col.STATUS, "status", MyXlsContentProvider.LENGTH_STD) };
    sheet.setColumns(cols);
    final PropertyMapping mapping = new PropertyMapping();
    for (final Kost1DO kost : kost1List) {
      mapping.add(Col.KOST, kost.getFormattedNumber());
      mapping.add(Col.STATUS, kost.getKostentraegerStatus());
      mapping.add(Col.DESCRIPTION, kost.getDescription());
      sheet.addRow(mapping.getMapping(), 0);
    }
    sheet.setZoom(3, 4); // 75%
    DownloadUtils.setDownloadTarget(xls.getAsByteArray(), filename);
  }

  @Override
  protected Kost1ListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new Kost1ListForm(this);
  }

  @Override
  protected Kost1Dao getBaseDao()
  {
    return kost1Dao;
  }

  protected Kost1Dao getKost1Dao()
  {
    return kost1Dao;
  }
}
