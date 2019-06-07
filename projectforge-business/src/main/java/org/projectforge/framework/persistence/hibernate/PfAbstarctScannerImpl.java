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

package org.projectforge.framework.persistence.hibernate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.scan.spi.ClassFileArchiveEntryHandler;
import org.hibernate.boot.archive.scan.spi.NonClassFileArchiveEntryHandler;
import org.hibernate.boot.archive.scan.spi.PackageInfoArchiveEntryHandler;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;

/**
 * @author Florian Blumenstein
 * @deprecated see JpaWithExtLibrariesScanner
 */
@Deprecated
public class PfAbstarctScannerImpl implements Scanner
{
  static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PfAbstarctScannerImpl.class);

  private final ArchiveDescriptorFactory archiveDescriptorFactory;
  private final Map<String, ArchiveDescriptorInfo> archiveDescriptorCache = new HashMap<String, ArchiveDescriptorInfo>();

  public PfAbstarctScannerImpl()
  {
    this(StandardArchiveDescriptorFactory.INSTANCE);
  }

  public PfAbstarctScannerImpl(ArchiveDescriptorFactory value)
  {
    this.archiveDescriptorFactory = value;
  }

  @Override
  public ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters parameters)
  {
    final ScanResultCollector collector = new ScanResultCollector(environment, options, parameters);

    if (environment.getNonRootUrls() != null) {
      final ArchiveContext context = new ArchiveContextImpl(false, collector);
      for (URL url : environment.getNonRootUrls()) {
        final ArchiveDescriptor descriptor = buildArchiveDescriptor(url, false);
        descriptor.visitArchive(context);
      }
    }

    if (environment.getRootUrl() != null) {
      final ArchiveContext context = new ArchiveContextImpl(true, collector);
      URL rootUrl = environment.getRootUrl();
      if (rootUrl.toString().contains("!")) {
        String customUrlStr = rootUrl.toString();
        customUrlStr = "jar:" + customUrlStr;
        log.info("Custom URL: " + customUrlStr);
        try {
          URL customUrl = new URL(customUrlStr);
          final ArchiveDescriptor descriptor = buildArchiveDescriptor(customUrl, true);
          descriptor.visitArchive(context);
        } catch (MalformedURLException e) {
          log.error("Error while getting custom URL: " + customUrlStr);
        }
      } else {
        final ArchiveDescriptor descriptor = buildArchiveDescriptor(rootUrl, true);
        descriptor.visitArchive(context);
      }

    }

    return collector.toScanResult();
  }

  private ArchiveDescriptor buildArchiveDescriptor(URL url, boolean isRootUrl)
  {
    final ArchiveDescriptor descriptor;
    final ArchiveDescriptorInfo descriptorInfo = archiveDescriptorCache.get(url.toString());
    if (descriptorInfo == null) {
      descriptor = archiveDescriptorFactory.buildArchiveDescriptor(url);
      archiveDescriptorCache.put(
          url.toString(),
          new ArchiveDescriptorInfo(descriptor, isRootUrl));
    } else {
      validateReuse(descriptorInfo, isRootUrl);
      descriptor = descriptorInfo.archiveDescriptor;
    }
    return descriptor;
  }

  // This needs to be protected and attributes/constructor visible in case
  // a custom scanner needs to override validateReuse.
  protected static class ArchiveDescriptorInfo
  {
    public final ArchiveDescriptor archiveDescriptor;
    public final boolean isRoot;

    public ArchiveDescriptorInfo(ArchiveDescriptor archiveDescriptor, boolean isRoot)
    {
      this.archiveDescriptor = archiveDescriptor;
      this.isRoot = isRoot;
    }
  }

  @SuppressWarnings("UnusedParameters")
  protected void validateReuse(ArchiveDescriptorInfo descriptor, boolean root)
  {
    // is it really reasonable that a single url be processed multiple times?
    // for now, throw an exception, mainly because I am interested in situations where this might happen
    throw new IllegalStateException("ArchiveDescriptor reused; can URLs be processed multiple times?");
  }

  public static class ArchiveContextImpl implements ArchiveContext
  {
    private final boolean isRootUrl;

    private final ClassFileArchiveEntryHandler classEntryHandler;
    private final PackageInfoArchiveEntryHandler packageEntryHandler;
    private final ArchiveEntryHandler fileEntryHandler;

    public ArchiveContextImpl(boolean isRootUrl, ScanResultCollector scanResultCollector)
    {
      this.isRootUrl = isRootUrl;

      this.classEntryHandler = new ClassFileArchiveEntryHandler(scanResultCollector);
      this.packageEntryHandler = new PackageInfoArchiveEntryHandler(scanResultCollector);
      this.fileEntryHandler = new NonClassFileArchiveEntryHandler(scanResultCollector);
    }

    @Override
    public boolean isRootUrl()
    {
      return isRootUrl;
    }

    @Override
    public ArchiveEntryHandler obtainArchiveEntryHandler(ArchiveEntry entry)
    {
      final String nameWithinArchive = entry.getNameWithinArchive();

      if (nameWithinArchive.endsWith("package-info.class")) {
        return packageEntryHandler;
      } else if (nameWithinArchive.endsWith(".class")) {
        return classEntryHandler;
      } else {
        return fileEntryHandler;
      }
    }
  }
}
