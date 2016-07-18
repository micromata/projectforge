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

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Represents an icon.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class InputPanel extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "input";

  private static final long serialVersionUID = -4126462093466172226L;

  private FormComponent< ? > field;

  private FieldType fieldType;

  public InputPanel(final String id, final FormComponent< ? > field)
  {
    super(id);
    add(this.field = field);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    if (fieldType != null) {
      if (fieldType == FieldType.PHONE_NO || fieldType == FieldType.MOBILE_PHONE_NO) {
        field.add(AttributeModifier.append("class", "phone"));
      }
    }
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    field.setOutputMarkupId(true);
    return field.getMarkupId();
  }

  /**
   * @return the field
   */
  public FormComponent< ? > getField()
  {
    return field;
  }

  /**
   * @return the fieldType
   */
  public FieldType getFieldType()
  {
    return fieldType;
  }

  /**
   * @param fieldType the fieldType to set
   * @return this for chaining.
   */
  public InputPanel setFieldType(final FieldType fieldType)
  {
    this.fieldType = fieldType;
    return this;
  }

  /**
   * Sets the html markup attribute type to the given value.
   * @param attr
   * @return this for chaining.
   */
  public InputPanel setTypeAttribute(final String attr)
  {
    field.add(AttributeModifier.replace("type", attr));
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent< ? > getFormComponent()
  {
    return this.field;
  }
}
