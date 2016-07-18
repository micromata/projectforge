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

package org.projectforge.web.wicket.components;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * This component is a text field or a read-only label, dependent on the environment setting. Therefore you can create read-only and
 * edit-pages with the same markup and Java code.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TextFieldOrLabelPanel<T> extends Panel
{
  private static final long serialVersionUID = -6923305015138423150L;
  
  final public static String INPUT_FIELD_WICKET_ID = "textfield";

  /**
   * @param id
   * @param model
   * @param text field to show in read-write-mode.
   * @param readonly If true then a label is shown otherwise the given text field.
   * @see org.apache.wicket.Component#Component(String, IModel)
   */
  public TextFieldOrLabelPanel(final String id, final IModel<T> model, final TextField<T> textField, final boolean readonly)
  {
    super(id, model);
    if (readonly == true) {
      add(new Label("label", model).setRenderBodyOnly(true));
      add(new Label(INPUT_FIELD_WICKET_ID, "[invisible]").setVisible(false));
    } else {
      add(textField);
      add(new Label("label", "[invisible]").setVisible(false));
    }
  }
}
