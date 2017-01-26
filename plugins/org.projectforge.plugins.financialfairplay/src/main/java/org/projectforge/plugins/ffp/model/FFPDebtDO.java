package org.projectforge.plugins.ffp.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_DEBT",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "EVENT_ID", "ATTENDEE_ID_FROM", "ATTENDEE_ID_TO" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_T_PLUGIN_FINANCIALFAIRPLAY_DEBT_event_id", columnList = "EVENT_ID"),
        @javax.persistence.Index(name = "idx_fk_T_PLUGIN_FINANCIALFAIRPLAY_DEBT_from_id", columnList = "ATTENDEE_ID_FROM"),
        @javax.persistence.Index(name = "idx_fk_T_PLUGIN_FINANCIALFAIRPLAY_DEBT_to_id", columnList = "ATTENDEE_ID_TO")
    })
@WithHistory
public class FFPDebtDO extends DefaultBaseDO
{

  private static final long serialVersionUID = 661129943149832435L;

  private FFPEventDO event;

  private EmployeeDO from;

  private EmployeeDO to;

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
  @JoinColumn(name = "ATTENDEE_ID_FROM")
  public EmployeeDO getFrom()
  {
    return from;
  }

  public void setFrom(EmployeeDO from)
  {
    this.from = from;
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "ATTENDEE_ID_TO")
  public EmployeeDO getTo()
  {
    return to;
  }

  public void setTo(EmployeeDO to)
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
