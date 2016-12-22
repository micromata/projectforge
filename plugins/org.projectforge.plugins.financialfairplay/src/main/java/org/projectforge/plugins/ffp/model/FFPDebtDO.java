package org.projectforge.plugins.ffp.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_DEBT")
@WithHistory
public class FFPDebtDO extends DefaultBaseDO {

	private static final long serialVersionUID = 661129943149832435L;

	private FFPEventDO event;

	private EmployeeDO from;

	private EmployeeDO to;

	private BigDecimal value;

	private boolean approvedByFrom;

	private boolean approvedByTo;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "EVENT_ID")
	public FFPEventDO getEvent() {
		return event;
	}

	public void setEvent(FFPEventDO event) {
		this.event = event;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ATTENDEE_ID")
	public EmployeeDO getFrom() {
		return from;
	}

	public void setFrom(EmployeeDO from) {
		this.from = from;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ATTENDEE_ID")
	public EmployeeDO getTo() {
		return to;
	}

	public void setTo(EmployeeDO to) {
		this.to = to;
	}

	@Column
	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public boolean isApprovedByFrom() {
		return approvedByFrom;
	}

	public void setApprovedByFrom(boolean approvedByFrom) {
		this.approvedByFrom = approvedByFrom;
	}

	public boolean isApprovedByTo() {
		return approvedByTo;
	}

	public void setApprovedByTo(boolean approvedByTo) {
		this.approvedByTo = approvedByTo;
	}

}
