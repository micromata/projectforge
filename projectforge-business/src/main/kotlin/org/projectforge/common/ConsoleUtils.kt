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

package org.projectforge.common

fun main() {
    println("Hello, colored World!")
    val black = ConsoleUtils.getText("black", ConsoleUtils.AnsiColor.BLACK)
    val red = ConsoleUtils.getText("red")
    val green = ConsoleUtils.getText("green", ConsoleUtils.AnsiColor.GREEN)
    val yellow = ConsoleUtils.getText("yellow", ConsoleUtils.AnsiColor.YELLOW)
    val blue = ConsoleUtils.getText("blue", ConsoleUtils.AnsiColor.BLUE)
    val magenta = ConsoleUtils.getText("magenta", ConsoleUtils.AnsiColor.MAGENTA)
    val cyan = ConsoleUtils.getText("cyan", ConsoleUtils.AnsiColor.CYAN)
    val white = ConsoleUtils.getText("white", ConsoleUtils.AnsiColor.WHITE)
    println(
        "Supported colors are $black, $red, $green, $yellow, $blue, $magenta, $cyan and $white."
    )
}

object ConsoleUtils {
    enum class AnsiColor(val code: String) {
        BLACK("\u001B[30m"),
        RED("\u001B[31m"),
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        BLUE("\u001B[34m"),
        MAGENTA("\u001B[35m"),
        CYAN("\u001B[36m"),
        WHITE("\u001B[37m"),
        RESET("\u001B[0m");

        override fun toString(): String {
            return code
        }
    }

    fun appendText(sb: StringBuilder, text: String, color: AnsiColor = AnsiColor.RED) {
        sb.append(color).append(text).append(AnsiColor.RESET)
    }

    fun getText(text: String, color: AnsiColor = AnsiColor.RED): String {
        return "$color$text${AnsiColor.RESET}"
    }
}
