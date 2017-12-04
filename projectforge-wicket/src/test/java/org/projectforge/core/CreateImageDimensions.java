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

package org.projectforge.core;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.projectforge.framework.xstream.AliasMap;
import org.projectforge.framework.xstream.XmlHelper;
import org.projectforge.framework.xstream.XmlObjectWriter;
import org.projectforge.web.core.ImageDimension;
import org.projectforge.web.wicket.WebConstants;

/**
 * Creates a dimenstion file for setting the html markup attributes width and size for images. It generates a dimension
 * file which is read by PresizedImage. Test case should be executed every time after modifying dimensions of web app
 * images or adding new images.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CreateImageDimensions
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateImageDimensions.class);

  private static final String PATH = "src/main/webapp/";

  private static final String[] SUB_DIRS = { "images", "mobile/jquery.mobile/images/" };

  private static final String DIMENSION_FILE = "src/main/resources/" + WebConstants.FILE_IMAGE_DIMENSIONS;

  private static final String[] IMAGE_SUFFIXES = new String[] { "png", "gif", "jpg" };

  public static void main(final String[] args) throws IOException
  {
    log.info("Create dimension file of all webapp images.");
    final List<ImageDimension> dimensions = new ArrayList<ImageDimension>();
    for (final String subDir : SUB_DIRS) {
      final String path = PATH + subDir;
      final Collection<File> files = FileUtils.listFiles(new File(path), IMAGE_SUFFIXES, true);
      final File absolutePathFile = new File(PATH);
      final String absolutePath = absolutePathFile.getAbsolutePath();
      for (final File file : files) {
        final Image image = Toolkit.getDefaultToolkit().getImage(file.getAbsolutePath());
        final ImageIcon icon = new ImageIcon(image);
        final String filename = file.getAbsolutePath().substring(absolutePath.length() + 1);
        final ImageDimension dimension = new ImageDimension(filename, icon.getIconWidth(), icon.getIconHeight());
        dimensions.add(dimension);
      }
    }
    final FileWriter writer = new FileWriter(DIMENSION_FILE);
    final XmlObjectWriter xmlWriter = new XmlObjectWriter();
    final AliasMap aliasMap = new AliasMap();
    aliasMap.put(ArrayList.class, "images");
    xmlWriter.setAliasMap(aliasMap);
    final String xml = xmlWriter.writeToXml(dimensions, true);
    writer.append(XmlHelper.XML_HEADER);
    writer.append(xml);
    IOUtils.closeQuietly(writer);
    log.info("Creation of dimension file done: " + DIMENSION_FILE);
  }
}
