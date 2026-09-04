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

package org.projectforge.rest.multiselect

import org.projectforge.ui.UIDataType
import org.projectforge.ui.UISelectValue

/**
 * Everything a hand built mass update page needs, served by [AbstractMultiSelectedPage.requestMeta].
 *
 * The layout free counterpart of `{page}/dynamic`, which answers the same information as a `UILayout`
 * of rows, columns, inputs and alerts. The relation is the same one [org.projectforge.rest.core.ListMetaData]
 * has to `initialList`: a client that renders the page itself needs the *rules* (which fields, of which
 * type, with which options), not a tree of components describing how the legacy frontend draws them.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MultiSelectMetaData(
    /** Translated title of the page, e. g. "Debitorenrechnungen: Massenupdate". */
    val title: String,
    /** How many entries the user ticked, i. e. how many this update would change. */
    val selectedCount: Int,
    /**
     * How many entries the list registered for selection, i. e. how many the user may pick from.
     *
     * Served next to [selectedCount] because it is what the "change selection" way back is offered
     * for: without registered ids there is nothing to go back to (see `getLayout`).
     */
    val registeredCount: Int,
    /** The fields this page may update, in the order they are shown. */
    val fields: List<MassUpdateFieldMeta>,
    /** Where "stop" returns to: the list page this selection came from. */
    val listPage: String,
    /** Upper bound of one update ([org.projectforge.framework.persistence.api.BaseDao.MAX_MASS_UPDATE]). */
    val maxMassUpdate: Int,
    /**
     * An entity specific note above the fields, as markdown - the warning of the invoice page that a
     * payment date also sets the amount paid and the status.
     */
    val info: String? = null,
    /**
     * What the selected entries add up to, as markdown - the invoice statistics
     * (`RechnungsStatistik.asMarkdown`).
     *
     * For the `UILayout` form, which has nowhere to put a number but a `UIAlert`. A hand built page reads
     * [statisticsData] instead: this text carries `<span style="color:blue">` for its colours and its
     * amounts are formatted server side, neither of which a next page can use.
     */
    @Deprecated("Use statisticsData, which serves the same numbers as values.")
    val statistics: String? = null,
    /**
     * The same summary as values, in the shape the entity's list already serves it in
     * (`InvoiceStatistics`) - so the mass update page renders it with the statistics line of that list
     * and formats the amounts in the user's locale and currency.
     */
    val statisticsData: Any? = null,
)

/**
 * The answer of [AbstractMultiSelectedPage.select] and [AbstractMultiSelectedPage.cancel]: where the
 * client goes next.
 *
 * The layout free counterpart of the `ResponseAction(targetType = REDIRECT)` both endpoints answer for
 * the legacy frontend. A hand built client routes itself, so it gets the url as a value; it is answered
 * at all because the way back out of a selection is the *caller's* url, which only the session knows
 * (see `MultiSelectionSupport.clear`).
 */
class MultiSelectNavigation(
    val url: String,
    /** How many entries the session holds after the call - zero after a cancel. */
    val selectedCount: Int = 0,
)

/**
 * One field of a mass update: which property, how its value is entered, and which of the four actions
 * (set, delete, replace, append) it offers.
 *
 * Resolved by the base class from the same [org.projectforge.ui.ElementsRegistry] entry the `UILayout`
 * path reads, so both say the same thing about a field and a declaration names only what it decides
 * (see [MassUpdateFieldDeclaration]).
 */
class MassUpdateFieldMeta(
    /** Name of the field, and the key of its [MassUpdateParameter] in the posted map. */
    val field: String,
    /**
     * Which property of the [MassUpdateParameter] the value goes into, e. g. `localDateValue` for a
     * date - the frontend must not derive this from [dataType], because the mapping is the backend's
     * (see `createInputFieldRow`).
     */
    val valueProperty: String,
    /** Translated label. */
    val label: String?,
    val dataType: UIDataType?,
    val maxLength: Int? = null,
    /** Number of rows of a textarea; null for a single line input. */
    val rows: Int? = null,
    /** The values of an enum field, translated - null for anything else. */
    val values: List<UISelectValue<String>>? = null,
    /** Offers clearing the value on every selected entry. */
    val deleteOption: Boolean = false,
    /** Offers replacing a substring instead of the whole text. */
    val replaceOption: Boolean = false,
    /** Offers appending to the existing text instead of overwriting it. */
    val appendOption: Boolean = false,
    /** Whether appending is the preset (`showAppendOption` of `createAndAddFields`). */
    val appendPreset: Boolean = false,
)

/**
 * What a page declares about one of its mass update fields: the property, and which of the optional
 * actions to offer where the default is not what is wanted.
 *
 * Everything else - label, type, length, enum values - comes from the entity via the
 * [org.projectforge.ui.ElementsRegistry], the same source the form layout and the Excel export read.
 * A declaration that repeated them would be a second place to be wrong.
 */
class MassUpdateFieldDeclaration(
    val field: String,
    /** Preset "append" for a text field, as `createAndAddFields(showAppendOption = true)` does. */
    val showAppendOption: Boolean? = null,
    /** Overrides the default, which offers deletion for every field the entity does not require. */
    val showDeleteOption: Boolean? = null,
    /** Set to false to hide the replace input a text field offers by default. */
    val showReplaceOption: Boolean? = null,
    val minLengthOfTextArea: Int = 4,
)

/**
 * What a mass update did, served by [AbstractMultiSelectedPage.update].
 *
 * The layout free counterpart of the `UIAlert`s the `massUpdate` endpoint answers with: the same
 * counters and errors, as values rather than as a markdown table built server side.
 */
class MassUpdateResult(
    val modifiedCounter: Int,
    val unmodifiedCounter: Int,
    /** Entries the update failed for; the same size as [errors]. */
    val errorCounter: Int,
    /**
     * The sentence summarizing the counters ("{0} entries were processed: ..."), translated.
     *
     * Served although the counters are: the wording is a message of the bundle
     * (`massUpdate.result`) and composing it in the frontend would be a second translation of the
     * same sentence.
     */
    val resultMessage: String,
    val errors: List<MassUpdateError>,
    /**
     * Url of the Excel protocol of this run, or null if none was stored. Expires after five minutes
     * (see `DownloadFileSupport`), which is why it is answered per run rather than derived.
     */
    val downloadUrl: String? = null,
    /** The fields the update acted on, translated - what the Excel protocol is named after. */
    val changedFields: List<String> = emptyList(),
)

/** One entry a mass update failed for: how it is identified, and why it failed. */
class MassUpdateError(val identifier: String, val message: String)

/**
 * What a mass update *would* do, served by [AbstractMultiSelectedPage.preview] before anything is
 * written.
 *
 * Answered by the server rather than composed in the frontend on purpose: the same
 * `checkParamHasAction` the real run uses decides which fields have an action here, so the dialog shows
 * exactly what the server understood from the posted params - a client that infers it itself could
 * drift from the backend, and the invalid combinations the run rejects would only surface after the
 * write instead of in the confirmation.
 */
class MassUpdatePreview(
    /** How many entries the update would change - the same count [MultiSelectMetaData.selectedCount] holds. */
    val selectedCount: Int,
    /** One entry per field the update would act on, in the order the fields are declared. */
    val changes: List<MassUpdatePreviewChange>,
)

/** Which of the four actions a field's preview describes - see [MassUpdateParameter]. */
enum class MassUpdateAction {
    /** Overwrite the field with [MassUpdatePreviewChange.value]. */
    SET,

    /** Append [MassUpdatePreviewChange.value] to the existing text. */
    APPEND,

    /** Replace [MassUpdatePreviewChange.value] with [MassUpdatePreviewChange.replaceValue]. */
    REPLACE,

    /** Clear the whole field. */
    DELETE,

    /** Delete only the occurrences of [MassUpdatePreviewChange.value]. */
    DELETE_OCCURRENCES,
}

/**
 * One field a mass update would act on: which action, and the value(s) it acts with, already formatted
 * for display (an enum's label rather than its id, a date and an amount in the user's locale).
 */
class MassUpdatePreviewChange(
    /** Name of the field, matching [MassUpdateFieldMeta.field]. */
    val field: String,
    /** Translated label of the field. */
    val label: String,
    val action: MassUpdateAction,
    /** The value the action acts with (the searched text for [MassUpdateAction.REPLACE]); null for a plain delete. */
    val value: String? = null,
    /** The replacement, for [MassUpdateAction.REPLACE] only. */
    val replaceValue: String? = null,
)
