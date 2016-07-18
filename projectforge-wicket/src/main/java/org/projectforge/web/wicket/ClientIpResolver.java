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

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.Strings;

public class ClientIpResolver extends WebClientInfo
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ClientIpResolver.class);

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

  public static String getClientIp(final ServletRequest request)
  {
    String remoteAddr = null;
    if (request instanceof HttpServletRequest) {
      remoteAddr = ((HttpServletRequest) request).getHeader("X-Forwarded-For");
    }
    if (remoteAddr != null) {
      if (remoteAddr.contains(",")) {
        // sometimes the header is of form client ip,proxy 1 ip,proxy 2 ip,...,proxy n ip,
        // we just want the client
        remoteAddr = Strings.split(remoteAddr, ',')[0].trim();
      }
      try {
        // If ip4/6 address string handed over, simply does pattern validation.
        InetAddress.getByName(remoteAddr);
      } catch (final UnknownHostException e) {
        remoteAddr = request.getRemoteAddr();
      }
    } else {
      remoteAddr = request.getRemoteAddr();
    }
    return remoteAddr;
  }

  /**
   * @param requestCycle
   */
  public ClientIpResolver(final RequestCycle requestCycle)
  {
    super(requestCycle);
  }

}
