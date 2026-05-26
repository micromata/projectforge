"use client";

import { useState } from "react";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/shared/app-sidebar";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { BooksTable } from "@/components/features/books/books-table";
import { BooksToolbar } from "@/components/features/books/books-toolbar";
import { BooksFilterPanel } from "@/components/features/books/books-filter-panel";
import { BOOKS } from "@/components/features/books/mock-data";

export default function BooksPage() {
  const [search, setSearch] = useState("");
  const [sortCol, setSortCol] = useState<string | null>(null);
  const [filters, setFilters] = useState([
    { key: "status", label: "Status: Aktiv" },
    { key: "autor", label: "Autor: Larkin" },
  ]);

  const filtered = BOOKS.filter((b) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      b.titel.toLowerCase().includes(q) ||
      b.autor.toLowerCase().includes(q) ||
      b.sig.toLowerCase().includes(q)
    );
  });

  return (
    <div className="flex h-full min-h-screen flex-col">
      <BrandStripe />
      <SidebarProvider className="flex flex-1 overflow-hidden">
        <AppSidebar />
        <SidebarInset className="flex flex-1 flex-col overflow-hidden">
          <BooksToolbar
            search={search}
            onSearch={setSearch}
            filters={filters}
            onRemove={(k) => setFilters((f) => f.filter((x) => x.key !== k))}
            onClearAll={() => setFilters([])}
          />
          <div className="flex flex-1 overflow-hidden">
            <main className="flex-1 overflow-auto bg-background">
              <BooksTable
                data={filtered}
                sortCol={sortCol}
                onSort={(c) => setSortCol((s) => (s === c ? null : c))}
              />
              <div className="flex items-center justify-between border-t px-4 py-2">
                <span className="text-xs font-medium text-muted-foreground">
                  1–50 von 234 Einträgen
                </span>
                <div className="flex items-center gap-1">
                  {["←", "1", "2", "3", "…", "5", "→"].map((p, i) => (
                    <button
                      key={`${p}-${i}`}
                      type="button"
                      className={
                        i === 1
                          ? "h-7 min-w-7 rounded-sm border border-primary bg-primary px-2 text-xs font-bold text-primary-foreground"
                          : "h-7 min-w-7 rounded-sm border bg-background px-2 text-xs font-medium text-muted-foreground hover:bg-muted"
                      }
                    >
                      {p}
                    </button>
                  ))}
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted-foreground">
                    Einträge / Seite:
                  </span>
                  <select className="h-7 rounded-sm border bg-background px-2 text-xs">
                    <option>25</option>
                    <option defaultValue="50">50</option>
                    <option>100</option>
                  </select>
                </div>
              </div>
            </main>
            <BooksFilterPanel className="hidden lg:flex" />
          </div>
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}
