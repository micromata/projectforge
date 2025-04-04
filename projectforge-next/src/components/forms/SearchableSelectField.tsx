"use client";

import React, { useState, useEffect, useCallback } from "react";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { CheckIcon, ChevronsUpDown, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface Option {
  id: string;
  label: string;
  description?: string;
}

interface SearchableSelectFieldProps {
  label: string;
  options?: Option[];
  loadOptions?: (query: string) => Promise<Option[]>;
  value?: Option | null;
  onSelect: (option: Option | null) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  required?: boolean;
  emptyMessage?: string;
  searchPlaceholder?: string;
}

export default function SearchableSelectField({
  label,
  options,
  loadOptions,
  value,
  onSelect,
  placeholder = "Select an option",
  disabled = false,
  className = "",
  required = false,
  emptyMessage = "No option found.",
  searchPlaceholder = "Search options...",
}: SearchableSelectFieldProps) {
  const [open, setOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [dynamicOptions, setDynamicOptions] = useState<Option[]>(options || []);

  // Fetch options when search query changes
  const fetchOptions = useCallback(
    async (query: string) => {
      if (!loadOptions) return;
      
      setIsLoading(true);
      try {
        const results = await loadOptions(query);
        setDynamicOptions(results);
      } catch (error) {
        console.error("Error loading options:", error);
      } finally {
        setIsLoading(false);
      }
    },
    [loadOptions]
  );

  // Handle search query changes
  useEffect(() => {
    if (loadOptions && (searchQuery.length > 1 || open)) {
      // Add debounce for search
      const handler = setTimeout(() => {
        fetchOptions(searchQuery);
      }, 300);
      
      return () => clearTimeout(handler);
    }
  }, [searchQuery, loadOptions, fetchOptions, open]);

  // Initialize with static options if provided
  useEffect(() => {
    if (options && !loadOptions) {
      setDynamicOptions(options);
    }
  }, [options, loadOptions]);

  // If using static options, filter them based on search query
  const filteredOptions = !loadOptions && options
    ? options.filter(option => 
        option.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
        option.description?.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : dynamicOptions;

  return (
    <div className={`space-y-2 ${className}`}>
      <Label>
        {label}{required && <span className="text-destructive ml-1">*</span>}
      </Label>
      <Popover open={open} onOpenChange={setOpen} modal={true}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            className="w-full justify-between"
            disabled={disabled}
          >
            {value ? value.label : placeholder}
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-full p-0">
          <Command>
            <CommandInput 
              placeholder={searchPlaceholder} 
              value={searchQuery}
              onValueChange={setSearchQuery}
            />
            {isLoading ? (
              <div className="py-6 text-center text-sm flex items-center justify-center">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                <span>Loading options...</span>
              </div>
            ) : (
              <>
                <CommandEmpty>{emptyMessage}</CommandEmpty>
                <CommandGroup>
                  {filteredOptions.map((option) => (
                    <CommandItem
                      key={option.id}
                      value={option.id}
                      onSelect={() => {
                        onSelect(option.id === value?.id ? null : option);
                        setOpen(false);
                      }}
                    >
                      <CheckIcon
                        className={cn(
                          "mr-2 h-4 w-4",
                          value?.id === option.id ? "opacity-100" : "opacity-0"
                        )}
                      />
                      <div>
                        <div>{option.label}</div>
                        {option.description && (
                          <div className="text-xs text-muted-foreground">
                            {option.description}
                          </div>
                        )}
                      </div>
                    </CommandItem>
                  ))}
                </CommandGroup>
              </>
            )}
          </Command>
        </PopoverContent>
      </Popover>
    </div>
  );
}