package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.framework.utils.ReflectionHelper;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;

/**
 * Created by vit on 20.12.16.
 */
public class LinkPanel extends Panel
{

  private final Link link;

  public LinkPanel(final String id, final String linkName, final Class<? extends WebPage> editClass,
      final WebPage returnToPage)
  {
    this(id, linkName, editClass, returnToPage, new PageParameters());
  }

  public LinkPanel(final String id, final String linkName, final Class<? extends WebPage> editClass,
      final WebPage returnToPage, final PageParameters pageParameters)
  {
    super(id);

    link = new Link<String>("link")
    {
      @Override
      public void onClick()
      {
        final AbstractSecuredPage editPage = (AbstractSecuredPage) ReflectionHelper.newInstance(editClass, PageParameters.class,
            pageParameters);
        if (editPage instanceof AbstractEditPage) {
          ((AbstractEditPage<?, ?, ?>) editPage).setReturnToPage(returnToPage);
        }
        setResponsePage(editPage);
      }
    };
    add(link);

    link.add(new Label("label", linkName));
  }

  public void addLinkAttribute(String attribute, String value)
  {
    link.add(new AttributeAppender(attribute, value));
  }
}
