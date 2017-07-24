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
    int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
    BufferedImage resizedImage = new BufferedImage(width, height, type);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(originalImage, 0, 0, width, height, null);
    g.dispose();
    return resizedImage;
  }

  private byte[] createBytesFromImage(BufferedImage image)
  {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      baos.flush();
      byte[] imageInByte = baos.toByteArray();
      baos.close();
      return imageInByte;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private BufferedImage createImageFromBytes(byte[] imageData)
  {
    ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
    try {
      return ImageIO.read(bais);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
