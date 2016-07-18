package org.projectforge.framework.persistence.history.entities;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;

/**
 * Entity holds Strings longer than fits into one attribute value.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 * 
 */
@Entity
@DiscriminatorValue("1")
@JpaXmlPersist(noStore = true)
public class PfHistoryAttrWithDataDO extends PfHistoryAttrDO
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = 1965960042100228573L;

  /**
   * Instantiates a new history attr with data do.
   */
  public PfHistoryAttrWithDataDO()
  {

  }

  /**
   * Instantiates a new history attr with data do.
   *
   * @param parent the parent
   */
  public PfHistoryAttrWithDataDO(PfHistoryMasterDO parent)
  {
    super(parent);
  }

  /**
   * Instantiates a new history attr with data do.
   *
   * @param parent the parent
   * @param propertyName the property name
   * @param type the type
   * @param value the value
   */
  public PfHistoryAttrWithDataDO(PfHistoryMasterDO parent, String propertyName, char type, String value)
  {
    super(parent, propertyName, type, value);
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = PfHistoryAttrDataDO.class,
      orphanRemoval = true, fetch = FetchType.EAGER)
  @OrderColumn(name = "datarow")
  @Override
  public List<JpaTabAttrDataBaseDO<?, Long>> getData()
  {
    return super.getData();
  }

}
