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

package org.projectforge.common.logging

/**
 * For filtering log messages.
 */
class LogFilter(
  /**
   * Search string for all fields.
   */
  var search: String? = null,
  var threshold: LogLevel = LogLevel.INFO,
  var maxSize: Int = 100,

  /**
   * false at default (default is descending order of the result).
   */
  var isAscendingOrder: Boolean = false,
  var isShowStackTraces: Boolean = false,

  /**
   * @return If given, all log entries with order orderNumber higher than this orderNumber will be queried.
   */
  var lastReceivedLogOrderNumber: Long? = null
)
