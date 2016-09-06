package org.projectforge.plugins.ffp.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_ACCOUNTING")
@WithHistory
public class FFPAccountingDO extends DefaultBaseDO
{
  private FFPEventDO event;

  private EmployeeDO attendee;

  private BigDecimal value;

  public FFPEventDO getEvent()
  {
    return event;
  }

  public void setEvent(FFPEventDO event)
  {
    this.event = event;
  }

  public EmployeeDO getAttendee()
  {
    return attendee;
  }

  public void setAttendee(EmployeeDO attendee)
  {
    this.attendee = attendee;
  }

  public BigDecimal getValue()
  {
    return value;
  }

  public void setValue(BigDecimal value)
  {
    this.value = value;
  }

}
