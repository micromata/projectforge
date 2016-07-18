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
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.projectforge.web.wicket.WicketRenderHeadUtils;

/**
 * Represents an icon.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TextAreaPanel extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "textarea";

  private static final long serialVersionUID = -4126462093466172226L;

  private TextArea< ? > field;

  private boolean autogrow = false;

  private int autogrowMinHeight = -1;

  private int autogrowMaxHeight = -1;

  private boolean rendered = false;

  public TextAreaPanel(final String id, final TextArea< ? > field)
  {
    this(id, field, false);
  }

  public TextAreaPanel(final String id, final TextArea< ? > field, final boolean autogrow)
  {
    super(id);
    this.autogrow = autogrow;
    add(this.field = field);
  }

  /**
   * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.head.IHeaderResponse)
   */
  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    if (autogrow == true) {
      WicketRenderHeadUtils.renderAutogrowJavaScriptIncludes(response);
    }
  }

  /**
   * @return
   */
  public TextAreaPanel setAutogrow()
  {
    this.autogrow = true;
    return this;
  }

  /**
   * class="autogrow"
   * @return this for chaining.
   */
  public TextAreaPanel setAutogrow(final int minHeight, final int maxHeight)
  {
    this.autogrow = true;
    this.autogrowMinHeight = minHeight;
    this.autogrowMaxHeight = maxHeight;
    return this;
  }

  /**
   * @see org.apache.wicket.Component#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    if (rendered == false && autogrow == true) {
      // Doesn't work as part in onInitialize() for ModalDialogs. onInitialized is called before setAutogrow() is executed.
      field.add(AttributeModifier.append("class", "autogrow"));
      if (autogrowMinHeight > 0) {
        field.add(AttributeModifier.append("autogrowMinHeight", autogrowMinHeight));
      }
      if (autogrowMaxHeight > 0) {
        field.add(AttributeModifier.append("autogrowMaxHeight", autogrowMaxHeight));
      }
    }
    rendered = true;
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
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent< ? > getFormComponent()
  {
    return field;
  }
}
