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

package org.projectforge.plugins.banking;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

@ListPage(editPage = BankAccountEditPage.class)
public class BankAccountListPage extends AbstractListPage<BankAccountListForm, BankAccountDao, BankAccountDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private BankAccountDao bankAccountDao;

  public BankAccountListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.banking.account");
  }

  public BankAccountListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "plugins.banking.account");
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final List<IColumn<BankAccountDO, String>> columns = new ArrayList<>();
    final CellItemListener<BankAccountDO> cellItemListener = new CellItemListener<BankAccountDO>()
    {
      public void populateItem(final Item<ICellPopulator<BankAccountDO>> item, final String componentId,
          final IModel<BankAccountDO> rowModel)
      {
        final BankAccountDO bankAccount = rowModel.getObject();
        appendCssClasses(item, bankAccount.getId(), bankAccount.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<BankAccountDO>(
        new Model<>(getString("plugins.banking.account.number")),
        "accountNumber", "accountNumber", cellItemListener)
    {
      @SuppressWarnings("unchecked")
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final BankAccountDO bankAccount = (BankAccountDO) rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, BankAccountEditPage.class, bankAccount.getId(),
            BankAccountListPage.this,
            String.valueOf(bankAccount.getAccountNumber())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns
        .add(new CellItemListenerPropertyColumn<>(
            new Model<>(getString("plugins.banking.account.name")), "name", "name", cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<>(new Model<>(getString("plugins.banking.bank")),
            "bank", "bank", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(
        new Model<>(getString("plugins.banking.bankIdentificationCode")), "bankIdentificationCode",
        "bankIdentificationCode", cellItemListener));
    dataTable = createDataTable(columns, "accountNumber", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected BankAccountListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new BankAccountListForm(this);
  }

  @Override
  public BankAccountDao getBaseDao()
  {
    return bankAccountDao;
  }

  protected BankAccountDao getBankAccountDao()
  {
    return bankAccountDao;
  }
}
