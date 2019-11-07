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

package org.projectforge.plugins.poll;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

/**
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
@EditPage(defaultReturnPage = PollListPage.class)
public class PollEditPage extends AbstractEditPage<PollDO, PollEditForm, PollDao>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PollEditPage.class);

  private static final long serialVersionUID = -3352981782657771662L;

  @SpringBean
  private PollDao pollDao;

  /**
   * @param parameters
   * @param i18nPrefix
   */
  public PollEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.poll");
    init();
    if (form.isNew()) {
      NewPollPage.redirectToNewPollPage(parameters);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected PollDao getBaseDao()
  {
    return pollDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#newEditForm(org.projectforge.web.wicket.AbstractEditPage,
   *      org.projectforge.core.AbstractBaseDO)
   */
  @Override
  protected PollEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final PollDO data)
  {
    return new PollEditForm(this, data);
  }
}
