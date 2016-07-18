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

package org.projectforge.plugins.poll.result;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.plugins.poll.attendee.PollAttendeeDO;
import org.projectforge.plugins.poll.event.PollEventDO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_POLL_RESULT", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_plugin_poll_result_tenant_id", columnList = "tenant_id")
})
public class PollResultDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -8378182859274204836L;

  @IndexedEmbedded(depth = 1)
  private PollEventDO pollEvent;

  @IndexedEmbedded(depth = 1)
  private PollAttendeeDO pollAttendee;

  private boolean result;

  public PollResultDO()
  {

  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "poll_event_fk")
  /**
   * @return the pollEvent
   */
  public PollEventDO getPollEvent()
  {
    return pollEvent;
  }

  /**
   * @param pollEvent the pollEvent to set
   * @return this for chaining.
   */
  public PollResultDO setPollEvent(final PollEventDO pollEvent)
  {
    this.pollEvent = pollEvent;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "poll_attendee_fk")
  /**
   * @return the pollAttendee
   */
  public PollAttendeeDO getPollAttendee()
  {
    return pollAttendee;
  }

  /**
   * @param pollAttendee the pollAttendee to set
   * @return this for chaining.
   */
  public PollResultDO setPollAttendee(final PollAttendeeDO pollAttendee)
  {
    this.pollAttendee = pollAttendee;
    return this;
  }

  @Column
  /**
   * @return the result
   */
  public boolean isResult()
  {
    return result;
  }

  /**
   * @param result the result to set
   * @return this for chaining.
   */
  public PollResultDO setResult(final boolean result)
  {
    this.result = result;
    return this;
  }
}
