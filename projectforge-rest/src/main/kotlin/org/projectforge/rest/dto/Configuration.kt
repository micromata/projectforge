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

package org.projectforge.rest.dto

import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.ConfigurationType
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.i18n.translate
import java.math.BigDecimal

/**
 * The DTO of a single system-configuration parameter (see [ConfigurationDO]), for the migrated
 * `/next/configuration` page (`ConfigurationEntityRest`).
 *
 * Why a DTO rather than serializing the DO: the DO stores a value that is polymorphic per
 * [ConfigurationType] in one of four typed columns, and its accessors guard the type —
 * [ConfigurationDO.value]'s setter throws for any non-String, and the typed getters call `checkType`.
 * This DTO carries the parameter's static description (parameter, type, the two i18n keys) plus one
 * nullable slot per storage column, and only the slot matching [configurationType] is ever populated.
 */
class Configuration(
    /**
     * The parameter key (the [org.projectforge.framework.configuration.ConfigurationParam] name). Read-only
     * for the client: the set of parameters is fixed, so it is never taken from the request (see
     * [copyTo]), only shown.
     */
    var parameter: String? = null,
    /**
     * The value type of this parameter. Read-only for the client as well — it comes from the DB row, and
     * [copyTo] switches on the DB-loaded type, never on a client-supplied one.
     */
    var configurationType: ConfigurationType? = null,
    /**
     * The i18n key of the parameter's label, `administration.configuration.param.<parameter>`.
     */
    var i18nKey: String? = null,
    /**
     * The i18n key of the parameter's description, `<i18nKey>.description`.
     */
    var descriptionI18nKey: String? = null,
    /**
     * The translated parameter label, in the logged-in user's locale. The edit page heading is built by
     * the frontend from `data` and has no translator there (the list cells translate `i18nKey` through
     * next-intl themselves), so the one place a translated title is needed takes it from here - the same
     * way a referenced entity's `displayName` arrives ready-formatted.
     */
    var label: String? = null,
    /**
     * The value slot for STRING, TEXT, JSON and TIME_ZONE (a time-zone id).
     */
    var stringValue: String? = null,
    /**
     * The value slot for LONG.
     */
    var longValue: Long? = null,
    /**
     * The value slot for FLOAT and PERCENT (a factor, e.g. 0.19 shown as 19 %).
     */
    var floatValue: BigDecimal? = null,
    /**
     * The value slot for BOOLEAN (stored as a string on the DO).
     */
    var booleanValue: Boolean? = null,
    /**
     * The route of the page this parameter is maintained on, if it has one of its own
     * ([ConfigurationParam.getEditPage]): the configuration page then shows it read-only and links there. The list
     * opens it directly. `ConfigurationEntityRest` clears it for users who aren't the parameter's editors.
     */
    var editPage: String? = null,
    /**
     * The value is rich text (HTML of the rich text editor), see [ConfigurationParam.isRichText].
     */
    var richText: Boolean? = null,
    override var writeAccess: Boolean? = null,
    override var deleteAccess: Boolean? = null,
) : BaseDTO<ConfigurationDO>(), EntityAccessSupport {

    override fun copyFrom(src: ConfigurationDO) {
        super.copyFrom(src)
        // i18nKey/descriptionI18nKey are @Transient computed getters on the DO (no backing field), so the
        // reflective, field-name based copy in BaseDTO does not carry them - fill them explicitly.
        i18nKey = src.i18nKey
        descriptionI18nKey = src.descriptionI18nKey
        label = src.i18nKey.let { translate(it) }
        // booleanValue is a computed getter on the DO (proxying stringValue) and is only meaningful for a
        // BOOLEAN parameter; fill it here since it, too, has no backing field to copy reflectively.
        booleanValue = if (src.configurationType == ConfigurationType.BOOLEAN) src.booleanValue else null
        val param = src.parameter?.let { ConfigurationParam.ofKey(it) }
        editPage = param?.editPage
        richText = param?.isRichText == true
    }

    /**
     * Writes only the value slot matching [ConfigurationDO.configurationType], which is expected to be set on
     * [dest] from the DB row already (see `ConfigurationEntityRest.transformForDB`). Deliberately does not call
     * `super.copyTo`: the parameter and the type are fixed and must never be taken from the request, and the
     * DO's typed setters would reject a value written into the wrong slot anyway.
     */
    override fun copyTo(dest: ConfigurationDO) {
        when (dest.configurationType) {
            ConfigurationType.STRING,
            ConfigurationType.TEXT,
            ConfigurationType.JSON,
            ConfigurationType.TIME_ZONE -> dest.stringValue = stringValue
            ConfigurationType.BOOLEAN -> dest.booleanValue = booleanValue
            ConfigurationType.LONG,
            ConfigurationType.CALENDAR -> dest.longValue = longValue
            ConfigurationType.FLOAT,
            ConfigurationType.PERCENT -> dest.floatValue = floatValue
            else -> {} // INTEGER/TASK are not editable through this page.
        }
    }
}
