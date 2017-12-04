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

package org.projectforge.framework.renderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.projectforge.AppVersion;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.scripting.GroovyEngine;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class provides the functionality for rendering pdf files. The underlaying technology is XSL-FO. The dynamic data
 * will be given in xml format and the transformation will be done via xslt-scripts. For a better ease of use a meta
 * language similiar to html will be used instead of plain xsl-fo. The html file with jelly script elements will be
 * rendered via xslt-scripts into xsl-fo and afterwards to pdf.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class PdfRenderer
{
  private static final Logger log = LoggerFactory.getLogger(PdfRenderer.class);

  public final static String DEFAULT_FO_STYLE = "default-style-fo.xsl";

  @Autowired
  private ConfigurationService configurationService;

  @Value("${projectforge.export.logoFile}")
  private String logoFileName;

  private String fontResourcePath;

  private String getFontResourcePath()
  {
    if (fontResourcePath == null) {
      final File dir = new File(configurationService.getFontsDir());
      if (dir.exists() == false) {
        log.error("Application's font dir does not exist: " + dir.getAbsolutePath());
      }
      this.fontResourcePath = dir.getAbsolutePath();
    }
    return fontResourcePath;
  }

  public byte[] render(final String stylesheet, final String groovyXml, final Map<String, Object> data)
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    data.put("createdLabel", ThreadLocalUserContext.getLocalizedString("created"));
    data.put("loggedInUser", user);
    data.put("baseDir", configurationService.getResourceDir());
    data.put("logoFile", configurationService.getResourceDir() + "/images/" + logoFileName);
    data.put("appId", AppVersion.APP_ID);
    data.put("appVersion", AppVersion.NUMBER);
    data.put("organization",
        StringUtils.defaultString(Configuration.getInstance().getStringValue(ConfigurationParam.ORGANIZATION),
            AppVersion.APP_ID));
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    log.info("stylesheet="
        + stylesheet
        + ", jellyXml="
        + groovyXml
        + ", baseDir="
        + configurationService.getResourceDir()
        + ", fontBaseDir="
        + getFontResourcePath());

    // configure fopFactory as desired
    final FopFactory fopFactory = FopFactory.newInstance();

    try {
      fopFactory.getFontManager().setFontBaseURL(getFontResourcePath());
    } catch (final MalformedURLException ex) {
      log.error(ex.getMessage(), ex);
    }

    final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
    try {
      foUserAgent.getFactory().getFontManager().setFontBaseURL(getFontResourcePath());
    } catch (final MalformedURLException ex) {
      log.error(ex.getMessage(), ex);
    }
    // configure foUserAgent as desired

    InputStream xsltInputStream = null;
    try {
      // Construct fop with desired output format
      final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, baos);

      // Setup XSLT
      final TransformerFactory factory = TransformerFactory.newInstance();
      Object[] result = configurationService.getResourceAsInputStream(stylesheet);
      xsltInputStream = (InputStream) result[0];
      final StreamSource xltStreamSource = new StreamSource(xsltInputStream);
      final String url = (String) result[1];
      if (url == null) {
        log.error("Url of xsl resource is null.");
        throw new InternalErrorException();
      }
      xltStreamSource.setSystemId(url);

      final Transformer transformer = factory.newTransformer(xltStreamSource);

      // Set the value of a <param> in the stylesheet
      for (final Map.Entry<String, Object> entry : data.entrySet()) {
        transformer.setParameter(entry.getKey(), entry.getValue());
      }

      // First run jelly through xmlData:
      result = configurationService.getResourceContentAsString(groovyXml);
      final GroovyEngine groovyEngine = new GroovyEngine(configurationService, data, ThreadLocalUserContext.getLocale(),
          ThreadLocalUserContext.getTimeZone());
      final String groovyXmlInput = groovyEngine.preprocessGroovyXml((String) result[0]);
      final String xmlData = groovyEngine.executeTemplate(groovyXmlInput);

      // Setup input for XSLT transformation
      final StringReader xmlDataReader = new StringReader(xmlData);
      final Source src = new StreamSource(xmlDataReader);

      // Resulting SAX events (the generated FO) must be piped through to FOP
      final Result res = new SAXResult(fop.getDefaultHandler());

      // Start XSLT transformation and FOP processing
      transformer.transform(src, res);
    } catch (final FOPException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } catch (final TransformerConfigurationException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } catch (final TransformerException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } finally {
      try {
        baos.close();
      } catch (final IOException ex) {
        log.error(ex.getMessage(), ex);
        throw new RuntimeException(ex);
      }
      IOUtils.closeQuietly(xsltInputStream);
    }
    return baos.toByteArray();
  }
}
