"use client";

import React from "react";

interface FormSectionProps {
  title?: string;
  children: React.ReactNode;
  className?: string;
}

export default function FormSection({
  title,
  children,
  className = "",
}: FormSectionProps) {
  return (
    <div className={`mt-6 space-y-4 ${className}`}>
      {title && (
        <h3 className="text-lg font-medium">{title}</h3>
      )}
      {children}
    </div>
  );
}