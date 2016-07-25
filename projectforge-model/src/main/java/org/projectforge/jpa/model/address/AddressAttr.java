package org.projectforge.jpa.model.address;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the t_address_attr database table.
 * 
 */
@Entity
@Table(name="t_address_attr")
@NamedQuery(name="AddressAttr.findAll", query="SELECT a FROM AddressAttr a")
public class AddressAttr implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer pk;

	private Timestamp createdat;

	private String createdby;

	private Timestamp modifiedat;

	private String modifiedby;

	private String propertyname;

	private String type;

	private Integer updatecounter;

	private String value;

	private String withdata;

	//bi-directional many-to-one association to Address
	@ManyToOne
	@JoinColumn(name="parent")
	private Address TAddress;

	//bi-directional many-to-one association to AddressAttrdata
	@OneToMany(mappedBy="TAddressAttr")
	private List<AddressAttrdata> TAddressAttrdata;

	public AddressAttr() {
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

	public String getPropertyname() {
		return this.propertyname;
	}

	public void setPropertyname(String propertyname) {
		this.propertyname = propertyname;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getUpdatecounter() {
		return this.updatecounter;
	}

	public void setUpdatecounter(Integer updatecounter) {
		this.updatecounter = updatecounter;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getWithdata() {
		return this.withdata;
	}

	public void setWithdata(String withdata) {
		this.withdata = withdata;
	}

	public Address getTAddress() {
		return this.TAddress;
	}

	public void setTAddress(Address TAddress) {
		this.TAddress = TAddress;
	}

	public List<AddressAttrdata> getTAddressAttrdata() {
		return this.TAddressAttrdata;
	}

	public void setTAddressAttrdata(List<AddressAttrdata> TAddressAttrdata) {
		this.TAddressAttrdata = TAddressAttrdata;
	}

	public AddressAttrdata addTAddressAttrdata(AddressAttrdata TAddressAttrdata) {
		getTAddressAttrdata().add(TAddressAttrdata);
		TAddressAttrdata.setTAddressAttr(this);

		return TAddressAttrdata;
	}

	public AddressAttrdata removeTAddressAttrdata(AddressAttrdata TAddressAttrdata) {
		getTAddressAttrdata().remove(TAddressAttrdata);
		TAddressAttrdata.setTAddressAttr(null);

		return TAddressAttrdata;
	}

}