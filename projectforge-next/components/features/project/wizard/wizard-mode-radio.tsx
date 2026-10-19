import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

/** A choice of a step between a few ways, e.g. an existing customer or a new one. */
export function WizardModeRadio<M extends string>({
  id,
  value,
  onChange,
  options,
  ariaLabel,
}: {
  id: string;
  value: M;
  onChange: (value: M) => void;
  options: { value: M; label: string }[];
  ariaLabel: string;
}) {
  return (
    <RadioGroup
      value={value}
      onValueChange={(next) => onChange(next as M)}
      aria-label={ariaLabel}
      className="flex flex-wrap gap-4"
    >
      {options.map((option) => (
        <div key={option.value} className="flex items-center gap-2">
          <RadioGroupItem id={`${id}-${option.value}`} value={option.value} />
          <Label
            htmlFor={`${id}-${option.value}`}
            className="text-sm font-normal"
          >
            {option.label}
          </Label>
        </div>
      ))}
    </RadioGroup>
  );
}
