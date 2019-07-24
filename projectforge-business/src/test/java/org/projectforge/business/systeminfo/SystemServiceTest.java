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

package org.projectforge.business.systeminfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.projectforge.business.jsonRest.RestCallService;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.model.rest.VersionCheck;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ThreadLocalUserContext.class, ConfigXml.class})
//Needed for: java.lang.ClassCastException: com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl cannot be cast to javax.xml.parsers.SAXParserFactory
@PowerMockIgnore({"javax.management.*", "javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*", "org.slf4j.*"})
public class SystemServiceTest {
  @InjectMocks
  SystemService systemService = new SystemService();

  @Mock
  private RestCallService restCallService;

  Locale locale = Locale.getDefault();

  TimeZone timeZone = TimeZone.getDefault();

  private VersionCheck targetVersionCheck = new VersionCheck();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockStatic(ThreadLocalUserContext.class);
    ConfigXml.createForJunitTests();
    PowerMockito.when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    PowerMockito.when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
    systemService.setEnableVersionCheck(true);
  }

  @Test
  public void isNewPFVersionAvailableNullTest() {
    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck());
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck(null, locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);
  }

  @Test
  public void isNewPFVersionAvailableTest() {
    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("1", null, locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("0", "1", locale, timeZone));
    assertTrue(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("1", "1.1", locale, timeZone));
    assertTrue(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("1", "1.0.0", locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("1.9", "1.1.1", locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("1.9", "1.10.1", locale, timeZone));
    assertTrue(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("6.18.0", "6.17.3", locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("6.18.0", "6.18.0.1", locale, timeZone));
    assertTrue(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("6.18.0-SNAPSHOT", "6.17.3", locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("6.18-SNAPSHOT", "6.17.3", locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("6.18.0-SNAPSHOT", "6.19.1", locale, timeZone));
    assertTrue(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck("6.18.0", "6.19.1-SNAPSHOT", locale, timeZone));
    assertTrue(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any()))
            .thenReturn(new VersionCheck("6.18.0-SNAPSHOT", "6.19.1-SNAPSHOT", locale, timeZone));
    assertTrue(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);
  }

}
