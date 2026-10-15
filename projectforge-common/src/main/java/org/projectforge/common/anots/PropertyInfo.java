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

package org.projectforge.common.anots;

import org.projectforge.common.props.PropertyType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Annotation for properties of DO classes for handling i18n keys and Excel-Exports.
 * @author Kai Reinhard
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
   * Smallest / largest value a numeric property accepts, as a decimal literal ("0", "100", "9999.99").
   * Empty means unbounded, which is the default: most numbers are bounded by their column, not by a
   * rule of the domain.
   * <p>
   * The one numeric rule the JPA {@code @Column} cannot express: {@code length} is a digit count, and
   * {@code precision}/{@code scale} are a storage size, neither of them a "0 to 100 percent". Wicket
   * says it per form field ({@code MinMaxNumberField}), which is why the rule only ever applied to
   * Wicket; declared here it reaches every client — the automatic validation of
   * {@code ValidationUtils.validateFields} and, through the generated metadata, the forms of
   * projectforge-next.
   * <p>
   * A string and not a {@code double}: an annotation may only hold compile-time constants, and a
   * {@code BigDecimal} bound written as a floating point number is no longer the bound that was meant.
   * Parsed once by {@code ElementsRegistry}, so a malformed literal fails at startup rather than
   * silently dropping the rule.
   */
  String min() default "";

  /**
   * @see #min()
   */
  String max() default "";

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
