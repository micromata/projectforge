package org.projectforge.plugins.ffp.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_EVENT")
@WithHistory
public class FFPEventDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 1579119768006685087L;

  @PropertyInfo(i18nKey = "plugins.ffp.organizer")
  @IndexedEmbedded(includePaths = { "user.firstname", "user.lastname" })
  private EmployeeDO organizer;

  @PropertyInfo(i18nKey = "plugins.ffp.title")
  private String title;

  @PropertyInfo(i18nKey = "plugins.ffp.eventDate")
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date eventDate;

  private Set<EmployeeDO> attendeeList;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  private Set<FFPAccountingDO> accountingList;

  private boolean finished;

  @PropertyInfo(i18nKey = "plugins.ffp.commonDebtValue")
  private BigDecimal commonDebtValue;

  /**
   * The organizer.
   *
   * @return the user
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "organizer_id", nullable = false)
  public EmployeeDO getOrganizer()
  {
    return organizer;
  }

  /**
   * @param organizer the organizer to set
   */
  public void setOrganizer(final EmployeeDO organizer)
  {
    this.organizer = organizer;
  }

  @Column(nullable = false, length = 1000)
  public String getTitle()
  {
    return title;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  @Temporal(TemporalType.DATE)
  @Column(nullable = false)
  public Date getEventDate()
  {
    return eventDate;
  }

  public void setEventDate(Date eventDate)
  {
    this.eventDate = eventDate;
  }

  @ManyToMany
  @JoinTable(
      name = "T_PLUGIN_FINANCIALFAIRPLAY_EVENT_ATTENDEE",
      joinColumns = @JoinColumn(name = "EVENT_PK", referencedColumnName = "PK"),
      inverseJoinColumns = @JoinColumn(name = "ATTENDEE_PK", referencedColumnName = "PK"))
  public Set<EmployeeDO> getAttendeeList()
  {
    if (attendeeList == null) {
      attendeeList = new HashSet<>();
    }
    return attendeeList;
  }

  public void setAttendeeList(Set<EmployeeDO> attendeeList)
  {
    this.attendeeList = attendeeList;
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "event", orphanRemoval = true)
  public Set<FFPAccountingDO> getAccountingList()
  {
    if (accountingList == null) {
      accountingList = new HashSet<>();
    }
    return accountingList;
  }

  public void setAccountingList(Set<FFPAccountingDO> accountingList)
  {
    this.accountingList = accountingList;
  }

  @Transient
  public void addAttendee(EmployeeDO employee)
  {
    getAttendeeList().add(employee);
  }

  @Column
  public boolean getFinished()
  {
    return finished;
  }

  public void setFinished(boolean finished)
  {
    this.finished = finished;
  }

  @Column
  public BigDecimal getCommonDebtValue()
  {
    return commonDebtValue;
  }

  public void setCommonDebtValue(BigDecimal commonDebtValue)
  {
    this.commonDebtValue = commonDebtValue;
  }

  @Transient
  public String getStatus()
  {
    if (getFinished()) {
      return I18nHelper.getLocalizedMessage("plugins.ffp.status.closed");
    } else {
      return I18nHelper.getLocalizedMessage("plugins.ffp.status.open");
    }
  }
}
