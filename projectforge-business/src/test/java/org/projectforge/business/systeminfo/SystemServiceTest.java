package org.projectforge.business.systeminfo;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Locale;
import java.util.TimeZone;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.projectforge.business.jsonRest.RestCallService;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.model.rest.VersionCheck;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@PrepareForTest({ ThreadLocalUserContext.class, ConfigXml.class })
//Needed for: java.lang.ClassCastException: com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl cannot be cast to javax.xml.parsers.SAXParserFactory
@PowerMockIgnore({ "javax.management.*", "javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*", "org.slf4j.*" })
public class SystemServiceTest extends PowerMockTestCase
{
  @InjectMocks
  SystemService systemService = new SystemService(null, null, null, null, null, null, true, "");

  @Mock
  private RestCallService restCallService;

  Locale locale = Locale.getDefault();

  TimeZone timeZone = TimeZone.getDefault();

  private VersionCheck targetVersionCheck = new VersionCheck();

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    mockStatic(ThreadLocalUserContext.class);
    mockStatic(ConfigXml.class);
    ConfigXml configXml = new ConfigXml("./target/Projectforge");
    PowerMockito.when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    PowerMockito.when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
    PowerMockito.when(ConfigXml.getInstance()).thenReturn(configXml);
  }

  @Test
  public void isNewPFVersionAvailableNullTest()
  {
    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck());
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);

    when(restCallService.callRestInterfaceForUrl(any(), any(), any(), any())).thenReturn(new VersionCheck(null, locale, timeZone));
    assertFalse(systemService.isNewPFVersionAvailable());
    systemService.setLastVersionCheckDate(null);
  }

  @Test
  public void isNewPFVersionAvailableTest()
  {
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
