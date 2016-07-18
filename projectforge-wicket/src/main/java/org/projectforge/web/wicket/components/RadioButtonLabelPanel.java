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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Panel containing only one radio-button followed by one label (label with for attribute).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RadioButtonLabelPanel<T> extends Panel
{
  private static final long serialVersionUID = 3068704086729359557L;

  final Radio<T> radioButton;

  public RadioButtonLabelPanel(final String id, final IModel<T> model, final String label)
  {
    super(id);
    radioButton = new Radio<T>("radioButton", model);
    add(radioButton);
    final Model<String> labelModel = new Model<String>(label);
    radioButton.setLabel(labelModel);
    // I18n key must be implemented as Model not as String because in constructor (before adding this component to parent) a warning will be
    // logged for using getString(String).
    add(new Label("label", labelModel).add(AttributeModifier.replace("for", radioButton.getMarkupId())));
    setRenderBodyOnly(true);
  }

  public RadioButtonLabelPanel<T> setSubmitOnChange()
  {
    radioButton.add(AttributeModifier.replace("onchange", "javascript:submit();"));
    return this;
  }
}
