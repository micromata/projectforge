"use client";

import { useQuery } from "@tanstack/react-query";
import {
  fetchNotificationSmsInfo,
  type NotificationSmsInfo,
} from "@/lib/rs/notification";

/** Is SMS configured, and the max length of an SMS (undefined while loading). */
export function useSmsInfo(): NotificationSmsInfo | undefined {
  const { data } = useQuery({
    queryKey: ["notificationRule", "smsInfo"],
    queryFn: ({ signal }) => fetchNotificationSmsInfo(signal),
    staleTime: Infinity,
  });
  return data;
}
