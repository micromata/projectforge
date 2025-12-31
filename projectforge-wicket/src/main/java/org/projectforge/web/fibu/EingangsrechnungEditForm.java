/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.fibu;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.CurrencyConversionCache;
import org.projectforge.business.fibu.CurrencyConversionService;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.common.IbanValidator;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.LocalDateModel;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EingangsrechnungEditForm extends
    AbstractRechnungEditForm<EingangsrechnungDO, EingangsrechnungsPositionDO, EingangsrechnungEditPage>
{
  private static final long serialVersionUID = 5286417118638335693L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EingangsrechnungEditForm.class);

  private MaxLengthTextField recieverField;
  private MaxLengthTextField ibanField;
  private MaxLengthTextField bicField;
  private MaxLengthTextField customernrField;

  public EingangsrechnungEditForm(final EingangsrechnungEditPage parentPage, final EingangsrechnungDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void onInit()
  {
    {
      // Subject
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "betreff");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "betreff")));
    }
    /* GRID50 - BLOCK */
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Date
      final FieldProperties<LocalDate> props = getDatumProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "datum");
      LocalDatePanel components = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      components.setRequired(true);
      fs.add(components);
    }
    {
      // Creditor
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "kreditor");
      final PFAutoCompleteTextField<String> kreditorField = new PFAutoCompleteTextField<String>(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "kreditor"))
      {
        @Override
        protected List<String> getChoices(final String input)
        {
          return parentPage.getBaseDao().getAutocompletion("kreditor", input);
        }
      };
      kreditorField.withMatchContains(true).withMinChars(2).withFocus(true).add(WicketUtils.setFocus());
      kreditorField.add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          autofillLatestKreditorInformations(kreditorField.getModelObject(), target);
        }
      });
      fs.add(kreditorField);
    }
    {
      // Customernr
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "customernr");
      customernrField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "customernr"));
      customernrField.setOutputMarkupId(true);
      fs.add(customernrField);
    }
    {
      // Reference
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "referenz");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "referenz")));
    }
    if (WicketSupport.get(KontoCache.class).isEmpty() == false) {
      // Account
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "konto");
      final KontoSelectPanel kontoSelectPanel = new KontoSelectPanel(fs.newChildId(),
          new PropertyModel<KontoDO>(data, "konto"),
          parentPage, "kontoId");
      kontoSelectPanel.setKontoNumberRanges(AccountingConfig.getInstance().getCreditorsAccountNumberRanges()).init();
      fs.add(kontoSelectPanel);
      fs.setLabelFor(kontoSelectPanel);
    }
    // Exchange rate info for foreign currency invoices
    if (data.getId() != null && data.getDatum() != null && data.getCurrency() != null) {
      final ConfigurationService configurationService = WicketSupport.get(ConfigurationService.class);
      final String systemCurrency = configurationService.getCurrency() != null ? configurationService.getCurrency() : "EUR";
      final String invoiceCurrency = data.getCurrency();

      if (!systemCurrency.equalsIgnoreCase(invoiceCurrency)) {
        final CurrencyConversionCache cache = WicketSupport.get(CurrencyConversionCache.class);
        // Search from invoice currency to system currency (e.g. USD -> EUR)
        final var lookup = cache.findCurrencyPairForConversion(invoiceCurrency, systemCurrency);

        if (lookup != null) {
          final BigDecimal rate = cache.getConversionRate(
            lookup.getPair().getId(),
            data.getDatum(),
            lookup.getUseInverseRate(),
            false
          );
          final LocalDate rateDate = cache.getActiveRateDate(
            lookup.getPair().getId(),
            data.getDatum(),
            false
          );

          if (rate != null && rateDate != null) {
            final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("fibu.currencyConversion.conversionRate"));

            fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
              @Override
              public String getObject() {
                final CurrencyConversionService conversionService = WicketSupport.get(CurrencyConversionService.class);
                final DateTimeFormatter dateTimeFormatter = WicketSupport.get(DateTimeFormatter.class);
                // Format rate without trailing zeros: 1 USD = 0,81 EUR (15.01.2025)
                final String formattedRate = rate.stripTrailingZeros().toPlainString();
                final String formattedDate = dateTimeFormatter.getFormattedDate(rateDate);
                final String rateInfo = String.format("1 %s = %s %s (%s)",
                  invoiceCurrency, formattedRate, systemCurrency, formattedDate);

                // Convert gross sum from invoice currency to system currency
                final BigDecimal grossSum = data.getInfo().getGrossSum();
                final LocalDate datum = data.getDatum();
                if (grossSum != null && datum != null) {
                  final BigDecimal convertedAmount = conversionService.convert(
                    grossSum,
                    systemCurrency,
                    invoiceCurrency,
                    datum,
                    2,
                    java.math.RoundingMode.HALF_UP,
                    false
                  );

                  if (convertedAmount != null) {
                    // brutto: 145,45 USD = 123,00 EUR
                    final String bruttoInfo = getString("fibu.common.brutto") + ": "
                      + CurrencyFormatter.format(grossSum, false) + " " + invoiceCurrency + " = "
                      + CurrencyFormatter.format(convertedAmount, false) + " " + systemCurrency;
                    return rateInfo + WebConstants.HTML_TEXT_DIVIDER + bruttoInfo;
                  }
                }

                return rateInfo;
              }
            }));
          }
        }
      }
    }
  }

  private void autofillLatestKreditorInformations(final String kreditor, final AjaxRequestTarget target)
  {
    if (StringUtils.isEmpty(kreditor)) {
      return;
    }

    final EingangsrechnungDO newestRechnung = WicketSupport.get(EingangsrechnungDao.class).findNewestByKreditor(kreditor);
    if (newestRechnung == null) {
      return;
    }

    // Update Customer No.
    getData().setCustomernr(newestRechnung.getCustomernr());
    target.add(customernrField);

    // Update Konto
    getData().setReceiver(newestRechnung.getReceiver());
    target.add(recieverField);

    getData().setIban(newestRechnung.getIban());
    target.add(ibanField);

    getData().setBic(newestRechnung.getBic());
    target.add(bicField);
  }

  @Override
  protected void addCurrencyField()
  {
    // Currency
    final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "currency");
    final MaxLengthTextField currencyField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "currency"));

    // Convert to uppercase on blur
    currencyField.add(new AjaxFormComponentUpdatingBehavior("blur")
    {
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        final String currency = data.getCurrency();
        if (StringUtils.isNotBlank(currency)) {
          data.setCurrency(currency.toUpperCase());
          target.add(currencyField);
        }
      }
    });

    currencyField.add((IValidator<String>) validatable -> {
      final String currency = validatable.getValue();
      if (StringUtils.isNotBlank(currency)) {
        final ConfigurationService configService = WicketSupport.get(ConfigurationService.class);
        final String systemCurrency = configService.getCurrency() != null ? configService.getCurrency() : "EUR";

        // Check if currency is system currency
        if (!systemCurrency.equalsIgnoreCase(currency)) {
          // Check if currency is known in currency conversion
          final CurrencyConversionCache cache = WicketSupport.get(CurrencyConversionCache.class);
          if (cache.findCurrencyPairForConversion(currency, systemCurrency) == null &&
              cache.findCurrencyPairForConversion(systemCurrency, currency) == null) {
            validatable.error(new ValidationError().addKey("fibu.rechnung.error.unknownCurrency").setVariable("currency", currency));
          }
        }
      }
    });

    currencyField.setOutputMarkupId(true);
    fs.add(currencyField);
  }

  @Override
  protected void addCellAfterDiscount()
  {
    {
      // DropDownChoice payment type
      gridBuilder.newSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "paymentType");
      final LabelValueChoiceRenderer<PaymentType> paymentTypeChoiceRenderer = new LabelValueChoiceRenderer<PaymentType>(
          this,
          PaymentType.values());
      final DropDownChoice<PaymentType> paymentTypeChoice = new DropDownChoice<PaymentType>(fs.getDropDownChoiceId(),
          new PropertyModel<PaymentType>(data, "paymentType"), paymentTypeChoiceRenderer.getValues(),
          paymentTypeChoiceRenderer);
      paymentTypeChoice.setNullValid(true);
      paymentTypeChoice.add((IValidator<PaymentType>) validatable -> {
        PaymentType pt = validatable.getValue();
        if (PaymentType.BANK_TRANSFER.equals(pt)) {
          if (data.getInfo().getGrossSum() != null && data.getInfo().getGrossSum().compareTo(BigDecimal.ZERO) < 0) {
            error(I18nHelper.getLocalizedMessage("fibu.rechnung.error.negativAmount"));
          }
        }
      });
      fs.add(paymentTypeChoice);
    }
    {
      // Receiver
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "receiver");
      recieverField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "receiver"));
      recieverField.setOutputMarkupId(true);
      fs.add(recieverField);
    }
    {
      // IBAN
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "iban");
      ibanField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "iban"));
      ibanField.setOutputMarkupId(true);
      ibanField.add(new IbanValidator());
      fs.add(ibanField);
    }
    {
      // BIC
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "bic");
      bicField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "bic"));
      bicField.setOutputMarkupId(true);
      fs.add(bicField);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected EingangsrechnungsPositionDO newPositionInstance()
  {
    return new EingangsrechnungsPositionDO();
  }
}
