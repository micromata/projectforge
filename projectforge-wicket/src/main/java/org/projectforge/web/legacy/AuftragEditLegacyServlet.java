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

package org.projectforge.web.legacy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * For old e-mail links sent after modifications of orders (before 3.3.0-2009-09-20 release).
 */
@WebServlet("/secure/fibu/AuftragEdit.action")
public class AuftragEditLegacyServlet extends HttpServlet
{
  private static final long serialVersionUID = 7778310216427808799L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AuftragEditLegacyServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    String uri = req.getRequestURI().replace("/secure/fibu/AuftragEdit.action", "/wa/editAuftrag/id/");
    String id = req.getParameter("id");
    String redirectUrl = uri + id + ";jsessionid=" + req.getSession().getId();
    log.info("Legacy servlet used, redirecting to new wicket page AuftragEditPage: " + redirectUrl);
    resp.sendRedirect(redirectUrl);
  }
}
