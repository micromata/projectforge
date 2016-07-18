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

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.PresizedImage;

/**
 * An image as submit button and optional with a tooltip.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class ImageSubmitLinkPanel extends Panel
{
  private static final long serialVersionUID = 1333929048394636569L;

  private final SubmitLink submitLink;

  @SuppressWarnings("serial")
  private ImageSubmitLinkPanel(final String id, final Form< ? > form)
  {
    super(id);
    submitLink = new SubmitLink("submitLink", form) {
      @Override
      public void onSubmit()
      {
        ImageSubmitLinkPanel.this.onSubmit();
      };
    };
    add(submitLink);
  }

  public ImageSubmitLinkPanel(final String id, final String relativeImagePath)
  {
    this(id, (Form< ? >) null);
    submitLink.add(new PresizedImage("image", relativeImagePath));
  }

  public ImageSubmitLinkPanel(final String id, final Form< ? > form, final String relativeImagePath)
  {
    this(id, form);
    submitLink.add(new PresizedImage("image", relativeImagePath));
  }

  public ImageSubmitLinkPanel(final String id, final String relativeImagePath, final String tooltip)
  {
    this(id, (Form< ? >) null);
    submitLink.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public ImageSubmitLinkPanel(final String id, final Form< ? > form, final String relativeImagePath, final String tooltip)
  {
    this(id, form);
    submitLink.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public ImageSubmitLinkPanel(final String id, final String relativeImagePath, final IModel<String> tooltip)
  {
    this(id, (Form< ? >) null);
    submitLink.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public ImageSubmitLinkPanel(final String id, final Form< ? > form, final String relativeImagePath, final IModel<String> tooltip)
  {
    this(id, form);
    submitLink.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public abstract void onSubmit();

  public ImageSubmitLinkPanel setDefaultFormProcessing(final boolean defaultFormProcessing)
  {
    submitLink.setDefaultFormProcessing(defaultFormProcessing);
    return this;
  }
}
