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

package org.projectforge.plugins.liquidityplanning.rest

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.liquidityplanning.LiquidityEntryDO
import org.projectforge.plugins.liquidityplanning.LiquidityEntryDao
import org.projectforge.plugins.liquidityplanning.LiquidityMaterializationService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.TextFieldModification
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
import org.projectforge.ui.UISelectValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

/**
 * Mass update of liquidity entries after selection in the next list (`liquidity.page.tsx`), the counterpart
 * of the invoice mass update ([org.projectforge.rest.fibu.EingangsrechnungMultiSelectedPageRest]). Editable
 * fields: amount, subject, the paid override, [LiquidityEntryDO.autoSetPaid] and comment.
 *
 * The paid override and autoSetPaid are booleans the metadata path does not carry natively (a boolean builds
 * a checkbox, for which no value property is resolved), so they are declared with explicit [values]: the
 * frontend then renders a combobox (empty = no change) and posts the choice as `textValue`. The paid override
 * is three-state (automatic / paid / unpaid), mirroring the nullable [LiquidityEntryDO.paid].
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/liquidity${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class LiquidityMultiSelectedPageRest : AbstractMultiSelectedPage<LiquidityEntryDO>() {

    @Autowired
    private lateinit var liquidityEntryDao: LiquidityEntryDao

    @Autowired
    private lateinit var liquidityEntityRest: LiquidityEntityRest

    @Autowired
    private lateinit var liquidityMaterializationService: LiquidityMaterializationService

    override val layoutContext: LayoutContext = LayoutContext(LiquidityEntryDO::class.java)

    override fun getTitleKey(): String {
        // Generic "Multi selection" / "Mehrfachauswahl"; no entity-specific title wanted.
        return "multiselection.button"
    }

    @PostConstruct
    private fun postConstruct() {
        pagesRest = liquidityEntityRest
    }

    /**
     * The layout free field set the next mass update page renders. `amount`/`subject` may only be set (no
     * delete); `comment` is a full text field (set/append/replace/delete); `paid` and `autoSetPaid` are
     * value-based comboboxes (empty = no change).
     */
    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        return listOf(
            MassUpdateFieldDeclaration(field = "amount", showDeleteOption = false),
            MassUpdateFieldDeclaration(field = "subject", showDeleteOption = false),
            MassUpdateFieldDeclaration(
                field = "paid",
                showDeleteOption = false,
                values = listOf(
                    UISelectValue(PAID_AUTOMATIC, translate("plugins.liquidityplanning.entry.paid.automatic")),
                    UISelectValue("true", translate("plugins.liquidityplanning.entry.paid.paid")),
                    UISelectValue("false", translate("plugins.liquidityplanning.entry.paid.unpaid")),
                ),
            ),
            MassUpdateFieldDeclaration(
                field = "autoSetPaid",
                showDeleteOption = false,
                values = listOf(
                    UISelectValue("true", translate("yes")),
                    UISelectValue("false", translate("no")),
                ),
            ),
            MassUpdateFieldDeclaration(field = "comment", showAppendOption = true),
        )
    }

    override fun fillForm(
        request: HttpServletRequest,
        layout: UILayout,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        selectedIds: Collection<Serializable>?,
        variables: MutableMap<String, Any>,
    ) {
        createAndAddFields(
            layoutContext,
            massUpdateData,
            layout,
            "amount",
            "subject",
            "paid",
            "autoSetPaid",
            showDeleteOption = false,
        )
        createAndAddFields(layoutContext, massUpdateData, layout, "comment", showAppendOption = true)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<LiquidityEntryDO>,
    ): ResponseEntity<*>? {
        // Virtual (recurring) occurrences are selected by their negative synthetic id; materialize them into
        // real, frozen rows before the update loop so a mass change over a mixed selection works uniformly.
        val entries = resolveSelectedEntries(selectedIds)
        if (entries.isEmpty()) {
            return null
        }
        val params = massUpdateContext.massUpdateParams
        entries.forEach { entry ->
            massUpdateContext.startUpdate(entry)
            params["amount"]?.let { param ->
                if (param.hasAction) {
                    entry.amount = param.decimalValue
                }
            }
            TextFieldModification.processTextParameter(entry, "subject", params)
            TextFieldModification.processTextParameter(entry, "comment", params)
            // Three-state paid override: "automatic" clears the override (null), "true"/"false" force it.
            params["paid"]?.let { param ->
                if (param.hasAction) {
                    entry.paid = when (param.textValue) {
                        PAID_AUTOMATIC -> null
                        else -> param.textValue?.toBoolean()
                    }
                }
            }
            params["autoSetPaid"]?.let { param ->
                if (param.hasAction) {
                    param.textValue?.toBoolean()?.let { entry.autoSetPaid = it }
                }
            }
            massUpdateContext.commitUpdate(
                identifier4Message = entry.subject ?: "#${entry.id}",
                entry,
                update = { liquidityEntryDao.update(entry) },
            )
        }
        return null
    }

    /** Liquidity entries may be deleted and restored in bulk (soft delete via [LiquidityEntryDao]). */
    override fun supportsMassDeletion(): Boolean = true

    /**
     * Soft-deletes the selected entries. Like [proceedMassUpdate] it first materializes selected virtual
     * (recurring) occurrences into real rows via [resolveSelectedEntries]: a materialized, then deleted row
     * suppresses its virtual occurrence anyway (the projector checks `(seriesId, seriesDate)` including
     * soft-deleted rows), so deleting an occurrence stays consistent with the series model.
     */
    override fun proceedMassDelete(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<LiquidityEntryDO>,
    ) {
        resolveSelectedEntries(selectedIds).forEach { entry ->
            massUpdateContext.startUpdate(entry)
            entry.deleted = true
            massUpdateContext.commitUpdate(
                identifier4Message = entry.subject ?: "#${entry.id}",
                entry,
                update = { liquidityEntryDao.markAsDeleted(entry) },
            )
        }
    }

    /**
     * Restores the selected, already deleted entries. Only real (positive) ids are undeleted; a virtual
     * occurrence was never materialized, so it is not "deleted" and is ignored.
     */
    override fun proceedMassUndelete(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<LiquidityEntryDO>,
    ) {
        val (realIds, _) = partitionIds(selectedIds)
        if (realIds.isEmpty()) {
            return
        }
        liquidityEntryDao.select(realIds)?.forEach { entry ->
            massUpdateContext.startUpdate(entry)
            entry.deleted = false
            massUpdateContext.commitUpdate(
                identifier4Message = entry.subject ?: "#${entry.id}",
                entry,
                update = { liquidityEntryDao.undelete(entry) },
            )
        }
    }

    /**
     * The same statistics line the list shows above its table, so the mass update page can repeat it
     * (`LIQUIDITY_PAGE.massUpdate.statisticsLine`).
     */
    override fun getStatisticsData(selectedIds: Collection<Serializable>?): Any? {
        selectedIds ?: return null
        val (realIds, virtualIds) = partitionIds(selectedIds)
        // The preview must not persist: virtual occurrences are built transiently, real ones are loaded.
        val entries = ArrayList<LiquidityEntryDO>()
        if (realIds.isNotEmpty()) {
            liquidityEntryDao.select(realIds)?.let { entries.addAll(it) }
        }
        virtualIds.forEach { id -> liquidityMaterializationService.preview(id)?.let { entries.add(it) } }
        return LiquidityEntityRest.LiquidityStatistics(liquidityEntryDao.buildStatistics(entries), null)
    }

    /** Loads the selected real entries and materializes the selected virtual occurrences into real rows. */
    private fun resolveSelectedEntries(selectedIds: Collection<Serializable>): List<LiquidityEntryDO> {
        val (realIds, virtualIds) = partitionIds(selectedIds)
        val entries = ArrayList<LiquidityEntryDO>()
        if (realIds.isNotEmpty()) {
            liquidityEntryDao.select(realIds)?.let { entries.addAll(it) }
        }
        virtualIds.forEach { id -> liquidityMaterializationService.materialize(id)?.let { entries.add(it) } }
        return entries
    }

    /** Splits selected ids into real (positive) and virtual (negative) ids, ignoring anything unparsable. */
    private fun partitionIds(selectedIds: Collection<Serializable>): Pair<List<Long>, List<Long>> {
        val ids = selectedIds.mapNotNull { (it as? Number)?.toLong() ?: it.toString().toLongOrNull() }
        return ids.filter { it >= 0 } to ids.filter { it < 0 }
    }

    override fun ensureUserLogSubscription(): LogSubscription {
        val username = ThreadLocalUserContext.loggedInUser!!.username ?: throw InternalError("User not given")
        val displayTitle = translate("multiselection.button")
        return LogSubscription.ensureSubscription(
            title = "LiquidityEntries",
            displayTitle = displayTitle,
            user = username,
            create = { title, user ->
                LogSubscription(
                    title,
                    user,
                    LogEventLoggerNameMatcher(
                        "org.projectforge.plugins.liquidityplanning.LiquidityEntryDao",
                        "org.projectforge.framework.persistence.api.BaseDaoSupport|LiquidityEntryDO",
                    ),
                    maxSize = 10000,
                    displayTitle = displayTitle,
                )
            },
        )
    }

    companion object {
        /** The `textValue` a value-based `paid` combobox posts for the "automatic" (null) state. */
        private const val PAID_AUTOMATIC = "auto"
    }
}
