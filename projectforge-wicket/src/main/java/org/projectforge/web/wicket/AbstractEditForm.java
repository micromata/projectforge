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

package org.projectforge.web.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

public abstract class AbstractEditForm<O extends AbstractBaseDO<Integer>, P extends AbstractEditPage<?, ?, ?>> extends
    AbstractSecuredForm<O, P>
{
  public static final String UPDATE_AND_STAY_BUTTON_MARKUP_ID = "updateAndStay";

  private static final long serialVersionUID = -6707610179583359099L;

  protected O data;

  /**
   * List to create content menu in the desired order before creating the RepeatingView.
   */
  protected MyComponentsRepeater<SingleButtonPanel> actionButtons;

  protected Button cancelButton;

  protected Button createButton;

  protected SingleButtonPanel createButtonPanel;

  protected Button updateButton;

  protected Button updateAndNextButton;

  protected SingleButtonPanel updateButtonPanel;

  protected SingleButtonPanel updateAndNextButtonPanel;

  protected SingleButtonPanel deleteButtonPanel;

  protected SingleButtonPanel markAsDeletedButtonPanel;

  protected SingleButtonPanel updateAndStayButtonPanel;

  protected Button undeleteButton;

  protected SingleButtonPanel undeleteButtonPanel;

  protected Button cloneButton;

  protected SingleButtonPanel cloneButtonPanel;

  protected FeedbackPanel feedbackPanel;

  protected GridBuilder gridBuilder;

  private O origData;
  /**
   * If set and supported by the edit page, the user is able to accept or discard changes.
   */
  protected O oldData;
  private AttributeAppender updateAndStayButtonClassHiddenAttributeAppender;

  protected boolean ignoreErrorOnClone;

  public AbstractEditForm(final P parentPage, final O data)
  {
    super(parentPage);
    this.data = data;
    this.ignoreErrorOnClone = false;
  }

  /**
   * @param oldData the oldData to set
   * @return this for chaining.
   */
  public AbstractEditForm<O, P> setOldData(final O oldData)
  {
    this.oldData = oldData;
    return this;
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    feedbackPanel = createFeedbackPanel();
    add(feedbackPanel);
    gridBuilder = newGridBuilder(this, "flowform");

    actionButtons = new MyComponentsRepeater<SingleButtonPanel>("buttons");
    add(actionButtons.getRepeatingView());
    {
      cancelButton = new Button("button", new Model<String>("cancel"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.cancel();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };
      cancelButton.setDefaultFormProcessing(false); // No validation of the
      final SingleButtonPanel cancelButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cancelButton,
          getString("cancel"),
          SingleButtonPanel.CANCEL);
      actionButtons.add(cancelButtonPanel);
    }
    {
      final Button markAsDeletedButton = new Button("button", new Model<String>("markAsDeleted"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.markAsDeleted();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };
      markAsDeletedButton.setMarkupId("markAsDeleted").setOutputMarkupId(true);
      markAsDeletedButton.add(AttributeModifier.replace("onclick", "return showDeleteQuestionDialog();"));
      markAsDeletedButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), markAsDeletedButton,
          getString("markAsDeleted"),
          SingleButtonPanel.DELETE);
      actionButtons.add(markAsDeletedButtonPanel);
    }
    {
      final Button deleteButton = new Button("button", new Model<String>("delete"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.delete();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };
      deleteButton.add(AttributeModifier.replace("onclick", "return showDeleteQuestionDialog();"));
      deleteButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), deleteButton, getString("delete"),
          SingleButtonPanel.DELETE);
      deleteButton.setDefaultFormProcessing(false);
      actionButtons.add(deleteButtonPanel);
    }
    {
      final Button resetButton = new Button("button", new Model<String>("reset"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.reset();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };
      final SingleButtonPanel resetButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), resetButton,
          getString("reset"),
          SingleButtonPanel.RESET);
      resetButtonPanel.setVisible(false);
      actionButtons.add(resetButtonPanel);
    }

    {
      updateButton = new Button("button", new Model<String>("update"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.update();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };
      updateButton.setMarkupId("update").setOutputMarkupId(true);
      updateButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), updateButton, getString("update"));
      actionButtons.add(updateButtonPanel);
    }

    /**
     * This button is used within the {@linkplain org.projectforge.web.common.timeattr.TimedAttributePanel}. It is not visible to the user.
     */
    {
      final Button updateAndStayButton = new Button("button", new Model<>("updateAndStay"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.updateAndStay();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };
      updateAndStayButton.setMarkupId(UPDATE_AND_STAY_BUTTON_MARKUP_ID).setOutputMarkupId(true);

      // This button does not need a name since it is not visible to the user.
      updateAndStayButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), updateAndStayButton, "");
      updateAndStayButtonClassHiddenAttributeAppender = AttributeModifier.append("class", "hidden");
      updateAndStayButtonPanel.getButton().add(updateAndStayButtonClassHiddenAttributeAppender);
      actionButtons.add(updateAndStayButtonPanel);
    }

    {
      updateAndNextButton = new Button("button", new Model<String>("updateAndNext"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.updateAndNext();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };

      updateAndNextButton.setMarkupId("updateAndNext").setOutputMarkupId(true);
      updateAndNextButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), updateAndNextButton,
          getString("updateAndNext"));
      actionButtons.add(updateAndNextButtonPanel);
    }
    {
      createButton = new Button("button", new Model<String>("create"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.create();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };

      createButton.setMarkupId("create").setOutputMarkupId(true);

      createButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), createButton, getString("create"));
      actionButtons.add(createButtonPanel);
    }
    {
      undeleteButton = new Button("button", new Model<String>("undelete"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            parentPage.undelete();
          } catch (final UserException ex) {
            AbstractEditForm.this.error(parentPage.translateParams(ex));
          }
        }
      };

      undeleteButton.setMarkupId("undelete").setOutputMarkupId(true);

      undeleteButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), undeleteButton, getString("undelete"));
      actionButtons.add(undeleteButtonPanel);
    }
    markDefaultButtons();
    updateButtonVisibility();
  }

  public void showUpdateAndStayButton()
  {
    updateAndStayButtonPanel.getButton().remove(updateAndStayButtonClassHiddenAttributeAppender);
  }

  @Override
  public void onBeforeRender()
  {
    actionButtons.render();
    updateButtonVisibility();
    super.onBeforeRender();
  }

  @SuppressWarnings("unchecked")
  protected IPersistenceService<O> getBaseDao()
  {
    return (IPersistenceService<O>) parentPage.getBaseDao();
  }

  /**
   * Sets the visibility of buttons update, create and markAsDeleted in dependency of the isNew() function. Currently
   * used by TimesheetEdit's clone function for redraw the buttons correctly after clone.
   */
  protected void updateButtonVisibility()
  {
    try {
      final IPersistenceService<O> baseDao = getBaseDao();
      if (isNew() == true) {
        updateButtonPanel.setVisible(false);
        updateAndNextButtonPanel.setVisible(false);
        undeleteButtonPanel.setVisible(false);
        markAsDeletedButtonPanel.setVisible(false);
        deleteButtonPanel.setVisible(false);
        createButtonPanel.setVisible(baseDao.hasLoggedInUserInsertAccess());
        if (createButtonPanel.isVisible() == true) {
          setDefaultButton(createButton);
        } else {
          setDefaultButton(cancelButton);
        }
      } else {
        if (origData == null) {
          origData = getBaseDao().getById(getData().getId());
        }
        createButtonPanel.setVisible(false);
        if (getData().isDeleted() == true) {
          undeleteButtonPanel.setVisible(baseDao.hasLoggedInUserUpdateAccess(origData, origData, false));
          if (undeleteButtonPanel.isVisible() == true) {
            setDefaultButton(undeleteButton);
          }
          markAsDeletedButtonPanel.setVisible(false);
          deleteButtonPanel.setVisible(false);
          updateButtonPanel.setVisible(false);
          updateAndNextButtonPanel.setVisible(false);
        } else {
          undeleteButtonPanel.setVisible(false);
          if (parentPage.getBaseDao().isHistorizable() == true) {
            deleteButtonPanel.setVisible(false);
            markAsDeletedButtonPanel.setVisible(baseDao.hasLoggedInUserDeleteAccess(origData, origData, false));
          } else {
            deleteButtonPanel.setVisible(baseDao.hasLoggedInUserDeleteAccess(origData, origData, false));
            markAsDeletedButtonPanel.setVisible(false);
          }
          if (cloneButtonPanel != null) {
            cloneButtonPanel.setVisible(baseDao.hasLoggedInUserInsertAccess());
          }
          updateButtonPanel.setVisible(baseDao.hasLoggedInUserUpdateAccess(origData, origData, false));
          if (parentPage.isUpdateAndNextSupported() == true) {
            updateAndNextButtonPanel.setVisible(updateButtonPanel.isVisible());
          } else {
            updateAndNextButton.setVisible(false);
          }
          if (updateButtonPanel.isVisible() == true) {
            setDefaultButton(updateButton);
          } else {
            setDefaultButton(cancelButton);
          }
        }
      }
    } catch (final RuntimeException ex) {
      // It's possible that an exception is thrown by the dao (e. g. Exception in TaskDao if a cyclic reference was detected).
      if (ex instanceof UserException) {
        // If an UserException was thrown then try to show the message as validation error:
        final String i18nKey = ((UserException) ex).getI18nKey();
        if (i18nKey != null) {
          addError(i18nKey);
        }
      } else {
        throw ex;
      }
    }
  }

  /**
   * Adds a clone button (only if the data isn't new) and calls {@link AbstractEditPage#cloneData()}.
   */
  @SuppressWarnings("serial")
  protected void addCloneButton()
  {
    if (isNew() == false) {
      // Clone button for existing and not deleted invoices:
      cloneButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("clone"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.cloneData();
        }

        @Override
        public void onError()
        {
          if (ignoreErrorOnClone) {
            parentPage.cloneData();
          }
        }
      };
      cloneButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cloneButton,
          getString("clone"))
      {
        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          return isNew() == false;
        }
      };
      actionButtons.add(2, cloneButtonPanel);
    }

  }

  /**
   * Set the style class for the default buttons. Overwrite this, if you have a different default button than create,
   * update or undelete (don't call super!).
   */
  protected void markDefaultButtons()
  {
    createButtonPanel.setClassnames(SingleButtonPanel.DEFAULT_SUBMIT);
    updateButtonPanel.setClassnames(SingleButtonPanel.DEFAULT_SUBMIT);
    updateAndNextButtonPanel.setClassnames(SingleButtonPanel.DEFAULT_SUBMIT);
    undeleteButtonPanel.setClassnames(SingleButtonPanel.DEFAULT_SUBMIT);
  }

  /**
   * @return true, if id of data is null (id not yet exists).
   */
  public boolean isNew()
  {
    return data.getId() == null;
  }

  public O getData()
  {
    return this.data;
  }

  /**
   * This class uses the logger of the extended class.
   */
  protected abstract Logger getLogger();

  public FeedbackPanel getFeedbackPanel()
  {
    return feedbackPanel;
  }
}
