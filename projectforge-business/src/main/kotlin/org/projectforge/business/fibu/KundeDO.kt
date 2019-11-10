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

import org.apache.commons.lang3.StringUtils
import org.apache.lucene.analysis.standard.ClassicAnalyzer
import org.hibernate.search.annotations.Analyzer
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.IManualIndex
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import javax.persistence.*

/**
 * Jeder Kunde bei Micromata hat eine Kundennummer. Die Kundennummer ist Bestandteil von KOST2 (2.-4. Ziffer). Aufträge
 * aus dem Auftragsbuch, sowie Rechnungen etc. werden Kunden zugeordnet.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_FIBU_KUNDE",
        indexes = [Index(name = "idx_fk_t_fibu_kunde_konto_id", columnList = "konto_id"),
            Index(name = "idx_fk_t_fibu_kunde_tenant_id", columnList = "tenant_id")])
@Analyzer(impl = ClassicAnalyzer::class)
open class KundeDO : AbstractHistorizableBaseDO<Int>(), ShortDisplayNameCapable, IManualIndex {

    /**
     * Kundennummer.
     *
     * @see .getId
     */
    @PropertyInfo(i18nKey = "fibu.kunde.nummer")
    @get:Id
    @get:Column(name = "pk")
    open var nummer: Int? = null

    @Transient
    override fun getId(): Int? {
        return nummer
    }

    override fun setId(value: Int?) {
        nummer = value
    }


    @PropertyInfo(i18nKey = "fibu.kunde.name")
    @Field
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    /**
     * The identifier is used e. g. for display the project as short name in human resources planning tables.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "fibu.kunde.identifier")
    @Field
    @get:Column(length = 20)
    open var identifier: String? = null

    @PropertyInfo(i18nKey = "fibu.kunde.division")
    @Field
    @get:Column(length = 255)
    open var division: String? = null

    @PropertyInfo(i18nKey = "status")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var status: KundeStatus? = null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 4000)
    open var description: String? = null

    /**
     * This Datev account number is used for the exports of invoices. This account numbers may-be overwritten by the
     * ProjektDO which is assigned to an invoice.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "fibu.konto")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "konto_id")
    open var konto: KontoDO? = null

    /**
     * @return "5.###" ("5.<kunde id>")</kunde> */
    open val kost: String
        @Transient
        get() = "5." + KostFormatter.format3Digits(nummer)

    /**
     * 1. Ziffer des Kostenträgers: Ist für Kunden immer 5.
     *
     * @return 5
     */
    val nummernkreis: Int
        @Transient
        get() = 5

    /**
     * @return Identifier if exists otherwise name of project.
     */
    val kundeIdentifierDisplayName: String?
        @Transient
        get() = if (StringUtils.isNotBlank(this.identifier)) {
            this.identifier
        } else this.name

    val kontoId: Int?
        @Transient
        get() = if (konto != null) konto!!.id else null

    /**
     * @see org.projectforge.framework.persistence.api.ShortDisplayNameCapable.getShortDisplayName
     * @see KostFormatter.format
     */
    @Transient
    override fun getShortDisplayName(): String {
        return KostFormatter.formatKunde(this)
    }

    companion object {
        val MAX_ID = 999
    }
}
