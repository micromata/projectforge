const KEY = "pf.chat.recentEmojis";
const MAX = 16;

/** The emoji the user reacted with lately (quick choice, MSG-10), kept in the browser's local storage. */
export const recentEmojis = {
  get(): string[] {
    try {
      const value = JSON.parse(
        window.localStorage.getItem(KEY) ?? "[]"
      ) as unknown;
      return Array.isArray(value)
        ? value.filter((it): it is string => typeof it === "string")
        : [];
    } catch {
      return [];
    }
  },
  add(emoji: string) {
    try {
      const list = [
        emoji,
        ...recentEmojis.get().filter((it) => it !== emoji),
      ].slice(0, MAX);
      window.localStorage.setItem(KEY, JSON.stringify(list));
    } catch {
      // Storage disabled: no quick choice then.
    }
  },
};
