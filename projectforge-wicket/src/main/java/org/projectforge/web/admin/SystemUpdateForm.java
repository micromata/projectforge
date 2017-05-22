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

package org.projectforge.web.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.projectforge.Version;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.web.wicket.AbstractForm;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.ImagePanel;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

public class SystemUpdateForm extends AbstractForm<SystemUpdateForm, SystemUpdatePage>
{
  private static final long serialVersionUID = 2492737003121592489L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SystemUpdateForm.class);

  private static final String IS_RUNNING = "isRunning";

  private static final String HAS_TO_WAIT = "hasToWait";

  protected WebMarkupContainer scripts;

  public boolean showOldUpdateScripts;

  private GridBuilder gridBuilder;

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler;

  /**
   * List to create content menu in the desired order before creating the RepeatingView.
   */
  protected MyComponentsRepeater<SingleButtonPanel> actionButtons;

  public SystemUpdateForm(final SystemUpdatePage parentPage)
  {
    super(parentPage);
    csrfTokenHandler = new CsrfTokenHandler(this);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    add(createFeedbackPanel());
    gridBuilder = newGridBuilder(this, "flowform");
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset("Show all");
      fs.add(new CheckBoxPanel(fs.newChildId(), new PropertyModel<Boolean>(this, "showOldUpdateScripts"), null, true)
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.CheckBoxPanel#onSelectionChanged(java.lang.Boolean)
         */
        @Override
        protected void onSelectionChanged(final Boolean newSelection)
        {
          parentPage.refresh();
        }
      });
    }
    scripts = new WebMarkupContainer("scripts");
    add(scripts);
    updateEntryRows();

    actionButtons = new MyComponentsRepeater<SingleButtonPanel>("buttons");
    add(actionButtons.getRepeatingView());
    {
      final Button refreshButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("refresh"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.refresh();
        }
      };
      //      refreshButton.add(new AbstractAjaxTimerBehavior(Duration.seconds(10)) {
      //        @Override
      //        protected void onTimer(AjaxRequestTarget target) {
      //            parentPage.refresh();
      //        }
      //      });
      final SingleButtonPanel refreshButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), refreshButton, "refresh",
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(refreshButtonPanel);
      setDefaultButton(refreshButton);
    }
  }

  protected void updateEntryRows()
  {
    if (parentPage.myDatabaseUpdater.getSystemUpdater().isUpdating()) {
      log.info("Update is still running.");
      return;
    }
    scripts.removeAll();
    final RepeatingView scriptRows = new RepeatingView("scriptRows");
    scripts.add(scriptRows);
    final SortedSet<UpdateEntry> updateEntries = parentPage.myDatabaseUpdater.getSystemUpdater().getUpdateEntries();
    if (updateEntries == null) {
      return;
    }
    boolean odd = true;
    final List<WebMarkupContainer> updatebleItems = new ArrayList<>();
    UpdateEntry entryToUpdate = null;
    boolean isRestartRequired = false;
    for (final UpdateEntry updateEntry : updateEntries) {
      if (showOldUpdateScripts == false && updateEntry.getPreCheckStatus() == UpdatePreCheckStatus.ALREADY_UPDATED) {
        continue;
      }
      final Version version = updateEntry.getVersion();
      final WebMarkupContainer item = new WebMarkupContainer(scriptRows.newChildId());
      item.setOutputMarkupId(true);
      scriptRows.add(item);
      if (odd == true) {
        item.add(AttributeModifier.append("class", "odd"));
      } else {
        item.add(AttributeModifier.append("class", "even"));
      }
      odd = !odd;
      item.add(new Label("regionId", updateEntry.getRegionId()));
      if (updateEntry.isInitial() == true) {
        item.add(new Label("version", "initial"));
      } else {
        item.add(new Label("version", version.toString()));
      }
      final String description = updateEntry.getDescription();
      item.add(new Label("description", StringUtils.isBlank(description) == true ? "" : description));
      item.add(new Label("date", updateEntry.getDate()));
      final String preCheckResult = updateEntry.getPreCheckResult();
      item.add(new Label("preCheckResult", HtmlHelper.escapeHtml(preCheckResult, true)));
      if (updateEntry.getPreCheckStatus() == UpdatePreCheckStatus.READY_FOR_UPDATE) {
        updatebleItems.add(item);
        entryToUpdate = updateEntry;
      } else if (updateEntry.getPreCheckStatus() == UpdatePreCheckStatus.RESTART_REQUIRED) {
        updatebleItems.add(item);
        isRestartRequired = true;
      } else {
        final String runningResult = updateEntry.getRunningResult();
        item.add(new Label("update", HtmlHelper.escapeHtml(runningResult, true)));
        item.add(createWaitingImagePanel(HAS_TO_WAIT));
      }
    }
    int countEntriesToUpdate = updatebleItems.size();
    int counter = 1;
    for (WebMarkupContainer updateItem : updatebleItems) {
      if (counter == countEntriesToUpdate && isRestartRequired == false) {
        // add update button only to last entry (lowest version)
        addButtonToItem(updateItem, entryToUpdate);
      } else {
        updateItem.add(new Label("update", HtmlHelper.escapeHtml("", true)));
        updateItem.add(createWaitingImagePanel(HAS_TO_WAIT));
      }
      counter++;
    }
  }

  @SuppressWarnings("serial")
  private void addButtonToItem(WebMarkupContainer item, UpdateEntry updateEntry)
  {
    ImagePanel waitingImagePanel = createWaitingImagePanel(IS_RUNNING);
    final AjaxButton updateButton = new AjaxButton("button", new Model<String>("update"), this)
    {
      @Override
      protected void onSubmit(AjaxRequestTarget target, Form<?> form)
      {
        parentPage.update(updateEntry);
        this.setVisible(false);
        waitingImagePanel.setVisible(true);
        target.add(item);
      }
    };
    item.add(new SingleButtonPanel("update", updateButton, "update"));
    item.add(waitingImagePanel);
  }

  @SuppressWarnings("serial")
  private ImagePanel createWaitingImagePanel(String type)
  {
    final NonCachingImage img = new NonCachingImage("image", new AbstractReadOnlyModel<DynamicImageResource>()
    {
      @Override
      public DynamicImageResource getObject()
      {
        final DynamicImageResource dir = new DynamicImageResource()
        {
          @Override
          protected byte[] getImageData(final Attributes attributes)
          {
            try {
              if (IS_RUNNING.equals(type)) {
                return IOUtils
                    .toByteArray(getClass().getClassLoader().getResource("images/gif/hourglass.gif").openStream());
              } else {
                return IOUtils
                    .toByteArray(getClass().getClassLoader().getResource("images/clock.png").openStream());
              }
            } catch (IOException e) {
              log.error("Error while getting Image.");
              return null;
            }
          }
        };
        if (IS_RUNNING.equals(type)) {
          dir.setFormat("image/gif");
        } else {
          dir.setFormat("image/png");
        }
        return dir;
      }
    });
    img.add(new AttributeModifier("height", Integer.toString(30)));
    ImagePanel imagePanel = new ImagePanel("imagePanel", img);
    if (IS_RUNNING.equals(type)) {
      imagePanel.setVisible(false);
    }
    return imagePanel;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractForm#onBeforeRender()
   */
  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    actionButtons.render();
  }

  @Override
  protected void onSubmit()
  {
    super.onSubmit();
    csrfTokenHandler.onSubmit();
  }
}
