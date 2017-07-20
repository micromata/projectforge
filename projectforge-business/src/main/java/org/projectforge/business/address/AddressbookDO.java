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

package org.projectforge.business.address;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.common.BaseUserGroupRightsDO;
import org.projectforge.business.teamcal.admin.model.HibernateSearchUsersGroupsBridge;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.ReflectionToString;

/**
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@ClassBridge(name = "usersgroups", index = Index.YES /* TOKENIZED */, store = Store.NO,
    impl = HibernateSearchUsersGroupsBridge.class)
@Table(name = "T_ADDRESSBOOK",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_addressbook_tenant_id", columnList = "tenant_id")
    })
public class AddressbookDO extends BaseUserGroupRightsDO
{
  private static final long serialVersionUID = 2869412345643084605L;

  // @UserPrefParameter(i18nKey = "plugins.teamcal.subject")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title;

  @IndexedEmbedded(depth = 1)
  private PFUserDO owner;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  public AddressbookDO()
  {
  }

  /**
   * @return the title
   */
  @Column(length = Constants.LENGTH_TITLE)
  public String getTitle()
  {
    return title;
  }

  /**
   * @param title the title to set
   * @return this for chaining.
   */
  public AddressbookDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  /**
   * @return the owner
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_fk")
  public PFUserDO getOwner()
  {
    return owner;
  }

  @Transient
  public Integer getOwnerId()
  {
    return owner != null ? owner.getId() : null;
  }

  /**
   * @param owner the owner to set
   * @return this for chaining.
   */
  public AddressbookDO setOwner(final PFUserDO owner)
  {
    this.owner = owner;
    return this;
  }

  @Column(length = Constants.LENGTH_TEXT)
  public String getDescription()
  {
    return description;
  }

  /**
   * @return this for chaining.
   */
  public AddressbookDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * @see Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder().append(this.getId());
    hcb.append(this.getTitle());
    return hcb.hashCode();
  }

  /**
   * }
   * if (obj instanceof AddressbookDO == false) {
   * return false;
   * }
   * final AddressbookDO other = (AddressbookDO) obj;
   * if (this.getId().equals(other.getId())) {
   * return true;
   * }
   * return StringUtils.equals(title, other.title);
   * }
   * <p>
   * /**
   * Returns string containing all fields (except the externalSubscriptionCalendarBinary) of given object (via
   * ReflectionToStringBuilder).
   *
   * @param user
   * @return
   */
  @Override
  public String toString()
  {
    return (new ReflectionToString(this)
    {
      @Override
      protected boolean accept(final java.lang.reflect.Field f)
      {
        return super.accept(f);
      }
    }).toString();
  }
}
