/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package de.micromata.wicket.request.mapper;

import org.apache.wicket.Application;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.info.PageInfo;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * This mapper represents exactly the same behavior like the default {@link MountedMapper}, except one behavior:<br/>
 * The default {@link MountedMapper} implementation prefers the pageId parameter with a higher priority then the rest of the
 * {@link PageParameters}.<br>
 * This preference leads to the following behavior:
 * 
 * <pre>
 * ...myApplication/User/4?15
 * </pre>
 * 
 * leads to the UserPage with the indexed {@link PageParameters} "4" at index 0. So the User with id 4 is shown. When a user manually
 * changes the URL to something like this:
 * 
 * <pre>
 * ...myApplication/User/5?15
 * </pre>
 * 
 * The {@link MountedMapper} would deliver the same page as before and would show the user with id 4 and not - like it is implicated in the
 * url - the user with id 5, because the pageId parameter is preferred during mapping process. <br/>
 * <br/>
 * Therefore this implementation compares the existing {@link PageParameters} of the cached page and the {@link PageParameters} of the
 * current request. If a difference between this two {@link PageParameters} are recognized, this mapper redirects to a fresh bookmarkable
 * instance of the current requested page. <br/>
 * <br/>
 * To use this mapper to mount your pages, you must declare the mapper programmatically in your {@link Application} by overwriting the
 * "public void init()" method, like shown below:
 * 
 * <pre>
 * &#064;Override
 * public void init()
 * {
 *   super.init();
 *   // Use our own mapper to mount the mountpage
 *   mount(new PageParameterAwareMountedMapper(&quot;Home&quot;, HomePage.class));
 * }
 * </pre>
 * 
 * @author <a href="mailto:j.unterstein@micromata.de">Johannes Unterstein</a>
 * 
 */
public class PageParameterAwareMountedMapper extends MountedMapper
{

  public PageParameterAwareMountedMapper(String mountPath, Class< ? extends IRequestablePage> pageClass)
  {
    super(mountPath, pageClass);
  }

  @Override
  protected IRequestHandler processHybrid(PageInfo pageInfo, Class< ? extends IRequestablePage> pageClass, PageParameters pageParameters,
      Integer renderCount)
  {
    IRequestHandler handler = null;
    try {
      handler = super.processHybrid(pageInfo, pageClass, pageParameters, renderCount);
    } catch (PageExpiredException e) {
      // in case of pageExpiredException at this point, we just redirect to previous bookmarkable resource
      return processBookmarkable(pageClass, pageParameters);
    }
    if (handler != null && handler instanceof RenderPageRequestHandler) {
      // in the current implementation (wicket 1.5.6) super.processHybrid
      // returns a RenderPageRequestHandler
      RenderPageRequestHandler renderPageHandler = (RenderPageRequestHandler) handler;
      if (renderPageHandler.getPageProvider() instanceof PageProvider) {
        PageProvider provider = (PageProvider) renderPageHandler.getPageProvider();
        // This check is necessary to prevent a RestartResponseAtInterceptPageException at the wrong time in request cycle
        if (provider.hasPageInstance()) {
          // get page classes
          Class< ? extends IRequestablePage> oldPageClass = renderPageHandler.getPageClass();
          Class< ? extends IRequestablePage> newPageClass = renderPageHandler.getPageProvider().getPageClass();

          // get page parameters
          PageParameters newPageParameters = renderPageHandler.getPageParameters();
          PageParameters oldPageParameters = renderPageHandler.getPageProvider().getPageInstance().getPageParameters();

          if (oldPageClass != null && oldPageClass.equals(newPageClass) == false) {
            return processBookmarkable(newPageClass, newPageParameters);
          }

          // if we recognize a change between the page parameter of the loaded
          // page and the page parameter of the current request, we redirect
          // to a fresh bookmarkable instance of that page.
          if (!PageParameters.equals(oldPageParameters, newPageParameters)) {
            return processBookmarkable(newPageClass, newPageParameters);
          }
        }
      }
    }
    return handler;
  }
}
