"use client";

import type { ReactNode } from "react";

export interface ListPageShellProps {
  toolbar: ReactNode;
  children: ReactNode;
  filterPanel?: ReactNode;
}

export function ListPageShell({
  toolbar,
  children,
  filterPanel,
}: ListPageShellProps) {
  return (
    <>
      {toolbar}
      <div className="flex flex-1 overflow-hidden">
        {children}
        {filterPanel}
      </div>
    </>
  );
}
