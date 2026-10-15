"use client";

import { useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import {
  fetchKreditorSuggestions,
  fetchNewestByKreditor,
  type KreditorAutofill,
} from "@/lib/rs/creditor-invoice";
import { EINGANGSRECHNUNG_METADATA } from "@/lib/metadata/eingangsrechnung.generated";

/**
 * Who the invoice is from, completing from the creditors the backend has already seen
 * (`EingangsrechnungDao` opts `kreditor` into the property autocompletion). A free string, not a
 * reference — hence the string-suggest field.
 *
 * Custom because settling on a known creditor carries its bank details over into the other fields, the way
 * Wicket's `EingangsrechnungEditForm.autofillLatestKreditorInformations` did on the autocomplete field's
 * change event: the payee, IBAN, BIC and customer number of that creditor's most recent invoice.
 *
 * The carry-over runs only when the creditor actually changes — [lastAutofilled] starts at the loaded value,
 * so merely tabbing through an existing invoice's creditor does not overwrite its stored bank details, while
 * picking or typing a different creditor does.
 */
export function KreditorField({ className }: { className?: string }) {
  const form = useEntityEditForm();
  const label = useFieldLabels(EINGANGSRECHNUNG_METADATA);

  // The creditor the fields below already reflect: the loaded one at mount, then whatever was last carried
  // over. Only a value differing from this triggers a lookup — see the class doc.
  const lastAutofilled = useRef<string>(
    ((form.getFieldValue("kreditor") as string | null) ?? "").trim()
  );

  const autofill = useMutation({
    mutationFn: (kreditor: string) => fetchNewestByKreditor(kreditor),
    onSuccess: (data: KreditorAutofill | null) => {
      if (!data) return;
      // Carry the whole block over, empty values included, exactly as the Wicket form did: the newest
      // invoice of this creditor is the authority on how to pay it.
      form.setFieldValue("receiver", data.receiver ?? null);
      form.setFieldValue("iban", data.iban ?? null);
      form.setFieldValue("bic", data.bic ?? null);
      form.setFieldValue("customernr", data.customernr ?? null);
    },
  });

  const onCommit = (value: string) => {
    const kreditor = value.trim();
    if (!kreditor || kreditor === lastAutofilled.current) return;
    lastAutofilled.current = kreditor;
    autofill.mutate(kreditor);
  };

  return (
    <StringSuggestField
      name="kreditor"
      label={label("kreditor")}
      className={className}
      suggest={fetchKreditorSuggestions}
      // The completions depend on nothing but the term the user is typing.
      queryKey={["incomingInvoice", "kreditor"]}
      onCommit={onCommit}
    />
  );
}
