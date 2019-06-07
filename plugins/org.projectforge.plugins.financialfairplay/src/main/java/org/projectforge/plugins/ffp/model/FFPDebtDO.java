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

package org.projectforge.plugins.ffp.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_DEBT",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "EVENT_ID", "ATTENDEE_USER_ID_FROM", "ATTENDEE_USER_ID_TO" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_T_PLUGIN_FINANCIALFAIRPLAY_DEBT_event_id", columnList = "EVENT_ID"),
        @javax.persistence.Index(name = "idx_fk_T_PLUGIN_FINANCIALFAIRPLAY_DEBT_from_id", columnList = "ATTENDEE_USER_ID_FROM"),
        @javax.persistence.Index(name = "idx_fk_T_PLUGIN_FINANCIALFAIRPLAY_DEBT_to_id", columnList = "ATTENDEE_USER_ID_TO")
    })
@WithHistory
public class FFPDebtDO extends DefaultBaseDO
{

  private static final long serialVersionUID = 661129943149832435L;

  private FFPEventDO event;

  private PFUserDO from;

  private PFUserDO to;

  @PropertyInfo(i18nKey = "plugins.ffp.value")
  private BigDecimal value;

  @PropertyInfo(i18nKey = "plugins.ffp.approvedByFrom")
  private boolean approvedByFrom;

  @PropertyInfo(i18nKey = "plugins.ffp.approvedByTo")
  private boolean approvedByTo;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "EVENT_ID")
  public FFPEventDO getEvent()
  {
    return event;
  }

  public void setEvent(FFPEventDO event)
  {
    this.event = event;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "ATTENDEE_USER_ID_FROM")
  public PFUserDO getFrom()
  {
    return from;
  }

  public void setFrom(PFUserDO from)
  {
    this.from = from;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "ATTENDEE_USER_ID_TO")
  public PFUserDO getTo()
  {
    return to;
  }

  public void setTo(PFUserDO to)
  {
    this.to = to;
  }

  @Column(nullable = false)
  public BigDecimal getValue()
  {
    return value;
  }

  public void setValue(BigDecimal value)
  {
    this.value = value;
  }

  @Column(nullable = false)
  public boolean isApprovedByFrom()
  {
    return approvedByFrom;
  }

  public void setApprovedByFrom(boolean approvedByFrom)
  {
    this.approvedByFrom = approvedByFrom;
  }

  @Column(nullable = false)
  public boolean isApprovedByTo()
  {
    return approvedByTo;
  }

  public void setApprovedByTo(boolean approvedByTo)
  {
    this.approvedByTo = approvedByTo;
  }

}
