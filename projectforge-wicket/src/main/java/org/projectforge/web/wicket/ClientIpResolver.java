/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.projectforge.web.WebUtils;

import jakarta.servlet.ServletRequest;

public class ClientIpResolver extends WebClientInfo
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClientIpResolver.class);

  private static final long serialVersionUID = -1001665111323720357L;

  public static String getClientIp()
  {
    final RequestCycle requestCycle = RequestCycle.get();
    if (requestCycle == null) {
      log.warn("Oups, requestCycle of Wicket is null (should only occur for test cases).");
      return "";
    }
    return new ClientIpResolver(requestCycle).getRemoteAddr(requestCycle);
  }

  /**
   * @see WebUtils#getClientIp(ServletRequest) X-Forwarded-For is only used if the request came from a trusted
   * proxy, see projectforge.security.trustedProxies.
   */
  public static String getClientIp(final ServletRequest request)
  {
    return WebUtils.getClientIp(request);
  }

  /**
   * @param requestCycle
   */
  public ClientIpResolver(final RequestCycle requestCycle)
  {
    super(requestCycle);
  }

}
