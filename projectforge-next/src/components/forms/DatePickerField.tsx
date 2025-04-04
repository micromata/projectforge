"use client";

import React from "react";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { CalendarIcon } from "lucide-react";
import { format } from "date-fns";

interface DatePickerFieldProps {
  label: string;
  id?: string;
  value?: Date | null;
  onChange?: (date: Date | null) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}

export default function DatePickerField({
  label,
  id,
  value,
  onChange,
  placeholder = "Select date",
  disabled = false,
  className = "",
}: DatePickerFieldProps) {
  return (
    <div className={`space-y-2 ${className}`}>
      <Label htmlFor={id}>{label}</Label>
      <Popover>
        <PopoverTrigger asChild>
          <Button
            id={id}
            variant="outline"
            className="w-full justify-start text-left font-normal"
            disabled={disabled}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            {value ? (
              <span>{format(value, "PPP")}</span>
            ) : (
              <span>{placeholder}</span>
            )}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0">
          <Calendar
            mode="single"
            selected={value || undefined}
            onSelect={onChange}
            disabled={disabled}
            initialFocus
          />
        </PopoverContent>
      </Popover>
    </div>
  );
}