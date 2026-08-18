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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.apache.commons.beanutils.NestedNullException
import org.apache.commons.beanutils.PropertyUtils
import org.hibernate.Hibernate
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MarkDeletableRecord
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.ui.ElementInfo
import org.projectforge.ui.ElementsRegistry
import org.projectforge.ui.ValidationError
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

/**
 * Utility functions for validation in REST endpoints.
 */
object ValidationUtils {
    /**
     * Validates an object against the field rules declared in the backend: `required` (from
     * `@PropertyInfo(required = true)` or a `NOT NULL` column), the maximum length of strings
     * (from the JPA `@Column(length = …)`) and the bounds of numbers (from
     * `@PropertyInfo(min = …, max = …)`, see [validateRange]). All of them are read from
     * [ElementsRegistry.getElementInfo], the single place those annotations are merged — nothing is
     * restated here.
     *
     * The properties come from [ElementsRegistry.listProperties], i.e. from the annotations of the
     * class, **not** from [ElementsRegistry.getProperties]: that one only returns what a UILayout
     * happened to put into the registry's cache before. Validating from the cache made the outcome
     * depend on which pages the JVM had served since its start, and left every field of a
     * hand-built page (projectforge-next builds no edit layout) unvalidated.
     *
     * Nested collections are validated too, see [validateRows]: an entity whose rows the client edits in
     * the same form (an order and its positions) has to answer for those rows in the same way.
     *
     * @param obj The object to validate (typically a DO or DTO)
     * @return List of validation errors, empty if the object is valid.
     */
    fun validateFields(obj: Any): MutableList<ValidationError> {
        return validateFields(obj, prefix = "")
    }

    /**
     * @param prefix Path of [obj] inside the entity being saved, e. g. `positionen[0].`, prepended to the
     * [ValidationError.fieldId] so the client can show the error at the row that caused it.
     * [ValidationError.fieldId] is free-form, so a nested path needs no framework change.
     */
    private fun validateFields(obj: Any, prefix: String): MutableList<ValidationError> {
        val validationErrors = mutableListOf<ValidationError>()
        val clazz = obj::class.java
        val properties = ElementsRegistry.listProperties(clazz)
        if (properties.isEmpty()) {
            log.error("Internal error, no @PropertyInfo found for '$clazz'. No validation errors will be built automatically.")
            return validationErrors
        }
        properties.forEach { property ->
            val elementInfo = ElementsRegistry.getElementInfo(clazz, property) ?: return@forEach
            if (elementInfo.readOnly) {
                return@forEach // Computed by the backend, the client can't send a wrong value.
            }
            val value =
                try {
                    PropertyUtils.getProperty(obj, property)
                } catch (ex: NestedNullException) {
                    null
                } catch (ex: Exception) {
                    log.warn("Unknown property '$clazz.$property': ${ex.message}.")
                    null
                }
            if (value is Collection<*>) {
                // The rows of a collection the same form edits, e. g. an order's positions. Only if they
                // are loaded already: the object validated here is the one the client posted, so its rows
                // are the posted ones, and touching an untouched lazy collection would fetch rows from the
                // database that no one is about to change.
                if (Hibernate.isInitialized(value)) {
                    validationErrors.addAll(validateRows(value, "$prefix$property"))
                }
                return@forEach
            }
            if (elementInfo.required == true && (value == null || (value is String && value.isBlank()))) {
                validationErrors.add(
                    ValidationError(
                        translateMsg("validation.error.fieldRequired", translate(elementInfo.i18nKey)),
                        fieldId = "$prefix$property", messageId = elementInfo.i18nKey
                    )
                )
                return@forEach // A blank value can't be too long as well.
            }
            // Length is a rule of strings only: @Column.length is preset to 255 for other types too,
            // and for an enum column it is the storage size of the constant name.
            val maxLength = if (elementInfo.propertyClass == String::class.java) elementInfo.maxLength else null
            if (maxLength != null && value is String && value.length > maxLength) {
                validationErrors.add(
                    ValidationError(
                        translateMsg("validation.error.maxLength", translate(elementInfo.i18nKey), maxLength),
                        fieldId = "$prefix$property", messageId = elementInfo.i18nKey
                    )
                )
            }
            validateRange(elementInfo, value, "$prefix$property")?.let { validationErrors.add(it) }
        }
        return validationErrors
    }

    /**
     * The bounds of `@PropertyInfo(min = …, max = …)`, the numeric rule of the domain (0 to 100 percent) that
     * no `@Column` can state — see [org.projectforge.common.anots.PropertyInfo.min].
     *
     * Reported with the message Wicket's `MinMaxNumberField` uses, so both frontends say the same thing for
     * the same value.
     *
     * @return the error, or null if the value is inside the bounds, is no number, or the property declares none.
     */
    private fun validateRange(elementInfo: ElementInfo, value: Any?, fieldId: String): ValidationError? {
        val min = elementInfo.min
        val max = elementInfo.max
        if (min == null && max == null) {
            return null
        }
        // Compared as BigDecimal, so an Int property and a BigDecimal one are checked by the same code. Any
        // other type would be a misannotated property, not a value the user could correct.
        val number = when (value) {
            null -> return null
            is BigDecimal -> value
            is Int -> BigDecimal(value)
            is Long -> BigDecimal(value)
            is Short -> BigDecimal(value.toInt())
            is Double -> BigDecimal.valueOf(value)
            else -> {
                log.warn("@PropertyInfo(min/max) of '$fieldId' ignored: '${value::class.java}' is no number.")
                return null
            }
        }
        if ((min != null && number < min) || (max != null && number > max)) {
            val i18nKey = "validation.error.range.integerOutOfRange"
            return ValidationError(
                translateMsg(
                    i18nKey,
                    min?.let { NumberFormatter.format(it) } ?: "",
                    max?.let { NumberFormatter.format(it) } ?: "",
                ),
                fieldId = fieldId, messageId = i18nKey
            )
        }
        return null
    }

    /**
     * Validates the rows of a nested collection, each under its index (`positionen[0].titel`).
     *
     * A row marked as deleted is skipped: it is only sent so the persistence layer doesn't remove it
     * physically (see `AuftragDO.positionen`, which has no `@SoftDeleteCollection`), and its values are
     * not the user's to correct any more.
     *
     * Rows without any `@PropertyInfo` are skipped silently rather than logged as an internal error: a
     * collection of something other than an entity (a list of strings) is nothing to validate, and
     * [validateFields] cannot tell one from a misannotated entity.
     */
    private fun validateRows(rows: Collection<*>, property: String): List<ValidationError> {
        val validationErrors = mutableListOf<ValidationError>()
        rows.forEachIndexed { index, row ->
            row ?: return@forEachIndexed
            if (row is MarkDeletableRecord<*> && row.deleted) {
                return@forEachIndexed
            }
            if (ElementsRegistry.listProperties(row::class.java).isEmpty()) {
                return@forEachIndexed
            }
            validationErrors.addAll(validateFields(row, prefix = "$property[$index]."))
        }
        return validationErrors
    }
}
