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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = CustomerListPage.class)
public class CustomerEditPage extends AbstractEditPage<KundeDO, CustomerEditForm, KundeDao>
{
  private static final long serialVersionUID = 8763884579951937296L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerEditPage.class);

  @SpringBean
  private KundeDao kundeDao;

  public CustomerEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.kunde");
    init();
  }

  @Override
  protected KundeDao getBaseDao()
  {
    return kundeDao;
  }

  @Override
  protected CustomerEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final KundeDO data)
  {
    return new CustomerEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  public boolean isNew()
  {
    boolean isNew = super.isNew();
    if (isNew == false) {
      isNew = getData().getCreated() == null;
    }
    return isNew;
  }
}
