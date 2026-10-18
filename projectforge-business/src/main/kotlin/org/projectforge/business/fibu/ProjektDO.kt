/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.business.common.NumberToStringValueBridge
import org.projectforge.business.fibu.numberrange.NumberRangeService
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.search.ClassBridge
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Projects belong to customers (or to no customer, e.g. internal projects). Each project has a system-wide unique
 * [projektnummer]. For projects of a configured number range it is computed from the customer and the local
 * [nummer] and its 6 digits are the first 6 digits of the project's Kost2 (see
 * [org.projectforge.business.fibu.numberrange.NumberRanges]); otherwise it is free.
 *
 * @author Kai Reinhard
 */
@Entity
@Indexed
@TypeBinding(binder = TypeBinderRef(type = HibernateSearchProjectKostTypeBinder::class))
@ClassBridge(name = "kost2") // kost2 should be used in HibernateSearchProjectKostBridge as field name.
//@ClassBridge(name = "kost2", impl = HibernateSearchProjectKostBridge::class)
@Table(
    name = "T_FIBU_PROJEKT",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["nummer", "kunde_id"]),
        UniqueConstraint(columnNames = ["nummer", "intern_kost2_4"]),
        UniqueConstraint(name = "unique_t_fibu_projekt_projektnummer", columnNames = ["projektnummer"]),
    ],
    indexes = [jakarta.persistence.Index(
        name = "idx_fk_t_fibu_projekt_konto_id",
        columnList = "konto_id"
    ), jakarta.persistence.Index(
        name = "idx_fk_t_fibu_projekt_kunde_id",
        columnList = "kunde_id"
    ), jakarta.persistence.Index(
        name = "idx_fk_t_fibu_projekt_projektmanager_group_fk",
        columnList = "projektmanager_group_fk"
    ), jakarta.persistence.Index(
        name = "idx_fk_t_fibu_projekt_projectManager_fk",
        columnList = "projectmanager_fk"
    ), jakarta.persistence.Index(
        name = "idx_fk_t_fibu_projekt_headofbusinessmanager_fk",
        columnList = "headofbusinessmanager_fk"
    ), jakarta.persistence.Index(
        name = "idx_fk_t_fibu_projekt_salesmanager_fk",
        columnList = "salesmanager_fk"
    ), jakarta.persistence.Index(name = "idx_fk_t_fibu_projekt_task_fk", columnList = "task_fk")]
)
// @WithHistory
@NamedQueries(
    NamedQuery(
        name = ProjektDO.FIND_BY_INTERNKOST24_AND_NUMMER,
        query = "from ProjektDO where internKost2_4=:internKost24 and nummer=:nummer"
    )
)
open class ProjektDO : DefaultBaseDO(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = KostFormatter.instance.formatProjekt(this, KostFormatter.FormatType.TEXT)

    /**
     * Local project number: per customer, or for projects without customer per number range (e.g. 0-99 for the
     * customers of range 5, 0-99999 for internal projects of range 4).
     */
    @PropertyInfo(i18nKey = "fibu.projekt.nummer")
    @GenericField(valueBridge = ValueBridgeRef(type = NumberToStringValueBridge::class))
    @get:Column(nullable = false)
    open var nummer: Int = 0

    /**
     * System-wide unique project number. Computed for projects of a number range (6 digits, e.g. 512345), free
     * otherwise. Set by [ProjektDao] on save. Null only for legacy projects which couldn't be migrated.
     */
    @PropertyInfo(i18nKey = "fibu.projekt.projektnummer")
    @GenericField(valueBridge = ValueBridgeRef(type = NumberToStringValueBridge::class))
    @get:Column(name = "projektnummer")
    open var projektnummer: Int? = null

    @PropertyInfo(i18nKey = "fibu.projekt.name")
    @FullTextField(analyzer = "wordPartsAnalyzer")
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    /**
     * The identifier is used e. g. for display the project as short name in human resources planning tables.
     */
    @PropertyInfo(i18nKey = "fibu.projekt.identifier")
    @FullTextField(analyzer = "wordPartsAnalyzer")
    @get:Column(length = 20)
    open var identifier: String? = null

    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kunde_id")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var kunde: KundeDO? = null

    /**
     * @see KundeFormatter.formatKundeAsString
     */
    val kundeAsString: String
        @Transient
        get() = KundeFormatter.formatKundeAsString(this.kunde)

    /**
     * Legacy: digits 2-4 of internal projects (4.###). Not used anymore: the local [nummer] of a project without
     * customer covers all digits after the prefix, see [projektnummer]. Kept for the existing data.
     */
    @PropertyInfo(i18nKey = "fibu.projekt.internKost2_4")
    @get:Column(name = "intern_kost2_4")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    open var internKost2_4: Int? = null

    @PropertyInfo(i18nKey = "status")
    @FullTextField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var status: ProjektStatus? = null

    @PropertyInfo(i18nKey = "description")
    @FullTextField
    @get:Column(length = 4000)
    open var description: String? = null

    /**
     * The member of this group have access to orders assigned to this project.
     */
    @PropertyInfo(i18nKey = "fibu.projekt.projektManagerGroup")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projektmanager_group_fk")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var projektManagerGroup: GroupDO? = null

    @PropertyInfo(i18nKey = "fibu.projectManager")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projectmanager_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var projectManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.headOfBusinessManager")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "headofbusinessmanager_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var headOfBusinessManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.salesManager")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "salesmanager_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var salesManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "task")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_fk", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var task: TaskDO? = null

    /**
     * This Datev account number is used for the exports of invoices. If not given then the account number assigned to the
     * KundeDO is used instead (default).
     */
    @PropertyInfo(i18nKey = "fibu.konto")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "konto_id")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var konto: KontoDO? = null

    @get:PropertyInfo(i18nKey = "fibu.kost2")
    @get:Transient
    @get:GenericField
    @get:IndexingDependency(derivedFrom = [ObjectPath(PropertyValue(propertyName = "id"))])
    val kost: String
        get() = KostFormatter.instance.formatProjekt(this)

    /**
     * The project number if it belongs to a configured number range, so it is the first 6 digits of its Kost2.
     * Null for free project numbers (no Kost2 mapping).
     */
    val rangeProjektnummer: Int?
        @Transient
        get() = projektnummer?.takeIf { NumberRangeService.current.rangeForProjectNumber(it) != null }

    /**
     * Digit 1 of the project's Kost2 (the prefix of its number range), null for free project numbers.
     */
    val nummernkreis: Int?
        @Transient
        get() = rangeProjektnummer?.let { it / 100_000 }

    /**
     * Digits 2-4 of the project's Kost2 (storage format of Kost2DO), null for free project numbers. Not the customer
     * number: depending on the number range the customer number has another position.
     */
    val bereich: Int?
        @Transient
        get() = rangeProjektnummer?.let { (it / 100) % 1000 }

    val projektManagerGroupId: Long?
        @Transient
        get() = if (projektManagerGroup != null) projektManagerGroup!!.id else null

    val projectManagerId: Long?
        @Transient
        get() = if (projectManager != null) projectManager!!.id else null

    val headOfBusinessManagerId: Long?
        @Transient
        get() = if (headOfBusinessManager != null) headOfBusinessManager!!.id else null

    val salesManagerId: Long?
        @Transient
        get() = if (salesManager != null) salesManager!!.id else null

    /**
     * @return Identifier if exists otherwise name of project.
     */
    val projektIdentifierDisplayName: String?
        @Transient
        get() = if (StringUtils.isNotBlank(this.identifier)) {
            this.identifier
        } else this.name

    /**
     * Digits 5-6 of the project's Kost2 (storage format of Kost2DO), null for free project numbers.
     */
    val teilbereich: Int?
        @Transient
        get() = rangeProjektnummer?.let { it % 100 }

    companion object {
        internal const val FIND_BY_INTERNKOST24_AND_NUMMER = "ProjektDO_FindByInternkostAndNummer"
    }
}
