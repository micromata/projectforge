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


package org.projectforge.framework.configuration

import org.projectforge.business.user.ProjectForgeGroup

/**
 * Who maintains a configuration parameter (see [ConfigurationParam.getEditors]). The parameters are split
 * between the system administration and the finance department, and neither side may change the other's:
 * an admin sees the finance parameters read-only, a finance user sees only the finance parameters. The
 * controlling parameters are seen by the finance department only, and changed only by those in both PF_Finance and
 * PF_Controlling.
 *
 * The groups are given here as data ([viewers], [editors], [allEditorGroupsRequired]), so the access check
 * ([ConfigurationDao.hasAccess]) and the list showing who sees and changes a parameter use the same source.
 *
 * @param viewers A member of any of these groups sees the parameter.
 * @param editors The groups changing the parameter, see [allEditorGroupsRequired].
 * @param allEditorGroupsRequired If true, a member of all [editors] is required, otherwise of any of them.
 */
enum class ConfigurationEditors(
    val viewers: List<ProjectForgeGroup>,
    val editors: List<ProjectForgeGroup>,
    val allEditorGroupsRequired: Boolean = false,
) {
    /** Members of PF_Admin. */
    ADMIN(
        viewers = listOf(ProjectForgeGroup.ADMIN_GROUP),
        editors = listOf(ProjectForgeGroup.ADMIN_GROUP),
    ),

    /** Members of PF_Finance or PF_Controlling (not admins, they see them read-only). */
    FINANCE(
        viewers = listOf(
            ProjectForgeGroup.ADMIN_GROUP,
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP,
        ),
        editors = listOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP),
    ),

    /**
     * Members of both PF_Finance and PF_Controlling; a member of only one of them sees them read-only, admins don't
     * see them at all.
     */
    CONTROLLING(
        viewers = listOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP),
        editors = listOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP),
        allEditorGroupsRequired = true,
    );

    /** The viewing groups as shown in the list, e.g. "PF_Admin, PF_Finance". */
    val viewersText: String
        get() = viewers.joinToString(", ") { it.key }

    /**
     * The editing groups as shown in the list: "PF_Finance, PF_Controlling" if any of them suffices,
     * "PF_Finance + PF_Controlling" if a member of all of them is required.
     */
    val editorsText: String
        get() = editors.joinToString(if (allEditorGroupsRequired) " + " else ", ") { it.key }
}
