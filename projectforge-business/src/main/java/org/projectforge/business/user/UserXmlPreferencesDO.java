/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.user;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;
import de.micromata.genome.jpa.DbRecord;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * For persistency of UserPreferencesData (stores them serialized).
 * The data are stored as xml.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Entity
@Table(name = "T_USER_XML_PREFS",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "key", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_user_xml_prefs_user_id", columnList = "user_id"),
        @javax.persistence.Index(name = "idx_fk_t_user_xml_prefs_tenant_id", columnList = "tenant_id")
    })
@JpaXmlPersist(beforePersistListener = UserXmlPreferenceXmlBeforePersistListener.class)
public class UserXmlPreferencesDO implements Serializable, DbRecord<Integer>
{
  public static final int MAX_SERIALIZED_LENGTH = 10000;

  private static final long serialVersionUID = 3203177155834463761L;

  /**
   * Don't forget to increase, if any changes in the object stored in user data are made. If not, the user preferences
   * will be lost because of unsupported (de)serialization.
   */
  public static final int CURRENT_VERSION = 4;

  private Integer id;

  private TenantDO tenant;

  private PFUserDO user;

  private String serializedSettings;

  private String key;

  private Date created;

  private Date lastUpdate;

  private int version;

  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  public void setId(final Integer id)
  {
    this.id = id;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  public TenantDO getTenant()
  {
    return this.tenant;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDO#getTenantId()
   */
  @Transient
  public Integer getTenantId()
  {
    return tenant != null ? tenant.getId() : null;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDO#setTenant(TenantDO)
   */
  public UserXmlPreferencesDO setTenant(final TenantDO tenant)
  {
    this.tenant = tenant;
    return this;
  }

  /**
   * The owner of this preference.
   *
   * @return the user
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   */
  public UserXmlPreferencesDO setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  @Transient
  public Integer getUserId()
  {
    if (this.user == null) {
      return null;
    }
    return user.getId();
  }

  /**
   * Optional if the user preference should be stored in its own data base entry.
   */
  @Column(length = 1000)
  public String getKey()
  {
    return key;
  }

  /**
   * @param key
   * @return this for chaining.
   */
  public UserXmlPreferencesDO setKey(final String key)
  {
    this.key = key;
    return this;
  }

  /**
   * Contains the serialized settings, stored in the database.
   *
   * @return
   */
  @Column(length = MAX_SERIALIZED_LENGTH)
  public String getSerializedSettings()
  {
    return serializedSettings;
  }

  /**
   * @param settings
   * @return this for chaining.
   */
  public UserXmlPreferencesDO setSerializedSettings(final String settings)
  {
    this.serializedSettings = settings;
    return this;
  }

  @Basic
  public Date getCreated()
  {
    return created;
  }

  public void setCreated(final Date created)
  {
    this.created = created;
  }

  public void setCreated()
  {
    this.created = new Date();
  }

  /**
   *
   * Last update will be modified automatically for every update of the database object.
   *
   * @return
   */
  @Basic
  @Column(name = "last_update")
  public Date getLastUpdate()
  {
    return lastUpdate;
  }

  public void setLastUpdate(final Date lastUpdate)
  {
    this.lastUpdate = lastUpdate;
  }

  public void setLastUpdate()
  {
    this.lastUpdate = new Date();
  }

  /**
   * For migrating older entries the version for every entry is given.
   */
  @Column
  public int getVersion()
  {
    return version;
  }

  public void setVersion(final int version)
  {
    this.version = version;
  }

  /**
   * Sets CURRENT_VERSION as version.
   *
   * @see #CURRENT_VERSION
   * @return this for chaining.
   */
  @Column
  public UserXmlPreferencesDO setVersion()
  {
    this.version = CURRENT_VERSION;
    return this;
  }

  @Override
  @Transient
  public Integer getPk()
  {
    return this.getId();
  }

  @Override
  public void setPk(Integer pk)
  {
    this.setId(pk);
  }
}
