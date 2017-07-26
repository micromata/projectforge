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

package org.projectforge.plugins.poll.event;

import java.util.Collection;
import java.util.List;

import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

/**
 * <b>ATTENTION</b> Just use this for disabled selections!
 *
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class PollEventDisabledChoiceProvider extends ChoiceProvider<PollEventDO>
{
  private static final long serialVersionUID = 6025289092378986901L;

  private final List<PollEventDO> attendees;

  /**
   *
   */
  public PollEventDisabledChoiceProvider(final List<PollEventDO> attendees)
  {
    this.attendees = attendees;
  }

  @Override
  public String getDisplayValue(final PollEventDO choice)
  {
    return choice == null ? "" : choice.toString();
  }

  @Override
  public String getIdValue(final PollEventDO choice)
  {
    return "" + choice.toString();
  }

  @Override
  public void query(final String term, final int page, final Response<PollEventDO> response)
  {
    // just do nothing
  }

  @Override
  public Collection<PollEventDO> toChoices(final Collection<String> ids)
  {
    return attendees;
  }

}
