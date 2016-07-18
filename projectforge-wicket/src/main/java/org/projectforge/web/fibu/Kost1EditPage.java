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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = Kost1ListPage.class)
public class Kost1EditPage extends AbstractEditPage<Kost1DO, Kost1EditForm, Kost1Dao>
{
  private static final long serialVersionUID = 1029345943027440760L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Kost1EditPage.class);

  @SpringBean
  private Kost1Dao kost1Dao;

  public Kost1EditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.kost1");
    init();
  }

  @Override
  protected Kost1Dao getBaseDao()
  {
    return kost1Dao;
  }

  @Override
  protected Kost1EditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final Kost1DO data)
  {
    return new Kost1EditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
