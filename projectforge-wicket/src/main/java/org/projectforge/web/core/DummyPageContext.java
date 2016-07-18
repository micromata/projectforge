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

package org.projectforge.web.core;

import java.io.IOException;
import java.util.Enumeration;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

/**
 * Stripes actions beans uses the tupel request, response and DisplayTag / ProjectForge uses PageContext. This dummy
 * PageContext is usefull for the Stripes action beans to use the PageContext based classes.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DummyPageContext extends PageContext
{
  private HttpServletRequest request;

  private HttpServletResponse response;

  public DummyPageContext(HttpServletRequest request, HttpServletResponse response)
  {
    this.request = request;
    this.response = response;
  }

  @Override
  public ServletRequest getRequest()
  {
    return request;
  }

  @Override
  public ServletResponse getResponse()
  {
    return response;
  }

  @Override
  public void forward(String arg0) throws ServletException, IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Exception getException()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getPage()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletConfig getServletConfig()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServletContext getServletContext()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpSession getSession()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void handlePageException(Exception arg0) throws ServletException, IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void handlePageException(Throwable arg0) throws ServletException, IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void include(String arg0) throws ServletException, IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void include(String arg0, boolean arg1) throws ServletException, IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void initialize(Servlet arg0, ServletRequest arg1, ServletResponse arg2, String arg3, boolean arg4, int arg5,
      boolean arg6)
          throws IOException, IllegalStateException, IllegalArgumentException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void release()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object findAttribute(String arg0)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getAttribute(String arg0)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getAttribute(String arg0, int arg1)
  {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getAttributeNamesInScope(int arg0)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getAttributesScope(String arg0)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExpressionEvaluator getExpressionEvaluator()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public JspWriter getOut()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public VariableResolver getVariableResolver()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAttribute(String arg0)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAttribute(String arg0, int arg1)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAttribute(String arg0, Object arg1)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAttribute(String arg0, Object arg1, int arg2)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ELContext getELContext()
  {
    throw new UnsupportedOperationException();
  }
}
