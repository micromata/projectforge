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

import org.apache.log4j.Logger;
import org.projectforge.web.wicket.AbstractListForm;

public class Kost2ArtListForm extends AbstractListForm<Kost2ArtListFilter, Kost2ArtListPage>
{
  private static final long serialVersionUID = -4055796963328089970L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Kost2ArtListForm.class);

  public Kost2ArtListForm(final Kost2ArtListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected Kost2ArtListFilter newSearchFilterInstance()
  {
    return new Kost2ArtListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
