/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractReindexTopRightMenu;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * @author Billy Duong (b.duong@micromata.de)
 * 
 */
public class SkillTreePage extends AbstractSecuredPage
{

  private static final long serialVersionUID = -3902220283833390881L;

  public static final String USER_PREFS_KEY_OPEN_SKILLS = "openSkills";

  public static final String I18N_KEY_SKILLTREE_TITLE = "plugins.skillmatrix.title.list";

  public static final String I18N_KEY_SKILLTREE_INFO = "plugins.skillmatrix.skilltree.info";

  private SkillTreeForm form;

  private ISelectCallerPage caller;

  private SkillTreeBuilder skillTreeBuilder;

  @SpringBean
  private SkillDao skillDao;

  private String selectProperty;

  private SkillListPage skillListPage;

  /**
   * @param parameters
   */
  public SkillTreePage(final PageParameters parameters)
  {
    super(parameters);
    init();
  }

  /**
   * Called if the user clicks on button "tree view".
   * 
   * @param skillListPage
   * @param parameters
   */
  public SkillTreePage(final SkillListPage skillListPage, final PageParameters parameters)
  {
    super(parameters);
    this.skillListPage = skillListPage;
    init();
  }

  public SkillTreePage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(new PageParameters());
    this.caller = caller;
    this.selectProperty = selectProperty;
    init();
  }

  @SuppressWarnings("serial")
  private void init()
  {
    if (!isSelectMode()) {
      final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Object>("link")
          {
            @Override
            public void onClick()
            {
              final PageParameters params = new PageParameters();
              final AbstractEditPage<?, ?, ?> editPage = new SkillEditPage(params);
              editPage.setReturnToPage(SkillTreePage.this);
              setResponsePage(editPage);
            };
          }, IconType.PLUS);
      addContentMenuEntry(menuEntry);

      new AbstractReindexTopRightMenu(contentMenuBarPanel, accessChecker.isLoggedInUserMemberOfAdminGroup())
      {
        @Override
        protected void rebuildDatabaseIndex(final boolean onlyNewest)
        {
          if (onlyNewest) {
            skillDao.rebuildDatabaseIndex4NewestEntries();
          } else {
            skillDao.rebuildDatabaseIndex();
          }
        }

        @Override
        protected String getString(final String i18nKey)
        {
          return SkillTreePage.this.getString(i18nKey);
        }
      };
    }
    form = new SkillTreeForm(this);
    body.add(form);
    form.init();
    skillTreeBuilder = new SkillTreeBuilder(skillDao).setCaller(caller)
        .setSelectProperty(selectProperty)
        .setSelectMode(isSelectMode()).setShowRootNode(accessChecker.isLoggedInUserMemberOfAdminGroup());
    form.add(skillTreeBuilder.createTree("tree", this, form.getSearchFilter()));

    body.add(new Label("info", new Model<>(getString(I18N_KEY_SKILLTREE_INFO))));
  }

  public void refresh()
  {
    form.getSearchFilter().resetMatch();
  }

  /**
   * @return true, if this page is called for selection by a caller otherwise false.
   */
  public boolean isSelectMode()
  {
    return this.caller != null;
  }

  protected void onSearchSubmit()
  {
    refresh();
  }

  protected void onResetSubmit()
  {
    form.getSearchFilter().reset();
    refresh();
    form.clearInput();
  }

  protected void onListViewSubmit()
  {
    if (skillListPage != null) {
      setResponsePage(skillListPage);
    } else {
      setResponsePage(new SkillListPage(this, getPageParameters()));
    }
  }

  protected void onCancelSubmit()
  {
    if (isSelectMode()) {
      WicketUtils.setResponsePage(this, caller);
      caller.cancelSelection(selectProperty);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString(I18N_KEY_SKILLTREE_TITLE);
  }

  public void setHighlightedRowId(final Integer highlightedRowId)
  {
    skillTreeBuilder.setHighlightedSkillNodeId(highlightedRowId);
  }

}
