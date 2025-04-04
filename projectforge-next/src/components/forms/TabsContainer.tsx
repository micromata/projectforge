"use client";

import React from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

interface Tab {
  id: string;
  label: string;
  content: React.ReactNode;
}

interface TabsContainerProps {
  tabs: Tab[];
  defaultTab?: string;
  className?: string;
}

export default function TabsContainer({
  tabs,
  defaultTab,
  className = "",
}: TabsContainerProps) {
  // If no default tab is provided, use the first tab
  const defaultTabId = defaultTab || (tabs.length > 0 ? tabs[0].id : "");

  return (
    <Tabs defaultValue={defaultTabId} className={`w-full ${className}`}>
      <TabsList className="mb-4">
        {tabs.map((tab) => (
          <TabsTrigger key={tab.id} value={tab.id}>
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>

      {tabs.map((tab) => (
        <TabsContent key={tab.id} value={tab.id}>
          {tab.content}
        </TabsContent>
      ))}
    </Tabs>
  );
}