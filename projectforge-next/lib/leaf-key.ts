/**
 * A backend message key that is both a text and a namespace.
 *
 * `fibu.rechnung.kostExcelExport` is a label of its own *and* the parent of
 * `fibu.rechnung.kostExcelExport.tooltip`; `fibu.kost1` is a text and the parent of a dozen keys. JSON
 * cannot hold a string and an object under one name, so the generator exports the text as `<key>._`
 * (see GenerateNextI18nMessagesMain). Asking `next-intl` for the bare key then throws
 * `INSUFFICIENT_PATH` — the value it finds is an object.
 *
 * Resolved at the call site rather than in the catalogue: which keys collide follows from
 * `I18nResources.properties` and changes with it, so a frontend that spelled out `…kostExcelExport._`
 * would break the day the child key is removed and the collision is gone.
 *
 * @param hasMessage `t.has` of the translator, i.e. whether the catalogue holds that key.
 */
export function leafKeyOf(
  key: string,
  hasMessage: (key: string) => boolean
): string {
  return hasMessage(`${key}._`) ? `${key}._` : key;
}
