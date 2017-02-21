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

package org.projectforge.plugins.ffp.wicket;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = FFPEventListPage.class)
public class FFPEventEditPage extends AbstractEditPage<FFPEventDO, FFPEventEditForm, FFPEventService>
    implements ISelectCallerPage
{
  private static final long serialVersionUID = -3899191243765232906L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FFPEventEditPage.class);

  @SpringBean
  private FFPEventService eventService;

  public FFPEventEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.ffp");
    init();
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#afterSaveOrUpdate()
   */
  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    super.afterSaveOrUpdate();
    getData().setAccountingList(new HashSet<>(form.accountingList));
    form.assignAttendeesListHelper.getItemsToAssign().forEach(emp -> {
      getData().addAttendee(emp);
    });
    form.assignAttendeesListHelper.getItemsToUnassign().forEach(emp -> {
      getData().getAttendeeList().remove(emp);
    });
    return null;
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    if (getData().getFinished() == true && eventService.debtExists(getData()) == false) {
      eventService.createDept(getData());
    }
    return null;
  }

  @Override
  protected FFPEventService getBaseDao()
  {
    return eventService;
  }

  @Override
  protected FFPEventEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final FFPEventDO data)
  {
    return new FFPEventEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  protected void createOrUpdate()
  {
    if (isNew()) {
      super.create();
    } else {
      super.update();
    }
  }
}
