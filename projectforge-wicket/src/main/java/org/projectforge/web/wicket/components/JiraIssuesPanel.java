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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.jira.JiraUtils;

/**
 * Panel containing only one check-box.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class JiraIssuesPanel extends Panel
{
  public JiraIssuesPanel(final String id, final String text)
  {
    this(id, new Model<String>(text));
  }

  public JiraIssuesPanel(final String id, final IModel<String> model)
  {
    super(id);
    setRenderBodyOnly(true);
    if (WicketUtils.isJIRAConfigured() == false) {
      final WebMarkupContainer dummy = new WebMarkupContainer("issues");
      setVisible(false);
      dummy.add(new ExternalLink("jiraLink", "dummy"));
      add(dummy);
      return;
    }
    final RepeatingView jiraIssuesRepeater = new RepeatingView("issues");
    add(jiraIssuesRepeater);
    final String[] jiraIssues = JiraUtils.checkForJiraIssues(model.getObject());
    if (jiraIssues == null) {
      jiraIssuesRepeater.setVisible(false);
    } else {
      for (final String issue : jiraIssues) {
        final WebMarkupContainer item = new WebMarkupContainer(jiraIssuesRepeater.newChildId());
        item.setRenderBodyOnly(true);
        jiraIssuesRepeater.add(item);
        item.add(new ExternalLink("jiraLink", JiraUtils.buildJiraIssueBrowseLinkUrl(issue), issue));
      }
    }
  }
}
