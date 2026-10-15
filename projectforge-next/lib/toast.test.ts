import { describe, expect, it, vi, beforeEach } from "vitest";

/** What sonner has on screen: `error` opens a toast, `dismiss` takes it away, `getToasts` lists them. */
const shown: { id: number; title: string }[] = [];
let nextId = 0;
const error = vi.fn((title: string) => {
  const id = ++nextId;
  shown.push({ id, title });
  return id;
});
const dismiss = vi.fn((id: number) => {
  const index = shown.findIndex((toast) => toast.id === id);
  if (index >= 0) shown.splice(index, 1);
});
vi.mock("sonner", () => ({
  toast: Object.assign(vi.fn(), {
    error,
    dismiss,
    getToasts: () => [...shown],
    success: vi.fn(),
    info: vi.fn(),
  }),
}));

const { toast, ERROR_TOAST_DURATION } = await import("./toast");

/**
 * The defaults every reported failure gets, and the one thing they are for: an error the user has to read
 * (hence the duration and the close button), said once however often it happens (hence the standing copy
 * of the same text is dismissed before the new one opens).
 */
describe("toast.error", () => {
  beforeEach(() => {
    error.mockClear();
    dismiss.mockClear();
    shown.length = 0;
  });

  it("stays long enough to be read and can be closed", () => {
    toast.error("Wert 'Nummer' nicht gegeben.");

    expect(error).toHaveBeenCalledWith(
      "Wert 'Nummer' nicht gegeben.",
      expect.objectContaining({
        duration: ERROR_TOAST_DURATION,
        closeButton: true,
      })
    );
  });

  it("replaces the standing toast of the same message instead of stacking a second one", () => {
    toast.error("Wert 'Nummer' nicht gegeben.");
    const first = shown[0].id;
    toast.error("Wert 'Nummer' nicht gegeben.");
    toast.error("Rechnung hat keine Positionen.");

    // The first copy was dismissed for the second, so that text is on screen once — and on top. A
    // different text is a different message and keeps a toast of its own.
    expect(dismiss).toHaveBeenCalledTimes(1);
    expect(dismiss).toHaveBeenCalledWith(first);
    expect(shown.map((toast) => toast.title)).toEqual([
      "Wert 'Nummer' nicht gegeben.",
      "Rechnung hat keine Positionen.",
    ]);
  });

  it("opens a fresh toast once the previous one is gone", () => {
    toast.error("Wert 'Nummer' nicht gegeben.");
    shown.length = 0; // closed by the user, or timed out
    toast.error("Wert 'Nummer' nicht gegeben.");

    expect(dismiss).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalledTimes(2);
  });

  it("leaves an id the caller chose alone", () => {
    // How a caller keeps several messages of one operation apart, or updates one it owns.
    toast.error("Upload failed", { id: "attachment-upload" });
    toast.error("Upload failed", { id: "attachment-upload" });

    expect(dismiss).not.toHaveBeenCalled();
    expect(error).toHaveBeenLastCalledWith(
      "Upload failed",
      expect.objectContaining({ id: "attachment-upload" })
    );
  });
});
