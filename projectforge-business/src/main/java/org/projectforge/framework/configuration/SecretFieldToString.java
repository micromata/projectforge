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


package org.projectforge.framework.configuration;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.lang.reflect.Field;

/**
 * toString() of configuration objects without revealing their secrets.
 */
public class SecretFieldToString {
    private static final String SECRET_PROPERTY_STRING = "******";

    /**
     * Replaces field values with annotation {@link SecretField} by "******".
     *
     * @param configObject
     * @return String representation of the given object.
     * @see ReflectionToStringBuilder#ReflectionToStringBuilder(Object)
     */
    public static String toString(final Object configObject) {
        return new ReflectionToStringBuilder(configObject) {
            @Override
            protected Object getValue(final Field field) throws IllegalArgumentException, IllegalAccessException {
                if (field.isAnnotationPresent(SecretField.class)) {
                    return SECRET_PROPERTY_STRING;
                }
                return super.getValue(field);
            }

        }.toString();
    }
}
