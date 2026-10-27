import type { EmojiPickerRootProps } from "frimousse";

type Resolver = NonNullable<EmojiPickerRootProps["resolveEmojiData"]>;

const SKIN_TONES = [
  "light",
  "medium-light",
  "medium",
  "medium-dark",
  "dark",
] as const;

/** Newest emoji version the usual operating systems render; newer ones would show as empty boxes. */
const MAX_EMOJI_VERSION = 15;

interface RawEmoji {
  emoji: string;
  label: string;
  group?: number;
  subgroup?: number;
  version: number;
  tags?: string[];
  skins?: { emoji: string; tone?: number | number[] }[];
}

interface RawMessages {
  groups: { key: string; order: number; message: string }[];
  subgroups: { key: string; order: number }[];
  skinTones: { key: string; message: string }[];
}

const capitalize = (text: string) =>
  text.charAt(0).toUpperCase() + text.slice(1);

/**
 * The emoji catalog from the `emojibase-data` package, bundled as a lazy chunk of the app: the default of
 * frimousse loads it from a CDN, which the Content Security Policy does not allow. The chunk is fetched only when
 * the full picker is opened, once per language.
 */
export const resolveEmojiData: Resolver = async (locale) => {
  const language = locale === "de" ? "de" : "en";
  const [data, messages] = await Promise.all([
    language === "de"
      ? import("emojibase-data/de/data.json")
      : import("emojibase-data/en/data.json"),
    language === "de"
      ? import("emojibase-data/de/messages.json")
      : import("emojibase-data/en/messages.json"),
  ]);
  const raw = data.default as unknown as RawEmoji[];
  const msg = messages.default as unknown as RawMessages;
  const flagSubgroups = new Set(
    msg.subgroups
      .filter(
        (it) => it.key === "country-flag" || it.key === "subdivision-flag"
      )
      .map((it) => it.order)
  );
  return {
    locale: language,
    categories: msg.groups
      .filter((it) => it.key !== "component")
      .map((it) => ({ index: it.order, label: capitalize(it.message) })),
    skinTones: Object.fromEntries(
      msg.skinTones.map((it) => [it.key, capitalize(it.message)])
    ) as ReturnType<Resolver> extends Promise<infer R>
      ? R extends { skinTones: infer S }
        ? S
        : never
      : never,
    emojis: raw
      .filter(
        (it) =>
          it.group !== undefined &&
          it.version <= MAX_EMOJI_VERSION &&
          // Flags render as two letters on Windows.
          !(it.subgroup !== undefined && flagSubgroups.has(it.subgroup))
      )
      .map((it) => {
        const skins = it.skins?.filter((skin) => typeof skin.tone === "number");
        return {
          emoji: it.emoji,
          category: it.group as number,
          version: it.version,
          label: capitalize(it.label),
          tags: it.tags ?? [],
          skins: skins?.length
            ? (Object.fromEntries(
                skins.map((skin) => [
                  SKIN_TONES[(skin.tone as number) - 1],
                  skin.emoji,
                ])
              ) as Record<(typeof SKIN_TONES)[number], string>)
            : undefined,
        };
      }),
  };
};
