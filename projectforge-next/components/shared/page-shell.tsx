"use client";

import type { ReactNode } from "react";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { TopNavigation } from "@/components/shared/top-navigation";

interface PageShellProps {
  children: ReactNode;
}

export function PageShell({ children }: PageShellProps) {
  return (
    <div className="flex h-screen flex-col overflow-hidden">
      <BrandStripe />
      <TopNavigation />
      <main className="flex flex-1 flex-col overflow-auto">
        {children}
      </main>
    </div>
  );
}
