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

package org.projectforge.plugins.licensemanagement;

import de.micromata.genome.db.jpa.history.api.NoHistory;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.utils.ReflectionToString;

import java.sql.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "owners", index = Index.YES /* TOKENIZED */, store = Store.NO,
    impl = HibernateSearchUsersBridge.class)
@Table(name = "T_PLUGIN_LM_LICENSE", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_plugin_lm_license_tenant_id", columnList = "tenant_id")
})
public class LicenseDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 1124854524084990283L;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String organization;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String product;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String version;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String updateFromVersion;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String licenseHolder;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String key;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private Integer numberOfLicenses;

  private String ownerIds;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String device;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private LicenseStatus status;

  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date validSince;

  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date validUntil;
  @NoHistory
  private byte[] file1;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String filename1;
  @NoHistory
  private byte[] file2;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String filename2;

  @Transient
  public String getOrderString()
  {
    return organization + "-" + product + "-" + version;
  }

  /**
   * @return the organization
   */
  @Column(length = 1000)
  public String getOrganization()
  {
    return organization;
  }

  /**
   * @param organization the organization to set
   * @return this for chaining.
   */
  public LicenseDO setOrganization(final String organization)
  {
    this.organization = organization;
    return this;
  }

  /**
   * @return the product
   */
  @Column(length = 1000)
  public String getProduct()
  {
    return product;
  }

  /**
   * @param product the product to set
   * @return this for chaining.
   */
  public LicenseDO setProduct(final String product)
  {
    this.product = product;
    return this;
  }

  /**
   * @return the version
   */
  @Column(length = 1000)
  public String getVersion()
  {
    return version;
  }

  /**
   * @param version the version to set
   * @return this for chaining.
   */
  public LicenseDO setVersion(final String version)
  {
    this.version = version;
    return this;
  }

  /**
   * @return the updateFromVersion
   */
  @Column(name = "update_from_version", length = 1000)
  public String getUpdateFromVersion()
  {
    return updateFromVersion;
  }

  /**
   * @param updateFromVersion the updateFromVersion to set
   * @return this for chaining.
   */
  public LicenseDO setUpdateFromVersion(final String updateFromVersion)
  {
    this.updateFromVersion = updateFromVersion;
    return this;
  }

  /**
   * @return the licenseHolder
   */
  @Column(length = 10000, name = "license_holder")
  public String getLicenseHolder()
  {
    return licenseHolder;
  }

  /**
   * @param licenseHolder the licenseHolder to set
   * @return this for chaining.
   */
  public LicenseDO setLicenseHolder(final String licenseHolder)
  {
    this.licenseHolder = licenseHolder;
    return this;
  }

  /**
   * @return the key
   */
  @Column(length = 10000)
  public String getKey()
  {
    return key;
  }

  /**
   * @param key the key to set
   * @return this for chaining.
   */
  public LicenseDO setKey(final String key)
  {
    this.key = key;
    return this;
  }

  /**
   * @return the numberOfLicenses
   */
  @Column(name = "number_of_licenses")
  public Integer getNumberOfLicenses()
  {
    return numberOfLicenses;
  }

  /**
   * @param numberOfLicenses the numberOfLicenses to set
   * @return this for chaining.
   */
  public LicenseDO setNumberOfLicenses(final Integer numberOfLicenses)
  {
    this.numberOfLicenses = numberOfLicenses;
    return this;
  }

  /**
   * Comma separated id's of owners (user id's).
   *
   * @return the owners
   */
  @Column(length = 4000)
  public String getOwnerIds()
  {
    return ownerIds;
  }

  /**
   * @param ownerIds the owners to set
   * @return this for chaining.
   */
  public LicenseDO setOwnerIds(final String ownerIds)
  {
    this.ownerIds = ownerIds;
    return this;
  }

  /**
   * @return the device(s) on which the software is installed.
   */
  @Column(length = 4000)
  public String getDevice()
  {
    return device;
  }

  /**
   * @param device the device to set
   * @return this for chaining.
   */
  public LicenseDO setDevice(final String device)
  {
    this.device = device;
    return this;
  }

  @Column(length = Constants.LENGTH_TEXT)
  public String getComment()
  {
    return comment;
  }

  /**
   * @return this for chaining.
   */
  public LicenseDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public LicenseStatus getStatus()
  {
    return status;
  }

  /**
   * @return this for chaining.
   */
  public LicenseDO setStatus(final LicenseStatus status)
  {
    this.status = status;
    return this;
  }

  /**
   * @return the validSince date
   */
  @Column(name = "valid_since")
  /**
   * @return the validSince
   */
  public Date getValidSince()
  {
    return validSince;
  }

  /**
   * @param validSince the validSince to set
   * @return this for chaining.
   */
  public LicenseDO setValidSince(final Date validSince)
  {
    this.validSince = validSince;
    return this;
  }

  /**
   * @return the validUntil date
   */
  @Column(name = "valid_until")
  public Date getValidUntil()
  {
    return validUntil;
  }

  /**
   * @param validUntil the validUntilDate to set
   * @return this for chaining.
   */
  public LicenseDO setValidUntil(final Date validUntil)
  {
    this.validUntil = validUntil;
    return this;
  }

  /**
   * @return the file1
   */
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "file1")
  @Type(type = "binary")
  public byte[] getFile1()
  {
    return file1;
  }

  /**
   * @param file1 the file to set
   * @return this for chaining.
   */
  public void setFile1(final byte[] file1)
  {
    this.file1 = file1;
  }

  /**
   * @return the filename1
   */
  @Column(name = "file_name1", length = 255)
  public String getFilename1()
  {
    return filename1;
  }

  /**
   * @param filename1 the filename to set
   * @return this for chaining.
   */
  public void setFilename1(final String filename1)
  {
    this.filename1 = filename1;
  }

  /**
   * @return the file2
   */
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "file2")
  @Type(type = "binary")
  public byte[] getFile2()
  {
    return file2;
  }

  /**
   * @param file2 the file to set
   * @return this for chaining.
   */
  public void setFile2(final byte[] file2)
  {
    this.file2 = file2;
  }

  /**
   * @return the filename2
   */
  @Column(name = "file_name2", length = 255)
  public String getFilename2()
  {
    return filename2;
  }

  /**
   * @param filename2 the filename to set
   * @return this for chaining.
   */
  public void setFilename2(final String filename2)
  {
    this.filename2 = filename2;
  }

  /**
   * Returns string containing all fields (except the file1/file2) of given object (via ReflectionToStringBuilder).
   *
   * @param user
   * @return
   */
  @Override
  public String toString()
  {
    return (new ReflectionToString(this)
    {
      @Override
      protected boolean accept(final java.lang.reflect.Field f)
      {
        return super.accept(f) && !"file1".equals(f.getName()) && !"file2".equals(f.getName());
      }
    }).toString();
  }
}
