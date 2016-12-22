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

  @PropertyInfo(i18nKey = "plugins.ffp.weighting")
  private BigDecimal weighting;

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

  @Column
  public BigDecimal getWeighting()
  {
    return weighting;
  }

  public void setWeighting(BigDecimal weighting)
  {
    this.weighting = weighting;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof FFPAccountingDO == false) {
      return false;
    }
    FFPAccountingDO other = (FFPAccountingDO) o;
    if (this.getPk() != null && other.getPk() != null) {
      return this.getPk().equals(other.getPk());
    }
    if (this.getEvent() != null && this.getEvent().getPk() != null && other.getEvent() != null && other.getEvent().getPk() != null
        && this.getAttendee() != null && other.getAttendee() != null) {
      return this.getEvent().getPk().equals(other.getEvent().getPk())
          && this.getAttendee().getPk().equals(other.getAttendee().getPk());
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    int result = 31 * (event != null ? event.getPk().hashCode() : 0);
    result = 31 * result + (attendee != null ? attendee.getPk().hashCode() : 0);
    return result;
  }
}
