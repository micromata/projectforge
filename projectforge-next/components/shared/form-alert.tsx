import { Alert, AlertDescription } from "@/components/ui/alert";
import { cn } from "@/lib/utils";

type Tone = "error" | "info" | "success";

const TONE_CLASSES: Record<Tone, string> = {
  error: "border-destructive/40 bg-destructive/10",
  info: "border-primary/30 bg-primary/5",
  success: "border-emerald-600/30 bg-emerald-600/10",
};

interface FormAlertProps {
  tone: Tone;
  children: React.ReactNode;
  className?: string;
}

/**
 * Inline feedback above a form (login error, "code sent", motd). Server messages
 * are already localized, so this only carries the tone.
 */
export function FormAlert({ tone, children, className }: FormAlertProps) {
  return (
    <Alert
      variant={tone === "error" ? "destructive" : "default"}
      className={cn("px-3 py-2 text-sm", TONE_CLASSES[tone], className)}
    >
      <AlertDescription
        className={cn(
          "text-sm",
          tone === "error" ? undefined : "text-foreground"
        )}
      >
        {children}
      </AlertDescription>
    </Alert>
  );
}
