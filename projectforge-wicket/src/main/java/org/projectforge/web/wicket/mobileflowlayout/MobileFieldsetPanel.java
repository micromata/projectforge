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

package org.projectforge.web.wicket.mobileflowlayout;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.projectforge.web.mobile.CollapsiblePanel;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldType;
import org.projectforge.web.wicket.flowlayout.InputPanel;

/**
 * Represents a entry of a group panel. This can be a label, text field or other form components.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MobileFieldsetPanel extends AbstractFieldsetPanel<MobileFieldsetPanel>
{
  private static final long serialVersionUID = 2845731250470151819L;

  @SuppressWarnings("serial")
  public MobileFieldsetPanel(final String id, final FieldProperties< ? > fieldProperties)
  {
    super(id);
    fieldset = new WebMarkupContainer("fieldset");
    superAdd(fieldset);
    this.labelText = fieldProperties.getLabel();
    fieldset.add((label = new WebMarkupContainer("label")));
    label.add(new Label("labeltext", new Model<String>() {
      @Override
      public String getObject()
      {
        return getString(labelText);
      };
    }).setRenderBodyOnly(true));
    fieldsRepeater = new RepeatingView("fields");
    fieldset.add(fieldsRepeater);
  }

  /**
   */
  public MobileFieldsetPanel(final CollapsiblePanel parent, final FieldProperties< ? > fieldProperties)
  {
    this(parent.newChildId(), fieldProperties);
    parent.add(this);
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel#addChild(org.apache.wicket.Component[])
   */
  @Override
  protected MarkupContainer addChild(final Component... childs)
  {
    return fieldset.add(childs);
  }

  @Override
  protected InputPanel setFieldType(final InputPanel input, final FieldType fieldType)
  {
    if (fieldType == FieldType.E_MAIL) {
      input.setTypeAttribute("email");
    } else if (fieldType == FieldType.WEB_PAGE) {
      input.setTypeAttribute("url");
    } else if (fieldType == FieldType.PHONE_NO) {
      input.setTypeAttribute("tel");
    }
    return input;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel#getThis()
   */
  @Override
  protected MobileFieldsetPanel getThis()
  {
    return this;
  }

  /**
   * Creates and add a new RepeatingView as div-child if not already exist.
   * @see RepeatingView#newChildId()
   */
  @Override
  public String newChildId()
  {
    return fieldsRepeater.newChildId();
  }

}
