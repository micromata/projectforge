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

package org.projectforge.web.dialog;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.web.core.NavTopPanel;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

import de.micromata.wicket.ajax.AjaxCallback;
import de.micromata.wicket.ajax.AjaxFormSubmitCallback;

/**
 * Base component for the ProjectForge modal dialogs.<br/>
 * This dialog is modal.<br/>
 *
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class ModalDialog extends Panel
{
  private static final long serialVersionUID = 4235521713603821639L;

  protected GridBuilder gridBuilder;

  protected final WebMarkupContainer mainContainer, mainSubContainer, gridContentContainer, buttonBarContainer;

  private boolean escapeKeyEnabled = true;

  private String closeButtonLabel;

  private SingleButtonPanel closeButtonPanel;

  private boolean showCancelButton;

  private boolean bigWindow;

  private boolean draggable = true;

  private Boolean resizable;

  private boolean lazyBinding;

  private WebMarkupContainer titleContainer;

  private Label titleLabel;

  protected Form<?> form;

  protected FeedbackPanel formFeedback;

  private final IModel<Boolean> useCloseHandler = Model.of(false);

  private final AjaxEventBehavior closeBehavior;

  /**
   * If true, a GridBuilder is automatically available.
   */
  protected boolean autoGenerateGridBuilder = true;

  /**
   * List to create action buttons in the desired order before creating the RepeatingView.
   */
  protected MyComponentsRepeater<Component> actionButtons;

  /**
   * Cross site request forgery token.
   */
  protected CsrfTokenHandler csrfTokenHandler;

  /**
   * @param id
   */
  @SuppressWarnings("serial")
  public ModalDialog(final String id)
  {
    super(id);
    actionButtons = new MyComponentsRepeater<Component>("actionButtons");
    mainContainer = new WebMarkupContainer("mainContainer");
    add(mainContainer.setOutputMarkupId(true));
    mainContainer.add(mainSubContainer = new WebMarkupContainer("mainSubContainer"));
    gridContentContainer = new WebMarkupContainer("gridContent");
    gridContentContainer.setOutputMarkupId(true);
    buttonBarContainer = new WebMarkupContainer("buttonBar");
    buttonBarContainer.setOutputMarkupId(true);
    closeBehavior = new AjaxEventBehavior("hidden.bs.modal")
    {
      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        handleCloseEvent(target);
      }

      @Override
      protected void updateAjaxAttributes(final AjaxRequestAttributes attributes)
      {
        super.updateAjaxAttributes(attributes);

        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
      }
    };
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    if (bigWindow == true) {
      mainSubContainer.add(AttributeModifier.append("class", "big-modal"));
    }
    if (useCloseHandler.getObject() == true) {
      mainContainer.add(closeBehavior);
    }
  }

  /**
   * Sets also draggable to false. Appends css class big-modal.
   */
  public ModalDialog setBigWindow()
  {
    bigWindow = true;
    draggable = false;
    return this;
  }

  /**
   * Only the div panel of the modal dialog is rendered without buttons and content. Default is false.
   *
   * @return this for chaining.
   */
  public ModalDialog setLazyBinding()
  {
    this.lazyBinding = true;
    mainSubContainer.setVisible(false);
    return this;
  }

  public void bind(final AjaxRequestTarget target)
  {
    actionButtons.render();
    mainSubContainer.setVisible(true);
    target.appendJavaScript(getJavaScriptAction());
  }

  /**
   * @return true if no lazy binding was used or bind() was already called.
   */
  public boolean isBound()
  {
    return mainSubContainer.isVisible();
  }

  /**
   * @param draggable the draggable to set (default is true).
   * @return this for chaining.
   */
  public ModalDialog setDraggable(final boolean draggable)
  {
    this.draggable = draggable;
    return this;
  }

  /**
   * @param resizable the resizable to set (default is true for bigWindows, otherwise false).
   * @return this for chaining.
   */
  public ModalDialog setResizable(final boolean resizable)
  {
    this.resizable = resizable;
    return this;
  }

  /**
   * Display the cancel button.
   *
   * @return this for chaining.
   */
  public ModalDialog setShowCancelButton()
  {
    this.showCancelButton = true;
    return this;
  }

  /**
   * @param escapeKeyEnabled the keyboard to set (default is true).
   * @return this for chaining.
   */
  public ModalDialog setEscapeKeyEnabled(final boolean escapeKeyEnabled)
  {
    this.escapeKeyEnabled = escapeKeyEnabled;
    return this;
  }

  /**
   * Close is used as default:
   *
   * @param closeButtonLabel the closeButtonLabel to set
   * @return this for chaining.
   */
  public ModalDialog setCloseButtonLabel(final String closeButtonLabel)
  {
    this.closeButtonLabel = closeButtonLabel;
    return this;
  }

  /**
   * Should be called directly after {@link #init()}.
   *
   * @param tooltipTitle
   * @param tooltipContent
   * @see WicketUtils#addTooltip(Component, IModel, IModel)
   */
  public ModalDialog setCloseButtonTooltip(final IModel<String> tooltipTitle, final IModel<String> tooltipContent)
  {
    WicketUtils.addTooltip(this.closeButtonPanel.getButton(), tooltipTitle, tooltipContent);
    return this;
  }

  public ModalDialog wantsNotificationOnClose()
  {
    setUseCloseHandler(true);
    return this;
  }

  public String getMainContainerMarkupId()
  {
    return mainContainer.getMarkupId(true);
  }

  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    if (lazyBinding == false) {
      final String script = getJavaScriptAction();
      response.render(OnDomReadyHeaderItem.forScript(script));
    }
  }

  private String getJavaScriptAction()
  {
    final StringBuffer script = new StringBuffer();
    script.append("$('#").append(getMainContainerMarkupId()).append("').modal({keyboard: ").append(escapeKeyEnabled)
        .append(", show: false });");
    final boolean isResizable = (resizable == null && bigWindow == true) || Boolean.TRUE.equals(resizable) == true;
    if (draggable == true || isResizable == true) {
      script.append(" $('#").append(getMainContainerMarkupId()).append("')");
    }
    if (draggable == true) {
      script.append(".draggable()");
    }
    if (isResizable) {
      script.append(".resizable({ alsoResize: '#")
          .append(getMainContainerMarkupId())
          // max-height of .modal-body is 600px, need to enlarge this setting for resizing.
          .append(
              ", .modal-body', resize: function( event, ui ) {$('.modal-body').css('max-height', '4000px');}, minWidth: 300, minHeight: 200 })");
    }
    if (useCloseHandler.getObject()) {
      script.append(";$('#").append(getMainContainerMarkupId()).append("').on('hidden', function () { ")
          .append("  Wicket.Ajax.ajax({'u':'").append(closeBehavior.getCallbackUrl()).append("','c':'").append(getMainContainerMarkupId())
          .append("'});").append("})");
    }
    return script.toString();
  }

  public ModalDialog open(final AjaxRequestTarget target)
  {
    target.appendJavaScript("$('#" + getMainContainerMarkupId() + "').modal('show');");
    return this;
  }

  public void close(final AjaxRequestTarget target)
  {
    csrfTokenHandler.onSubmit();
    target.appendJavaScript("$('#" + getMainContainerMarkupId() + "').modal('hide');");
  }

  /**
   * Add the content to the AjaxRequestTarget if the content is changed.
   *
   * @param target
   * @return this for chaining.
   */
  public ModalDialog addContent(final AjaxRequestTarget target)
  {
    target.add(gridContentContainer);
    return this;
  }

  /**
   * Add the button bar to the AjaxRequestTarget if the buttons or their visibility are changed.
   *
   * @param target
   * @return this for chaining.
   */
  public ModalDialog addButtonBar(final AjaxRequestTarget target)
  {
    target.add(buttonBarContainer);
    return this;
  }

  /**
   * @param target
   * @return this for chaining.
   */
  public ModalDialog addTitleLabel(final AjaxRequestTarget target)
  {
    target.add(titleLabel);
    return this;
  }

  public abstract void init();

  /**
   * @param title
   * @return this for chaining.
   */
  public ModalDialog setTitle(final String title)
  {
    return setTitle(Model.of(title));
  }

  /**
   * @param title
   * @return this for chaining.
   */
  public ModalDialog setTitle(final IModel<String> title)
  {
    titleContainer = new WebMarkupContainer("titleContainer");
    mainSubContainer.add(titleContainer.setOutputMarkupId(true));
    titleContainer.add(titleLabel = new Label("titleText", title));
    titleLabel.setOutputMarkupId(true);
    return this;
  }

  /**
   * The gridContentContainer is cleared (all child elements are removed). This is useful for Ajax dialogs with dynamic content (see
   * {@link NavTopPanel} for an example).
   *
   * @return
   */
  public ModalDialog clearContent()
  {
    gridContentContainer.removeAll();
    if (autoGenerateGridBuilder == true) {
      gridBuilder = new GridBuilder(gridContentContainer, "flowform");
    }
    initFeedback(gridContentContainer);
    return this;
  }

  @SuppressWarnings("serial")
  protected void init(final Form<?> form)
  {
    this.form = form;
    csrfTokenHandler = new CsrfTokenHandler(form);
    mainSubContainer.add(form);
    form.add(gridContentContainer);
    form.add(buttonBarContainer);
    if (showCancelButton == true) {
      final SingleButtonPanel cancelButton = appendNewAjaxActionButton(new AjaxCallback()
      {
        @Override
        public void callback(final AjaxRequestTarget target)
        {
          csrfTokenHandler.onSubmit();
          onCancelButtonSubmit(target);
          close(target);
        }
      }, getString("cancel"), SingleButtonPanel.CANCEL);
      cancelButton.getButton().setDefaultFormProcessing(false);
    }
    closeButtonPanel = appendNewAjaxActionButton(new AjaxFormSubmitCallback()
    {

      @Override
      public void callback(final AjaxRequestTarget target)
      {
        csrfTokenHandler.onSubmit();
        if (onCloseButtonSubmit(target)) {
          close(target);
        }
      }

      @Override
      public void onError(final AjaxRequestTarget target, final Form<?> form)
      {
        csrfTokenHandler.onSubmit();
        ModalDialog.this.onError(target, form);
      }
    }, closeButtonLabel != null ? closeButtonLabel : getString("close"), SingleButtonPanel.NORMAL);
    buttonBarContainer.add(actionButtons.getRepeatingView());
    form.setDefaultButton(closeButtonPanel.getButton());
    if (autoGenerateGridBuilder == true) {
      gridBuilder = new GridBuilder(gridContentContainer, "flowform");
    }
    initFeedback(gridContentContainer);
  }

  private void initFeedback(final WebMarkupContainer container)
  {
    if (formFeedback == null) {
      formFeedback = new FeedbackPanel("formFeedback", new ComponentFeedbackMessageFilter(form));
      formFeedback.setOutputMarkupId(true);
      formFeedback.setOutputMarkupPlaceholderTag(true);
    }
    container.add(formFeedback);
  }

  protected void ajaxError(final String error, final AjaxRequestTarget target)
  {
    csrfTokenHandler.onSubmit();
    form.error(error);
    target.add(formFeedback);
  }

  /**
   * Called if {@link #wantsNotificationOnClose()} was chosen and the dialog is closed (by pressing esc, clicking outside or clicking the
   * upper right cross).
   *
   * @param target
   */
  protected void handleCloseEvent(final AjaxRequestTarget target)
  {
    csrfTokenHandler.onSubmit();
  }

  /**
   * Called if user hit the cancel button.
   *
   * @param target
   */
  protected void onCancelButtonSubmit(final AjaxRequestTarget target)
  {
  }

  /**
   * Called if user hit the close button.
   *
   * @param target
   * @return true if the dialog can be close, false if errors occured.
   */
  protected boolean onCloseButtonSubmit(final AjaxRequestTarget target)
  {
    return true;
  }

  protected void onError(final AjaxRequestTarget target, final Form<?> form)
  {
  }

  /**
   * @see org.apache.wicket.Component#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    if (lazyBinding == false) {
      actionButtons.render();
    }
  }

  public String getFormId()
  {
    return "form";
  }

  public SingleButtonPanel appendNewAjaxActionButton(final AjaxCallback ajaxCallback, final String label, final String... classnames)
  {
    final SingleButtonPanel result = addNewAjaxActionButton(ajaxCallback, label, classnames);
    this.actionButtons.add(result);
    return result;
  }

  public SingleButtonPanel prependNewAjaxActionButton(final AjaxCallback ajaxCallback, final String label, final String... classnames)
  {
    return insertNewAjaxActionButton(ajaxCallback, 0, label, classnames);
  }

  /**
   * @param ajaxCallback
   * @param position     0 is the first position.
   * @param label
   * @param classnames
   * @return
   */
  public SingleButtonPanel insertNewAjaxActionButton(final AjaxCallback ajaxCallback, final int position, final String label,
      final String... classnames)
  {
    final SingleButtonPanel result = addNewAjaxActionButton(ajaxCallback, label, classnames);
    this.actionButtons.add(position, result);
    return result;
  }

  private SingleButtonPanel addNewAjaxActionButton(final AjaxCallback ajaxCallback, final String label, final String... classnames)
  {
    final AjaxButton button = new AjaxButton("button", form)
    {
      private static final long serialVersionUID = -5306532706450731336L;

      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        csrfTokenHandler.onSubmit();
        ajaxCallback.callback(target);
      }

      @Override
      protected void onError(final AjaxRequestTarget target)
      {
        if (ajaxCallback instanceof AjaxFormSubmitCallback) {
          ((AjaxFormSubmitCallback) ajaxCallback).onError(target, this.getForm());
        }
      }
    };
    final SingleButtonPanel buttonPanel = new SingleButtonPanel(this.actionButtons.newChildId(), button, label, classnames);
    buttonPanel.add(button);
    return buttonPanel;
  }

  /**
   * @return the mainContainer
   */
  public WebMarkupContainer getMainContainer()
  {
    return mainContainer;
  }

  /**
   * Sets whether the close handler is used or not. Default is false.
   *
   * @param useCloseHandler True if close handler should be used
   * @return This
   */
  public final ModalDialog setUseCloseHandler(final boolean useCloseHandler)
  {
    this.useCloseHandler.setObject(useCloseHandler);
    return this;
  }
}
