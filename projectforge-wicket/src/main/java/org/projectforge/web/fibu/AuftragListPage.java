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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.fibu.AuftragsStatus;
import org.projectforge.business.fibu.OrderExport;
import org.projectforge.business.fibu.RechnungCache;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.CurrencyPropertyColumn;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.RowCssClass;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@ListPage(editPage = AuftragEditPage.class)
public class AuftragListPage extends AbstractListPage<AuftragListForm, AuftragDao, AuftragDO>
    implements IListPageColumnsCreator<AuftragDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  protected static final String[] MY_BOOKMARKABLE_INITIAL_PROPERTIES = mergeStringArrays(
      BOOKMARKABLE_INITIAL_PROPERTIES, new String[] {
          "f.year|y", "f.listType|lt", "f.auftragsPositionsArt|art" });

  @SpringBean
  private AuftragDao auftragDao;

  @SpringBean
  private OrderExport orderExport;

  @SpringBean
  private UserFormatter userFormatter;

  @SpringBean
  private RechnungCache rechnungCache;

  public AuftragListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.auftrag");
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<AuftragDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<AuftragDO, String>> columns = new ArrayList<IColumn<AuftragDO, String>>();
    final CellItemListener<AuftragDO> cellItemListener = new CellItemListener<AuftragDO>()
    {
      public void populateItem(final Item<ICellPopulator<AuftragDO>> item, final String componentId,
          final IModel<AuftragDO> rowModel)
      {
        final AuftragDO auftrag = rowModel.getObject();
        if (auftrag.getAuftragsStatus() == null) {
          // Should not occur:
          return;
        }
        final boolean isDeleted = auftrag.isDeleted() == true
            || auftrag.getAuftragsStatus().isIn(AuftragsStatus.ABGELEHNT, AuftragsStatus.ERSETZT) == true;
        appendCssClasses(item, auftrag.getId(), auftrag.isDeleted());
        if (isDeleted) {
          // Do nothing further.
        } else if (auftrag.isAbgeschlossenUndNichtVollstaendigFakturiert() == true
            || auftrag.isZahlplanAbgeschlossenUndNichtVollstaendigFakturiert() == true) {
          appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
        } else if (auftrag.getAuftragsStatus().isIn(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.LOI) == true) {
          appendCssClasses(item, RowCssClass.SUCCESS_ROW);
        } else if (auftrag.getAuftragsStatus().isIn(AuftragsStatus.ESKALATION) == true) {
          appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
        }
      }
    };
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(new Model<String>(getString("fibu.auftrag.nummer.short")),
        "nummer",
        "nummer", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<AuftragDO>> item, final String componentId,
          final IModel<AuftragDO> rowModel)
      {
        final AuftragDO auftrag = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, AuftragEditPage.class, auftrag.getId(), returnToPage,
            String
                .valueOf(auftrag.getNummer())));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.kunde"), "kundeAsString", "kundeAsString",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.projekt"), "projekt.name", "projekt.name",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.auftrag.titel"), "titel", "titel",
        cellItemListener));
    columns.add(new AbstractColumn<AuftragDO, String>(new Model<String>(getString("label.position.short")))
    {
      public void populateItem(final Item<ICellPopulator<AuftragDO>> cellItem, final String componentId,
          final IModel<AuftragDO> rowModel)
      {
        final AuftragDO auftrag = rowModel.getObject();
        auftragDao.calculateInvoicedSum(auftrag);
        final List<AuftragsPositionDO> fullList = auftrag.getPositionen();
        final List<AuftragsPositionDO> list = fullList.stream().filter(pos -> pos.isDeleted() == false).collect(Collectors.toList());
        final Label label = new Label(componentId, new Model<String>("#" + list.size()));
        if (list != null) {
          final StringBuffer buf = new StringBuffer();
          list.forEach(pos -> {
            buf.append("#").append(pos.getNumber()).append(": ");
            if (pos.getPersonDays() != null && pos.getPersonDays().compareTo(BigDecimal.ZERO) != 0) {
              buf.append("(").append(NumberFormatter.format(pos.getPersonDays())).append(" ")
                  .append(getString("projectmanagement.personDays.short")).append(") ");
            }
            if (pos.getNettoSumme() != null) {
              buf.append(CurrencyFormatter.format(pos.getNettoSumme()));
              if (StringUtils.isNotBlank(pos.getTitel()) == true) {
                buf.append(": ").append(pos.getTitel());
              }
              buf.append(": ");
            }
            if (pos.getTaskId() != null) {
              buf.append(WicketTaskFormatter.getTaskPath(pos.getTaskId(), false, OutputType.HTML));
            } else {
              buf.append(getString("fibu.auftrag.position.noTaskGiven"));
            }
            if (pos.getStatus() != null) {
              buf.append(", ").append(getString(pos.getStatus().getI18nKey()));
            }
            buf.append("\n");
          });
          if (buf.length() > 1 && (buf.lastIndexOf("\n") == buf.length() - 1)) {
            buf.delete(buf.length() - 1, buf.length());
          }
          WicketUtils.addTooltip(label, NumberFormatter.format(auftrag.getPersonDays())
              + " "
              + getString("projectmanagement.personDays.short"), buf.toString());
        }
        cellItem.add(label);
        cellItemListener.populateItem(cellItem, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(

        getString("projectmanagement.personDays.short"),
        "personDays", "personDays",
        cellItemListener)

    {
      @Override
      public void populateItem(final Item<ICellPopulator<AuftragDO>> item, final String componentId,
          final IModel<AuftragDO> rowModel)
      {
        item.add(new Label(componentId, NumberFormatter.format(rowModel.getObject().getPersonDays())));
        item.add(AttributeModifier.append("style", new Model<String>("text-align: right;")));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns
        .add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.common.customer.reference"), "referenz", "referenz",
            cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.common.assignedPersons"), "assignedPersons", "assignedPersons",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.auftrag.datum"), "angebotsDatum", "angebotsDatum",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.auftrag.entscheidung.datum"), "entscheidungsDatum", "entscheidungsDatum",
        cellItemListener));
    columns.add(new CurrencyPropertyColumn<AuftragDO>(getString("fibu.auftrag.nettoSumme"), "nettoSumme", "nettoSumme",
        cellItemListener));
    columns.add(new CurrencyPropertyColumn<AuftragDO>(

        getString("fibu.auftrag.commissioned"), "beauftragtNettoSumme",
        "beauftragtNettoSumme", cellItemListener));
    columns.add(new CurrencyPropertyColumn<AuftragDO>(

        getString("fibu.fakturiert"), "fakturiertSum", "fakturiertSum",
        cellItemListener).setSuppressZeroValues(true));
    columns.add(new CurrencyPropertyColumn<AuftragDO>(getString("fibu.tobeinvoiced"), "zuFakturierenSum", "zuFakturierenSum",
        cellItemListener)
        .setSuppressZeroValues(true));
    columns
        .add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.periodOfPerformance.from"), "periodOfPerformanceBegin", "periodOfPerformanceBegin",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.periodOfPerformance.to"), "periodOfPerformanceEnd", "periodOfPerformanceEnd",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AuftragDO>(getString("fibu.probabilityOfOccurrence"), "probabilityOfOccurrence", "probabilityOfOccurrence",
        cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<AuftragDO>(new Model<String>(

            getString("status")), "auftragsStatusAsString",
            "auftragsStatusAsString", cellItemListener));
    return columns;
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "nummer", SortOrder.DESCENDING);
    form.add(dataTable);
    final ContentMenuEntryPanel exportExcelButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Object>("link")
        {
          @Override
          public void onClick()
          {
            final List<AuftragDO> list = getList();
            final byte[] xls = orderExport.export(list);
            if (xls == null || xls.length == 0) {
              form.addError("datatable.no-records-found");
              return;
            }
            final String filename = "ProjectForge-OrderExport_" + DateHelper.getDateAsFilenameSuffix(new Date())
                + ".xls";
            DownloadUtils.setDownloadTarget(xls, filename);
          }
        }, getString("exportAsXls")).setTooltip(getString("tooltip.export.excel"));
    addContentMenuEntry(exportExcelButton);

  }

  @Override
  public void refresh()
  {
    super.refresh();
    form.refresh();
  }

  @Override
  protected AuftragListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new AuftragListForm(this);
  }

  @Override
  public AuftragDao getBaseDao()
  {
    return auftragDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBookmarkableInitialProperties()
   */
  @Override
  protected String[] getBookmarkableInitialProperties()
  {
    return MY_BOOKMARKABLE_INITIAL_PROPERTIES;
  }
}
