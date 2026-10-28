"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";

interface Props {
  busy: boolean;
  onParseText: (text: string) => void;
  onParseVcf: (file: File) => void;
}

/**
 * What the text parser reads: a pasted text (an e-mail signature, a business card) or a dropped vCard file.
 */
export function AddressTextParserInput({
  busy,
  onParseText,
  onParseVcf,
}: Props) {
  const t = useTranslations();
  const [text, setText] = useState("");
  return (
    <div className="grid gap-4 md:grid-cols-[2fr_1fr]">
      <div className="flex flex-col gap-2">
        <label htmlFor="address-parse-text" className="text-sm font-medium">
          {t("address.parseText.inputLabel")}
        </label>
        <Textarea
          id="address-parse-text"
          rows={7}
          value={text}
          placeholder={t("address.parseText.inputPlaceholder")}
          onChange={(event) => setText(event.target.value)}
        />
        <div>
          <Button
            type="button"
            variant="outline"
            disabled={busy}
            onClick={() => onParseText(text)}
          >
            {t("parse")}
          </Button>
        </div>
      </div>
      <div className="flex flex-col gap-2">
        <span className="text-sm font-medium">
          {t("address.book.vCardsImport.dropLabel")}
        </span>
        <AttachmentDropArea
          onFiles={(files) => files[0] && onParseVcf(files[0])}
          accept=".vcf"
          multiple={false}
          disabled={busy}
          label={t("address.book.vCardsImport.dropHint")}
        />
        <p className="text-xs text-muted-foreground">
          {t("address.book.vCardsImport.dropInfo")}
        </p>
      </div>
    </div>
  );
}
