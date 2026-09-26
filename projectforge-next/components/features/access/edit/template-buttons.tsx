"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { convertEntity } from "@/lib/rs/entity";
import { leafKeyOf } from "@/lib/leaf-key";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import { normalizeEntries } from "../values";
import type { AccessDetail } from "../types";

/** The five quick-fill presets, in the order the Wicket form lines them up; the label doubles as the endpoint. */
const TEMPLATES = [
  { name: "clear", labelKey: "access.templates.clear" },
  { name: "guest", labelKey: "access.templates.guest" },
  { name: "employee", labelKey: "access.templates.employee" },
  { name: "leader", labelKey: "access.templates.leader" },
  { name: "administrator", labelKey: "access.templates.administrator" },
] as const;

/**
 * The row of preset buttons above the permission matrix — clear, guest, employee, leader, administrator,
 * the Wicket `AccessEditForm` quick-fills. Each posts the current form entity to the non-persisting
 * `access/template/{name}` endpoint (GroupAccessServicesRest) and drops the computed matrix back into the
 * form for the user to review; nothing is saved until the user hits Save, so a misclick costs a second
 * button press, not a wrong grant.
 *
 * A custom field because the presets act on `accessEntries` as a whole and have no metadata field of
 * their own.
 */
export function TemplateButtons({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();

  const applyTemplate = useMutation({
    // The whole form travels with the request; the backend fills the matrix and returns it unsaved.
    mutationFn: (name: string) =>
      convertEntity<AccessDetail>(
        "access",
        `template/${name}`,
        form.state.values
      ),
    onSuccess: (result) =>
      form.setFieldValue(
        "accessEntries" as never,
        normalizeEntries(result.accessEntries) as never
      ),
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <span className="text-sm font-medium text-muted-foreground">
        {t(leafKeyOf("access.templates", t.has))}
      </span>
      <div className="flex flex-wrap gap-2">
        {TEMPLATES.map((template) => (
          <Button
            key={template.name}
            type="button"
            variant="outline"
            size="sm"
            disabled={readOnly || applyTemplate.isPending}
            onClick={() => applyTemplate.mutate(template.name)}
          >
            {t(template.labelKey)}
          </Button>
        ))}
      </div>
    </div>
  );
}
