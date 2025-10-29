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

package org.projectforge.business.image;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
    BufferedImage bufferedImage = minimizeImage(imageFromBytes, width, height);
    return createBytesFromImage(bufferedImage);
  }

  /**
   * Shrinks an image to fit within a maximum file size using binary search for optimal dimensions.
   * Uses a combination of JPEG quality reduction and binary search to find the best size quickly.
   * @param originalImage The original image as byte array
   * @param maxSizeBytes Maximum file size in bytes
   * @param format Image format (png, jpg, jpeg)
   * @return Minimized image as byte array that fits within maxSizeBytes
   */
  public byte[] shrinkToMaxFileSize(byte[] originalImage, long maxSizeBytes, String format)
  {
    if (originalImage == null || originalImage.length <= maxSizeBytes) {
      return originalImage; // Already small enough
    }

    BufferedImage image = createImageFromBytes(originalImage);
    if (image == null) {
      log.error("Could not decode image for minimization");
      return originalImage;
    }

    // Normalize format
    String outputFormat = normalizeFormat(format);

    // Define optimal range (95-100% of maxSizeBytes)
    long targetMinSize = (long) (maxSizeBytes * 0.95);

    // Constants
    final int MIN_DIMENSION = 100;
    final int MAX_ITERATIONS = 10;
    final float DEFAULT_JPEG_QUALITY = 0.85f;

    byte[] bestResult = originalImage;
    int originalWidth = image.getWidth();
    int originalHeight = image.getHeight();

    // Strategy 1: Try JPEG quality reduction first (if JPEG)
    if ("jpg".equals(outputFormat)) {
      byte[] qualityResult = tryReduceJpegQuality(image, maxSizeBytes, targetMinSize);
      if (qualityResult != null) {
        // If result is in optimal range, return it immediately
        if (qualityResult.length >= targetMinSize && qualityResult.length <= maxSizeBytes) {
          log.info("Image shrinked by reducing JPEG quality to optimal range: {} bytes -> {} bytes",
              originalImage.length, qualityResult.length);
          return qualityResult;
        } else if (qualityResult.length <= maxSizeBytes) {
          bestResult = qualityResult; // Keep as fallback, but try to get closer to target
        }
      }
    }

    // Strategy 2: Binary search for optimal scale factor
    double minScale = 0.0;
    double maxScale = 1.0;
    byte[] result = null;

    for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
      double currentScale = (minScale + maxScale) / 2.0;

      int newWidth = (int) (originalWidth * currentScale);
      int newHeight = (int) (originalHeight * currentScale);

      // Check minimum dimension constraint BEFORE creating the image
      if (newWidth < MIN_DIMENSION || newHeight < MIN_DIMENSION) {
        log.warn("Reached minimum dimension limit ({}x{}) during binary search at iteration {}, using best result from previous iterations",
            newWidth, newHeight, iteration + 1);
        break;
      }

      // Resize image
      BufferedImage resized = minimizeImage(image, newWidth, newHeight);
      float quality = "jpg".equals(outputFormat) ? DEFAULT_JPEG_QUALITY : 1.0f;
      result = createBytesFromImage(resized, outputFormat, quality);

      if (result == null) {
        log.warn("Failed to create image bytes at scale {} during iteration {}", currentScale, iteration + 1);
        break;
      }

      long resultSize = result.length;

      // Check if we're in the optimal range (95-100% of target)
      if (resultSize >= targetMinSize && resultSize <= maxSizeBytes) {
        log.info("Image minimized via binary search (iteration {}): {} bytes -> {} bytes ({}x{} -> {}x{}, scale={}%)",
            iteration + 1, originalImage.length, resultSize,
            originalWidth, originalHeight, newWidth, newHeight, (int)(currentScale * 100));
        return result;
      }

      // Update best result - always keep the smallest result we've created
      // Even if it's slightly over the limit, it's better than the original
      if (bestResult == originalImage || resultSize < bestResult.length) {
        bestResult = result;
      }

      // Adjust search range for binary search
      if (resultSize > maxSizeBytes) {
        // Image too large, search smaller scales
        maxScale = currentScale;
      } else {
        // Image too small, search larger scales to get closer to target
        minScale = currentScale;
      }
    }

    // Return best result found
    if (bestResult.length <= maxSizeBytes && bestResult.length != originalImage.length) {
      log.info("Image minimized to best fit: {} bytes -> {} bytes", originalImage.length, bestResult.length);
      return bestResult;
    }

    // If still too large, return best effort
    log.warn("Could not minimize image to {} bytes, returning best effort: {} bytes", maxSizeBytes, bestResult.length);
    return bestResult;
  }

  private String normalizeFormat(String format)
  {
    if (format == null) {
      return "png";
    }
    String lower = format.toLowerCase();
    if ("jpeg".equals(lower)) {
      return "jpg";
    }
    return lower;
  }

  /**
   * Tries to shrink JPEG quality to fit within size constraints.
   * Only reduces quality down to 0.7 to maintain acceptable image quality.
   * @param image The image to compress
   * @param maxSizeBytes Maximum file size in bytes
   * @param targetMinSize Minimum target size (95% of maxSizeBytes) for optimal result
   * @return Shrinked image bytes, or null if no acceptable result found
   */
  private byte[] tryReduceJpegQuality(BufferedImage image, long maxSizeBytes, long targetMinSize)
  {
    // Only reduce quality down to 0.7 (not lower) to maintain quality
    float[] qualities = {0.9f, 0.8f, 0.7f};
    byte[] bestResult = null;

    for (float quality : qualities) {
      byte[] result = createBytesFromImage(image, "jpg", quality);
      if (result != null) {
        // If in optimal range (95-100% of target), return immediately
        if (result.length >= targetMinSize && result.length <= maxSizeBytes) {
          return result;
        }
        // Keep best result that fits under the limit
        if (result.length <= maxSizeBytes) {
          bestResult = result;
        }
      }
    }
    return bestResult;
  }

  private BufferedImage minimizeImage(BufferedImage originalImage, int width, int height)
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
    return createBytesFromImage(image, "png", 1.0f);
  }

  private byte[] createBytesFromImage(BufferedImage image, String format, float quality)
  {
    if (image != null) {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if ("jpg".equals(format) || "jpeg".equals(format)) {
          // Use JPEG compression with specified quality
          javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
          javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
          if (param.canWriteCompressed()) {
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality); // 0.0 - 1.0
          }
          javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
          writer.setOutput(ios);
          writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
          writer.dispose();
          ios.close();
        } else {
          // Use PNG or other format
          ImageIO.write(image, format, baos);
        }

        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
      } catch (IOException e) {
        log.error("Error while writing image : " + e.getMessage());
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
