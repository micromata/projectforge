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

package org.projectforge.web.wicket;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.image.ContextImage;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.xmlstream.AliasMap;
import org.projectforge.framework.xmlstream.XmlObjectReader;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.core.ImageDimension;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class PresizedImage extends ContextImage
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PresizedImage.class);

  private static final long serialVersionUID = -1456397971086283625L;

  private static final Map<String, Dimension> registry;

  private static final Set<String> notFound;

  static {
    log.info("Reading image dimensions from file " + WebConstants.FILE_IMAGE_DIMENSIONS + " ...");
    registry = new HashMap<String, Dimension>();
    notFound = new TreeSet<String>();
    final ClassLoader cLoader = Configuration.class.getClassLoader();
    final InputStream is = cLoader.getResourceAsStream(WebConstants.FILE_IMAGE_DIMENSIONS);
    final XmlObjectReader reader = new XmlObjectReader();
    final AliasMap aliasMap = new AliasMap();
    aliasMap.put(List.class, "images");
    reader.setAliasMap(aliasMap).initialize(ImageDimension.class);
    String xml = null;
    try {
      xml = IOUtils.toString(is);
    } catch (final IOException ex) {
      log.error("Couldn't read image dimensions file: " + ex.getMessage(), ex);
    }
    if (xml == null) {
      log.error("Couldn't read image dimensions file.");
    } else {
      @SuppressWarnings("unchecked")
      final List<ImageDimension> list = (List<ImageDimension>) reader.read(xml);
      for (final ImageDimension dimension : list) {
        registry.put(dimension.getPath(), dimension.getDimension());
      }
      log.info("Reading image dimensions from file was successful: " + WebConstants.FILE_IMAGE_DIMENSIONS);
    }
  }

  /**
   * @param id
   * @param request
   * @param relativePath path relative to web apps image dir.
   */
  public PresizedImage(final String id, final ImageDef image)
  {
    this(id, image.getPath());
  }

  /**
   * @param id
   * @param request
   * @param relativePath path relative to web apps image dir.
   */
  public PresizedImage(final String id, final String path)
  {
    super(id, path);
    final Dimension dimension = registry.get(path);
    if (dimension == null) {
      if (WebConfiguration.isDevelopmentMode() == true) {
        log.warn("Image " + path + " not found (please update image dimension file via ImageDimensionsCreator).");
      } else {
        if (notFound.contains(path) == false) {
          log.warn("Image " + path + " not found (please update image dimension file via ImageDimensionsCreator).");
          notFound.add(path);
        }
      }
    } else {
      add(AttributeModifier.replace("height", String.valueOf(dimension.height)));
      add(AttributeModifier.replace("width", String.valueOf(dimension.width)));
    }
  }
}
