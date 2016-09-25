package org.projectforge.plugins.ffp.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_ACCOUNTING")
@WithHistory
public class FFPAccountingDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -1361266966025898919L;

  private FFPEventDO event;

  private EmployeeDO attendee;

  @PropertyInfo(i18nKey = "plugins.ffp.value")
  private BigDecimal value;

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
  @JoinColumn(name = "ATTENDEE_ID")
  public EmployeeDO getAttendee()
  {
    return attendee;
  }

  public void setAttendee(EmployeeDO attendee)
  {
    this.attendee = attendee;
  }

  @Column
  public BigDecimal getValue()
  {
    return value;
  }

  public void setValue(BigDecimal value)
  {
    this.value = value;
  }

}
