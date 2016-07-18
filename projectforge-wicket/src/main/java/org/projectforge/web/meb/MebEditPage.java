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

package org.projectforge.web.meb;

import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.meb.MebDao;
import org.projectforge.business.meb.MebEntryDO;
import org.projectforge.business.meb.MebEntryStatus;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = MebListPage.class)
public class MebEditPage extends AbstractEditPage<MebEntryDO, MebEditForm, MebDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = -652121498646785007L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MebEditPage.class);

  @SpringBean
  private MebDao mebDao;

  public MebEditPage(final PageParameters parameters)
  {
    super(parameters, "meb");
    super.init();
    if (isNew() == true) {
      getData().setDate(new Date());
      mebDao.setOwner(getData(), getUserId());
      getData().setSender("");
      getData().setStatus(MebEntryStatus.OPEN);
    } else if (getData().getOwnerId() != null && getData().getStatus() == MebEntryStatus.RECENT) {
      // Mark entry as read (OPEN) automatically.
      getData().setStatus(MebEntryStatus.OPEN);
      mebDao.internalUpdate(getData());
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  public void select(final String property, final Object selectedValue)
  {
    if ("ownerId".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      getBaseDao().setOwner(getData(), id);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  public void unselect(final String property)
  {
    if ("ownerId".equals(property) == true) {
      getData().setOwner(null);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  protected MebDao getBaseDao()
  {
    return mebDao;
  }

  @Override
  protected MebEditForm newEditForm(final AbstractEditPage< ? , ? , ? > parentPage, final MebEntryDO data)
  {
    return new MebEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  protected void createTimesheet()
  {
    final PageParameters parameters = new PageParameters();
    parameters.add(TimesheetEditPage.PARAMETER_KEY_START_DATE_IN_MILLIS, getData().getDate().getTime());
    parameters.add(TimesheetEditPage.PARAMETER_KEY_DESCRIPTION, getData().getMessage());
    final TimesheetEditPage timesheetEditPage = new TimesheetEditPage(parameters);
    timesheetEditPage.setReturnToPage(this);
    setResponsePage(timesheetEditPage);
  }
}
