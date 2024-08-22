/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Mark entity, which should be has History.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
public @interface WithHistory {

    /**
     * Which code should be used to retrieve properties.
     *
     * @return the class ? extends history property provider []
     */
    //Class<? extends HistoryPropertyProvider>[] propertyProvider() default { DefaultHistoryPropertyProvider.class };

    /**
     * List of properties names not create history entries.
     *
     * @return the string[]
     */
    String[] noHistoryProperties() default {};

    /**
     * List of properties, which should make history, even it is in noHistoryProperties.
     *
     * @return the string[]
     */
    String[] forceHistoryProperties() default {};

    /**
     * A class, which have nested history entities.
     *
     * @return the nested entities
     */
    Class<?>[] nestedEntities() default {};

}
