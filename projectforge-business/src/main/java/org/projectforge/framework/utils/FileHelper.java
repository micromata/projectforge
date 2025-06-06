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

package org.projectforge.framework.utils;

import org.projectforge.common.FileUtils;
import org.projectforge.common.FilenameUtils;

/**
 * Some helper methods ...
 * Deprecated: Use {@link org.projectforge.common.FileUtils} and {@link org.projectforge.common.FilenameUtils} instead.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Deprecated
public class FileHelper {
    /**
     * Return the given path itself if it is already absolute, otherwise absolute path of given path relative to given parent.
     * @param parent
     * @param path
     * @return
     */
    public static String getAbsolutePath(String parent, String path) {
        return FileUtils.getAbsolutePath(parent, path);
    }

    /**
     * Creates a safe filename from the given string by converting all non specified characters will replaces by an underscore or will be
     * substitute. Example: "Schrödinger" -&gt; "Schroedinger", "http://www.micromata.de" -&gt; "http_www.micromata.de".
     *
     * @param str
     * @param maxlength The maximum length of the result.
     * @return
     */
    public static String createSafeFilename(String str, int maxlength) {
        return createSafeFilename(str, null, maxlength, false);
    }

    /**
     * FileHelper.createSafeFilename("basename", ".pdf", 8, true)) -> "basename_2010-08-12.pdf".
     * @param str
     * @param suffix
     * @param maxlength
     * @param appendTimestamp
     * @return
     */
    public static String createSafeFilename(final String str, final String suffix, final int maxlength, final boolean appendTimestamp) {
        return FilenameUtils.createSafeFilename(str, suffix, maxlength, appendTimestamp);
    }
}
