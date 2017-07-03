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

package org.projectforge.web.doc;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.WicketUtils;

public class DocumentationPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 1680968273313948593L;

  public DocumentationPage(final PageParameters parameters)
  {
    super(parameters);
    //    final Locale locale = ThreadLocalUserContext.getLocale();
    //    final boolean isGerman = locale != null && locale.toString().startsWith("de") == true;
    boolean isGerman = false;
    String docroot = "http://www.projectforge.org/";

    addDocLink(body, "newsLink", docroot + "projectforge-news.html");
    addDocLink(body, "tutorialLink", docroot + "documentation/quickstart.html");
    addDocLink(body, "handbookLink", docroot + "documentation/user-guide.html");
    addDocLink(body, "faqLink", docroot + "projectforge-faq.html");
    addDocLink(body, "licenseLink", docroot + "application/terms-of-use.html");

    addDocLink(body, "adminGuideLink", docroot + "documentation/administration-guide.html");
    addDocLink(body, "developerGuideLink", docroot + "documentation/developer-guide.html");
    //addDocLink(body, "projectDocLink", "site/index.html");
    //addDocLink(body, "javaDocLink", "site/apidocs/index.html");
    //addDocLink(body, "adminLogbuchLink", docroot + "pf-common/AdminLogbuch");
  }

  /**
   * Adds BookmarkablePageLink with given id to the given parentContainer.
   *
   * @param id              id of the link (shouldn't bee "newsLink" in body, because it's already used by DocumentationPage).
   * @param parentContainer Page (normally body)
   */
  public static final AbstractLink addNewsLink(final WebMarkupContainer parentContainer, final String id)
  {
    final AbstractLink link = new ExternalLink(id,
        WicketUtils.getUrl(parentContainer.getRequestCycle(), "secure/doc/News.html", true));
    parentContainer.add(link);
    return link;
  }

  private static void addDocLink(final WebMarkupContainer parentContainer, final String id, final String url)
  {
    final WebMarkupContainer linkContainer = new WebMarkupContainer(id);
    String surl = url;
    if (surl.startsWith("http") == false) {
      surl = WicketUtils.getUrl(parentContainer.getRequestCycle(), "secure/" + url, true);
    }
    linkContainer.add(AttributeModifier.replace("onclick",
        "javascript:openDoc('" + surl + "');"));
    linkContainer.add(AttributeModifier.replace("onmouseover", "style.cursor='pointer'"));
    parentContainer.add(linkContainer);
  }

  @Override
  protected String getTitle()
  {
    return getString("doc.title");
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#isBreadCrumbVisible()
   */
  @Override
  protected boolean isBreadCrumbVisible()
  {
    return false;
  }
}
