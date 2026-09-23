import { BrandStripe } from "@/components/shared/brand-stripe";
import { LogoRow } from "@/components/shared/logo-row";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface AuthCardProps {
  title: string;
  children: React.ReactNode;
  /** Overrides the default card width (e.g. a wider `max-w-*` for content-heavy pages). */
  className?: string;
}

/** Centered card shell shared by all public auth pages (login, password reset). */
export function AuthCard({ title, children, className }: AuthCardProps) {
  return (
    <div className="flex min-h-screen flex-col bg-muted/40">
      {/* Always visible here: this page does not scroll, so there is nothing for the row to get out of
          the way of. collapsible={false} rather than trusting the flag - arriving from a scrolled list
          must not leave the logo hidden. */}
      <LogoRow collapsible={false} />
      <BrandStripe />
      <div className="flex flex-1 items-center justify-center px-4 py-8">
        <Card className={cn("w-full max-w-md", className)}>
          <CardHeader className="text-center">
            <CardTitle className="text-2xl">{title}</CardTitle>
          </CardHeader>
          <CardContent>{children}</CardContent>
        </Card>
      </div>
    </div>
  );
}
