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

package de.micromata.less;

import java.io.File;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

/**
 * Wicket adaption of LESS resource reference
 *
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class LessResourceReference extends ResourceReference
{
  private File file;

  public LessResourceReference(final String name, File file)
  {
    super(name);
    this.file = file;
  }

  @Override
  public IResource getResource()
  {
    return new LessPackageResource(getName(), file);
  }

  private static class LessPackageResource extends PackageResource
  {

    private File file;

    protected LessPackageResource(String name, File file)
    {
      super(LessPackageResource.class, name, null, null, null);
      this.file = file;
    }

    @Override
    public IResourceStream getResourceStream()
    {
      return new FileResourceStream(file);
    }
  }
}
