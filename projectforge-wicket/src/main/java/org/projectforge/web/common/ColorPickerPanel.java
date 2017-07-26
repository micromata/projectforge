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

package org.projectforge.web.common;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.collections.MicroMap;
import org.apache.wicket.util.template.JavaScriptTemplate;
import org.apache.wicket.util.template.PackageTextTemplate;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class ColorPickerPanel extends Panel
{
  private static final long serialVersionUID = 2327758640305381880L;

  private String selectedColor;

  /**
   * @param id
   */
  public ColorPickerPanel(final String id)
  {
    this(id, "#FAAF26");
  }

  /**
   * @param string
   * @param color
   */
  public ColorPickerPanel(final String id, final String color)
  {
    super(id);
    this.selectedColor = color;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    final Form<Void> colorForm = new Form<Void>("colorForm");
    add(colorForm);
    final TextField<String> colorField = new TextField<String>("color",
        new PropertyModel<String>(this, "selectedColor"));
    colorField.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      private static final long serialVersionUID = 1L;

      /**
       * @see org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior#onUpdate(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        onColorUpdate(selectedColor);
      }
    });
    colorForm.add(colorField);
    // colorpicker js
    final JavaScriptTemplate jsTemplate = new JavaScriptTemplate(
        new PackageTextTemplate(ColorPickerPanel.class, "ColorPicker.js.template"));
    final String javaScript = jsTemplate.asString(new MicroMap<String, String>("markupId", colorField.getMarkupId()));
    add(new Label("template", javaScript).setEscapeModelStrings(false));
  }

  /**
   * Hook method
   *
   * @param selectedColor
   */
  protected void onColorUpdate(final String selectedColor)
  {

  }

  /**
   * @return the selectedColor
   */
  public String getSelectedColor()
  {
    return selectedColor;
  }

  /**
   * @param selectedColor the selectedColor to set
   * @return this for chaining.
   */
  public ColorPickerPanel setSelectedColor(final String selectedColor)
  {
    this.selectedColor = selectedColor;
    return this;
  }

}
