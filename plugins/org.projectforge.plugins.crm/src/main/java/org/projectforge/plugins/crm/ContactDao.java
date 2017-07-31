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

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.Validate;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Repository
public class ContactDao extends BaseDao<ContactDO>
{

  private Configuration configuration;

  @Autowired
  private TaskDao taskDao;

  private final XmlConverter<SocialMediaValue> socialMediaConverter;
  private final XmlConverter<PhoneValue> phoneConverter;
  private final XmlConverter<EmailValue> emailConverter;

  public ContactDao()
  {
    super(ContactDO.class);
    final SocialMediaValue socialMedia = new SocialMediaValue();
    final PhoneValue phone = new PhoneValue();
    final EmailValue email = new EmailValue();
    socialMediaConverter = new XmlConverter<SocialMediaValue>(socialMedia);
    phoneConverter = new XmlConverter<PhoneValue>(phone);
    emailConverter = new XmlConverter<EmailValue>(email);
  }

  public void setConfiguration(final Configuration configuration)
  {
    this.configuration = configuration;
  }

  public void setTaskDao(final TaskDao taskDao)
  {
    this.taskDao = taskDao;
  }

  //  private String getNormalizedFullname(final ContactDO address)
  //  {
  //    final StringBuilder builder = new StringBuilder();
  //    if (address.getFirstName() != null) {
  //      builder.append(address.getFirstName().toLowerCase().trim());
  //    }
  //    if (address.getName() != null) {
  //      builder.append(address.getName().toLowerCase().trim());
  //    }
  //    return builder.toString();
  //  }

  /**
   * @param address
   * @param taskId  If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setTask(final ContactDO address, final Integer taskId)
  {
    final TaskDO task = taskDao.getOrLoad(taskId);
    address.setTask(task);
  }

  /**
   * return Always true, no generic select access needed for address objects.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final ContactDO obj, final ContactDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, operationType, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUpdateAccess(Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final ContactDO obj, final ContactDO dbObj,
      final boolean throwException)
  {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    Validate.notNull(dbObj.getTaskId());
    Validate.notNull(obj.getTaskId());
    if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, OperationType.UPDATE,
        throwException) == false) {
      return false;
    }
    if (dbObj.getTaskId().equals(obj.getTaskId()) == false) {
      // User moves the object to another task:
      if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, OperationType.INSERT,
          throwException) == false) {
        // Inserting of object under new task not allowed.
        return false;
      }
      if (accessChecker.hasPermission(user, dbObj.getTaskId(), AccessType.TASKS, OperationType.DELETE,
          throwException) == false) {
        // Deleting of object under old task not allowed.
        return false;
      }
    }
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#newInstance()
   */
  @Override
  public ContactDO newInstance()
  {
    return new ContactDO();
  }

  /**
   * Exports xml string as List of Social Media values.
   *
   * @param SocialMediaValue values
   */
  public List<SocialMediaValue> readSocialMediaValues(final String valuesAsXml)
  {
    return socialMediaConverter.readValues(valuesAsXml);
  }

  /**
   * Exports the Social Media values as xml string.
   *
   * @param SocialMediaValue values
   */
  public String getSocialMediaValuesAsXml(final SocialMediaValue... values)
  {
    return socialMediaConverter.getValuesAsXml(values);
  }

  public String getSocialMediaValuesAsXml(final List<SocialMediaValue> values)
  {
    return socialMediaConverter.getValuesAsXml(values);
  }

  public List<EmailValue> readEmailValues(final String valuesAsXml)
  {
    return emailConverter.readValues(valuesAsXml);
  }

  public String getEmailValuesAsXml(final EmailValue... values)
  {
    return emailConverter.getValuesAsXml(values);
  }

  public String getEmailValuesAsXml(final List<EmailValue> values)
  {
    return emailConverter.getValuesAsXml(values);
  }

  public List<PhoneValue> readPhoneValues(final String valuesAsXml)
  {
    return phoneConverter.readValues(valuesAsXml);
  }

  public String getPhoneValuesAsXml(final PhoneValue... values)
  {
    return phoneConverter.getValuesAsXml(values);
  }

  public String getPhoneValuesAsXml(final List<PhoneValue> values)
  {
    return phoneConverter.getValuesAsXml(values);
  }

  public List<Locale> getUsedCommunicationLanguages()
  {
    @SuppressWarnings("unchecked")
    final List<Locale> list = (List<Locale>) getHibernateTemplate()
        .find(
            "select distinct a.communicationLanguage from ContactDO a where deleted=false and a.communicationLanguage is not null order by a.communicationLanguage");
    return list;
  }
}
