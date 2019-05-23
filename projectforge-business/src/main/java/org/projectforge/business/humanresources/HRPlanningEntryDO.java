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

package org.projectforge.business.humanresources;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.*;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektFormatter;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.ObjectHelper;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * @author Mario Groß (m.gross@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_HR_PLANNING_ENTRY",
       indexes = {
           @javax.persistence.Index(name = "idx_fk_t_hr_planning_entry_planning_fk", columnList = "planning_fk"),
           @javax.persistence.Index(name = "idx_fk_t_hr_planning_entry_projekt_fk", columnList = "projekt_fk"),
           @javax.persistence.Index(name = "idx_fk_t_hr_planning_entry_tenant_id", columnList = "tenant_id")
       })
public class HRPlanningEntryDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = -7788797217095084177L;

  @JsonManagedReference
  @IndexedEmbedded(depth = 3)
  private HRPlanningDO planning;

  @IndexedEmbedded(depth = 2)
  private ProjektDO projekt;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private HRPlanningEntryStatus status;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private Priority priority;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO,
         bridge = @FieldBridge(impl = IntegerBridge.class))
  private Integer probability;

  /**
   * Ohne Wochentagszuordnung.
   */
  private BigDecimal unassignedHours;

  private BigDecimal mondayHours;

  private BigDecimal tuesdayHours;

  private BigDecimal wednesdayHours;

  private BigDecimal thursdayHours;

  private BigDecimal fridayHours;

  private BigDecimal weekendHours;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "planning_fk", nullable = false)
  public HRPlanningDO getPlanning()
  {
    return planning;
  }

  public void setPlanning(HRPlanningDO planning)
  {
    this.planning = planning;
  }

  @Transient
  public Integer getPlanningId()
  {
    if (this.planning == null) {
      return null;
    }
    return this.planning.getId();
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public Priority getPriority()
  {
    return priority;
  }

  public void setPriority(Priority priority)
  {
    this.priority = priority;
  }

  @Column
  public Integer getProbability()
  {
    return probability;
  }

  public void setProbability(Integer probability)
  {
    this.probability = probability;
  }

  /**
   * @return Hours without assigned day of week (unspecified). This means, it doesn't matter on which day of week the
   * job will be done.
   */
  @Column(scale = 2, precision = 5)
  public BigDecimal getUnassignedHours()
  {
    return unassignedHours;
  }

  public void setUnassignedHours(BigDecimal unassignedHours)
  {
    this.unassignedHours = unassignedHours;
  }

  @Column(scale = 2, precision = 5)
  public BigDecimal getMondayHours()
  {
    return mondayHours;
  }

  public void setMondayHours(BigDecimal mondayHours)
  {
    this.mondayHours = mondayHours;
  }

  @Column(scale = 2, precision = 5)
  public BigDecimal getTuesdayHours()
  {
    return tuesdayHours;
  }

  public void setTuesdayHours(BigDecimal tuesdayHours)
  {
    this.tuesdayHours = tuesdayHours;
  }

  @Column(scale = 2, precision = 5)
  public BigDecimal getWednesdayHours()
  {
    return wednesdayHours;
  }

  public void setWednesdayHours(BigDecimal wednesdayHours)
  {
    this.wednesdayHours = wednesdayHours;
  }

  @Column(scale = 2, precision = 5)
  public BigDecimal getThursdayHours()
  {
    return thursdayHours;
  }

  public void setThursdayHours(BigDecimal thursdayHours)
  {
    this.thursdayHours = thursdayHours;
  }

  @Column(scale = 2, precision = 5)
  public BigDecimal getFridayHours()
  {
    return fridayHours;
  }

  public void setFridayHours(BigDecimal fridayHours)
  {
    this.fridayHours = fridayHours;
  }

  @Column(scale = 2, precision = 5)
  public BigDecimal getWeekendHours()
  {
    return weekendHours;
  }

  public void setWeekendHours(BigDecimal weekendHours)
  {
    this.weekendHours = weekendHours;
  }

  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  @Transient
  public String getShortDescription()
  {
    return StringUtils.abbreviate(getDescription(), 50);
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "projekt_fk")
  public ProjektDO getProjekt()
  {
    return projekt;
  }

  public void setProjekt(ProjektDO projekt)
  {
    this.projekt = projekt;
  }

  @Transient
  public Integer getProjektId()
  {
    if (this.projekt == null) {
      return null;
    }
    return this.projekt.getId();
  }

  @Transient
  public String getProjektName()
  {
    if (this.projekt == null) {
      return "";
    }
    return this.projekt.getName();
  }

  @Transient
  public String getProjektNameOrStatus()
  {
    if (this.status != null) {
      return ThreadLocalUserContext.getLocalizedString(status.getI18nKey());
    } else {
      return getProjektName();
    }
  }

  /**
   * Gets the customer of the project.
   *
   * @see ProjektFormatter#formatProjektKundeAsString(ProjektDO, KundeDO, String)
   */
  @Transient
  public String getProjektKundeAsString()
  {
    return ProjektFormatter.formatProjektKundeAsString(this.projekt, null, null);
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20)
  public HRPlanningEntryStatus getStatus()
  {
    return status;
  }

  public void setStatus(HRPlanningEntryStatus status)
  {
    this.status = status;
  }

  /**
   * @return The total duration of all assigned hours (unassigned hours, monday, tuesday...)
   */
  @Transient
  public BigDecimal getTotalHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (this.unassignedHours != null) {
      duration = duration.add(this.unassignedHours);
    }
    if (this.mondayHours != null) {
      duration = duration.add(this.mondayHours);
    }
    if (this.tuesdayHours != null) {
      duration = duration.add(this.tuesdayHours);
    }
    if (this.wednesdayHours != null) {
      duration = duration.add(this.wednesdayHours);
    }
    if (this.thursdayHours != null) {
      duration = duration.add(this.thursdayHours);
    }
    if (this.fridayHours != null) {
      duration = duration.add(this.fridayHours);
    }
    if (this.weekendHours != null) {
      duration = duration.add(this.weekendHours);
    }
    return duration;
  }

  @Transient
  public boolean isEmpty()
  {
    return ObjectHelper.isEmpty(this.description, this.mondayHours, this.tuesdayHours, this.wednesdayHours,
        this.thursdayHours,
        this.fridayHours, this.weekendHours, this.priority, this.probability, this.projekt);
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof HRPlanningEntryDO) {
      HRPlanningEntryDO other = (HRPlanningEntryDO) o;
      if (this.getId() != null || other.getId() != null) {
        return ObjectUtils.equals(this.getId(), other.getId());
      } else {
        return hasNoFieldChanges(other);
      }
    }
    return false;
  }

  public boolean hasNoFieldChanges(final HRPlanningEntryDO other)
  {
    if ((this.getStatus() != null && other.getStatus() == null) || (this.getStatus() == null && other.getStatus() != null)) {
      return false;
    }
    if (this.getStatus() != null && other.getStatus() != null) {
      if (this.getStatus().equals(other.getStatus()) == false) {
        return false;
      }
    }
    if ((this.getProjektId() != null && other.getProjektId() == null) || (this.getProjektId() == null && other.getProjektId() != null)) {
      return false;
    }
    if (this.getProjektId() != null && other.getProjektId() != null) {
      if (this.getProjektId().equals(other.getProjektId()) == false) {
        return false;
      }
    }
    if ((this.getUnassignedHours() != null && other.getUnassignedHours() == null) || (this.getUnassignedHours() == null
        && other.getUnassignedHours() != null)) {
      return false;
    }
    if (this.getUnassignedHours() != null && other.getUnassignedHours() != null) {
      if (this.getUnassignedHours().compareTo(other.getUnassignedHours()) != 0) {
        return false;
      }
    }
    if ((this.getMondayHours() != null && other.getMondayHours() == null) || (this.getMondayHours() == null && other.getMondayHours() != null)) {
      return false;
    }
    if (this.getMondayHours() != null && other.getMondayHours() != null) {
      if (this.getMondayHours().compareTo(other.getMondayHours()) != 0) {
        return false;
      }
    }
    if ((this.getTuesdayHours() != null && other.getTuesdayHours() == null) || (this.getTuesdayHours() == null && other.getTuesdayHours() != null)) {
      return false;
    }
    if (this.getTuesdayHours() != null && other.getTuesdayHours() != null) {
      if (this.getTuesdayHours().compareTo(other.getTuesdayHours()) != 0) {
        return false;
      }
    }
    if ((this.getWednesdayHours() != null && other.getWednesdayHours() == null) || (this.getWednesdayHours() == null
        && other.getWednesdayHours() != null)) {
      return false;
    }
    if (this.getWednesdayHours() != null && other.getWednesdayHours() != null) {
      if (this.getWednesdayHours().compareTo(other.getWednesdayHours()) != 0) {
        return false;
      }
    }
    if ((this.getThursdayHours() != null && other.getThursdayHours() == null) || (this.getThursdayHours() == null && other.getThursdayHours() != null)) {
      return false;
    }
    if (this.getThursdayHours() != null && other.getThursdayHours() != null) {
      if (this.getThursdayHours().compareTo(other.getThursdayHours()) != 0) {
        return false;
      }
    }
    if ((this.getFridayHours() != null && other.getFridayHours() == null) || (this.getFridayHours() == null && other.getFridayHours() != null)) {
      return false;
    }
    if (this.getFridayHours() != null && other.getFridayHours() != null) {
      if (this.getFridayHours().compareTo(other.getFridayHours()) != 0) {
        return false;
      }
    }
    if ((this.getWeekendHours() != null && other.getWeekendHours() == null) || (this.getWeekendHours() == null && other.getWeekendHours() != null)) {
      return false;
    }
    if (this.getWeekendHours() != null && other.getWeekendHours() != null) {
      if (this.getWeekendHours().compareTo(other.getWeekendHours()) != 0) {
        return false;
      }
    }
    if (this.isDeleted() != other.isDeleted()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode()
  {
    HashCodeBuilder hcb = new HashCodeBuilder();
    if (getId() != null) {
      hcb.append(getId());
    } else {
      if (getPlanningId() != null) {
        hcb.append(getPlanningId());
      }
      if (getProjektId() != null) {
        hcb.append(getProjektId());
      }
      if (getStatus() != null) {
        hcb.append(getStatus());
      }
    }
    return hcb.toHashCode();
  }

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return getProjekt() != null ? getProjekt().getName() : "";
  }

  /**
   * Clones this entry (without id's).
   *
   * @return
   */
  public HRPlanningEntryDO newClone()
  {
    final HRPlanningEntryDO entry = new HRPlanningEntryDO();
    entry.copyValuesFrom(this, "id");
    return entry;
  }
}
