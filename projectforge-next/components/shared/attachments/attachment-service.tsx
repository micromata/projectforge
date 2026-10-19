"use client";

import { createContext, useContext, type ReactNode } from "react";
import {
  DEFAULT_ATTACHMENT_SERVICE,
  type AttachmentService,
} from "@/lib/rs/attachments";

const AttachmentServiceContext = createContext<AttachmentService>(
  DEFAULT_ATTACHMENT_SERVICE
);

/**
 * Points every attachment component and hook below it to other endpoints than
 * `AttachmentsServicesRest` — the external access of a data transfer area. Without a provider they
 * use the default, so the lists of the entities need none.
 */
export function AttachmentServiceProvider({
  service,
  children,
}: {
  service: AttachmentService;
  children: ReactNode;
}) {
  return (
    <AttachmentServiceContext.Provider value={service}>
      {children}
    </AttachmentServiceContext.Provider>
  );
}

export function useAttachmentService(): AttachmentService {
  return useContext(AttachmentServiceContext);
}
