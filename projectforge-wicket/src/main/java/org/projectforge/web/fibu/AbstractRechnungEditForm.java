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

package org.projectforge.web.fibu;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.fibu.kost.KostZuweisungenCopyHelper;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.converter.BigDecimalPercentConverter;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.flowlayout.*;
import org.projectforge.web.wicket.flowlayout.ToggleContainerPanel.ToggleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRechnungEditForm<O extends AbstractRechnungDO, T extends AbstractRechnungsPositionDO, P extends AbstractEditPage<?, ?, ?>>
        extends AbstractEditForm<O, P> {
  private static final long serialVersionUID = 9073611406229693582L;

  private static final int[] ZAHLUNGSZIELE_IN_TAGEN = {7, 14, 30, 60, 90};

  private static final Component[] COMPONENT_ARRAY = new Component[0];

  private RepeatingView positionsRepeater;

  private boolean costConfigured;

  private CostEditModalDialog costEditModalDialog;

  private final List<Component> ajaxUpdateComponents = new ArrayList<>();

  private Component[] ajaxUpdateComponentsArray;

  public AbstractRechnungEditForm(final P parentPage, final O data) {
    super(parentPage, data);
  }

  protected abstract void onInit();

  @SuppressWarnings("serial")
  @Override
  protected void init() {
    super.init();
    RechnungCalculator.INSTANCE.calculate(data);

    if (Configuration.getInstance().isCostConfigured() == true) {
      costConfigured = true;
    }
    addCloneButton();

    onInit();

    // GRID 50% - BLOCK
    gridBuilder.newSplitPanel(GridSize.COL50, true);
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Currency
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "currency");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "currency")));
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Net sum
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.common.netto"));
      final DivTextPanel netPanel = new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          return CurrencyFormatter.format(data.getInfo().getNetSum());
        }
      }, TextStyle.FORM_TEXT);
      fs.add(netPanel);
      fs.suppressLabelForWarning();
      ajaxUpdateComponents.add(netPanel.getLabel4Ajax());
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Vat amount
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.common.vatAmount"));
      final DivTextPanel vatPanel = new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          return CurrencyFormatter.format(data.getInfo().getVatAmount());
        }
      }, TextStyle.FORM_TEXT);
      fs.add(vatPanel);
      fs.suppressLabelForWarning();
      ajaxUpdateComponents.add(vatPanel.getLabel4Ajax());
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Brutto
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.common.brutto"));
      final DivTextPanel grossPanel = new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          return CurrencyFormatter.format(data.getInfo().getGrossSum());
        }
      }, TextStyle.FORM_TEXT);
      fs.add(grossPanel);
      fs.suppressLabelForWarning();
      ajaxUpdateComponents.add(grossPanel.getLabel4Ajax());
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Bezahldatum
      final FieldProperties<LocalDate> props = getBezahlDatumProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "bezahlDatum");
      LocalDatePanel components = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(components);
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Zahlbetrag
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "zahlBetrag");
      final TextField<BigDecimal> zahlBetragField = new TextField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<BigDecimal>(data,
              "zahlBetrag")) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public IConverter getConverter(final Class type) {
          return new CurrencyConverter();
        }
      };
      fs.add(zahlBetragField);
    }
    {
      gridBuilder.newSubSplitPanel(GridSize.COL50);
      // Fälligkeit und Zahlungsziel
      final FieldProperties<LocalDate> props = getFaelligkeitProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "faelligkeit");
      LocalDatePanel faelligkeitsPanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(faelligkeitsPanel);
      fs.setLabelFor(faelligkeitsPanel);

      // DropDownChoice ZahlungsZiel
      final LabelValueChoiceRenderer<Integer> zielChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
      for (final int days : ZAHLUNGSZIELE_IN_TAGEN) {
        zielChoiceRenderer.addValue(days, String.valueOf(days) + " " + getString("days"));
      }
      final DropDownChoice<Integer> zahlungsZielChoice = new DropDownChoice<Integer>(fs.getDropDownChoiceId(), new PropertyModel<>(data, "zahlungsZielInTagen"),
              zielChoiceRenderer.getValues(), zielChoiceRenderer) {
        @Override
        public boolean isVisible() {
          return data.getFaelligkeit() == null;
        }
      };
      zahlungsZielChoice.setNullValid(true);
      zahlungsZielChoice.setRequired(false);

      fs.add(zahlungsZielChoice);
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          data.recalculate();
          return data.getZahlungsZielInTagen() + " " + getString("days");
        }
      }) {
        @Override
        public boolean isVisible() {
          return data.getFaelligkeit() != null;
        }
      });
    }
    {
      gridBuilder.newSubSplitPanel(GridSize.COL50);
      // Discount
      final FieldProperties<LocalDate> props = getDiscountProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("fibu.rechnung.discount"));
      LocalDatePanel discountPanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(discountPanel);
      fs.setLabelFor(discountPanel);

      // DropDownChoice DiscountZahlungsZiel
      final LabelValueChoiceRenderer<Integer> discountZielChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
      for (final int days : ZAHLUNGSZIELE_IN_TAGEN) {
        discountZielChoiceRenderer.addValue(days, String.valueOf(days) + " " + getString("days"));
      }
      final DropDownChoice<Integer> discountZahlungsZielChoice = new DropDownChoice<Integer>(fs.getDropDownChoiceId(),
              new PropertyModel<>(data, "discountZahlungsZielInTagen"), discountZielChoiceRenderer.getValues(), discountZielChoiceRenderer) {
        @Override
        public boolean isVisible() {
          return data.getDiscountMaturity() == null;
        }
      };
      discountZahlungsZielChoice.setNullValid(true);
      discountZahlungsZielChoice.setRequired(false);

      fs.add(discountZahlungsZielChoice);
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          data.recalculate();
          return data.getDiscountZahlungsZielInTagen() + " " + getString("days");
        }
      }) {
        @Override
        public boolean isVisible() {
          return data.getDiscountMaturity() != null;
        }
      });
      TextField<BigDecimal> discountPercentField = new TextField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<BigDecimal>(data, "discountPercent"));
      discountPercentField.add(AttributeModifier.replace("style", "max-width: 50px;"));
      fs.add(discountPercentField);
    }

    addCellAfterDiscount();

    // GRID 50% - BLOCK
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Bemerkung
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "bemerkung");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "bemerkung")), true);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Besonderheiten
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "besonderheiten");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "besonderheiten")), true);
    }
    gridBuilder.newGridPanel();
    positionsRepeater = gridBuilder.newRepeatingView();
    if (costConfigured == true) {
      addCostEditModalDialog();
    }
    refreshPositions();
    if (getBaseDao().hasInsertAccess(getUser()) == true) {
      final DivPanel panel = gridBuilder.newGridPanel().getPanel();
      final Button addPositionButton = new Button(SingleButtonPanel.WICKET_ID) {
        @Override
        public final void onSubmit() {
          final T position = newPositionInstance();
          data.addPosition(position);
          if (position.getNumber() > 1) {
            final AbstractRechnungsPositionDO predecessor = data.getAbstractPosition(position.getNumber() - 2);
            if (predecessor != null) {
              position.setVat(predecessor.getVat()); // Preset the vat from the predecessor position.
            }
          }
          refreshPositions();
        }
      };
      final SingleButtonPanel addPositionButtonPanel = new SingleButtonPanel(panel.newChildId(), addPositionButton, getString("add"));
      addPositionButtonPanel.setTooltip(getString("fibu.rechnung.tooltip.addPosition"));
      panel.add(addPositionButtonPanel);
    }
  }

  protected FieldProperties<LocalDate> getDatumProperties() {
    return new FieldProperties<>("fibu.rechnung.datum", new PropertyModel<>(super.data, "datum"));
  }

  private FieldProperties<LocalDate> getBezahlDatumProperties() {
    return new FieldProperties<>("fibu.rechnung.bezahlDatum", new PropertyModel<>(super.data, "bezahlDatum"));
  }

  private FieldProperties<LocalDate> getFaelligkeitProperties() {
    return new FieldProperties<>("fibu.rechnung.faelligkeit", new PropertyModel<>(super.data, "faelligkeit"));
  }

  private FieldProperties<LocalDate> getDiscountProperties() {
    return new FieldProperties<>("fibu.rechnung.discountMaturity", new PropertyModel<>(super.data, "discountMaturity"));
  }


  protected void addCellAfterDiscount() {
    // Do nothing.
  }

  protected abstract T newPositionInstance();

  @SuppressWarnings("serial")
  protected void refreshPositions() {
    positionsRepeater.removeAll();
    final boolean hasInsertAccess = getBaseDao().hasInsertAccess(getUser());
    if (CollectionUtils.isEmpty(data.getAbstractPositionen()) == true) {
      // Ensure that at least one position is available:
      final T position = newPositionInstance();
      position.setVat(Configuration.getInstance().getPercentValue(ConfigurationParam.FIBU_DEFAULT_VAT));
      data.addPosition(position);
    }

    if (data instanceof RechnungDO) {
      ((RechnungDO) data).getPositionen().removeIf(AbstractBaseDO::getDeleted);
    } else {
      ((EingangsrechnungDO) data).getPositionen().removeIf(AbstractBaseDO::getDeleted);
    }
    RechnungCalculator.INSTANCE.calculate(data);

    for (final AbstractRechnungsPositionDO position : data.getAbstractPositionen()) {
      // Fetch all kostZuweisungen:
      if (CollectionUtils.isNotEmpty(position.getKostZuweisungen()) == true) {
        for (final KostZuweisungDO zuweisung : position.getKostZuweisungen()) {
          zuweisung.getNetto(); // Fetch
        }
      }
      final List<Component> ajaxUpdatePositionComponents = new ArrayList<Component>();
      final RechnungsPositionDO rechnungsPosition = (position instanceof RechnungsPositionDO) ? (RechnungsPositionDO) position : null;
      final ToggleContainerPanel positionsPanel = new ToggleContainerPanel(positionsRepeater.newChildId()) {
        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#wantsOnStatusChangedNotification()
         */
        @Override
        protected boolean wantsOnStatusChangedNotification() {
          return true;
        }

        /**
         */
        @Override
        protected void onToggleStatusChanged(final AjaxRequestTarget target, final ToggleStatus toggleStatus) {
          if (toggleStatus == ToggleStatus.OPENED) {
            data.getUiStatus().openPosition(position.getNumber());
          } else {
            data.getUiStatus().closePosition(position.getNumber());
          }
          setHeading(getPositionHeading(position, this));
        }
      };
      positionsPanel.getContainer().setOutputMarkupId(true);
      positionsRepeater.add(positionsPanel);
      if (data.getUiStatus().isClosed(position.getNumber()) == true) {
        positionsPanel.setClosed();
      } else {
        positionsPanel.setOpen();
      }
      positionsPanel.setHeading(getPositionHeading(position, positionsPanel));
      final GridBuilder posGridBuilder = positionsPanel.createGridBuilder();
      final GridSize gridSize = (rechnungsPosition != null) ? GridSize.COL25 : GridSize.COL33;
      {
        posGridBuilder.newSplitPanel(GridSize.COL50, true);
        if (rechnungsPosition != null) {
          // Order
          posGridBuilder.newSubSplitPanel(gridSize); // COL25
          final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.auftrag")).setLabelSide(false);
          fieldset.add(new InputPanel(fieldset.newChildId(), new AuftragsPositionFormComponent(InputPanel.WICKET_ID,
                  new PropertyModel<AuftragsPositionDO>(position, "auftragsPosition"), false)));
          fieldset.add(new IconPanel(fieldset.newIconChildId(), IconType.GOTO, getString("show")) {
            /**
             * @see org.apache.wicket.markup.html.link.Link#onClick()
             */
            @Override
            public void onClick() {
              if (rechnungsPosition.getAuftragsPosition() != null) {
                final PageParameters parameters = new PageParameters();
                parameters.add(AbstractEditPage.PARAMETER_KEY_ID, rechnungsPosition.getAuftragsPosition().getAuftrag().getId());
                final AuftragEditPage auftragEditPage = new AuftragEditPage(parameters);
                auftragEditPage.setReturnToPage(getParentPage());
                setResponsePage(auftragEditPage);
              }
            }

            @Override
            public boolean isVisible() {
              return rechnungsPosition.getAuftragsPosition() != null;
            }
          }.enableAjaxOnClick(), FieldSetIconPosition.TOP_RIGHT);
        }
        {
          // Menge
          posGridBuilder.newSubSplitPanel(gridSize);
          final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.rechnung.menge")).setLabelSide(false);
          final TextField<BigDecimal> amountTextField = new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID,
                  new PropertyModel<BigDecimal>(position, "menge"), BigDecimal.ZERO, NumberHelper.BILLION);
          amountTextField.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                RechnungCalculator.INSTANCE.calculate(data);
                addAjaxComponents(target, ajaxUpdatePositionComponents);
            }
          });
          fieldset.add(amountTextField);
        }
        {
          // Net price
          posGridBuilder.newSubSplitPanel(gridSize);
          final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.rechnung.position.einzelNetto")).setLabelSide(false);
          final TextField<BigDecimal> netTextField = new TextField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<BigDecimal>(
                  position, "einzelNetto")) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            public IConverter getConverter(final Class type) {
              return new CurrencyConverter();
            }
          };
          netTextField.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
              RechnungCalculator.INSTANCE.calculate(data);
              addAjaxComponents(target, ajaxUpdatePositionComponents);
            }
          });
          fieldset.add(netTextField);
        }
        {
          // VAT
          posGridBuilder.newSubSplitPanel(gridSize);
          final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.rechnung.mehrwertSteuerSatz")).setLabelSide(false);
          final TextField<BigDecimal> vatTextField = new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<BigDecimal>(
                  position, "vat"), BigDecimal.ZERO, NumberHelper.HUNDRED) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            public IConverter getConverter(final Class type) {
              return new BigDecimalPercentConverter(true);
            }
          };
          vatTextField.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
              RechnungCalculator.INSTANCE.calculate(data);
              addAjaxComponents(target, ajaxUpdatePositionComponents);
            }
          });
          fieldset.add(vatTextField);
        }
      }
      {
        posGridBuilder.newSplitPanel(GridSize.COL50, true);
        posGridBuilder.newSubSplitPanel(GridSize.COL33);
        {
          final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.common.netto")).setLabelSide(false)
                  .suppressLabelForWarning();
          final TextPanel netTextPanel = new TextPanel(fieldset.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
              return CurrencyFormatter.format(position.getInfo().getNetSum());
            }
          });
          ajaxUpdatePositionComponents.add(netTextPanel.getLabel4Ajax());
          fieldset.add(netTextPanel);
        }
      }
      {
        posGridBuilder.newSubSplitPanel(GridSize.COL33);
        {
          final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.common.vatAmount")).setLabelSide(false)
                  .suppressLabelForWarning();
          final TextPanel vatTextPanel = new TextPanel(fieldset.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
              return CurrencyFormatter.format(position.getInfo().getVatAmount());
            }
          });
          fieldset.add(vatTextPanel);
          ajaxUpdatePositionComponents.add(vatTextPanel.getLabel4Ajax());
        }
      }
      {
        posGridBuilder.newSubSplitPanel(GridSize.COL33);
        {
          final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.common.brutto")).setLabelSide(false)
                  .suppressLabelForWarning();
          final TextPanel grossTextPanel = new TextPanel(fieldset.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
              return CurrencyFormatter.format(position.getInfo().getGrossSum());
            }
          });
          fieldset.add(grossTextPanel);
          ajaxUpdatePositionComponents.add(grossTextPanel.getLabel4Ajax());
        }
      }
      {
        // Text
        if (costConfigured == true) {
          posGridBuilder.newSplitPanel(GridSize.COL50);
        } else {
          posGridBuilder.newGridPanel();
        }
        final FieldsetPanel fieldset = posGridBuilder.newFieldset(getString("fibu.rechnung.text"));
        fieldset.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(position, "text")), true);
      }
      // Cost assignments
      if (costConfigured == true) {
        posGridBuilder.newSplitPanel(GridSize.COL50, true);
        {
          posGridBuilder.newSubSplitPanel(GridSize.COL50);
          DivPanel panel = posGridBuilder.getPanel();
          final RechnungCostTablePanel costTable = new RechnungCostTablePanel(panel.newChildId(), position) {
            /**
             * @see org.projectforge.web.fibu.RechnungCostTablePanel#onRenderCostRow(org.projectforge.business.fibu.AbstractRechnungsPositionDO,
             *      org.apache.wicket.Component, org.apache.wicket.Component)
             */
            @Override
            protected void onRenderCostRow(final AbstractRechnungsPositionDO position, final KostZuweisungDO costAssignment,
                                           final Component cost1, final Component cost2) {
              AbstractRechnungEditForm.this.onRenderCostRow(position, costAssignment, cost1, cost2);
            }
          };
          panel.add(costTable);
          ajaxUpdatePositionComponents.add(costTable.refresh().getTable());

          posGridBuilder.newSubSplitPanel(GridSize.COL50);
          panel = posGridBuilder.getPanel();
          WicketSupport.get(RechnungCache.class).ensureRechnungPosInfo(position);
          final BigDecimal fehlbetrag = position.getInfo().getKostZuweisungNetFehlbetrag();
          if (hasInsertAccess == true) {
            ButtonType buttonType;
            if (NumberHelper.isNotZero(fehlbetrag) == true) {
              buttonType = ButtonType.RED;
            } else {
              buttonType = ButtonType.LIGHT;
            }
            final AjaxButton editCostButton = new AjaxButton(ButtonPanel.BUTTON_ID, this) {
              @Override
              protected void onSubmit(final AjaxRequestTarget target)
              {
                costEditModalDialog.open(target);
                // Redraw the content:
                costEditModalDialog.redraw(position, costTable);
                // The content was changed:
                costEditModalDialog.addContent(target);
              }

              @Override
              protected void onError(final AjaxRequestTarget target)
              {
                target.add(AbstractRechnungEditForm.this.feedbackPanel);
              }
            };
            editCostButton.setDefaultFormProcessing(false);
            panel.add(new ButtonPanel(panel.newChildId(), getString("edit"), editCostButton, buttonType));
          } else {
            panel.add(new TextPanel(panel.newChildId(), " "));
          }
          panel.add(new TextPanel(panel.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
              final BigDecimal fehlbetrag = position.getInfo().getKostZuweisungNetFehlbetrag();
              if (NumberHelper.isNotZero(fehlbetrag) == true) {
                return CurrencyFormatter.format(fehlbetrag);
              } else {
                return "";
              }
            }
          }, TextStyle.RED));
        }
      }
      if (getBaseDao().hasLoggedInUserUpdateAccess(data, data, false) == true) {
        GridBuilder removeButtonGridBuilder = posGridBuilder.newGridPanel();
        {
          // Remove Position
          DivPanel divPanel = removeButtonGridBuilder.getPanel();
          final Button removePositionButton = new Button(SingleButtonPanel.WICKET_ID) {
            @Override
            public final void onSubmit() {
              position.setDeleted(true);
              refreshPositions();
            }
          };
          removePositionButton.add(AttributeModifier.append("class", ButtonType.DELETE.getClassAttrValue()));
          final SingleButtonPanel removePositionButtonPanel = new SingleButtonPanel(divPanel.newChildId(), removePositionButton,
                  getString("delete"));
          removePositionButtonPanel.setVisible(isNew() == true);
          divPanel.add(removePositionButtonPanel);
        }
      }

      if (position.getDeleted()) {
        positionsPanel.setVisible(false);
      }
      onRenderPosition(posGridBuilder, position);
    }
  }

  /**
   * Does nothing at default.
   *
   * @param position
   * @param costAssignment
   * @param cost1
   * @param cost2
   */
  protected void onRenderCostRow(final AbstractRechnungsPositionDO position, final KostZuweisungDO costAssignment, final Component cost1,
                                 final Component cost2) {
  }

  protected String getPositionHeading(final AbstractRechnungsPositionDO position, final ToggleContainerPanel positionsPanel) {
    if (positionsPanel.getToggleStatus() == ToggleStatus.OPENED) {
      return getString("label.position.short") + " #" + position.getNumber();
    }
    final StringBuilder heading = new StringBuilder();
    heading.append(escapeHtml(getString("label.position.short"))).append(" #").append(position.getNumber());
    heading.append(": ").append(CurrencyFormatter.format(position.getInfo().getNetSum()));
    if (StringHelper.isNotBlank(position.getText()) == true) {
      heading.append(" ").append(StringUtils.abbreviate(position.getText(), 80));
    }
    return heading.toString();
  }

  /**
   * Overwrite this method if you need to add own form elements for a order position.
   */
  protected void onRenderPosition(final GridBuilder posGridBuilder, final AbstractRechnungsPositionDO position) {
  }

  protected void addCostEditModalDialog() {
    costEditModalDialog = new CostEditModalDialog();
    final String title = (isNew() == true) ? "create" : "update";
    costEditModalDialog.setCloseButtonLabel(getString(title)).setOutputMarkupId(true);
    parentPage.add(costEditModalDialog);
    costEditModalDialog.init();
  }

  private class CostEditModalDialog extends ModalDialog {
    private static final long serialVersionUID = 7113006438653862995L;

    private RechnungCostEditTablePanel rechnungCostEditTablePanel;

    private AbstractRechnungsPositionDO position;

    private RechnungCostTablePanel costTable;

    CostEditModalDialog() {
      super(parentPage.newModalDialogId());
      setBigWindow().setEscapeKeyEnabled(false);
    }

    @Override
    public void init() {
      setTitle(getString("fibu.rechnung.showEditableKostZuweisungen"));
      init(new Form<String>(getFormId()));
    }

    public void redraw(final AbstractRechnungsPositionDO position, final RechnungCostTablePanel costTable) {
      this.position = position;
      this.costTable = costTable;
      clearContent();
      {
        final DivPanel panel = gridBuilder.getPanel();
        rechnungCostEditTablePanel = new RechnungCostEditTablePanel(panel.newChildId());
        panel.add(rechnungCostEditTablePanel);
        rechnungCostEditTablePanel.add(position);
      }
    }

    /**
     * @see org.projectforge.web.dialog.ModalDialog#onCloseButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
     */
    @Override
    protected boolean onCloseButtonSubmit(final AjaxRequestTarget target) {
      // Copy edited values to DO object.
      final AbstractRechnungsPositionDO srcPosition = rechnungCostEditTablePanel.getPosition();
      KostZuweisungenCopyHelper.copy(srcPosition.getKostZuweisungen(), position);
      target.add(costTable.refresh().getTable());
      return super.onCloseButtonSubmit(target);
    }
  }

  /**
   * @return null
   */
  public Long getBezahlDatumInMillis() {
    return null;
  }

  /**
   * Dummy method. Does nothing.
   *
   * @param bezahlDatumInMillis
   */
  public void setBezahlDatumInMillis(final Long bezahlDatumInMillis) {
  }

  private void addAjaxComponents(final AjaxRequestTarget target, final List<Component> components) {
    target.add(components.toArray(COMPONENT_ARRAY));
    if (ajaxUpdateComponentsArray == null) {
      ajaxUpdateComponentsArray = ajaxUpdateComponents.toArray(COMPONENT_ARRAY);
    }
    target.add(ajaxUpdateComponentsArray);
  }
}
