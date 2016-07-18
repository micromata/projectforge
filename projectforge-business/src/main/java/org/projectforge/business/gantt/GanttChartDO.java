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

package org.projectforge.business.gantt;

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

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_GANTT_CHART",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_gantt_chart_owner_fk", columnList = "owner_fk"),
        @javax.persistence.Index(name = "idx_fk_t_gantt_chart_task_fk", columnList = "task_fk"),
        @javax.persistence.Index(name = "idx_fk_t_gantt_chart_tenant_id", columnList = "tenant_id")
    })
public class GanttChartDO extends AbstractBaseDO<Integer>
{
  private static final long serialVersionUID = -8031766412540117292L;

  private Integer id;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String name;

  @IndexedEmbedded(depth = 1)
  private TaskDO task;

  private GanttChartStyle style;

  private String styleAsXml;

  private GanttChartSettings settings;

  private String settingsAsXml;

  private String ganttObjectsAsXml;

  private GanttAccess readAccess;

  private GanttAccess writeAccess;

  @IndexedEmbedded(depth = 1)
  private PFUserDO owner;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_fk", nullable = false)
  public TaskDO getTask()
  {
    return task;
  }

  public GanttChartDO setTask(final TaskDO task)
  {
    this.task = task;
    return this;
  }

  @Transient
  public Integer getTaskId()
  {
    if (this.task == null)
      return null;
    return task.getId();
  }

  /**
   * Free usable name.
   */
  @Column(length = 1000)
  public String getName()
  {
    return name;
  }

  public GanttChartDO setName(String name)
  {
    this.name = name;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_fk")
  public PFUserDO getOwner()
  {
    return owner;
  }

  public GanttChartDO setOwner(final PFUserDO owner)
  {
    this.owner = owner;
    return this;
  }

  @Transient
  public Integer getOwnerId()
  {
    if (this.owner == null)
      return null;
    return owner.getId();
  }

  @Transient
  public GanttChartStyle getStyle()
  {
    return style;
  }

  public GanttChartDO setStyle(GanttChartStyle style)
  {
    this.style = style;
    return this;
  }

  /**
   * GanttChartStyle (serialized).
   * 
   * @see GanttChartStyle
   */
  @Column(name = "style_as_xml", length = 10000)
  public String getStyleAsXml()
  {
    return styleAsXml;
  }

  public void setStyleAsXml(String styleAsXml)
  {
    this.styleAsXml = styleAsXml;
  }

  @Transient
  public GanttChartSettings getSettings()
  {
    return settings;
  }

  public GanttChartDO setSettings(GanttChartSettings settings)
  {
    this.settings = settings;
    return this;
  }

  /**
   * GanttChartSettings (serialized).
   * 
   * @see GanttChartSettings
   */
  @Column(name = "settings_as_xml", length = 10000)
  public String getSettingsAsXml()
  {
    return settingsAsXml;
  }

  public void setSettingsAsXml(String settingsAsXml)
  {
    this.settingsAsXml = settingsAsXml;
  }

  /**
   * List (of modified) GanttObject (serialized).
   * 
   * @see GanttTask
   */
  @Column(name = "gantt_objects_as_xml", length = 10000)
  public String getGanttObjectsAsXml()
  {
    return ganttObjectsAsXml;
  }

  public void setGanttObjectsAsXml(String ganttObjectsAsXml)
  {
    this.ganttObjectsAsXml = ganttObjectsAsXml;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "read_access", length = 16)
  public GanttAccess getReadAccess()
  {
    return readAccess;
  }

  public void setReadAccess(GanttAccess readAccess)
  {
    this.readAccess = readAccess;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "write_access", length = 16)
  public GanttAccess getWriteAccess()
  {
    return writeAccess;
  }

  public void setWriteAccess(GanttAccess writeAccess)
  {
    this.writeAccess = writeAccess;
  }
}
