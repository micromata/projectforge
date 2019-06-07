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

package org.projectforge.business.teamcal.event.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Indexed;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.mail.MailAttachment;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

@Entity
@Indexed
@Table(name = "T_PLUGIN_CALENDAR_EVENT_ATTACHMENT",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attachment_team_event_fk2",
            columnList = "team_event_fk2"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attachment_tenant_id", columnList = "tenant_id")
    })
public class TeamEventAttachmentDO extends DefaultBaseDO implements Comparable<TeamEventAttachmentDO>, MailAttachment
{
  private static final long serialVersionUID = -7858238331041883784L;

  private String filename;

  private byte[] content;

  @Override
  @Column
  public String getFilename()
  {
    return filename;
  }

  public TeamEventAttachmentDO setFilename(final String filename)
  {
    this.filename = filename;
    return this;
  }

  @Override
  @Column
  @Type(type = "binary")
  public byte[] getContent()
  {
    return content;
  }

  public TeamEventAttachmentDO setContent(final byte[] content)
  {
    this.content = content;
    return this;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final TeamEventAttachmentDO arg0)
  {
    if (getId() != null && Objects.equals(this.getId(), arg0.getId()) == true) {
      return 0;
    }
    return this.toString().toLowerCase().compareTo(arg0.toString().toLowerCase());
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(getId());
    if (getId() != null) {
      return hcb.toHashCode();
    }
    hcb.append(this.filename);
    hcb.append(this.content);
    return hcb.toHashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof TeamEventAttachmentDO == false) {
      return false;
    }
    final TeamEventAttachmentDO other = (TeamEventAttachmentDO) o;
    if (getId() != null && Objects.equals(this.getId(), other.getId()) == true) {
      return true;
    }
    if (StringUtils.equals(this.getFilename(), other.getFilename()) == false) {
      return false;
    }
    if (Objects.equals(this.getContent(), other.getContent()) == false) {
      return false;
    }
    return true;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    if (StringUtils.isBlank(filename) == true) {
      return String.valueOf(getId());
    }
    return StringUtils.defaultString(this.filename);
  }
}
