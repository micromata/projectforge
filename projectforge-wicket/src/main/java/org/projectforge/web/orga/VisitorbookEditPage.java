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

package org.projectforge.web.orga;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.orga.VisitorbookDO;
import org.projectforge.business.orga.VisitorbookService;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = VisitorbookListPage.class)
public class VisitorbookEditPage extends AbstractEditPage<VisitorbookDO, VisitorbookEditForm, VisitorbookService>
    implements ISelectCallerPage
{
  private static final long serialVersionUID = -3899191243765232906L;

  private static final Logger log = Logger.getLogger(VisitorbookEditPage.class);

  @SpringBean
  private VisitorbookService visitorbookService;

  public VisitorbookEditPage(final PageParameters parameters)
  {
    super(parameters, "orga.visitorbook");
    init();
  }

  /**
   * @see ISelectCallerPage#select(String, Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
  }

  /**
   * @see ISelectCallerPage#unselect(String)
   */
  @Override
  public void unselect(final String property)
  {
  }

  /**
   * @see ISelectCallerPage#cancelSelection(String)
   */
  @Override
  public void cancelSelection(final String property)
  {
  }

  @Override
  protected VisitorbookService getBaseDao()
  {
    return visitorbookService;
  }

  @Override
  protected VisitorbookEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final VisitorbookDO data)
  {
    return new VisitorbookEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#afterSaveOrUpdate()
   */
  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    super.onSaveOrUpdate();
    getData().getContactPersons().addAll(form.assignContactPersonsListHelper.getItemsToAssign());
    getData().getContactPersons().removeAll(form.assignContactPersonsListHelper.getItemsToUnassign());
    return null;
  }

}
