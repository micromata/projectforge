/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.crm;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.address.AddressStatus;
import org.projectforge.business.address.ContactStatus;
import org.projectforge.business.address.FormOfAddress;
import org.projectforge.business.address.InstantMessagingType;
import org.projectforge.business.task.TaskDO;
import org.projectforge.common.StringHelper;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.LabelValueBean;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Entity
@Indexed
@Table(name = "T_CONTACT", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_contact_tenant_id", columnList = "tenant_id")
})
public class ContactDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -1177059694759828682L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContactDO.class);

  private TaskDO task;

  @PropertyInfo(i18nKey = "name")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name; // 255 not null

  @PropertyInfo(i18nKey = "firstName")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String firstName; // 255

  @PropertyInfo(i18nKey = "form")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private FormOfAddress form;

  @PropertyInfo(i18nKey = "title")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title; // 255

  @PropertyInfo(i18nKey = "birthday")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date birthday;

  @PropertyInfo(i18nKey = "contact.imValues")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String socialMediaValues;

  @PropertyInfo(i18nKey = "contact.emailValues")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String emailValues;

  @PropertyInfo(i18nKey = "contact.phoneValues")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String phoneValues;

  @PropertyInfo(i18nKey = "contact.contacts")
  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  @IndexedEmbedded(depth = 1)
  private Set<ContactEntryDO> contactEntries = null;

  private ContactStatus contactStatus = ContactStatus.ACTIVE;

  private AddressStatus addressStatus = AddressStatus.UPTODATE;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String publicKey; // 7000

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String fingerprint; // 255

  private Locale communicationLanguage;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String website; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String organization; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String division; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String positionText; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment; // 5000;

  // @FieldBridge(impl = HibernateSearchInstantMessagingBridge.class)
  // @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  // TODO: Prepared for hibernate search.
  private List<LabelValueBean<InstantMessagingType, String>> socialMedia = null;

  /**
   * Used for representation in the data base and for hibernate search (lucene).
   */
  static String getSocialMediaAsString(final List<LabelValueBean<InstantMessagingType, String>> list)
  {
    if (list == null || list.size() == 0) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final LabelValueBean<InstantMessagingType, String> lv : list) {
      if (StringUtils.isBlank(lv.getValue()) == true) {
        continue; // Do not write empty entries.
      }
      if (first == true) {
        first = false;
      } else {
        buf.append("\n");
      }
      buf.append(lv.getLabel()).append("=").append(lv.getValue());
    }
    if (first == true) {
      return null; // No entry was written.
    }
    return buf.toString();
  }

  /**
   * List of instant messaging contacts in the form of a property file: {skype=hugo.mustermann\naim=12345dse}. Only for
   * data base access, use getter an setter of instant messaging instead.
   *
   * @return
   */
  // @Column(name = "instant_messaging", length = 4000)
  @Transient
  // TODO: Prepared for data base persistence.
  public String getSocialMedia4DB()
  {
    return getSocialMediaAsString(socialMedia);
  }

  public void setSocialMedia4DB(final String properties)
  {
    if (StringUtils.isBlank(properties) == true) {
      this.socialMedia = null;
    } else {
      final StringTokenizer tokenizer = new StringTokenizer(properties, "\n");
      while (tokenizer.hasMoreTokens() == true) {
        final String line = tokenizer.nextToken();
        if (StringUtils.isBlank(line) == true) {
          continue;
        }
        final int idx = line.indexOf('=');
        if (idx <= 0) {
          log.error("Wrong social media entry format in data base: " + line);
          continue;
        }
        String label = line.substring(0, idx);
        final String value = "";
        if (idx < line.length()) {
          label = line.substring(idx);
        }
        InstantMessagingType type = null;
        try {
          type = InstantMessagingType.get(label);
        } catch (final Exception ex) {
          log.error("Ignoring unknown social media entry: " + label, ex);
          continue;
        }
        setSocialMedia(type, value);
      }
    }
  }

  /**
   * Instant messaging settings as property file.
   *
   * @return
   */
  @Transient
  public List<LabelValueBean<InstantMessagingType, String>> getSocialMedia()
  {
    return socialMedia;
  }

  public void setSocialMedia(final InstantMessagingType type, final String value)
  {
    if (this.socialMedia == null) {
      this.socialMedia = new ArrayList<LabelValueBean<InstantMessagingType, String>>();
    } else {
      for (final LabelValueBean<InstantMessagingType, String> entry : this.socialMedia) {
        if (entry.getLabel() == type) {
          // Entry found;
          if (StringUtils.isBlank(value) == true) {
            // Remove this entry:
            this.socialMedia.remove(entry);
          } else {
            // Modify existing entry:
            entry.setValue(value);
          }
          return;
        }
      }
    }
    this.socialMedia.add(new LabelValueBean<InstantMessagingType, String>(type, value));
  }

  /**
   * Get the contact entries for this object.
   */
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "contact",
      targetEntity = ContactEntryDO.class)
  @IndexColumn(name = "number", base = 1)
  public Set<ContactEntryDO> getContactEntries()
  {
    return this.contactEntries;
  }

  public ContactDO setContactEntries(final Set<ContactEntryDO> contactEntries)
  {
    this.contactEntries = contactEntries;
    return this;
  }

  /**
   * @param number
   * @return ContactEntryDO with given position number or null (iterates through the list of contacts and compares the
   * number), if not exist.
   */
  public ContactEntryDO getContactEntry(final short number)
  {
    if (contactEntries == null) {
      return null;
    }
    for (final ContactEntryDO contact : this.contactEntries) {
      if (contact.getNumber() == number) {
        return contact;
      }
    }
    return null;
  }

  public ContactDO addContactEntry(final ContactEntryDO contactEntry)
  {
    ensureAndGetContactEntries();
    short number = 1;
    for (final ContactEntryDO pos : contactEntries) {
      if (pos.getNumber() >= number) {
        number = pos.getNumber();
        number++;
      }
    }
    contactEntry.setNumber(number);
    contactEntry.setContact(this);
    this.contactEntries.add(contactEntry);
    return this;
  }

  public Set<ContactEntryDO> ensureAndGetContactEntries()
  {
    if (this.contactEntries == null) {
      setContactEntries(new LinkedHashSet<ContactEntryDO>());
    }
    return getContactEntries();
  }

  @Column
  public Date getBirthday()
  {
    return birthday;
  }

  public ContactDO setBirthday(final Date birthday)
  {
    this.birthday = birthday;
    return this;
  }

  @Column(name = "first_name", length = 255)
  public String getFirstName()
  {
    return firstName;
  }

  public ContactDO setFirstName(final String firstName)
  {
    this.firstName = firstName;
    return this;
  }

  @Transient
  public String getFullName()
  {
    return StringHelper.listToString(", ", name, firstName);
  }

  @Transient
  public String getFullNameWithTitleAndForm()
  {
    final StringBuffer buf = new StringBuffer();
    if (getForm() != null) {
      buf.append(ThreadLocalUserContext.getLocalizedString(getForm().getI18nKey())).append(" ");
    }
    if (getTitle() != null) {
      buf.append(getTitle()).append(" ");
    }
    if (getFirstName() != null) {
      buf.append(getFirstName()).append(" ");
    }
    if (getName() != null) {
      buf.append(getName());
    }
    return buf.toString();
  }

  @Column(length = 255, nullable = false)
  public String getName()
  {
    return name;
  }

  public ContactDO setName(final String name)
  {
    this.name = name;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "form", length = 10)
  public FormOfAddress getForm()
  {
    return form;
  }

  public ContactDO setForm(final FormOfAddress form)
  {
    this.form = form;
    return this;
  }

  /**
   * Not used as object due to performance reasons.
   *
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  public TaskDO getTask()
  {
    return task;
  }

  public void setTask(final TaskDO task)
  {
    this.task = task;
  }

  @Transient
  public Integer getTaskId()
  {
    if (this.task == null) {
      return null;
    }
    return task.getId();
  }

  @Column(length = 255)
  public String getTitle()
  {
    return title;
  }

  public ContactDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  @Column
  public String getSocialMediaValues()
  {
    return socialMediaValues;
  }

  public ContactDO setSocialMediaValues(final String socialMediaValues)
  {
    this.socialMediaValues = socialMediaValues;
    return this;
  }

  @Column
  public String getEmailValues()
  {
    return emailValues;
  }

  public ContactDO setEmailValues(final String emailValues)
  {
    this.emailValues = emailValues;
    return this;
  }

  @Column
  public String getPhoneValues()
  {
    return phoneValues;
  }

  public ContactDO setPhoneValues(final String phoneValues)
  {
    this.phoneValues = phoneValues;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_status", length = 20, nullable = false)
  public ContactStatus getContactStatus()
  {
    return contactStatus;
  }

  public ContactDO setContactStatus(final ContactStatus contactStatus)
  {
    this.contactStatus = contactStatus;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "address_status", length = 20, nullable = false)
  public AddressStatus getAddressStatus()
  {
    return addressStatus;
  }

  public ContactDO setAddressStatus(final AddressStatus addressStatus)
  {
    this.addressStatus = addressStatus;
    return this;
  }

  @Column(name = "public_key", length = 7000)
  public String getPublicKey()
  {
    return publicKey;
  }

  public ContactDO setPublicKey(final String publicKey)
  {
    this.publicKey = publicKey;
    return this;
  }

  @Column(length = 255)
  public String getFingerprint()
  {
    return fingerprint;
  }

  public ContactDO setFingerprint(final String fingerprint)
  {
    this.fingerprint = fingerprint;
    return this;
  }

  /**
   * @return The communication will take place in this language.
   */
  @Column(name = "communication_language")
  public Locale getCommunicationLanguage()
  {
    return communicationLanguage;
  }

  public ContactDO setCommunicationLanguage(final Locale communicationLanguage)
  {
    this.communicationLanguage = communicationLanguage;
    return this;
  }

  @Column(length = 255)
  public String getWebsite()
  {
    return website;
  }

  public ContactDO setWebsite(final String website)
  {
    this.website = website;
    return this;
  }

  @Column(length = 255)
  public String getOrganization()
  {
    return organization;
  }

  public ContactDO setOrganization(final String organization)
  {
    this.organization = organization;
    return this;
  }

  @Column(name = "comment", length = 5000)
  public String getComment()
  {
    return comment;
  }

  public ContactDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  @Column(length = 255)
  public String getPositionText()
  {
    return positionText;
  }

  public ContactDO setPositionText(final String positionText)
  {
    this.positionText = positionText;
    return this;
  }

  @Column(length = 255)
  public String getDivision()
  {
    return division;
  }

  public ContactDO setDivision(final String division)
  {
    this.division = division;
    return this;
  }

}
