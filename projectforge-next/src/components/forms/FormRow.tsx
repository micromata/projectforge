"use client";

import React from "react";

interface FormRowProps {
  children: React.ReactNode;
  className?: string;
}

export default function FormRow({
  children,
  className = "",
}: FormRowProps) {
  return (
    <div className={`grid grid-cols-1 md:grid-cols-2 gap-6 ${className}`}>
      {children}
    </div>
  );
}

interface FormColumnProps {
  children: React.ReactNode;
  className?: string;
}

export function FormColumn({
  children,
  className = "",
}: FormColumnProps) {
  return (
    <div className={`space-y-4 ${className}`}>
      {children}
    </div>
  );
}