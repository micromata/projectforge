"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useMenu } from "@/hooks/use-menu";
import { useAuth } from "@/hooks/use-auth";
import { logout } from "@/lib/rs/client";
import type { MenuItem } from "@/lib/rs/types";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function TopNavigation() {
  const { data: menu } = useMenu();
  const { user } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();

  async function handleLogout() {
    await logout();
    queryClient.clear();
    router.push("/login");
  }

  return (
    <nav className="flex h-12 items-center border-b bg-background px-4 gap-2">
      <CategoriesDropdown categories={menu?.mainMenu?.menuItems ?? []} />
      <FavoritesBar items={menu?.favoritesMenu?.menuItems ?? []} />
      <div className="ml-auto">
        <UserMenu
          items={menu?.myAccountMenu?.menuItems ?? []}
          username={user?.fullname ?? user?.username ?? ""}
          onLogout={handleLogout}
        />
      </div>
    </nav>
  );
}

function CategoriesDropdown({ categories }: { categories: MenuItem[] }) {
  const [open, setOpen] = useState(false);

  if (categories.length === 0) return null;

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="sm" className="gap-1.5">
          <MenuIcon className="h-4 w-4" />
          <span className="hidden sm:inline">Menü</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-[600px] max-h-[70vh] overflow-y-auto">
        <div className="grid grid-cols-2 gap-4 p-3 sm:grid-cols-3 lg:grid-cols-4">
          {categories.map((category) => (
            <CategoryColumn
              key={category.id ?? category.title}
              category={category}
              onSelect={() => setOpen(false)}
            />
          ))}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function CategoryColumn({
  category,
  onSelect,
}: {
  category: MenuItem;
  onSelect: () => void;
}) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="px-2 py-1 text-xs font-semibold text-muted-foreground uppercase tracking-wide">
        {category.title}
      </span>
      {category.subMenu?.map((item) => (
        <Link
          key={item.key ?? item.url ?? item.title}
          href={item.url ?? "#"}
          onClick={onSelect}
          className="rounded-sm px-2 py-1 text-sm hover:bg-accent hover:text-accent-foreground transition-colors"
        >
          {item.title}
          {item.badge?.counter ? (
            <span className="ml-1.5 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-xs text-primary-foreground">
              {item.badge.counter}
            </span>
          ) : null}
        </Link>
      ))}
    </div>
  );
}

function FavoritesBar({ items }: { items: MenuItem[] }) {
  if (items.length === 0) return null;

  return (
    <div className="hidden md:flex items-center gap-1">
      {items.map((item) => (
        <Button key={item.key ?? item.url ?? item.title} variant="ghost" size="sm" asChild>
          <Link href={item.url ?? "#"}>{item.title}</Link>
        </Button>
      ))}
    </div>
  );
}

function UserMenu({
  items,
  username,
  onLogout,
}: {
  items: MenuItem[];
  username: string;
  onLogout: () => void;
}) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="sm">
          {username}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {items.map((item) => {
          if (item.url === "/logout" || item.key === "LOGOUT") {
            return (
              <DropdownMenuItem key="logout" onSelect={onLogout}>
                {item.title}
              </DropdownMenuItem>
            );
          }
          return (
            <DropdownMenuItem key={item.key ?? item.url ?? item.title} asChild>
              <Link href={item.url ?? "#"}>{item.title}</Link>
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function MenuIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <line x1="3" y1="6" x2="21" y2="6" />
      <line x1="3" y1="12" x2="21" y2="12" />
      <line x1="3" y1="18" x2="21" y2="18" />
    </svg>
  );
}
