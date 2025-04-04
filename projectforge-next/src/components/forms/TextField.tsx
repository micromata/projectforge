"use client";

import React from "react";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";

interface TextFieldProps {
  label: string;
  id?: string;
  value?: string;
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  type?: string;
  required?: boolean;
  step?: string;
  min?: number;
  max?: number;
}

export default function TextField({
  label,
  id,
  value,
  onChange,
  placeholder = "",
  disabled = false,
  className = "",
  type = "text",
  required = false,
  step,
  min,
  max,
}: TextFieldProps) {
  return (
    <div className={`space-y-2 ${className}`}>
      <Label htmlFor={id}>
        {label}{required && <span className="text-destructive ml-1">*</span>}
      </Label>
      <Input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        required={required}
        step={step}
        min={min}
        max={max}
      />
    </div>
  );
}