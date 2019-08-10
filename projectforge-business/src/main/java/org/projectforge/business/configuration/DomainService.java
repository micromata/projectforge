/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.configuration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DomainService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DomainService.class);

  @Value("${projectforge.domain}")
  private String domain;
  private String domainWithContextPath;
  @Value("${projectforge.servletContextPath}")
  private String contextPath;

  private String protocol;

  /**
   * @return domain (host) in form https://www.acme.de:8443/
   */
  public String getDomain() {
    return domain;
  }

  /**
   * @return The domain + context path, e.g. https://www.projectforge.org/demo or https://www.acme.com/ProjectForge.
   */
  public String getDomainWithContextPath() {
    if (domainWithContextPath == null) {
      domainWithContextPath = domain;
      if (StringUtils.isNotBlank(contextPath)) {
        domainWithContextPath = domain.endsWith("/") ? domain + contextPath : domain + "/" + contextPath;
      }
    }
    return domainWithContextPath;
  }

  /**
   * The servlet's context path, "/ProjectForge" at default. You should configure another context path such as "/" if
   * the ProjectForge app runs in another context, such as root context.
   */
  public String getContextPath() {
    return StringUtils.isBlank(contextPath) ? "" : contextPath;
  }

  public String getProtocol() {
    if (protocol == null && domain != null) {
      protocol = extractProtocol(domain);
    }
    return protocol;
  }

  /**
   * Should only be used by setup wizard.
   *
   * @param baseUrlString
   */
  public static DomainService internalCreate(String baseUrlString) {
    DomainService ds = new DomainService();
    int protocolEnd = baseUrlString.indexOf("://");
    int domainEnd = baseUrlString.indexOf('/', protocolEnd + 3);
    if (domainEnd >= 0) {
      ds.domain = baseUrlString.substring(0, domainEnd);
      if (domainEnd < baseUrlString.length() + 1) {
        ds.contextPath = baseUrlString.substring(domainEnd + 1);
        if (ds.contextPath.endsWith("/")) {
          // Remove trailing end from context.
          ds.contextPath = ds.contextPath.substring(0, ds.contextPath.length() - 1);
        }
      }
    } else {
      ds.domain = baseUrlString;
      ds.contextPath = "";
    }
    ds.protocol = extractProtocol(baseUrlString);
    return ds;
  }

  private static String extractProtocol(String url) {
    int index = url.indexOf("://");
    if (index < 0) {
      log.error("Oups, can't extract protocol (such as http or https) from url ('://' missed): " + url);
      return "http";
    }
    return url.substring(0, index);
  }
}
