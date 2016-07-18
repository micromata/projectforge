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

import java.util.LinkedList;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.wicket.AbstractSecuredPage;

import name.fraser.neil.plaintext.DiffMatchPatch;
import name.fraser.neil.plaintext.DiffMatchPatch.Diff;

/**
 * Represents a text panel showing the difference between two texts. The old and new version of the text is shown in a
 * pop-over.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DiffTextPanel extends Panel
{
  private static final long serialVersionUID = -4688885351297467119L;

  public static final String WICKET_ID = "text";

  private final Label label;

  private String prettyHtml;

  private ModalDialog modalDialog;

  private boolean showModalDialog = true;

  private IModel<String> newText, oldText;

  @SuppressWarnings("serial")
  public DiffTextPanel(final String id, final IModel<String> newText, final IModel<String> oldText,
      final Behavior... behaviors)
  {
    super(id);
    this.newText = newText;
    this.oldText = oldText;
    label = new Label(WICKET_ID, new Model<String>()
    {
      @Override
      public String getObject()
      {
        if (prettyHtml == null) {
          final DiffMatchPatch diffMatchPatch = new DiffMatchPatch();
          String newValue = newText.getObject();
          if (newValue == null || "null".equals(newValue) == true) {
            newValue = getString("label.null");
          }
          String oldValue = oldText.getObject();
          if (oldValue == null || "null".equals(oldValue) == true) {
            oldValue = getString("label.null");
          }
          final LinkedList<Diff> diffs = diffMatchPatch.diff_main(oldValue, newValue);
          diffMatchPatch.diff_cleanupSemantic(diffs);
          prettyHtml = getPrettyHtml(diffs);
        }
        return prettyHtml;
      }
    });
    label.add(behaviors).setEscapeModelStrings(false);
    add(label);
  }

  /**
   * If set to true, then the user is able to click on this panel and a modal dialog will open with the old and new text
   * to copy.
   * 
   * @param showModalDialog the showModalDialog to set (default is true).
   * @return this for chaining.
   */
  public DiffTextPanel setShowModalDialog(final boolean showModalDialog)
  {
    this.showModalDialog = showModalDialog;
    return this;
  }

  /**
   * If the models of old and/or new text were changed you may call this method to force a new running of the diff
   * algorithm.
   * 
   * @return this for chaining.
   */
  public DiffTextPanel recalculate()
  {
    prettyHtml = null;
    return this;
  }

  /**
   * Calls setRenderBodyOnly(false) and setOutputMarkupId(true) for the enclosed label.
   * 
   * @return the label
   */
  public Label getLabel4Ajax()
  {
    label.setRenderBodyOnly(false).setOutputMarkupId(true);
    return label;
  }

  /**
   * @return the label
   */
  public Label getLabel()
  {
    return label;
  }

  /**
   * @see org.apache.wicket.Component#setMarkupId(java.lang.String)
   */
  @Override
  public DiffTextPanel setMarkupId(final String markupId)
  {
    label.setOutputMarkupId(true);
    label.setMarkupId(markupId);
    return this;
  }

  /**
   * @see WicketUtils#setStrong(org.apache.wicket.markup.html.form.FormComponent)
   * @return this for chaining.
   */
  public DiffTextPanel setStrong()
  {
    WicketUtils.setStrong(label);
    return this;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @SuppressWarnings("serial")
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    if (showModalDialog == true && getPage() != null && getPage() instanceof AbstractSecuredPage) {
      final AbstractSecuredPage parentPage = (AbstractSecuredPage) getPage();
      modalDialog = new ModalDialog(parentPage.newModalDialogId())
      {
        @Override
        public void init()
        {
          setTitle(getString("changes"));
          init(new Form<String>(getFormId()));
          {
            final FieldsetPanel fs = gridBuilder.newFieldset(getString("history.oldValue")).setLabelSide(false);
            final TextArea<String> textArea = new TextArea<String>(fs.getTextAreaId(), oldText);
            fs.add(textArea).setAutogrow(1, 10);
            textArea.add(AttributeModifier.replace("onclick", "$(this).select();"));
          }
          {
            final FieldsetPanel fs = gridBuilder.newFieldset(getString("history.newValue")).setLabelSide(false);
            final TextArea<String> textArea = new TextArea<String>(fs.getTextAreaId(), newText);
            fs.add(textArea).setAutogrow(1, 10);
            textArea.add(AttributeModifier.replace("onclick", "$(this).select();"));
          }
        }
      };
      modalDialog.setBigWindow();
      modalDialog.setLazyBinding();
      parentPage.add(modalDialog);
      final AjaxEventBehavior behavior = new AjaxEventBehavior("onclick")
      {
        @Override
        protected void onEvent(final AjaxRequestTarget target)
        {
          if (modalDialog.isBound() == false) {
            // First call, have to initialize it.
            modalDialog.init();
            target.add(modalDialog.getMainContainer());
            modalDialog.bind(target);
          }
          modalDialog.open(target);
        }
      };
      label.add(behavior);
      label.add(AttributeModifier.append("style", "cursor: pointer;"));
    }
  }

  protected String getPrettyHtml(final LinkedList<Diff> diffs)
  {
    final StringBuilder html = new StringBuilder();
    for (final Diff aDiff : diffs) {
      final String text = HtmlHelper.escapeHtml(aDiff.text, true);
      switch (aDiff.operation) {
        case INSERT:
          html.append("<ins>").append(text).append("</ins>");
          break;
        case DELETE:
          html.append("<del>").append(text).append("</del>");
          break;
        case EQUAL:
          html.append("<span>").append(text).append("</span>");
          break;
      }
    }
    return html.toString();

  }
}
