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

package org.projectforge.web.scripting;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.scripting.ScriptDao;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@EditPage(defaultReturnPage = ScriptListPage.class)
public class ScriptEditPage extends AbstractEditPage<ScriptDO, ScriptEditForm, ScriptDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 4156917767160708873L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ScriptEditPage.class);

  @SpringBean
  private ScriptDao scriptDao;

  @SuppressWarnings("serial")
  public ScriptEditPage(final PageParameters parameters)
  {
    super(parameters, "scripting");
    init();
    if (StringUtils.isNotEmpty(form.getData().getScriptBackupAsString()) == true) {
      // Show backup script button:
      final AjaxLink<Void> showBackupScriptButton = new AjaxLink<Void>(ContentMenuEntryPanel.LINK_ID) {
        @Override
        public void onClick(final AjaxRequestTarget target)
        {
          form.showBackupScriptDialog.open(target);
        }
      };
      final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), showBackupScriptButton,
          getString("scripting.scriptBackup.show"));
      addContentMenuEntry(menu);
    }
  }

  @Override
  protected ScriptDao getBaseDao()
  {
    return scriptDao;
  }

  @Override
  protected ScriptEditForm newEditForm(final AbstractEditPage< ? , ? , ? > parentPage, final ScriptDO data)
  {
    return new ScriptEditForm(this, data);
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
