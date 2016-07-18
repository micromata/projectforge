/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.util.Date;

import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;
import org.projectforge.framework.xstream.XmlOmitField;

@XmlObject(alias = "test")
public class TestObject implements TestObjectIFace
{
  static double OMIT_STATIC = 1.0;
  
  final int omitFinal = 5;
  
  transient int omitTransient = 7;
  
  TestEnum color1;

  @XmlField(defaultStringValue = "BLUE")
  TestEnum color2 = TestEnum.BLUE;

  String s0;

  @XmlField(asAttribute = true)
  String s1;

  @XmlField(alias = "string2", asAttribute = true)
  String s2;

  @XmlField(defaultStringValue = "Hurzel", asAttribute = true)
  String s3 = "Hurzel";

  @XmlField(defaultStringValue = "", asAttribute = true)
  String s4 = "";

  @XmlOmitField
  String t0;

  @XmlField
  String t1;

  @XmlField(defaultStringValue = "Hurzel")
  String t2 = "Hurzel";

  @XmlField
  double d1;

  @XmlField(defaultDoubleValue = 5.0)
  double d2 = 5.0;

  @XmlField
  int i1;

  @XmlField(defaultIntValue = 42)
  int i2 = 42;
  
  boolean b1;
  
  @XmlField(defaultBooleanValue = true)
  boolean b2 = true;

  @XmlField(defaultBooleanValue = false)
  boolean b3 = false;
  
  Date date;
  
  TestObject testObject;
}
