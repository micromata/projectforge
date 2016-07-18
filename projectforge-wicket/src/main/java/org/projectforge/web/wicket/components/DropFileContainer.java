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

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.CsrfTokenHandler;

/**
 * The panel which includes the drop behavior for several files. If the dropped file (string) was sucessfully importet, the hook method
 * {@link #onStringImport(AjaxRequestTarget, String, String)} is called.
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public abstract class DropFileContainer extends Panel
{
  private static final long serialVersionUID = 3622467918922963503L;

  private final WebMarkupContainer main;

  private final String mimeType;

  /**
   * Cross site request forgery token.
   */
  private  CsrfTokenHandler csrfTokenHandler;

  /**
   * @param id
   */
  public DropFileContainer(final String id)
  {
    this(id, null);
  }

  public DropFileContainer(final String id, final String mimeType)
  {
    super(id);
    this.mimeType = mimeType;
    main = new WebMarkupContainer("main");
    add(main);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    final Form<FormBean> hiddenForm = new Form<FormBean>("hiddenForm", new CompoundPropertyModel<FormBean>(new FormBean()));
    hiddenForm.add(AttributeModifier.replace("data-mimetype", mimeType));
    main.add(hiddenForm);
    hiddenForm.add(new TextArea<String>("importString"));
    hiddenForm.add(new TextArea<String>("importFileName"));
    hiddenForm.add(new AjaxSubmitLink("submitButton") {
      private static final long serialVersionUID = 6140567784494429257L;

      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form< ? > form)
      {
        csrfTokenHandler.onSubmit();
        final FormBean modelObject = hiddenForm.getModel().getObject();
        onStringImport(target, modelObject.importFileName, modelObject.importString);
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final Form< ? > form)
      {
        // nothing to do here
      }

    });
    csrfTokenHandler = new CsrfTokenHandler(hiddenForm);
  }

  /**
   * @param content
   * @return this for chaining.
   */
  public DropFileContainer setTooltip(final String content)
  {
    WicketUtils.addTooltip(main, content);
    return this;
  }

  /**
   * @param title
   * @param content
   * @return this for chaining.
   */
  public DropFileContainer setTooltip(final String title, final String content)
  {
    WicketUtils.addTooltip(main, title, content);
    return this;
  }

  protected abstract void onStringImport(final AjaxRequestTarget target, final String filename, final String content);

  /**
   * Just the form model
   * 
   */
  private class FormBean implements Serializable
  {
    private static final long serialVersionUID = 4250094235574838882L;

    private String importString;

    private String importFileName;
  }
}
