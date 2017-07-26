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

package org.projectforge.plugins.poll.attendee;

import java.util.Collection;
import java.util.List;

import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

/**
 * <b>ATTENTION</b> Just use this for disabled selections!
 *
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class PollAttendeeDisabledChoiceProvider extends ChoiceProvider<PollAttendeeDO>
{
  private static final long serialVersionUID = 6025289092378986901L;

  private final List<PollAttendeeDO> attendees;

  /**
   *
   */
  public PollAttendeeDisabledChoiceProvider(final List<PollAttendeeDO> attendees)
  {
    this.attendees = attendees;
  }

  @Override
  public String getDisplayValue(final PollAttendeeDO choice)
  {
    return choice == null ? "" : choice.toString();
  }

  @Override
  public String getIdValue(final PollAttendeeDO choice)
  {
    return choice.getUser() == null ? choice.getEmail() : "" + choice.getUser().getId();
  }

  @Override
  public void query(final String term, final int page, final Response<PollAttendeeDO> response)
  {
    // do nothing
  }

  @Override
  public Collection<PollAttendeeDO> toChoices(final Collection<String> ids)
  {
    return attendees;
  }

}
