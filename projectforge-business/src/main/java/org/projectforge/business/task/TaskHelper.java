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

package org.projectforge.business.task;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.common.StringHelper;


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TaskHelper {
    /**
     * Removes all white spaces and normalizes the item list (csv with coma as separator char without any spaces) and sort. Examples:
     * <ul>
     * <li>null -&gt; null</li>
     * <li>"" -&gt; ""</li>
     * <li>"5.123.13.09.02; 08, .12" -&gt; ".12,08,5.123.13.09.02"</li>
     * </ul>
     *
     * @param task
     * @return The normalized sorted string.
     */
    public static String normalizeKost2BlackWhiteList(final TaskDO task) {
        return normalizeKost2BlackWhiteList(task.getKost2BlackWhiteList());
    }

    /**
     * Removes all white spaces and normalizes the item list (csv with coma as separator char without any spaces) and sort. Examples:
     * <ul>
     * <li>null -&gt; null</li>
     * <li>"" -&gt; ""</li>
     * <li>"5.123.13.09.02; 08, .12" -&gt; ".12,08,5.123.13.09.02"</li>
     * </ul>
     *
     * @param task
     * @return The normalized sorted string.
     */
    public static String normalizeKost2BlackWhiteList(final String kost2BlackWhiteList) {
        final String[] items = TaskDO.Companion.getKost2BlackWhiteItems(kost2BlackWhiteList);
        final String[] sortedItems = StringHelper.sortAndUnique(items);
        return StringHelper.listToString(",", sortedItems);
    }

    /**
     * Adds the given kost to the kost2BlackWhiteList string and returns the normalized string.
     *
     * @param taskTree
     * @param task
     * @param kost
     * @return
     * @see #normalizeKost2BlackWhiteList(String)
     */
    public static String addKost2(final TaskTree taskTree, final TaskDO task, final Kost2DO kost) {
        if (kost == null) {
            return task.getKost2BlackWhiteList();
        }
        final StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotBlank(task.getKost2BlackWhiteList())) {
            buf.append(task.getKost2BlackWhiteList()).append(",");
        }
        if (task.getId() == null && task.getParentTaskId() != null) {
            buf.append(kost.getFormattedNumber());
        } else {
            final ProjektDO projekt = task.getId() != null ? taskTree.getProjekt(task.getId()) : taskTree.getProjekt(task.getParentTaskId());
            if (projekt == null) {
                buf.append(kost.getFormattedNumber());
            } else {
                final String projektKost2 = projekt.getKost();
                final String kost2 = kost.getFormattedNumber();
                if (!kost2.startsWith(projektKost2)) {
                    buf.append(kost.getFormattedNumber());
                } else {
                    final Kost2ArtDO kost2Art = kost.getKost2Art();
                    if (kost2Art != null) {
                        buf.append(StringHelper.format2DigitNumber(kost2Art.getId()));
                    }
                }
            }
        }
        return normalizeKost2BlackWhiteList(buf.toString());
    }
}
