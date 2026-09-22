import { describe, expect, it, vi, beforeEach } from "vitest";

const error = vi.fn();
vi.mock("sonner", () => ({
  toast: Object.assign(vi.fn(), { error, success: vi.fn(), info: vi.fn() }),
}));

const { toast, ERROR_TOAST_DURATION } = await import("./toast");

/**
 * The defaults every reported failure gets, and the one thing they are for: an error the user has to read
 * (hence the duration and the close button), said once however often it happens (hence the id).
 */
describe("toast.error", () => {
  beforeEach(() => error.mockClear());

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

  it("renews the toast of the same message instead of stacking a second one", () => {
    toast.error("Wert 'Nummer' nicht gegeben.");
    toast.error("Wert 'Nummer' nicht gegeben.");
    toast.error("Rechnung hat keine Positionen.");

    // Same text, same id — sonner updates the toast that is there. A different text is a different
    // message and gets a toast of its own.
    const ids = error.mock.calls.map(([, options]) => options.id);
    expect(ids[0]).toBe(ids[1]);
    expect(ids[2]).not.toBe(ids[0]);
  });

  it("leaves an id the caller chose alone", () => {
    // How a caller keeps several messages of one operation apart, or updates one it owns.
    toast.error("Upload failed", { id: "attachment-upload" });

    expect(error).toHaveBeenCalledWith(
      "Upload failed",
      expect.objectContaining({ id: "attachment-upload" })
    );
  });
});
