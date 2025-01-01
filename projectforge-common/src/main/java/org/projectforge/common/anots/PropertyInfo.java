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

package org.projectforge.common.anots;

import org.projectforge.common.props.PropertyType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Annotation for properties of DO classes for handling i18n keys and Excel-Exports.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Target({ FIELD, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyInfo {
  /**
   * i18n key.
   */
  String i18nKey();

  /**
   * i18n key of additional key. Used e. g. by phone number to specify business or private number.
   */
  String additionalI18nKey() default "";

  /**
   * Optional tooltip (i18n key) to display.
   */
  String tooltip() default "";

  boolean required() default false;

  /**
   * @see PropertyType
   */
  PropertyType type() default PropertyType.UNSPECIFIED;

  /**
   * For own types if {@link PropertyType} doesn't provide needed type.
   * @return
   */
  String customType() default "";
}
