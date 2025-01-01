/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.menu

enum class MenuItemTargetType {
    /**
     * The client should redirect to the given url. If no type is given, REDIRECT is used as default.
     */
    REDIRECT,
    /**
     * The client should open a model with the given url.
     */
    MODAL,
    /**
     * The client will receive a download file after calling the rest service with the given url.
     */
    DOWNLOAD,
    /**
     * The client calls the rest service with the given url and will receive a response.
     */
    RESTCALL }
