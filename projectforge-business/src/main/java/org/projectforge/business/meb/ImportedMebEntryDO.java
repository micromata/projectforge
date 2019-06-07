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

package org.projectforge.business.meb;

import org.projectforge.framework.persistence.entities.AbstractBaseDO;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * All imported meb entries (by mail or by SMS servlet) will be registered as imported MEB entry for avoiding multiple imports of the same
 * messages.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(name = "T_IMPORTED_MEB_ENTRY", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "sender", "date", "check_sum" }) },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_imported_meb_entry_tenant_id", columnList = "tenant_id")
    })
public class ImportedMebEntryDO extends AbstractBaseDO<Integer>
{
  private static final long serialVersionUID = 8867280267511657921L;

  private Integer id;

  private String sender;

  private String checkSum;

  private Date date;

  private String source;

  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  public void setId(Integer id)
  {
    this.id = (Integer) id;
  }

  @Column(length = 255, nullable = false)
  public String getSender()
  {
    return sender;
  }

  public void setSender(String sender)
  {
    this.sender = sender;
  }

  @Column(nullable = false)
  public Date getDate()
  {
    return date;
  }

  public void setDate(Date date)
  {
    this.date = date;
  }

  /**
   * Only the check sum of an entry is registered for protecting privacy.
   */
  @Column(name = "check_sum", length = 255, nullable = false)
  public String getCheckSum()
  {
    return checkSum;
  }

  public void setCheckSum(String checkSum)
  {
    this.checkSum = checkSum;
  }

  /**
   * From which source was this entry imported (MAIL or SERVLET)?
   */
  @Column(length = 10)
  public String getSource()
  {
    return source;
  }

  public void setSource(String source)
  {
    this.source = source;
  }
}
