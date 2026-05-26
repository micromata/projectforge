"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  BookIcon,
  Calendar01Icon,
  ChartBarLineIcon,
  CheckmarkSquare02Icon,
  Clock01Icon,
  Exchange01Icon,
  FolderTreeIcon,
  Location01Icon,
  ArrowDown01Icon,
} from "@hugeicons/core-free-icons";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar";
import { MicromataIcon } from "@/components/shared/micromata-icon";

const projectMgmt = [
  {
    id: "buecher",
    label: "Bücherliste",
    icon: BookIcon,
    href: "/books",
    active: true,
  },
  { id: "zeit", label: "Zeiterfassung", icon: Clock01Icon, href: "#" },
  { id: "aufg", label: "Aufgaben", icon: CheckmarkSquare02Icon, href: "#" },
  { id: "gantt", label: "Gantt-Diagramm", icon: ChartBarLineIcon, href: "#" },
];

const topLevel = [
  { id: "struktur", label: "Strukturbaum", icon: FolderTreeIcon, href: "#" },
  { id: "kalender", label: "Kalender", icon: Calendar01Icon, href: "#" },
  { id: "adressen", label: "Adressen", icon: Location01Icon, href: "#" },
  { id: "transfer", label: "Datentransfer", icon: Exchange01Icon, href: "#" },
];

export function AppSidebar() {
  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <div className="flex items-center gap-2.5 px-2 py-1.5">
          <MicromataIcon size={28} />
          <div className="flex min-w-0 flex-col group-data-[collapsible=icon]:hidden">
            <span className="text-sm font-bold leading-tight tracking-tight text-sidebar-foreground">
              ProjectForge
            </span>
            <span className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
              by Micromata
            </span>
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Projektmanagement</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {projectMgmt.map((item) => (
                <SidebarMenuItem key={item.id}>
                  <SidebarMenuSub>
                    <SidebarMenuSubItem>
                      <SidebarMenuSubButton asChild isActive={item.active}>
                        <a href={item.href}>
                          <HugeiconsIcon icon={item.icon} size={15} />
                          <span>{item.label}</span>
                        </a>
                      </SidebarMenuSubButton>
                    </SidebarMenuSubItem>
                  </SidebarMenuSub>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {topLevel.map((item) => (
                <SidebarMenuItem key={item.id}>
                  <SidebarMenuButton asChild tooltip={item.label}>
                    <a href={item.href}>
                      <HugeiconsIcon icon={item.icon} size={16} />
                      <span>{item.label}</span>
                    </a>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" tooltip="Fin Reinhard">
              <div
                className="flex size-8 shrink-0 items-center justify-center rounded-full text-xs font-bold text-primary-foreground"
                style={{
                  background:
                    "linear-gradient(135deg, var(--brand-teal), var(--brand-teal-dark))",
                  boxShadow: "0 0 0 2px rgba(0,155,163,0.22)",
                }}
              >
                FR
              </div>
              <div className="flex min-w-0 flex-1 flex-col">
                <span className="truncate text-sm font-semibold">
                  Fin Reinhard
                </span>
                <span className="truncate text-[11px] text-muted-foreground">
                  Administrator
                </span>
              </div>
              <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
