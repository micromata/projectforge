package org.projectforge.plugins.ffp.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Resolution;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_EVENT")
@WithHistory
public class FFPEventDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 1579119768006685087L;

  @PropertyInfo(i18nKey = "plugins.ffp.title")
  private String title;

  @PropertyInfo(i18nKey = "plugins.ffp.eventDate")
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date eventDate;

  private List<EmployeeDO> attendeeList;

  private List<FFPAccountingDO> accountingList;

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
      joinColumns = @JoinColumn(name = "ATTENDEE_PK", referencedColumnName = "PK"),
      inverseJoinColumns = @JoinColumn(name = "EVENT_PK", referencedColumnName = "PK"))
  public List<EmployeeDO> getAttendeeList()
  {
    if (attendeeList == null) {
      attendeeList = new ArrayList<>();
    }
    return attendeeList;
  }

  public void setAttendeeList(List<EmployeeDO> attendeeList)
  {
    this.attendeeList = attendeeList;
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "event", orphanRemoval = true)
  public List<FFPAccountingDO> getAccountingList()
  {
    if (accountingList == null) {
      accountingList = new ArrayList<>();
    }
    return accountingList;
  }

  public void setAccountingList(List<FFPAccountingDO> accountingList)
  {
    this.accountingList = accountingList;
  }

  @Transient
  public void addAttendee(EmployeeDO employee)
  {
    getAttendeeList().add(employee);
  }

}
