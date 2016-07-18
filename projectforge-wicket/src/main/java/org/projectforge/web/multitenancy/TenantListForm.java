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

package org.projectforge.web.multitenancy;

import org.apache.log4j.Logger;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.AbstractListForm;

public class TenantListForm extends AbstractListForm<BaseSearchFilter, TenantListPage>
{
  private static final long serialVersionUID = -7136808109586845027L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TenantListForm.class);

  @SpringBean
  private TenantDao tenantDao;

  public TenantListForm(final TenantListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected BaseSearchFilter newSearchFilterInstance()
  {
    return new BaseSearchFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
