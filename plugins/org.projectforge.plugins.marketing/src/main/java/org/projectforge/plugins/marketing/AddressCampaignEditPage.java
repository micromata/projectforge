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

package org.projectforge.plugins.marketing;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

/**
 * The controler of the edit formular page. Most functionality such as insert, update, delete etc. is done by the super
 * class.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@EditPage(defaultReturnPage = AddressCampaignListPage.class)
public class AddressCampaignEditPage
    extends AbstractEditPage<AddressCampaignDO, AddressCampaignEditForm, AddressCampaignDao>
{
  private static final long serialVersionUID = -5058143025817192156L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressCampaignEditPage.class);

  @SpringBean
  private AddressCampaignDao addressCampaignDao;

  public AddressCampaignEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.marketing.addressCampaign");
    init();
  }

  @Override
  protected AddressCampaignDao getBaseDao()
  {
    return addressCampaignDao;
  }

  @Override
  protected AddressCampaignEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage,
      final AddressCampaignDO data)
  {
    return new AddressCampaignEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
