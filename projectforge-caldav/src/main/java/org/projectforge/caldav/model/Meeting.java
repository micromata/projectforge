package org.projectforge.caldav.model;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.annotations.ModifiedDate;
import io.milton.annotations.Name;
import io.milton.annotations.UniqueId;

public class Meeting
{
  private static Logger log = LoggerFactory.getLogger(Meeting.class);

  private String uid;
  private Calendar calendar;
  private String name; // filename for the meeting. Must be unique within the user
  private Date createDate;
  private Date modifiedDate;
  private byte[] icalData;

  public Meeting(Calendar calendar)
  {
    this.calendar = calendar;
  }

  @UniqueId
  public String getUniqueId()
  {
    return uid;
  }

  @ModifiedDate
  public Date getModifiedDate()
  {
    return modifiedDate;
  }

  public Calendar getCalendar()
  {
    return calendar;
  }

  public void setCalendar(Calendar calendar)
  {
    this.calendar = calendar;
  }

  @Name
  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getUid()
  {
    return uid;
  }

  public void setUid(String uid)
  {
    this.uid = uid;
  }

  public void setModifiedDate(Date modifiedDate)
  {
    this.modifiedDate = modifiedDate;
  }

  public byte[] getIcalData()
  {
    return icalData;
  }

  public void setIcalData(byte[] icalData)
  {
    this.icalData = icalData;
  }

  public void setCreateDate(Date createDate)
  {
    this.createDate = createDate;
  }

  public Date getCreateDate()
  {
    return createDate;
  }
}