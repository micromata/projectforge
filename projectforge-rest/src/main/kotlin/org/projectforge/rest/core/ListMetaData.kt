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

import org.projectforge.NextMigration
import org.projectforge.favorites.Favorites
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UILayout
import java.io.Serializable

/**
 * Everything a hand built list page needs beside its rows, served by
 * [AbstractEntityRest.requestListMeta].
 *
 * The counterpart of `AbstractPagesRest.InitialListData` for a client that renders the page itself: no
 * layout, no page menu, no translations and no result set - the page fetches the rows from `list`
 * itself, which is the only call it needs on every filter change.
 *
 * @author Kai Reinhard
 */
class ListMetaData(
    /** The filter the user left this page with, restored from their user prefs. */
    val filter: MagicFilter,
    /** The user's saved filters of this entity, the same ones the legacy list page offers. */
    val filterFavorites: List<Favorites.FavoriteIdTitle>,
    /**
     * The saved filter [filter] is based on, as it is stored — null while the current filter comes from
     * no favorite (or names one that is gone).
     *
     * The values, not just the name: without them a client that was reloaded cannot tell an edited filter
     * from an unchanged one and has to offer "save" unconditionally, which is what the legacy frontend
     * does (`SearchFilter.jsx`). The comparison itself is the client's, because it is the client that
     * normalises the entries it sends ([MagicFilter.isModified] compares entry objects by identity).
     */
    val filterFavorite: MagicFilter?,
    /**
     * The filter fields of this entity, derived from the DAO's search fields (see
     * `LayoutListFilterUtils.createNamedSearchFilterContainer`).
     *
     * These are `org.projectforge.ui` elements, but not page layout: the serialization of a
     * [org.projectforge.ui.filter.UIFilterElement] is what a filter field *is* for both frontends -
     * label, type and the value list of a select.
     */
    val filterElements: List<UILabelledElement>,
    /** Url template of the page a row leads to, with [NextMigration.ID_PLACEHOLDER] for the id. */
    val standardEditPage: String,
    /**
     * The way back to the legacy list page, or null if this page has no legacy counterpart any more
     * (see `NextMigration.NextPage.legacyApp`).
     */
    val legacyListPage: String?,
    /**
     * Whether [legacyListPage] is offered as an entry of the list's gear menu instead of the prominent
     * button (see `NextMigration.NextPage.legacyListInMenu`). The escape hatch stays loud while a new
     * page still has gaps; a trusted page demotes it into the menu, per entity. Only meaningful with a
     * non-null [legacyListPage] - a page with no way back offers nothing either way.
     */
    val legacyListInMenu: Boolean,
    /** The legacy edit page with [NextMigration.ID_PLACEHOLDER] for the id, or null. */
    val legacyEditPage: String?,
    /**
     * The legacy page for adding an entry, or null. Served next to [legacyEditPage], because it isn't
     * derivable from it: the Wicket edit page carries the id as a query parameter, so dropping the
     * placeholder is a per-app rule, not a suffix cut.
     */
    val legacyNewEntryPage: String?,
    /**
     * What the logged-in user may do with this entity: insert, update, delete, see the history.
     *
     * A hint for the UI, **not** an authorization: `update` is reported as true without asking anybody
     * (see `AbstractEntityRest.getListMeta`), and the flags that are asked for are read with
     * `throwException = false`. The DAO stays the authority — a client that offers a button on the
     * strength of a flag here still gets an `AccessException` from the write it triggers.
     */
    val userAccess: UILayout.UserAccess,
    /**
     * The entries this user had ticked for a mass update, so a reload or a detour through the legacy
     * app restores them ([org.projectforge.rest.multiselect.MultiSelectionSupport.SessionContext]).
     *
     * Null while no selection is open, which is the normal case - the page then shows a plain list.
     * The ids are the ones `{page}/select` was last posted, under the same session key
     * (`AbstractEntityRest::class.java.name`), so the list and its mass update page agree without
     * either of them saying so.
     *
     * Served here rather than on [ResultSet.selectedEntityIds], which carries the same ids for the
     * legacy grid: that one is only filled in the `?multiSelectionMode=true` branch of
     * `AbstractPagesRestUtils`, which replaces the result set with `getListByIds` and flips the
     * user's stored filter - a list-wide mode, where this is one restored detail of a normal list.
     */
    val selectedIds: Collection<Serializable>? = null,
    /** Page specific additions, see [AbstractEntityRest.addVariablesForListPage]. */
    var variables: Map<String, Any>? = null,
)
