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

package org.projectforge.business.fibu

import de.micromata.genome.db.jpa.history.api.WithHistory
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.*
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.*

/**
 * Projekte sind Kunden zugeordnet und haben eine zweistellige Nummer. Sie sind Bestandteile von KOST2 (5. und 6.
 * Ziffer).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "kost2", impl = HibernateSearchProjectKostBridge::class)
@Table(name = "T_FIBU_PROJEKT", uniqueConstraints = [UniqueConstraint(columnNames = ["nummer", "kunde_id", "tenant_id"]), UniqueConstraint(columnNames = ["nummer", "intern_kost2_4", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_projekt_konto_id", columnList = "konto_id"), javax.persistence.Index(name = "idx_fk_t_fibu_projekt_kunde_id", columnList = "kunde_id"), javax.persistence.Index(name = "idx_fk_t_fibu_projekt_projektmanager_group_fk", columnList = "projektmanager_group_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_projekt_projectManager_fk", columnList = "projectmanager_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_projekt_headofbusinessmanager_fk", columnList = "headofbusinessmanager_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_projekt_salesmanager_fk", columnList = "salesmanager_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_projekt_task_fk", columnList = "task_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_projekt_tenant_id", columnList = "tenant_id")])
@WithHistory
@NamedQueries(
        NamedQuery(name = ProjektDO.FIND_BY_INTERNKOST24_AND_NUMMER, query = "from ProjektDO where internKost2_4=:internKost24 and nummer=:nummer"))
open class ProjektDO : DefaultBaseDO(), ShortDisplayNameCapable {

    /**
     * Ziffer 5-6 von KOST2 (00-99)
     */
    @PropertyInfo(i18nKey = "fibu.projekt.nummer")
    @get:Column(nullable = false)
    @Field(analyze = Analyze.NO)
    open var nummer: Int = 0

    @PropertyInfo(i18nKey = "fibu.projekt.name")
    @Field
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    /**
     * The identifier is used e. g. for display the project as short name in human resources planning tables.
     */
    @PropertyInfo(i18nKey = "fibu.projekt.identifier")
    @Field
    @get:Column(length = 20)
    open var identifier: String? = null

    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "kunde_id")
    open var kunde: KundeDO? = null

    /**
     * Nur bei internen Projekten ohne Kundennummer, stellt diese Nummer die Ziffern 2-4 aus 4.* dar.
     */
    @PropertyInfo(i18nKey = "fibu.projekt.internKost2_4")
    @get:Column(name = "intern_kost2_4")
    @Field(analyze = Analyze.NO)
    open var internKost2_4: Int? = null

    @PropertyInfo(i18nKey = "status")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var status: ProjektStatus? = null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 4000)
    open var description: String? = null

    /**
     * The member of this group have access to orders assigned to this project.
     */
    @PropertyInfo(i18nKey = "fibu.projekt.projektManagerGroup")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projektmanager_group_fk")
    @IndexedEmbedded(depth = 1)
    open var projektManagerGroup: GroupDO? = null

    @PropertyInfo(i18nKey = "fibu.projectManager")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projectmanager_fk")
    open var projectManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.headOfBusinessManager")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "headofbusinessmanager_fk")
    open var headOfBusinessManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "fibu.salesManager")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "salesmanager_fk")
    open var salesManager: PFUserDO? = null

    @PropertyInfo(i18nKey = "task")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_fk", nullable = true)
    open var task: TaskDO? = null

    /**
     * This Datev account number is used for the exports of invoices. If not given then the account number assigned to the
     * KundeDO is used instead (default).
     */
    @PropertyInfo(i18nKey = "fibu.konto")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "konto_id")
    open var konto: KontoDO? = null

    val kost: String
        @Transient
        get() = KostFormatter.format(this)

    /**
     * 1. Ziffer des Kostenträgers: Ist 4 für interne Projekte (kunde nicht gegeben) ansonsten 5.
     */
    val nummernkreis: Int
        @Transient
        get() = if (kunde != null) 5 else 4

    /**
     * Wenn Kunde gesetzt ist, wird die Kundennummer, ansonsten internKost2_4 zurückgegeben.
     */
    val bereich: Int?
        @Transient
        get() = if (kunde != null) kunde!!.id else internKost2_4

    val projektManagerGroupId: Int?
        @Transient
        get() = if (projektManagerGroup != null) projektManagerGroup!!.id else null

    val projectManagerId: Int?
        @Transient
        get() = if (projectManager != null) projectManager!!.id else null

    val headOfBusinessManagerId: Int?
        @Transient
        get() = if (headOfBusinessManager != null) headOfBusinessManager!!.id else null

    val salesManagerId: Int?
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

    val kundeId: Int?
        @Transient
        get() = if (this.kunde == null) {
            null
        } else kunde!!.id

    val taskId: Int?
        @Transient
        get() = if (this.task != null) task!!.id else null

    val kontoId: Int?
        @Transient
        get() = if (konto != null) konto!!.id else null

    /**
     * @see .getNummer
     */
    val teilbereich: Int
        @Transient
        get() = nummer

    @Transient
    override fun getShortDisplayName(): String {
        return KostFormatter.formatProjekt(this)
    }

    companion object {
        internal const val FIND_BY_INTERNKOST24_AND_NUMMER = "ProjektDO_FindByInternkostAndNummer"
    }
}
