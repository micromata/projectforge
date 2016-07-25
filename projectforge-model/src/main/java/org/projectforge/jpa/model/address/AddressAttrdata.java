package org.projectforge.jpa.model.address;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the t_address_attrdata database table.
 * 
 */
@Entity
@Table(name="t_address_attrdata")
@NamedQuery(name="AddressAttrdata.findAll", query="SELECT a FROM AddressAttrdata a")
public class AddressAttrdata implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer pk;

	private Timestamp createdat;

	private String createdby;

	private String datacol;

	private Integer datarow;

	private Timestamp modifiedat;

	private String modifiedby;

	private Integer updatecounter;

	//bi-directional many-to-one association to AddressAttr
	@ManyToOne
	@JoinColumn(name="parent_id")
	private AddressAttr TAddressAttr;

	public AddressAttrdata() {
	}

	public Integer getPk() {
		return this.pk;
	}

	public void setPk(Integer pk) {
		this.pk = pk;
	}

	public Timestamp getCreatedat() {
		return this.createdat;
	}

	public void setCreatedat(Timestamp createdat) {
		this.createdat = createdat;
	}

	public String getCreatedby() {
		return this.createdby;
	}

	public void setCreatedby(String createdby) {
		this.createdby = createdby;
	}

	public String getDatacol() {
		return this.datacol;
	}

	public void setDatacol(String datacol) {
		this.datacol = datacol;
	}

	public Integer getDatarow() {
		return this.datarow;
	}

	public void setDatarow(Integer datarow) {
		this.datarow = datarow;
	}

	public Timestamp getModifiedat() {
		return this.modifiedat;
	}

	public void setModifiedat(Timestamp modifiedat) {
		this.modifiedat = modifiedat;
	}

	public String getModifiedby() {
		return this.modifiedby;
	}

	public void setModifiedby(String modifiedby) {
		this.modifiedby = modifiedby;
	}

	public Integer getUpdatecounter() {
		return this.updatecounter;
	}

	public void setUpdatecounter(Integer updatecounter) {
		this.updatecounter = updatecounter;
	}

	public AddressAttr getTAddressAttr() {
		return this.TAddressAttr;
	}

	public void setTAddressAttr(AddressAttr TAddressAttr) {
		this.TAddressAttr = TAddressAttr;
	}

}