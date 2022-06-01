/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.projectforge.framework.persistence.api.BaseSearchFilter;

import java.util.ArrayList;
import java.util.Collection;

@XStreamAlias("EingangsrechnungFilter")
public class EingangsrechnungListFilter extends RechnungFilter
{
  private static final long serialVersionUID = -9163400923075871920L;

  private Collection<PaymentType> paymentTypes = new ArrayList<>();

  public EingangsrechnungListFilter()
  {
  }

  public EingangsrechnungListFilter(final BaseSearchFilter filter)
  {
    super(filter);

    if (filter instanceof EingangsrechnungListFilter) {
      setShowKostZuweisungStatus(((EingangsrechnungListFilter) filter).isShowKostZuweisungStatus());
      this.paymentTypes = ((EingangsrechnungListFilter) filter).getPaymentTypes();
    }
  }

  public Collection<PaymentType> getPaymentTypes()
  {
    return paymentTypes;
  }

  public void setPaymentTypes(final Collection<PaymentType> paymentTypes)
  {
    this.paymentTypes = paymentTypes;
  }

  @Override
  public RechnungFilter reset()
  {
    paymentTypes = new ArrayList<>();
    return super.reset();
  }

}
