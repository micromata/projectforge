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

package org.projectforge.plugins.poll.result;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.poll.PollBasePage;
import org.projectforge.plugins.poll.PollDao;
import org.projectforge.plugins.poll.attendee.PollAttendeeDao;
import org.projectforge.plugins.poll.event.PollEventDao;

/**
 * 
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 *
 */
public class PollResultPage extends PollBasePage
{
  private static final long serialVersionUID = 7667632498760754905L;

  @SpringBean
  private PollDao pollDao;

  @SpringBean
  private PollAttendeeDao pollAttendeeDao;

  @SpringBean
  private PollEventDao pollEventDao;

  @SpringBean
  private PollResultDao pollResultDao;

  /**
   * 
   */
  public PollResultPage(final PageParameters parameters)
  {
    super(parameters);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString("plugins.poll.result");
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onConfirm()
   */
  @Override
  protected void onConfirm()
  {
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onCancel()
   */
  @Override
  protected void onCancel()
  {
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onBack()
   */
  @Override
  protected void onBack()
  {
    //    setResponsePage()
  }

}
