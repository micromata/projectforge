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

package org.projectforge.business.orga;

import java.io.Serializable;

import org.projectforge.framework.persistence.utils.ReflectionToString;
import org.projectforge.framework.utils.ILabelValueBean;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * Can't use LabelValueBean because XStream doesn't support generics (does it?).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "contractType")
public class ContractType implements ILabelValueBean<String, String>, Serializable
{
  private static final long serialVersionUID = 553564992110970022L;

  @XmlField(asAttribute = true)
  private String label;

  @XmlField(asAttribute = true)
  private String value;

  public String getLabel()
  {
    return label;
  }

  public ContractType setLabel(final String label)
  {
    this.label = label;
    return this;
  }

  public String getValue()
  {
    return value;
  }

  public ContractType setValue(final String value)
  {
    this.value = value;
    return this;
  }

  @Override
  public String toString()
  {
    return new ReflectionToString(this).toString();
  }
}
