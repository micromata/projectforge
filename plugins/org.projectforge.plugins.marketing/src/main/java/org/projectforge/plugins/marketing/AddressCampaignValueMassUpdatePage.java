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

package org.projectforge.plugins.marketing;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.PersonalAddressDO;
import org.projectforge.framework.utils.MyBeanComparator;
import org.projectforge.web.wicket.AbstractMassEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;

import java.util.*;

public class AddressCampaignValueMassUpdatePage extends AbstractMassEditPage
{
  private static final long serialVersionUID = 7327415575226885117L;

  @SpringBean
  private AddressCampaignValueDao addressCampaignValueDao;

  private final List<AddressDO> addresses;

  private final AddressCampaignValueMassUpdateForm form;

  public AddressCampaignValueMassUpdatePage(final AbstractSecuredPage callerPage, final List<AddressDO> addresses,
      final AddressCampaignDO addressCampaign, final Map<Integer, PersonalAddressDO> personalAddressMap,
      final Map<Integer, AddressCampaignValueDO> addressCampaignValueMap)
  {
    super(new PageParameters(), callerPage);
    this.addresses = addresses;
    form = new AddressCampaignValueMassUpdateForm(this, addressCampaign);
    body.add(form);
    form.init();
    final List<IColumn<AddressDO, String>> columns = AddressCampaignValueListPage.createColumns(this, false, true, null,
        personalAddressMap,
        addressCampaignValueMap);
    @SuppressWarnings("serial")
    final SortableDataProvider<AddressDO, String> sortableDataProvider = new SortableDataProvider<AddressDO, String>()
    {
      @Override
      public Iterator<? extends AddressDO> iterator(final long first, final long count)
      {
        final SortParam sp = getSort();
        if (addresses == null) {
          return null;
        }
        final Comparator<AddressDO> comp = new MyBeanComparator<>(sp.getProperty().toString(),
            sp.isAscending());
        addresses.sort(comp);
        return addresses.subList((int) first, (int) (first + count)).iterator();
      }

      public long size()
      {
        return addresses != null ? addresses.size() : 0;
      }

      public IModel<AddressDO> model(final AddressDO object)
      {
        return new Model<AddressDO>()
        {
          @Override
          public AddressDO getObject()
          {
            return object;
          }
        };
      }
    };
    sortableDataProvider.setSort("name", SortOrder.DESCENDING);

    final DefaultDataTable<AddressDO, String> dataTable = new DefaultDataTable<>("table", columns,
        sortableDataProvider, 1000);
    body.add(dataTable);
  }

  @Override
  protected String getTitle()
  {
    return getString("addressCampaignValue.massupdate.title");
  }

  /**
   * @see org.projectforge.web.wicket.AbstractMassEditPage#updateAll()
   */
  @Override
  protected void updateAll()
  {
    final AddressCampaignValueDO data = form.data;
    addressCampaignValueDao.massUpdate(addresses, data.getAddressCampaign(), data.getValue(), data.getComment());
    super.updateAll();
  }
}
