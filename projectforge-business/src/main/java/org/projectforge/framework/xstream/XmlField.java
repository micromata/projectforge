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

package org.projectforge.framework.xstream;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for properties which should be (de-)serialized from and to xml attributes.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Target( { FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlField {
  /**
   * If given then this alias will be used for (de-)serialization (as attribute name). If not given, the field name itself is used.
   */
  String alias() default "";

  /*
   * Useful for annotating fields with interfaces. The implementation class instead will be instantiated.
   */
  // Class< ? > implementationClass() default Void.class;

  /**
   * Some attributes will be serialized as attributes as default. Please note: asAttribute=false has no effect (please use asElement=true
   * instead)!
   * @see XmlObjectWriter#asAttributeAsDefault(Class)
   */
  boolean asAttribute() default false;

  /**
   * Some attributes will be serialized as attributes as default. asElement=true suppresses this. Please note: asElement=false has no effect
   * (please use asAttribute=true instead)!
   * @see XmlObjectWriter#asAttributeAsDefault(Class)
   */
  boolean asElement() default false;

  /**
   * For properties of type double this value declares the default value. If this property value is equal to the default value it will not
   * be serialized.
   */
  double defaultDoubleValue() default Double.NaN;

  /**
   * For properties of type int this value declares the default value. If this property value is equal to the default value it will not be
   * serialized.
   */
  int defaultIntValue() default XmlConstants.MAGIC_INT_NUMBER; // magic number.

  /**
   * For properties of type String and Enum this value declares the default value. If this property value is equal to the default value it
   * will not be serialized.
   */
  String defaultStringValue() default XmlConstants.MAGIC_STRING;

  /**
   * For properties of type Boolean this value declares the default value. If this property value is equal to the default value it will not
   * be serialized.
   */
  boolean defaultBooleanValue() default false;

  /**
   * If true then the string will be encapsulated in <![CDATA[...]]>
   * @return
   */
  boolean asCDATA() default false;
}
