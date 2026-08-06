"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useMenu } from "@/hooks/use-menu";
import { useAuth } from "@/hooks/use-auth";
import { logout } from "@/lib/rs/client";
import { MainMenuDropdown } from "@/components/shared/main-menu-dropdown";
import { FavoritesBar } from "@/components/shared/favorites-bar";
import { UserMenu } from "@/components/shared/user-menu";

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
    <nav className="flex h-12 items-center gap-2 border-b bg-background px-4">
      <MainMenuDropdown categories={menu?.mainMenu?.menuItems ?? []} />
      <FavoritesBar items={menu?.favoritesMenu?.menuItems ?? []} />
      {/* ml-auto keeps the user menu right-aligned even when there are no favourites at all. */}
      <div className="ml-auto shrink-0">
        <UserMenu
          items={menu?.myAccountMenu?.menuItems ?? []}
          username={user?.fullname ?? user?.username ?? ""}
          onLogout={handleLogout}
        />
      </div>
    </nav>
  );
}
