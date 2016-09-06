package org.projectforge.plugins.ffp.model;

import java.sql.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_EVENT")
@WithHistory
public class FFPEventDO extends DefaultBaseDO
{
  private String title;

  private Date eventDate;

  private List<EmployeeDO> attendeeList;

  private List<FFPAccountingDO> accountingList;

  public String getTitle()
  {
    return title;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public Date getEventDate()
  {
    return eventDate;
  }

  public void setEventDate(Date eventDate)
  {
    this.eventDate = eventDate;
  }

  public List<EmployeeDO> getAttendeeList()
  {
    return attendeeList;
  }

  public void setAttendeeList(List<EmployeeDO> attendeeList)
  {
    this.attendeeList = attendeeList;
  }

  public List<FFPAccountingDO> getAccountingList()
  {
    return accountingList;
  }

  public void setAccountingList(List<FFPAccountingDO> accountingList)
  {
    this.accountingList = accountingList;
  }

}
