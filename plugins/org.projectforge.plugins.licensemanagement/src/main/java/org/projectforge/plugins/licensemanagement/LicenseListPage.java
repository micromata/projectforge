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

package org.projectforge.plugins.licensemanagement;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The controller of the list page. Most functionality such as search etc. is done by the super class.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@ListPage(editPage = LicenseEditPage.class)
public class LicenseListPage extends AbstractListPage<LicenseListForm, LicenseDao, LicenseDO>
    implements IListPageColumnsCreator<LicenseDO>
{
  private static final long serialVersionUID = -1802352700020870211L;

  @SpringBean
  private LicenseDao licenseDao;

  public LicenseListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.licensemanagement");
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<LicenseDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<LicenseDO, String>> columns = new ArrayList<>();
    final CellItemListener<LicenseDO> cellItemListener = new CellItemListener<LicenseDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<LicenseDO>> item, final String componentId,
          final IModel<LicenseDO> rowModel)
      {
        final LicenseDO license = rowModel.getObject();
        appendCssClasses(item, license.getId(), license.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<LicenseDO>(new Model<>(getString("organization")),
        getSortable("organization", sortable),
        "organization", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<LicenseDO>> item, final String componentId,
          final IModel<LicenseDO> rowModel)
      {
        final LicenseDO license = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, LicenseEditPage.class, license.getId(), returnToPage,
            rowModel.getObject().getOrganization()));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<>(
        new Model<>(getString("plugins.licensemanagement.product")), getSortable(
        "product", sortable),
        "product", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(
        new Model<>(getString("plugins.licensemanagement.version")), getSortable(
        "version", sortable),
        "version", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(
        new Model<>(getString("plugins.licensemanagement.numberOfLicenses")), getSortable(
        "numberOfLicenses", sortable),
        "numberOfLicenses", cellItemListener));
    columns.add(new AbstractColumn<LicenseDO, String>(new Model<>(getString("plugins.licensemanagement.owner")))
    {
      @Override
      public void populateItem(final Item<ICellPopulator<LicenseDO>> cellItem, final String componentId,
          final IModel<LicenseDO> rowModel)
      {
        final LicenseDO license = rowModel.getObject();
        final String owners = licenseDao.getSortedOwnernames(license);
        final Label label = new Label(componentId, new Model<>(owners));
        cellItem.add(label);
        cellItemListener.populateItem(cellItem, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<>(
        new Model<>(getString("plugins.licensemanagement.device")), getSortable(
        "device", sortable),
        "device", cellItemListener));
    if (accessChecker.isLoggedInUserMemberOfAdminGroup()) {
      columns.add(new CellItemListenerPropertyColumn<LicenseDO>(
          new Model<>(getString("plugins.licensemanagement.key")), getSortable(
          "key", sortable),
          "key", cellItemListener)
      {
        @Override
        public void populateItem(final Item<ICellPopulator<LicenseDO>> item, final String componentId,
            final IModel<LicenseDO> rowModel)
        {
          final LicenseDO license = rowModel.getObject();
          final Label label = new Label(componentId, new Model<>(StringUtils.abbreviate(license.getKey(), 40)));
          cellItemListener.populateItem(item, componentId, rowModel);
          item.add(label);
        }
      });
    }
    columns.add(new CellItemListenerPropertyColumn<LicenseDO>(new Model<>(getString("comment")),
        getSortable("comment", sortable), "comment", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<LicenseDO>> item, final String componentId,
          final IModel<LicenseDO> rowModel)
      {
        final LicenseDO license = rowModel.getObject();
        final Label label = new Label(componentId,
            new Model<>(StringUtils.abbreviate(license.getComment(), 100)));
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    columns.add(
        new CellItemListenerPropertyColumn<>(getString("created"), getSortable("created", sortable), "created",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(getString("modified"),
        getSortable("lastUpdate", sortable), "lastUpdate",
        cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "orderString", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected LicenseListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new LicenseListForm(this);
  }

  @Override
  public LicenseDao getBaseDao()
  {
    return licenseDao;
  }

  protected LicenseDao getLicenseDao()
  {
    return licenseDao;
  }
}
