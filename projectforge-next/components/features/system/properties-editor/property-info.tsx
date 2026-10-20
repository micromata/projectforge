import { useTranslations } from "next-intl";
import type { PropertyParam } from "@/lib/rs/properties-editor";
import { PropertyDescription } from "./property-description";

/** Whether [PropertyInfo] has anything to say about the parameter beyond its key. */
export function hasPropertyInfo(param: PropertyParam): boolean {
  return Boolean(
    param.description ||
    param.fileComment ||
    (!param.secret && (param.defaultValue || param.example))
  );
}

/**
 * The explanation of a parameter behind its info icon: the description from application.properties,
 * whether it is optional, its default (or example) and the admin's own comment in the file.
 */
export function PropertyInfo({
  param,
  query,
}: {
  param: PropertyParam;
  query: string;
}) {
  const t = useTranslations();
  const defaultText =
    param.defaultValue != null && param.defaultValue !== ""
      ? `${t("system.propertiesEditor.default")}: ${param.defaultValue}`
      : param.example
        ? `${t("system.propertiesEditor.example")}: ${param.example}`
        : null;
  return (
    <div className="flex max-w-md flex-col gap-2 text-left">
      <p className="font-mono text-xs font-medium break-all">{param.key}</p>
      {param.description && (
        <PropertyDescription text={param.description} query={query} />
      )}
      {param.optional && param.known && (
        <p className="text-xs text-muted-foreground">
          {t("system.propertiesEditor.optional")}
        </p>
      )}
      {defaultText && !param.secret && (
        <p className="font-mono text-xs break-all">{defaultText}</p>
      )}
      {param.fileComment && (
        <div className="flex flex-col gap-0.5">
          <p className="text-xs font-medium">
            {t("system.propertiesEditor.fileComment")}
          </p>
          <p className="border-l-2 pl-2 font-mono text-xs whitespace-pre-line text-muted-foreground">
            {param.fileComment}
          </p>
        </div>
      )}
    </div>
  );
}
