"use client";

import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { CheckIcon, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface Project {
  id: string;
  name: string;
  customer?: {
    id: string;
    name: string;
  };
}

interface ProjectSelectProps {
  onSelect: (project: Project | null) => void;
  value?: Project | null;
  placeholder?: string;
}

// This is mock data - in a real app, this would come from an API
const mockProjects: Project[] = [
  { 
    id: "1", 
    name: "Website Redesign", 
    customer: { id: "1", name: "Acme Corporation" } 
  },
  { 
    id: "2", 
    name: "Mobile App Development", 
    customer: { id: "2", name: "Globex Industries" } 
  },
  { 
    id: "3", 
    name: "Cloud Migration", 
    customer: { id: "1", name: "Acme Corporation" } 
  },
  { 
    id: "4", 
    name: "Security Audit", 
    customer: { id: "3", name: "Wayne Enterprises" } 
  },
  { 
    id: "5", 
    name: "CRM Implementation", 
    customer: { id: "4", name: "Stark Industries" } 
  },
];

export default function ProjectSelect({ 
  onSelect, 
  value = null,
  placeholder = "Select a project"
}: ProjectSelectProps) {
  const [open, setOpen] = useState(false);
  const [projects] = useState<Project[]>(mockProjects);

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
          <CommandInput placeholder="Search projects..." />
          <CommandEmpty>No project found.</CommandEmpty>
          <CommandGroup>
            {projects.map((project) => (
              <CommandItem
                key={project.id}
                value={project.id}
                onSelect={() => {
                  onSelect(project.id === value?.id ? null : project);
                  setOpen(false);
                }}
              >
                <CheckIcon
                  className={cn(
                    "mr-2 h-4 w-4",
                    value?.id === project.id ? "opacity-100" : "opacity-0"
                  )}
                />
                <div>
                  <div>{project.name}</div>
                  {project.customer && (
                    <div className="text-xs text-muted-foreground">
                      {project.customer.name}
                    </div>
                  )}
                </div>
              </CommandItem>
            ))}
          </CommandGroup>
        </Command>
      </PopoverContent>
    </Popover>
  );
}