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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Iterator;

/**
 * The controler of the edit formular page. Most functionality such as insert, update, delete etc. is done by the super
 * class.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@EditPage(defaultReturnPage = AddressCampaignValueListPage.class)
public class AddressCampaignValueEditPage extends
    AbstractEditPage<AddressCampaignValueDO, AddressCampaignValueEditForm, AddressCampaignValueDao>
{
  public static final String PARAMETER_ADDRESS_ID = "addressId";

  public static final String PARAMETER_ADDRESS_CAMPAIGN_ID = "campaignId";

  private static final long serialVersionUID = -5058143025817192156L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressCampaignValueEditPage.class);

  @SpringBean
  private AddressCampaignValueDao addressCampaignValueDao;

  @SpringBean
  private AddressCampaignDao addressCampaignDao;

  @SpringBean
  private AddressDao addressDao;

  public AddressCampaignValueEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.marketing.addressCampaign");
    StringValue sval = parameters.get(AbstractEditPage.PARAMETER_KEY_ID);
    final Integer id = sval.isEmpty() ? null : sval.toInteger();
    if (id == null) {
      // Create new entry.
      sval = parameters.get(PARAMETER_ADDRESS_ID);
      final Integer addressId = sval.isEmpty() ? null : sval.toInteger();
      sval = parameters.get(PARAMETER_ADDRESS_CAMPAIGN_ID);
      final Integer addressCampaignId = sval.isEmpty() || "null".equals(sval.toString()) ? null : sval.toInteger();
      if (addressId == null || addressCampaignId == null) {
        throw new UserException("plugins.marketing.addressCampaignValue.error.addressOrCampaignNotGiven");
      }
      final AddressDO address = addressDao.getById(addressId);
      final AddressCampaignDO addressCampaign = addressCampaignDao.getById(addressCampaignId);
      if (address == null || addressCampaign == null) {
        throw new UserException("plugins.marketing.addressCampaignValue.error.addressOrCampaignNotGiven");
      }
      AddressCampaignValueDO data = addressCampaignValueDao.get(addressId, addressCampaignId);
      if (data == null) {
        data = new AddressCampaignValueDO();
        data.setAddress(address);
        data.setAddressCampaign(addressCampaign);
      }
      init(data);
    } else {
      init();
    }
  }

  @Override
  public boolean isUpdateAndNextSupported()
  {
    return true;
  }

  @Override
  protected void updateAndNext()
  {
    if (getData().getId() == null) {
      if (log.isDebugEnabled()) {
        log.debug("update in " + this.editPageSupport.getClass() + ": " + getData());
      }
      create();
      this.editPageSupport.setUpdateAndNext(true);
      setResponsePage();
    } else {
      super.updateAndNext();
    }
  }

  @Override
  public void setResponsePage()
  {
    if (this.editPageSupport.isUpdateAndNext()) {
      this.editPageSupport.setUpdateAndNext(false);
      final AddressCampaignValueListPage listPage = (AddressCampaignValueListPage) this.returnToPage;
      final Iterator<AddressDO> it = listPage.getList().iterator();
      while (it.hasNext()) {
        if (it.next().getId().equals(getHighlightedRowId()) && it.hasNext()) {
          // Found current entry and next entry available.
          final AddressDO address = it.next();
          final PageParameters parameters = new PageParameters();
          parameters.add(AddressCampaignValueEditPage.PARAMETER_ADDRESS_ID, String.valueOf(address.getId()));
          parameters.add(AddressCampaignValueEditPage.PARAMETER_ADDRESS_CAMPAIGN_ID,
              String.valueOf(getData().getAddressCampaignId()));
          final AddressCampaignValueEditPage editPage = new AddressCampaignValueEditPage(parameters);
          editPage.setReturnToPage(this.returnToPage);
          setResponsePage(editPage);
          return;
        }
      }
    }
    super.setResponsePage();
  }

  /**
   * @return Address id instead of address campaign value id.
   * @see org.projectforge.web.wicket.AbstractEditPage#getHighlightedRowId()
   */
  @Override
  protected Serializable getHighlightedRowId()
  {
    return getData().getAddressId();
  }

  @Override
  protected AddressCampaignValueDao getBaseDao()
  {
    return addressCampaignValueDao;
  }

  @Override
  protected AddressCampaignValueEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage,
      final AddressCampaignValueDO data)
  {
    return new AddressCampaignValueEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
