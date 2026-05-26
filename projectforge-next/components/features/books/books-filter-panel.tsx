"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Search01Icon } from "@hugeicons/core-free-icons";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

const SAVED = [
  { id: 1, label: "Aktiv ausgeliehen", count: 6, color: "var(--brand-pink)" },
  {
    id: 2,
    label: "Chemie · alle Jahrgänge",
    count: 11,
    color: "var(--brand-yellow)",
  },
  { id: 3, label: "Meine Ausleihen", count: 3, color: "var(--brand-teal)" },
];

const AUTHORS = [
  "Larkin, Peter J.",
  "Müller, Hans",
  "Einstein, Albert",
  "Curie, Marie",
  "Feynman, Richard",
];

function FilterGroup({
  label,
  badge,
  defaultOpen = false,
  children,
}: {
  label: string;
  badge?: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Collapsible defaultOpen={defaultOpen} className="border-b">
      <CollapsibleTrigger className="group flex w-full items-center gap-2 px-4 py-2.5 text-left hover:bg-muted/50">
        <HugeiconsIcon
          icon={ArrowDown01Icon}
          size={12}
          className="-rotate-90 text-muted-foreground transition-transform group-data-[state=open]:rotate-0"
        />
        <span className="flex-1 text-xs font-medium text-foreground/80">
          {label}
        </span>
        {badge && (
          <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">
            {badge}
          </span>
        )}
      </CollapsibleTrigger>
      <CollapsibleContent className="px-4 pb-3 pt-1">
        {children}
      </CollapsibleContent>
    </Collapsible>
  );
}

export function BooksFilterPanel({ className }: { className?: string }) {
  const [status, setStatus] = useState("aktiv");
  const [authorQ, setAuthorQ] = useState("");
  const [checkedA, setCheckedA] = useState<Record<string, boolean>>({
    "Larkin, Peter J.": true,
  });
  const filtAuthors = AUTHORS.filter((a) =>
    a.toLowerCase().includes(authorQ.toLowerCase())
  );

  return (
    <aside
      className={cn(
        "flex w-72 shrink-0 flex-col border-l bg-muted/30",
        className
      )}
    >
      <div className="flex h-13 items-center gap-2 border-b bg-background px-4 py-3">
        <span className="text-sm font-bold">Filter</span>
        <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">
          2 aktiv
        </span>
        <div className="flex-1" />
        <button
          type="button"
          className="text-xs font-semibold text-primary hover:underline"
        >
          Löschen
        </button>
      </div>

      <div className="flex-1 overflow-y-auto bg-background">
        <FilterGroup label="★ Gespeicherte Filter" defaultOpen>
          <ul className="space-y-1">
            {SAVED.map((sf) => (
              <li
                key={sf.id}
                className="flex cursor-pointer items-center gap-2.5 rounded-sm px-2 py-1.5 hover:bg-muted/60"
              >
                <span
                  className="size-2 shrink-0 rounded-full"
                  style={{ background: sf.color }}
                />
                <span className="flex-1 text-xs font-medium">{sf.label}</span>
                <span className="rounded-full border bg-muted px-1.5 text-[11px] font-medium text-muted-foreground">
                  {sf.count}
                </span>
              </li>
            ))}
            <li className="cursor-pointer px-2 py-1 text-xs font-semibold text-primary hover:underline">
              + Filter speichern
            </li>
          </ul>
        </FilterGroup>

        <FilterGroup label="Status" badge="1" defaultOpen>
          <RadioGroup
            value={status}
            onValueChange={setStatus}
            className="gap-2"
          >
            {[
              ["alle", "Alle", "234"],
              ["aktiv", "Aktiv", "189"],
              ["inaktiv", "Inaktiv", "45"],
            ].map(([v, l, n]) => (
              <div key={v} className="flex items-center gap-2.5">
                <RadioGroupItem value={v} id={`status-${v}`} />
                <Label
                  htmlFor={`status-${v}`}
                  className="flex-1 cursor-pointer text-xs"
                >
                  {l}
                </Label>
                <span className="rounded-sm border bg-muted px-1.5 text-[10px] text-muted-foreground">
                  {n}
                </span>
              </div>
            ))}
          </RadioGroup>
        </FilterGroup>

        <FilterGroup label="Autor:innen" badge="1" defaultOpen>
          <div className="relative mb-2">
            <HugeiconsIcon
              icon={Search01Icon}
              size={12}
              className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground"
            />
            <Input
              value={authorQ}
              onChange={(e) => setAuthorQ(e.target.value)}
              placeholder="Suchen…"
              className="h-8 pl-7 text-xs"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            {filtAuthors.map((a) => (
              <div key={a} className="flex items-center gap-2.5">
                <Checkbox
                  id={`autor-${a}`}
                  checked={!!checkedA[a]}
                  onCheckedChange={(v) =>
                    setCheckedA((c) => ({ ...c, [a]: v === true }))
                  }
                />
                <Label
                  htmlFor={`autor-${a}`}
                  className="flex-1 cursor-pointer text-xs"
                >
                  {a}
                </Label>
              </div>
            ))}
          </div>
        </FilterGroup>

        <FilterGroup label="Jahr der Veröffentlichung">
          <div className="flex gap-2">
            <Input placeholder="Von" className="h-8 text-xs" />
            <Input placeholder="Bis" className="h-8 text-xs" />
          </div>
        </FilterGroup>
        <FilterGroup label="Signatur">
          <Input placeholder="z.B. NAT-5" className="h-8 text-xs" />
        </FilterGroup>
        <FilterGroup label="Schlüsselworte">
          <Input placeholder="z.B. chemistry" className="h-8 text-xs" />
        </FilterGroup>
        <FilterGroup label="Ausgeliehen von">
          <Input placeholder="Name suchen…" className="h-8 text-xs" />
        </FilterGroup>
      </div>
    </aside>
  );
}
