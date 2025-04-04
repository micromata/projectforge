"use client"

import * as React from "react"
import { CalendarIcon } from "lucide-react"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

export type CalendarProps = {
  className?: string
  selected?: Date
  onSelect?: (date: Date | undefined) => void
  disabled?: boolean
  mode?: "single" | "range" | "multiple"
  initialFocus?: boolean
}

// A simplified calendar component that uses an input[type="date"] instead of react-day-picker
function Calendar({
  className,
  selected,
  onSelect,
  disabled,
  ...props
}: CalendarProps) {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const date = e.target.value ? new Date(e.target.value) : undefined
    if (onSelect) {
      onSelect(date)
    }
  }

  const formatDate = (date?: Date) => {
    if (!date) return ""
    // Format as YYYY-MM-DD for the input
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, "0")
    const day = date.getDate().toString().padStart(2, "0")
    return `${year}-${month}-${day}`
  }

  return (
    <div className={cn("relative", className)}>
      <CalendarIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
      <Input
        type="date"
        className="pl-10"
        value={formatDate(selected)}
        onChange={handleChange}
        disabled={disabled}
      />
    </div>
  )
}
Calendar.displayName = "Calendar"

export { Calendar }