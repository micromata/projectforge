"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useMenu } from "@/hooks/use-menu";
import { useAuth } from "@/hooks/use-auth";
import { useThemeSync } from "@/hooks/use-theme-sync";
import { logout } from "@/lib/rs/client";
import { Menubar } from "@/components/ui/menubar";
import { MainMenuDropdown } from "@/components/shared/main-menu-dropdown";
import { QuickAccessSearch } from "@/components/shared/quick-access-search";
import { FavoritesBar } from "@/components/shared/favorites-bar";
import { UserMenu } from "@/components/shared/user-menu";

export function TopNavigation() {
  const { data: menu } = useMenu();
  const { user } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  // Apply the user's server-stored light/dark choice once they're authenticated.
  useThemeSync();

  async function handleLogout() {
    await logout();
    queryClient.clear();
    router.push("/login");
  }

  return (
    // One Menubar around all menus: that is what lets a single click switch from an open menu to
    // another one, instead of the first click only closing what was open.
    <Menubar
      asChild
      className="h-12 gap-2 rounded-none border-x-0 border-t-0 bg-background px-4"
    >
      <nav>
        <MainMenuDropdown
          categories={menu?.mainMenu?.menuItems ?? []}
          badge={menu?.mainMenu?.badge}
        />
        <QuickAccessSearch />
        <FavoritesBar items={menu?.favoritesMenu?.menuItems ?? []} />
        {/* ml-auto keeps the user menu right-aligned even when there are no favourites at all. */}
        <div className="ml-auto flex shrink-0 items-center">
          <UserMenu
            items={menu?.myAccountMenu?.menuItems ?? []}
            username={user?.fullname ?? user?.username ?? ""}
            onLogout={handleLogout}
          />
        </div>
      </nav>
    </Menubar>
  );
}
