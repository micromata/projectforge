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

package org.projectforge.business.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

/**
 * Created by fdesel on 24.07.17.
 */
@Service
public class ImageService
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageService.class);

  public byte[] resizeImage(byte[] originalImage)
  {
    return resizeImage(originalImage, 25, 25);
  }

  public byte[] resizeImage(byte[] originalImage, int width, int height)
  {
    BufferedImage imageFromBytes = createImageFromBytes(originalImage);
    BufferedImage bufferedImage = compressImage(imageFromBytes, width, height);
    return createBytesFromImage(bufferedImage);
  }

  private BufferedImage compressImage(BufferedImage originalImage, int width, int height)
  {
    if (originalImage != null) {
      int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
      BufferedImage resizedImage = new BufferedImage(width, height, type);
      Graphics2D g = resizedImage.createGraphics();
      g.drawImage(originalImage, 0, 0, width, height, null);
      g.dispose();
      return resizedImage;
    }
    return null;
  }

  private byte[] createBytesFromImage(BufferedImage image)
  {
    if (image != null) {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
      } catch (IOException e) {
        log.error("Error while reading image : " + e.getMessage());
        return null;
      }
    }
    return null;
  }

  private BufferedImage createImageFromBytes(byte[] imageData)
  {
    ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
    try {
      return ImageIO.read(bais);
    } catch (IOException e) {
      log.error("Error while read ByteArrayInputStream : " + e.getMessage());
      return null;
    }
  }
}
