/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.projectforge.caldav.controller;

import java.util.List;

import org.projectforge.caldav.config.ApplicationContextProvider;
import org.projectforge.caldav.model.AddressBook;
import org.projectforge.caldav.model.Contact;
import org.projectforge.caldav.model.ContactsHome;
import org.projectforge.caldav.model.User;
import org.projectforge.caldav.model.UsersHome;
import org.projectforge.caldav.rest.AddressRest;

import io.milton.annotations.AddressBooks;
import io.milton.annotations.ChildrenOf;
import io.milton.annotations.ContactData;
import io.milton.annotations.Delete;
import io.milton.annotations.Get;
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;

@ResourceController
public class ProjectforgeCarddavController extends BaseDavController
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectforgeCarddavController.class);

  private AddressRest addressRest;

  public ProjectforgeCarddavController()
  {
    log.info("ProjectforgeCarddavController()");
  }

  @Root
  public ProjectforgeCarddavController getRoot()
  {
    return this;
  }

  @ChildrenOf
  public UsersHome getUsersHome(ProjectforgeCarddavController root)
  {
    if (usersHome == null) {
      log.info("Create new UsersHome");
      usersHome = new UsersHome();
    }
    return usersHome;
  }

  @ChildrenOf
  public ContactsHome getContactsHome(User user)
  {
    ContactsHome contactsHome = getUserCache().getUserContactsHomeMap().get(user.getPk());
    if (contactsHome != null) {
      return contactsHome;
    }
    log.info("Creating ContactsHome for user:" + user);
    contactsHome = new ContactsHome(user);
    getUserCache().getUserContactsHomeMap().put(user.getPk(), contactsHome);
    return contactsHome;
  }

  @ChildrenOf
  @AddressBooks
  public AddressBook getAddressBook(ContactsHome cons)
  {
    AddressBook addressBook = getUserCache().getUserAddressBookMap().get(cons.getUser().getPk());
    if (addressBook != null) {
      return addressBook;
    }
    log.info("Creating AddressBook for user:" + cons.getUser());
    addressBook = new AddressBook(cons.getUser());
    getUserCache().getUserAddressBookMap().put(cons.getUser().getPk(), addressBook);
    return addressBook;
  }

  @ChildrenOf
  public List<Contact> getContact(AddressBook ab)
  {
    return getAddressRest().getContactList(ab);
  }

  @Get
  @ContactData
  public byte[] getContactData(Contact c)
  {
    return c.getVcardData();
  }

  @PutChild
  public Contact createContact(AddressBook ab, byte[] vcardBytearray, String newName)
  {
    log.info("CreateContact: " + newName);
    Contact c = getAddressRest().createContact(ab, vcardBytearray);
    return c;
  }

  @PutChild
  public Contact updateContact(Contact c, byte[] vcardBytearray)
  {
    log.info("updateContact: " + c.getName());
    c = getAddressRest().updateContact(c, vcardBytearray);
    return c;
  }

  @Delete
  public void deleteContact(Contact c)
  {
    log.info("deleteContact: " + c.getName());
    getAddressRest().deleteContact(c);
  }

  private AddressRest getAddressRest()
  {
    if (addressRest == null) {
      addressRest = ApplicationContextProvider.getApplicationContext().getBean(AddressRest.class);
    }
    return addressRest;
  }

}
