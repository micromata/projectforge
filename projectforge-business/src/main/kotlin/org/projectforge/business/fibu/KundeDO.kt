/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.business.common.NumberToStringValueBridge
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO

/**
 * Jeder Kunde bei Micromata hat eine Kundennummer. Die Kundennummer ist Bestandteil von KOST2 (2.-4. Ziffer). Aufträge
 * aus dem Auftragsbuch, sowie Rechnungen etc. werden Kunden zugeordnet.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_FIBU_KUNDE", indexes = [Index(name = "idx_fk_t_fibu_kunde_konto_id", columnList = "konto_id")])
open class KundeDO : AbstractHistorizableBaseDO<Long>(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = OldKostFormatter.formatKunde(this)

    /**
     * Kundennummer.
     *
     * @see .getId
     */
    @PropertyInfo(i18nKey = "fibu.kunde.nummer")
    @GenericField(valueBridge = ValueBridgeRef(type = NumberToStringValueBridge::class))
    @get:Id
    @get:Column(name = "pk")
    open var nummer: Long? = null

    /**
     * Don't use this field directly. Use [nummer] instead. Otherwise, the kunde object will be fetched from the database.
     */
    @get:Transient
    override var id: Long?
        get() = nummer
        set(value) {
            nummer = value
        }

    @PropertyInfo(i18nKey = "fibu.kunde.name")
    @FullTextField(analyzer = "customAnalyzer")
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    /**
     * The identifier is used e. g. for display the project as short name in human resources planning tables.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "fibu.kunde.identifier")
    @FullTextField
    @get:Column(length = 20)
    open var identifier: String? = null

    @PropertyInfo(i18nKey = "fibu.kunde.division")
    @FullTextField
    @get:Column(length = 255)
    open var division: String? = null

    @PropertyInfo(i18nKey = "status")
    @FullTextField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var status: KundeStatus? = null

    @PropertyInfo(i18nKey = "description")
    @FullTextField
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
    @JsonSerialize(using = IdOnlySerializer::class)
    open var konto: KontoDO? = null

    /**
     * @return "5.###" ("5.<kunde id>")</kunde>
     * */
    open val kost: String
        @JsonIgnore
        @PropertyInfo(i18nKey = "fibu.kunde.nummer")
        @Transient
        @GenericField
        @IndexingDependency(derivedFrom = [ObjectPath(PropertyValue(propertyName = "nummer"))])
        get() = KostFormatter.instance.formatKunde(this, KostFormatter.FormatType.FORMATTED_NUMBER)

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

    companion object {
        const val MAX_ID = 999L
    }
}
