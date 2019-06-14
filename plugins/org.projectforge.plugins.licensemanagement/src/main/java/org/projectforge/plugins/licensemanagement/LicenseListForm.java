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

import org.slf4j.Logger;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.AbstractListForm;

/**
 * The list formular for the list view (this example has no filter settings). See ToDoListPage for seeing how to use
 * filter settings.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LicenseListForm extends AbstractListForm<BaseSearchFilter, LicenseListPage>
{
  private static final long serialVersionUID = -8159930022688216785L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LicenseListForm.class);

  public LicenseListForm(final LicenseListPage parentPage)
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
