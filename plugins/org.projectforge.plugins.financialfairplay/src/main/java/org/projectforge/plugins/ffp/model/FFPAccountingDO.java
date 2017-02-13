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

  @PropertyInfo(i18nKey = "plugins.ffp.comment")
  private String comment;

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
  public BigDecimal getWeighting()
  {
    return weighting;
  }

  public void setWeighting(BigDecimal weighting)
  {
    this.weighting = weighting;
  }

  @Column
  public String getComment()
  {
    return comment;
  }

  public void setComment(String comment)
  {
    this.comment = comment;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof FFPAccountingDO == false) {
      return false;
    }
    FFPAccountingDO other = (FFPAccountingDO) o;
    if (this.getId() != null && other.getId() != null) {
      return this.getId().equals(other.getId());
    }
    if (this.getEvent() != null && this.getEvent().getId() != null && other.getEvent() != null && other.getEvent().getId() != null
        && this.getAttendee() != null && other.getAttendee() != null) {
      return this.getEvent().getId().equals(other.getEvent().getId())
          && this.getAttendee().getId().equals(other.getAttendee().getId());
    }
    //Case new event
    if (this.getAttendee() != null && other.getAttendee() != null) {
      return this.getAttendee().getId().equals(other.getAttendee().getId());
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    int result = (getId() != null) ? getId().hashCode() : 0;
    result = 31 * result + (event != null && event.getId() != null ? event.getId().hashCode() : 0);
    result = 31 * result + (attendee != null ? attendee.getId().hashCode() : 0);
    return result;
  }
}
