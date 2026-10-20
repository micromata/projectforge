"use client";

import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { POSTAUSGANG_METADATA } from "@/lib/metadata/postausgang.generated";
import { POSTEINGANG_METADATA } from "@/lib/metadata/posteingang.generated";
import { fetchPostSuggestions, type PostEntity } from "@/lib/rs/post";

type FieldProps = { className?: string };

/**
 * A free text completing from the values the other entries of the same mail book already have — the
 * autocompletion of the removed React form (`UIInput.enableAutoCompletion`).
 */
function PostSuggestField({
  entity,
  property,
  label,
  className,
}: {
  entity: PostEntity;
  property: string;
  label: string;
  className?: string;
}) {
  return (
    <StringSuggestField
      name={property}
      label={label}
      className={className}
      suggest={(search, signal) =>
        fetchPostSuggestions(entity, property, search, signal)
      }
      // The completions depend on nothing but the entity, the property and the term the user is typing.
      queryKey={[entity, "autocomplete", property]}
    />
  );
}

/** The completing fields of the inbox (PosteingangDao.isAutocompletionPropertyEnabled). */
function inboxField(property: "absender" | "person" | "inhalt") {
  function InboxSuggestField({ className }: FieldProps) {
    const label = useFieldLabels(POSTEINGANG_METADATA);
    return (
      <PostSuggestField
        entity="incomingMail"
        property={property}
        label={label(property)}
        className={className}
      />
    );
  }
  return InboxSuggestField;
}

/** The completing fields of the outbox (PostausgangDao.isAutocompletionPropertyEnabled). */
function outboxField(
  property: "empfaenger" | "person" | "absender" | "inhalt"
) {
  function OutboxSuggestField({ className }: FieldProps) {
    const label = useFieldLabels(POSTAUSGANG_METADATA);
    return (
      <PostSuggestField
        entity="outgoingMail"
        property={property}
        label={label(property)}
        className={className}
      />
    );
  }
  return OutboxSuggestField;
}

export const InboxSenderField = inboxField("absender");
export const InboxPersonField = inboxField("person");
export const InboxContentField = inboxField("inhalt");

export const OutboxReceiverField = outboxField("empfaenger");
export const OutboxPersonField = outboxField("person");
export const OutboxSenderField = outboxField("absender");
export const OutboxContentField = outboxField("inhalt");
