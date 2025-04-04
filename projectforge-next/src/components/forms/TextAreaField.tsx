"use client";

import React from "react";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

interface TextAreaFieldProps {
  label: string;
  id?: string;
  value?: string;
  onChange?: (event: React.ChangeEvent<HTMLTextAreaElement>) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  rows?: number;
  required?: boolean;
}

export default function TextAreaField({
  label,
  id,
  value,
  onChange,
  placeholder = "",
  disabled = false,
  className = "",
  rows = 3,
  required = false,
}: TextAreaFieldProps) {
  return (
    <div className={`space-y-2 ${className}`}>
      <Label htmlFor={id}>
        {label}{required && <span className="text-destructive ml-1">*</span>}
      </Label>
      <Textarea
        id={id}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        rows={rows}
        required={required}
      />
    </div>
  );
}