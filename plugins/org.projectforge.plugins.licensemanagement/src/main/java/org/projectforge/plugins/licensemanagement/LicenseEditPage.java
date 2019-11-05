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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

/**
 * The controller of the edit formular page. Most functionality such as insert, update, delete etc. is done by the super
 * class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@EditPage(defaultReturnPage = LicenseListPage.class)
public class LicenseEditPage extends AbstractEditPage<LicenseDO, LicenseEditForm, LicenseDao>
{
  private static final long serialVersionUID = -5058143025817192156L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LicenseEditPage.class);

  @SpringBean
  private LicenseDao licenseDao;

  public LicenseEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.licensemanagement");
    init();
    if (isNew()) {
      getData().setNumberOfLicenses(1);
    }
  }

  @Override
  protected LicenseDao getBaseDao()
  {
    return licenseDao;
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    licenseDao.setOwners(getData(), form.assignOwnersListHelper.getAssignedItems());
    return super.onSaveOrUpdate();
  }

  @Override
  protected LicenseEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final LicenseDO data)
  {
    return new LicenseEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
