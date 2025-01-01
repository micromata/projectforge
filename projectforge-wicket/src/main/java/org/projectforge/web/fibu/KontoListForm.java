/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.slf4j.Logger;


public class KontoListForm extends AbstractListForm<BaseSearchFilter, KontoListPage>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KontoListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  public KontoListForm(final KontoListPage parentPage)
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
