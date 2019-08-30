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

package org.projectforge.web.tree;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.PresizedImage;
import org.projectforge.web.wicket.WebConstants;

/**
 * Panel showing the icons of a tree list table.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class TreeIconsActionPanel<T extends Serializable> extends Panel
{
  public static final String LABEL_ID = "label";

  private final Link< ? > link;

  private boolean useAjaxAtDefault;

  private boolean useSubmitLinkImages;

  private final TreeTable< ? > treeTable;

  private TreeTableNode treeNode;

  private PresizedImage folderImage;

  private PresizedImage folderOpenImage;

  /**
   * Constructor for list view in selection mode.
   * @param id component id
   * @param model model for contact
   * @param caller The calling page.
   * @param selectProperty The property (name) of the caller to select.
   * @param objectId The id of the object to select on click.
   * @param label The label to show (additional to the row_pointer.png). The id of the label should be LABEL_ID.
   */
  public TreeIconsActionPanel(final String id, final IModel< ? > model, final ISelectCallerPage caller, final String selectProperty,
      final Integer objectId, final Label label, final TreeTable< ? > treeTable)
  {
    super(id, model);
    this.treeTable = treeTable;
    final Link<Void> selectLink = new Link<Void>("select") {
      @Override
      public void onClick()
      {
        WicketUtils.setResponsePage(this, caller);
        caller.select(selectProperty, objectId);
      };
    };
    this.link = selectLink;
    add(selectLink);
    add(label);
  }

  /**
   * @param id component id
   * @param model model for contact
   * @param editClass The edit page to redirect to.
   * @param objectId The id of the object to edit in edit page.
   * @param label The label to show (additional to the row_pointer.png). The id of the label should be LABEL_ID.
   */
  public TreeIconsActionPanel(final String id, final IModel< ? > model, final Class< ? extends AbstractEditPage< ? , ? , ? >> editClass,
      final Integer objectId, final Label label, final TreeTable< ? > treeTable)
  {
    super(id, model);
    this.treeTable = treeTable;
    final BookmarkablePageLink<Void> bookmarkablePagelink = new BookmarkablePageLink<Void>("select", editClass);
    bookmarkablePagelink.getPageParameters().add(AbstractEditPage.PARAMETER_KEY_ID, objectId);
    this.link = bookmarkablePagelink;
    add(link);
    add(label);
  }

  /**
   * Without click support (neither for selection nor for editing).
   * @param id component id
   * @param model model for contact
   * @param label The label to show (additional to the row_pointer.png). The id of the label should be LABEL_ID.
   */
  public TreeIconsActionPanel(final String id, final IModel< ? > model, final Label label, final TreeTable< ? > treeTable)
  {
    super(id, model);
    this.treeTable = treeTable;
    final Link<Void> selectLink = new Link<Void>("select") {
      @Override
      public void onClick()
      {
        // Do nothing
      };
    };
    selectLink.setVisible(false);
    this.link = selectLink;
    add(selectLink);
    add(label);
  }

  public TreeIconsActionPanel<T> setUseAjaxAtDefault(final boolean useAjaxAtDefault)
  {
    this.useAjaxAtDefault = useAjaxAtDefault;
    return this;
  }

  /**
   * Default is false meaning that only click images will be used for browsing.
   * @param useSubmitLinkImages
   */
  public TreeIconsActionPanel<T> setUseSubmitLinkImages(final boolean useSubmitLinkImages)
  {
    this.useSubmitLinkImages = useSubmitLinkImages;
    return this;
  }

  public void init(final TreeTablePanel page, final TreeTableNode treeNode)
  {
    this.treeNode = treeNode;
    final ContextImage spacerImage = new PresizedImage("spacer", WebConstants.IMAGE_SPACER);
    final boolean showExploreIcon = treeNode.hasChildren();
    int spacerWidth;
    if (showExploreIcon == true)
      spacerWidth = treeNode.getIndent() * WebConstants.IMAGE_TREE_ICON_WIDTH + 1;
    else spacerWidth = (treeNode.getIndent() + 1) * WebConstants.IMAGE_TREE_ICON_WIDTH + 1;
    spacerImage.add(AttributeModifier.replace("width", String.valueOf(spacerWidth)));
    if (this.link.isVisible() == true) {
      link.add(spacerImage);
      add(WicketUtils.getInvisibleDummyImage("spacer", getRequestCycle()));
    } else {
      add(spacerImage);
    }
    final ContextImage leafImage = new PresizedImage("leaf", WebConstants.IMAGE_TREE_ICON_LEAF);
    leafImage.setVisible(treeNode.isLeaf());
    add(leafImage);
    final WebMarkupContainer iconSpan = new WebMarkupContainer("icons");
    add(iconSpan);
    if (useAjaxAtDefault == false) {
      iconSpan.setRenderBodyOnly(true);
    }
    {
      final WebMarkupContainer exploreLink;
      if (useAjaxAtDefault == true) {
        exploreLink = new AjaxFallbackLink<Object>("explore") {
          @Override
          public void onClick(final AjaxRequestTarget target)
          {
            if (target == null || treeTable == null) {
              // Link with right mouse button and select new browser window / tab?
              return;
            }
            treeTable.setOpenedStatusOfNode(TreeTableEvent.EXPLORE, treeNode.getHashId());
            if (treeNode.isFolder() == true) {
              // Implore
              page.setEvent(target, TreeTableEvent.IMPLORE, treeNode);
            } else {
              // Explore
              page.setEvent(target, TreeTableEvent.EXPLORE, treeNode);
            }
          };
        };
      } else if (useSubmitLinkImages == true) {
        exploreLink = new SubmitLink("explore") {
          @Override
          public void onSubmit()
          {
            treeTable.setOpenedStatusOfNode(TreeTableEvent.EXPLORE, treeNode.getHashId());
            page.setEventNode(treeNode.getHashId());
          }
        };
      } else {
        exploreLink = new Link<Object>("explore") {
          @Override
          public void onClick()
          {
            treeTable.setOpenedStatusOfNode(TreeTableEvent.EXPLORE, treeNode.getHashId());
            page.setEventNode(treeNode.getHashId());
          };
        };
      }
      exploreLink.setVisible(showExploreIcon);
      iconSpan.add(exploreLink);
      // Get string results in warnings (panel was added before)?
      final ContextImage exploreImage = new PresizedImage("explore", WebConstants.IMAGE_TREE_ICON_EXPLOSION);
      exploreLink.add(exploreImage);
    }
    {
      final WebMarkupContainer folderLink;
      if (useAjaxAtDefault == true) {
        folderLink = new AjaxFallbackLink<TreeTableNode>("folder", new Model<TreeTableNode>(treeNode)) {
          @Override
          public void onClick(final AjaxRequestTarget target)
          {
            if (target == null || treeTable == null) {
              // Link with right mouse button and select new browser window / tab?
              return;
            }
            if (getModelObject().isOpened() == true) {
              treeTable.setOpenedStatusOfNode(TreeTableEvent.CLOSE, treeNode.getHashId());
              page.setEvent(target, TreeTableEvent.CLOSE, treeNode);
            } else {
              treeTable.setOpenedStatusOfNode(TreeTableEvent.OPEN, treeNode.getHashId());
              page.setEvent(target, TreeTableEvent.OPEN, treeNode);
            }
          };
        };
      } else if (useSubmitLinkImages == true) {
        folderLink = new SubmitLink("folder", new Model<TreeTableNode>(treeNode)) {
          @Override
          public void onSubmit()
          {
            if (((TreeTableNode) getDefaultModelObject()).isOpened() == true) {
              treeTable.setOpenedStatusOfNode(TreeTableEvent.CLOSE, treeNode.getHashId());
              page.setEventNode(treeNode.getHashId());
            } else {
              treeTable.setOpenedStatusOfNode(TreeTableEvent.OPEN, treeNode.getHashId());
              page.setEventNode(treeNode.getHashId());
            }
          }
        };
      } else {
        folderLink = new Link<TreeTableNode>("folder", new Model<TreeTableNode>(treeNode)) {
          @Override
          public void onClick()
          {
            if (getModelObject().isOpened() == true) {
              treeTable.setOpenedStatusOfNode(TreeTableEvent.CLOSE, treeNode.getHashId());
              page.setEventNode(treeNode.getHashId());
            } else {
              treeTable.setOpenedStatusOfNode(TreeTableEvent.OPEN, treeNode.getHashId());
              page.setEventNode(treeNode.getHashId());
            }
          };
        };
      }
      folderLink.setVisible(treeNode.hasChildren() == true);
      iconSpan.add(folderLink);
      folderImage = new PresizedImage("folderImage", WebConstants.IMAGE_TREE_ICON_FOLDER);
      folderImage.setOutputMarkupId(true);
      folderOpenImage = new PresizedImage("folderOpenImage", WebConstants.IMAGE_TREE_ICON_FOLDER_OPEN);
      folderOpenImage.setOutputMarkupId(true);
      folderLink.add(folderImage).add(folderOpenImage);
    }
    final Label clickedEntryLabel = new Label("clickedEntry", "<a name=\"clickedEntry\" />");
    clickedEntryLabel.setEscapeModelStrings(false);
    final Serializable eventNode = page.getEventNode();
    clickedEntryLabel.setVisible(eventNode != null && eventNode.equals(treeNode.getHashId()) == true);
    add(clickedEntryLabel);
  }

  public static ContextImage getCurrentFolderImage(final Response response, final AbstractLink folderLink, final TreeTableNode node)
  {
    final ContextImage folderImage = (ContextImage) folderLink.get("folderImage");
    final ContextImage folderOpenImage = (ContextImage) folderLink.get("folderOpenImage");
    final boolean isOpen = node.isOpened();
    folderImage.setVisible(!isOpen);
    folderOpenImage.setVisible(isOpen);
    if (isOpen == true) {
      return folderOpenImage;
    } else {
      return folderImage;
    }
  }

  @Override
  protected void onBeforeRender()
  {
    final boolean isOpen = treeNode.isOpened();
    folderImage.setVisible(!isOpen);
    folderOpenImage.setVisible(isOpen);
    super.onBeforeRender();
  }
}
