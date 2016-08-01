package org.projectforge.web.wicket.components;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.AbstractItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.web.wicket.bootstrap.GridBuilder;

public class TabPanel extends Panel
{
  private final RepeatingView tabs;

  private final RepeatingView tabPanes;

  public TabPanel(String id)
  {
    super(id);

    tabs = new RepeatingView("tabs");
    add(tabs);

    tabPanes = new RepeatingView("tabPanes");
    add(tabPanes);
  }

  public GridBuilder newTab(final String i18nKey, final boolean active)
  {
    final String markupId = "tab_" + i18nKey.replace('.', '_');
    
    final AbstractItem tab = new AbstractItem(tabs.newChildId());
    tab.add(new ExternalLink("tabLink", "#" + markupId, getString(i18nKey)));
    tabs.add(tab);

    final AbstractItem tabPane = new AbstractItem(tabPanes.newChildId());
    tabPane.add(AttributeAppender.replace("id", markupId));
    tabPanes.add(tabPane);

    if (active) {
      tab.add(AttributeAppender.append("class", "active"));
      tabPane.add(AttributeAppender.append("class", "active"));
    }

    return new GridBuilder(tabPane, "tabContainer");
  }
}
