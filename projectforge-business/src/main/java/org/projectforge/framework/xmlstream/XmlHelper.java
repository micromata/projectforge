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

package org.projectforge.framework.xmlstream;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.projectforge.framework.i18n.InternalErrorException;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

public class XmlHelper
{
  public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XmlHelper.class);

  /**
   * Replace all single quotes by quotation marks. This is use-ful for writing xml string constants e. g. in test cases.
   * &lt;node name='name' locale='en'/&gt; is transformed to &lt;node name="name" locale="en"/&gt;
   * @param xml
   * @return
   */
  public static String replaceQuotes(final String xml)
  {
    final String text = xml.replace('\'', '"');
    return text;
  }

  public static Element fromString(final String str)
  {
    if (StringUtils.isBlank(str)) {
      return null;
    }
    try {
      final Document document = DocumentHelper.parseText(str);
      return document.getRootElement();
    } catch (final DocumentException ex) {
      log.error("Exception encountered " + ex.getMessage());
    }
    return null;
  }

  public static String toString(final Element el)
  {
    return toString(el, false);
  }

  public static String toString(final Element el, final boolean prettyFormat)
  {
    if (el == null) {
      return "";
    }
    final StringWriter out = new StringWriter();
    final OutputFormat format = new OutputFormat();
    if (prettyFormat) {
      format.setNewlines(true);
      format.setIndentSize(2);
    }
    final XMLWriter writer = new XMLWriter(out, format);
    String result = null;
    try {
      writer.write(el);
      result = out.toString();
      writer.close();
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
    }
    return result;
  }

  public static String toString(final org.w3c.dom.Document document, final boolean prettyFormat)
  {
    final TransformerFactory tranFactory = TransformerFactory.newInstance();
    final Transformer transformer;
    try {
      transformer = tranFactory.newTransformer();
    } catch (final TransformerConfigurationException ex) {
      log.error("Exception encountered while transcoding org.w3c.dom.Document to a string: " + ex.getMessage(), ex);
      throw new InternalErrorException("Exception encountered while transcoding org.w3c.dom.Document to a string: " + ex.getMessage());
    }
    if (prettyFormat) {
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }
    final Source src = new DOMSource(document);
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final Result dest = new StreamResult(bout);
    try {
      transformer.transform(src, dest);
    } catch (final TransformerException ex) {
      log.error("Exception encountered while transcoding org.w3c.dom.Document to a string: " + ex.getMessage(), ex);
      throw new InternalErrorException("Exception encountered while transcoding org.w3c.dom.Document to a string: " + ex.getMessage());
    }
    final String result;
    try {
      result = new String(bout.toByteArray(), "UTF-8");
    } catch (final UnsupportedEncodingException ex) {
      log.error("Exception encountered while transcoding org.w3c.dom.Document to a string: " + ex.getMessage(), ex);
      throw new InternalErrorException("Exception encountered while transcoding org.w3c.dom.Document to a string: " + ex.getMessage());
    }
    return result;
  }
}
