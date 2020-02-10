package org.projectforge.caldav.model;

import java.util.Date;

import io.milton.annotations.ModifiedDate;
import io.milton.annotations.UniqueId;

public class Contact
{

  private long id;
  private String name;  // filename for the meeting. Must be unique within the user
  private Date modifiedDate;
  private byte[] vcardData;
  private AddressBook addressBook;

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  @UniqueId
  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  @ModifiedDate
  public Date getModifiedDate()
  {
    return modifiedDate;
  }

  public void setModifiedDate(Date modifiedDate)
  {
    this.modifiedDate = modifiedDate;
  }

  public byte[] getVcardData()
  {
    return vcardData;
  }

  public void setVcardData(byte[] vcardData)
  {
    this.vcardData = vcardData;
  }

  public AddressBook getAddressBook()
  {
    return addressBook;
  }

  public void setAddressBook(final AddressBook addressBook)
  {
    this.addressBook = addressBook;
  }
}
