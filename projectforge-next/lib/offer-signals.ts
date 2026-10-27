// Which offers have been signalled (sound, notification) already, in any tab of this browser: a reload or a second
// tab mustn't ring again for an offer the user has heard. The ids are kept in localStorage, the last few only.

const STORAGE_KEY = "pf.chat.secretSignalled";
const KEEP = 20;

/** True if the offer is not signalled yet, and marks it as signalled. True as well where storage is unavailable. */
export function claimOfferSignal(offerId: string): boolean {
  try {
    const seen: string[] = JSON.parse(
      window.localStorage.getItem(STORAGE_KEY) ?? "[]"
    );
    if (seen.includes(offerId)) return false;
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify([...seen, offerId].slice(-KEEP))
    );
    return true;
  } catch {
    return true;
  }
}
