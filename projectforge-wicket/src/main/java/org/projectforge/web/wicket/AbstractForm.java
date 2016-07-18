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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.upload.FileUploadException;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.bootstrap.GridBuilder;

public abstract class AbstractForm<F, P extends AbstractUnsecureBasePage> extends Form<F>
{
  private static final long serialVersionUID = -5703197102062729288L;

  private static final Logger LOG = Logger.getLogger(AbstractForm.class);

  protected final P parentPage;

  private final ShinyFormVisitor shinyVisitor = new ShinyFormVisitor();

  private transient TenantRegistry tenantRegistry;

  /**
   * Convenience method for creating a component which is in the mark-up file but should not be visible.
   * 
   * @param wicketId
   * @return
   * @see AbstractUnsecureBasePage#createInvisibleDummyComponent(String)
   */
  public static Label createInvisibleDummyComponent(final String wicketId)
  {
    return AbstractUnsecureBasePage.createInvisibleDummyComponent(wicketId);
  }

  public AbstractForm(final P parentPage)
  {
    this(parentPage, "form");
  }

  public AbstractForm(final P parentPage, final String id)
  {
    super(id);
    this.parentPage = parentPage;
  }

  protected void initUpload(final Bytes maxSize)
  {
    // set this form to multipart mode (always needed for uploads!)
    setMultiPart(true);
    setMaxSize(maxSize);
  }

  protected FeedbackPanel createFeedbackPanel()
  {
    final FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
    feedbackPanel.setOutputMarkupId(true);
    return feedbackPanel;
  }

  public P getParentPage()
  {
    return parentPage;
  }

  /**
   * Is called by parent page directly after creating this form (constructor call).
   */
  protected void init()
  {
  }

  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    visitChildren(shinyVisitor);
  }

  public void addError(final String msgKey)
  {
    error(getString(msgKey));
  }

  public void addError(final String msgKey, final Object... params)
  {
    error(MessageFormat.format(getString(msgKey), params));
  }

  public void addFieldRequiredError(final String fieldKey)
  {
    error(MessageFormat.format(getString("validation.error.fieldRequired"), getString(fieldKey)));
  }

  public void addComponentError(final Component component, final String msgKey)
  {
    component.error(getString(msgKey));
  }

  public String getLocalizedMessage(final String key, final Object... params)
  {
    if (params == null) {
      return getString(key);
    }
    return MessageFormat.format(getString(key), params);
  }

  /**
   * @see AbstractUnsecureBasePage#escapeHtml(String)
   */
  protected String escapeHtml(final String str)
  {
    return parentPage.escapeHtml(str);
  }

  /**
   * @see AbstractSecuredBasePage#getUser()
   */
  protected PFUserDO getUser()
  {
    return this.parentPage.getUser();
  }

  /**
   * @see AbstractSecuredBasePage#getUserId()
   */
  protected Integer getUserId()
  {
    return this.parentPage.getUserId();
  }

  public MySession getMySession()
  {
    return (MySession) getSession();
  }

  /**
   * @see GridBuilder#GridBuilder(RepeatingView, MySession)
   */
  public GridBuilder newGridBuilder(final WebMarkupContainer parent, final String id)
  {
    return new GridBuilder(parent, id);
  }

  /**
   * For getting caches etc.
   * 
   * @return The current tenantRegistry also for systems without tenants configured.
   */
  protected TenantRegistry getTenantRegistry()
  {
    if (tenantRegistry == null) {
      tenantRegistry = TenantRegistryMap.getInstance().getTenantRegistry();
    }
    return tenantRegistry;
  }

  @Override
  protected boolean handleMultiPart()
  {
    ServletWebRequest request = (ServletWebRequest) getRequest();
    HttpServletRequest contreq = request.getContainerRequest();

    if (isMultiPart()) {

      // Change the request to a multipart web request so parameters are
      // parsed out correctly
      try {

        // Patched for servlet 3.0 
        Servlet3MultipartServletWebRequest multipartWebRequest = new Servlet3MultipartServletWebRequest(contreq,
            request.getFilterPrefix(), getMaxSize(), getPage().getId());
        //multipartWebRequest.setFileMaxSize(getFileMaxSize());
        multipartWebRequest.parseFileParts();

        // TODO: Can't this be detected from header?
        getRequestCycle().setRequest(multipartWebRequest);
      } catch (final FileUploadException fux) {
        // Create model with exception and maximum size values
        final Map<String, Object> model = new HashMap<String, Object>();
        model.put("exception", fux);
        model.put("maxSize", getMaxSize());
        //model.put("fileMaxSize", getFileMaxSize());

        onFileUploadException(fux, model);

        // don't process the form if there is a FileUploadException
        return false;
      }
    }
    return true;
  }
}
