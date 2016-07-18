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

package org.projectforge.business.meb;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_MEB_ENTRY",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_meb_entry_owner_fk", columnList = "owner_fk"),
        @javax.persistence.Index(name = "idx_fk_t_meb_entry_tenant_id", columnList = "tenant_id")
    })
public class MebEntryDO extends AbstractBaseDO<Integer>
{
  private static final long serialVersionUID = 4424813938259685100L;
  //  HIBERNATE5
  //  @Field(index = Index.YES, analyze = Analyze.NO /*UN_TOKENIZED*/, store = Store.NO)
  private Integer id;

  @IndexedEmbedded(depth = 1)
  private PFUserDO owner;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String sender;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String message;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date date;

  private MebEntryStatus status;

  @Override
  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  @Override
  public void setId(Integer id)
  {
    this.id = id;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_fk")
  public PFUserDO getOwner()
  {
    return owner;
  }

  @Transient
  public Integer getOwnerId()
  {
    if (owner == null) {
      return null;
    } else {
      return owner.getId();
    }
  }

  public MebEntryDO setOwner(PFUserDO owner)
  {
    this.owner = owner;
    return this;
  }

  @Column(length = 255, nullable = false)
  public String getSender()
  {
    return sender;
  }

  public MebEntryDO setSender(String sender)
  {
    this.sender = sender;
    return this;
  }

  @Column(length = 4000)
  public String getMessage()
  {
    return message;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  public MebEntryStatus getStatus()
  {
    return status;
  }

  public MebEntryDO setStatus(MebEntryStatus status)
  {
    this.status = status;
    return this;
  }

  @Column(nullable = false)
  public Date getDate()
  {
    return date;
  }

  public MebEntryDO setDate(Date date)
  {
    this.date = date;
    return this;
  }
}
