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

package org.projectforge.business.teamcal.filter;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.utils.NumberHelper;

/**
 * Persist the settings of one calendar entry in the filter.
 * 
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TemplateCalendarProperties implements Serializable, Comparable<TemplateCalendarProperties>, Cloneable
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(TemplateCalendarProperties.class);

  private static final long serialVersionUID = 6173766812848285792L;

  private Integer calId;

  private String colorCode = TeamCalCalendarFilter.DEFAULT_COLOR;

  private boolean visible = true;

  private long millisOfLastChange = System.currentTimeMillis();

  public void updateMillisOfLastChange()
  {
    millisOfLastChange = System.currentTimeMillis();
  }

  /**
   * Only used for evaluating color of latest modified properties.
   * 
   * @return
   */
  public Long getMillisOfLastChange()
  {
    return millisOfLastChange;
  }

  public TemplateCalendarProperties setMillisOfLastChange(final Long millisOfLastChange)
  {
    this.millisOfLastChange = millisOfLastChange;
    return this;
  }

  public Integer getCalId()
  {
    return calId;
  }

  public TemplateCalendarProperties setCalId(final Integer calId)
  {
    this.calId = calId;
    return this;
  }

  public String getColorCode()
  {
    return colorCode;
  }

  public TemplateCalendarProperties setColorCode(final String colorCode)
  {
    this.colorCode = colorCode;
    updateMillisOfLastChange();
    return this;
  }

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Please note: Don't forget to call {@link TemplateEntry#setDirty()} after calling this method. Otherwise the set of
   * visible calendars may not up-to-date in the template entry.
   * 
   * @param visible
   * @return
   */
  public TemplateCalendarProperties setVisible(final boolean visible)
  {
    this.visible = visible;
    return this;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((calId == null) ? 0 : calId.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final TemplateCalendarProperties other = (TemplateCalendarProperties) obj;
    if (calId == null) {
      if (other.calId != null)
        return false;
    } else if (!calId.equals(other.calId))
      return false;
    return true;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final TemplateCalendarProperties o)
  {
    if (this == o) {
      return 0;
    }
    if (this.calId == null) {
      if (o.calId == null) {
        return 0;
      }
      return -1;
    }
    if (o.calId == null) {
      return 1;
    }
    return this.calId.compareTo(o.calId);
  }

  @Override
  public TemplateCalendarProperties clone()
  {
    try {
      final TemplateCalendarProperties cloned = (TemplateCalendarProperties) super.clone();
      cloned.calId = this.calId;
      cloned.colorCode = this.colorCode;
      cloned.millisOfLastChange = this.millisOfLastChange;
      cloned.visible = this.visible;
      return cloned;
    } catch (final CloneNotSupportedException ex) {
      log.error(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * For avoiding reload of Calendar if no changes are detected. (Was für'n Aufwand für so'n kleines Feature...)
   * 
   * @param filter
   * @return
   */
  public boolean isModified(final TemplateCalendarProperties other)
  {
    if (NumberHelper.isEqual(this.calId, other.calId) == false) {
      return true;
    }
    if (StringUtils.equals(this.colorCode, other.colorCode) == false) {
      return true;
    }
    if (this.millisOfLastChange != other.millisOfLastChange) {
      return true;
    }
    if (this.visible != other.visible) {
      return true;
    }
    return false;
  }
}
