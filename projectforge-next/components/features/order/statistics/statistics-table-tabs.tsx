"use client";

import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export interface StatisticsTableTab {
  value: string;
  label: string;
  /** The number of rows, shown behind the label; left out for a table without a meaningful count. */
  count?: number;
  content: React.ReactNode;
}

/**
 * The sub-tabs of the data tables under the charts of the order statistics, one per sheet of the Excel
 * export. Only the open tab is mounted, so a long table costs nothing while another one is shown.
 */
export function StatisticsTableTabs({ tabs }: { tabs: StatisticsTableTab[] }) {
  const [tab, setTab] = useState(tabs[0]?.value);
  return (
    <Tabs value={tab} onValueChange={setTab} className="space-y-2">
      <TabsList className="h-auto w-fit max-w-full flex-wrap justify-start">
        {tabs.map((it) => (
          <TabsTrigger key={it.value} value={it.value}>
            {it.label}
            {it.count != null && (
              <span className="text-muted-foreground tabular-nums">
                ({it.count})
              </span>
            )}
          </TabsTrigger>
        ))}
      </TabsList>
      {tabs.map((it) => (
        <TabsContent key={it.value} value={it.value}>
          {it.content}
        </TabsContent>
      ))}
    </Tabs>
  );
}
