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
 *
 * The tone is mirrored as `data-tone`: several of these can share a form (the motd
 * and a login error), and `role="alert"` alone cannot tell them apart - neither for
 * a test nor for styling.
 */
export function FormAlert({ tone, children, className }: FormAlertProps) {
  return (
    <Alert
      variant={tone === "error" ? "destructive" : "default"}
      data-tone={tone}
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
