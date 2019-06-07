/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.user.api;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An user preference parameter is a parameter which can be stored via UserPrefDao in the data base. It's used e. g. for storing favorite
 * time sheets of the user (see annotated fields of TimesheetDO).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Target( { FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPrefParameter {
  /**
   * Used for displaying the ordered parameters.
   */
  String orderString() default "";

  /**
   * Used for displaying the parameter.
   */
  String i18nKey();

  /**
   * I18n key for an optional tool tip.
   */
  String tooltipI18nKey() default "";

  /**
   * Needed e. g. for Kost2DO entries, because the drop down choice contains entries dependent form a task. The task property should be
   * given via dependsOn.
   * @return
   */
  String dependsOn() default "";

  /**
   * Used for displaying the parameter and validating the user's input.
   */
  boolean required() default false;

  /**
   * Used for displaying the parameter as text area (only for type String).
   */
  boolean multiline() default false;
}
