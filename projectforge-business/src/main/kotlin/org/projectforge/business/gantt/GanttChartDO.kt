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

package org.projectforge.business.gantt

import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GANTT_CHART", indexes = [Index(name = "idx_fk_t_gantt_chart_owner_fk", columnList = "owner_fk"), Index(name = "idx_fk_t_gantt_chart_task_fk", columnList = "task_fk"), Index(name = "idx_fk_t_gantt_chart_tenant_id", columnList = "tenant_id")])
class GanttChartDO : AbstractBaseDO<Int>() {

    private var id: Int? = null

    /**
     * Free usable name.
     */
    @PropertyInfo(i18nKey = "gantt.name")
    @Field
    @get:Column(length = 1000)
    var name: String? = null

    @PropertyInfo(i18nKey = "task")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_fk", nullable = false)
    var task: TaskDO? = null

    @get:Transient
    var style: GanttChartStyle? = null

    /**
     * GanttChartStyle (serialized).
     *
     * @see GanttChartStyle
     */
    @get:Column(name = "style_as_xml", length = 10000)
    var styleAsXml: String? = null

    @get:Transient
    var settings: GanttChartSettings? = null

    /**
     * GanttChartSettings (serialized).
     *
     * @see GanttChartSettings
     */
    @get:Column(name = "settings_as_xml", length = 10000)
    var settingsAsXml: String? = null

    /**
     * List (of modified) GanttObject (serialized).
     *
     * @see GanttTask
     */
    @get:Column(name = "gantt_objects_as_xml", length = 10000)
    var ganttObjectsAsXml: String? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "read_access", length = 16)
    var readAccess: GanttAccess? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "write_access", length = 16)
    var writeAccess: GanttAccess? = null

    @PropertyInfo(i18nKey = "gantt.owner")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    var owner: PFUserDO? = null

    val taskId: Int?
        @Transient
        get() = if (this.task == null) null else task!!.id

    val ownerId: Int?
        @Transient
        get() = if (this.owner == null) null else owner!!.id

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getId(): Int? {
        return id
    }

    override fun setId(id: Int?) {
        this.id = id
    }
}
