import Link from "next/link";
import { resolveMenuUrl } from "@/lib/menu-url";

/**
 * Renders a menu entry as a client-side link when it belongs to this app, and as a plain anchor
 * (full page load) when it points at the legacy React app or Wicket.
 */
export function MenuLink({
  url,
  children,
  className,
  title,
  onClick,
}: {
  url: string | undefined;
  children: React.ReactNode;
  className?: string;
  title?: string;
  onClick?: () => void;
}) {
  const target = resolveMenuUrl(url);
  if (target.kind === "external") {
    return (
      <a
        href={target.href}
        className={className}
        title={title}
        onClick={onClick}
      >
        {children}
      </a>
    );
  }
  return (
    <Link
      href={target.href}
      className={className}
      title={title}
      onClick={onClick}
    >
      {children}
    </Link>
  );
}
