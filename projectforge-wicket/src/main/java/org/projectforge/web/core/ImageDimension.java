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

package org.projectforge.web.core;

import java.awt.*;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * For (de-)serializing web app's image dimensions. Used by PresizedImage and GetImageDimensionsTest.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "image")
public class ImageDimension
{
  @XmlField(asAttribute = true)
  private String path;

  private int width;

  private int height;

  /**
   * Only for reading the image dimension file.
   */
  @SuppressWarnings("unused")
  private ImageDimension()
  {
  }

  public ImageDimension(final String imagePath, final int width, final int height)
  {
    this.path = imagePath;
    this.width = width;
    this.height = height;
  }

  public String getPath()
  {
    return path;
  }

  public void setPath(String path)
  {
    this.path = path;
  }

  public int getWidth()
  {
    return width;
  }

  public void setWidth(int width)
  {
    this.width = width;
  }

  public int getHeight()
  {
    return height;
  }

  public void setHeight(int height)
  {
    this.height = height;
  }

  public Dimension getDimension()
  {
    return new Dimension(width, height);
  }

  @Override
  public String toString()
  {
    final ReflectionToStringBuilder tos = new ReflectionToStringBuilder(this);
    return tos.toString();
  }
}
