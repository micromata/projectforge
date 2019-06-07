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

package org.projectforge.framework.persistence.user.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.user.UserPrefAreaRegistry;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.user.api.UserPrefArea;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;

/**
 * Stores preferences of the user for any objects such as list filters or templates for adding new objects (time sheets
 * etc.).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 * 
 */
@Entity
@Indexed
@Table(name = "T_USER_PREF",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_fk", "area", "name", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_user_pref_user_fk", columnList = "user_fk"),
        @javax.persistence.Index(name = "idx_fk_t_user_pref_tenant_id", columnList = "tenant_id")
    })
@JpaXmlPersist(beforePersistListener = UserPrefXmlBeforePersistListener.class)
public class UserPrefDO extends AbstractBaseDO<Integer>
{
  private static final long serialVersionUID = -7752620237173115542L;

  @IndexedEmbedded(depth = 1)
  private PFUserDO user;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name; // 255 not null

  private UserPrefArea area; // 20;

  private Set<UserPrefEntryDO> prefEntries;
  //  HIBERNATE5
  //  @Field(index = Index.YES, analyze = Analyze.NO /*UN_TOKENIZED*/, store = Store.NO)
  private Integer id;

  @Override
  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  @Override
  public void setId(final Integer id)
  {
    this.id = id;
  }

  @Column(length = 255, nullable = false)
  public String getName()
  {
    return name;
  }

  public void setName(final String name)
  {
    this.name = name;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_fk", nullable = false)
  public PFUserDO getUser()
  {
    return user;
  }

  public void setUser(final PFUserDO user)
  {
    this.user = user;
  }

  @Transient
  public UserPrefArea getArea()
  {
    return area;
  }

  public void setArea(final UserPrefArea area)
  {
    this.area = area;
  }

  /**
   * Only for storing the user pref area in the data base.
   */
  @Column(name = "area", length = UserPrefArea.MAX_ID_LENGTH, nullable = false)
  public String getAreaString()
  {
    return area != null ? area.getId() : null;
  }

  /**
   * Only for restoring the user pref area from the data base.
   */
  public void setAreaString(final String areaId)
  {
    this.area = areaId != null ? UserPrefAreaRegistry.instance().getEntry(areaId) : null;
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinColumn(name = "user_pref_fk")
  public Set<UserPrefEntryDO> getUserPrefEntries()
  {
    return this.prefEntries;
  }

  @Transient
  public Set<UserPrefEntryDO> getSortedUserPrefEntries()
  {
    final SortedSet<UserPrefEntryDO> result = new TreeSet<UserPrefEntryDO>(new Comparator<UserPrefEntryDO>()
    {
      @Override
      public int compare(final UserPrefEntryDO o1, final UserPrefEntryDO o2)
      {
        return StringHelper.compareTo(o1.orderString, o2.orderString);
      }
    });
    result.addAll(this.prefEntries);
    return result;
  }

  public void setUserPrefEntries(final Set<UserPrefEntryDO> userPrefEntries)
  {
    this.prefEntries = userPrefEntries;
  }

  public void addUserPrefEntry(final UserPrefEntryDO userPrefEntry)
  {
    if (this.prefEntries == null) {
      this.prefEntries = new HashSet<UserPrefEntryDO>();
    }
    this.prefEntries.add(userPrefEntry);
  }

  /**
   * Copies all values from the given src object excluding the values created and modified. Null values will be
   * excluded.
   * 
   * @param src
   */
  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> source, final String... ignoreFields)
  {
    ModificationStatus modificationStatus = super.copyValuesFrom(source, ignoreFields);
    final UserPrefDO src = (UserPrefDO) source;
    if (src.getUserPrefEntries() != null) {
      for (final UserPrefEntryDO srcEntry : src.getUserPrefEntries()) {
        final UserPrefEntryDO destEntry = ensureAndGetAccessEntry(srcEntry.getParameter());
        final ModificationStatus st = destEntry.copyValuesFrom(srcEntry);
        modificationStatus = getModificationStatus(modificationStatus, st);
      }
      final Iterator<UserPrefEntryDO> iterator = getUserPrefEntries().iterator();
      while (iterator.hasNext()) {
        final UserPrefEntryDO destEntry = iterator.next();
        if (src.getUserPrefEntry(destEntry.getParameter()) == null) {
          iterator.remove();
        }
      }
    }
    return modificationStatus;
  }

  public UserPrefEntryDO ensureAndGetAccessEntry(final String parameter)
  {
    if (this.prefEntries == null) {
      setUserPrefEntries(new TreeSet<UserPrefEntryDO>());
    }
    UserPrefEntryDO entry = getUserPrefEntry(parameter);
    if (entry == null) {
      entry = new UserPrefEntryDO();
      entry.setParameter(parameter);
      this.addUserPrefEntry(entry);
    }
    return entry;
  }

  @Transient
  public UserPrefEntryDO getUserPrefEntry(final String parameter)
  {
    if (this.prefEntries == null) {
      return null;
    }
    for (final UserPrefEntryDO entry : this.prefEntries) {
      if (entry.getParameter().equals(parameter) == true) {
        return entry;
      }
    }
    return null;
  }

  @Transient
  public String getUserPrefEntryAsString(final String parameter)
  {
    final UserPrefEntryDO entry = getUserPrefEntry(parameter);
    if (entry == null) {
      return null;
    }
    return entry.getValue();
  }

  /**
   * @param parameter
   * @return A list of all parameters which depends on the given parameter or null if no dependent parameter exists for
   *         this parameter.
   */
  public List<UserPrefEntryDO> getDependentUserPrefEntries(final String parameter)
  {
    List<UserPrefEntryDO> list = null;
    for (final UserPrefEntryDO entry : this.prefEntries) {
      if (parameter.equals(entry.dependsOn) == true) {
        if (list == null) {
          list = new ArrayList<UserPrefEntryDO>();
        }
        list.add(entry);
      }
    }
    return list;
  }
}
