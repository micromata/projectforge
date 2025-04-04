"use client";

import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { CheckIcon, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface Customer {
  id: string;
  name: string;
}

interface CustomerSelectProps {
  onSelect: (customer: Customer | null) => void;
  value?: Customer | null;
  placeholder?: string;
}

// This is mock data - in a real app, this would come from an API
const mockCustomers: Customer[] = [
  { id: "1", name: "Acme Corporation" },
  { id: "2", name: "Globex Industries" },
  { id: "3", name: "Wayne Enterprises" },
  { id: "4", name: "Stark Industries" },
  { id: "5", name: "Umbrella Corporation" },
];

export default function CustomerSelect({ 
  onSelect, 
  value = null,
  placeholder = "Select a customer"
}: CustomerSelectProps) {
  const [open, setOpen] = useState(false);
  const [customers] = useState<Customer[]>(mockCustomers);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="w-full justify-between"
        >
          {value ? value.name : placeholder}
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-full p-0">
        <Command>
          <CommandInput placeholder="Search customers..." />
          <CommandEmpty>No customer found.</CommandEmpty>
          <CommandGroup>
            {customers.map((customer) => (
              <CommandItem
                key={customer.id}
                value={customer.id}
                onSelect={() => {
                  onSelect(customer.id === value?.id ? null : customer);
                  setOpen(false);
                }}
              >
                <CheckIcon
                  className={cn(
                    "mr-2 h-4 w-4",
                    value?.id === customer.id ? "opacity-100" : "opacity-0"
                  )}
                />
                {customer.name}
              </CommandItem>
            ))}
          </CommandGroup>
        </Command>
      </PopoverContent>
    </Popover>
  );
}