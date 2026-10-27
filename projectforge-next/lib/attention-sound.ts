// A short attention signal (three rising tones), made by Web Audio: no audio file, nothing for the Content
// Security Policy. Browsers allow sound only after the user interacted with the page (autoplay policy): the
// AudioContext is created or resumed by a click or key press on the page, see unlockAttentionSound.

const STORAGE_KEY = "pf.chat.secretSound";

let context: AudioContext | null = null;

function audioContext(): AudioContext | null {
  if (context) return context;
  if (typeof window === "undefined" || !window.AudioContext) return null;
  try {
    context = new AudioContext();
  } catch {
    return null;
  }
  return context;
}

/**
 * Unlocks the sound with the interactions with the page: a context made or resumed in a click or key press may
 * play, one without stays suspended (a reload resets this, a page nobody touched since is silent). Done on every
 * interaction, not only the first, as a browser may suspend the context again. Returns the cleanup of the listeners.
 */
export function unlockAttentionSound(): () => void {
  const unlock = () => {
    const ctx = audioContext();
    if (ctx && ctx.state !== "running") void ctx.resume().catch(() => {});
  };
  window.addEventListener("pointerdown", unlock, true);
  window.addEventListener("keydown", unlock, true);
  return () => {
    window.removeEventListener("pointerdown", unlock, true);
    window.removeEventListener("keydown", unlock, true);
  };
}

/** The notes of the signal (Hz): three rising tones, a bell-like decay each. */
const NOTES = [660, 880, 1100];
const NOTE_START_S = 0.22;
const NOTE_DECAY_S = 0.4;
const PEAK_GAIN = 0.5;

/** Plays the signal, if switched on and allowed by the browser; silently nothing otherwise. */
export function playAttentionSound(): void {
  if (!isAttentionSoundOn()) return;
  const ctx = audioContext();
  if (!ctx) return;
  // A page with an interaction since it was loaded may resume; the sound then starts a moment later.
  if (ctx.state !== "running") {
    void ctx
      .resume()
      .then(() => ctx.state === "running" && playNotes(ctx))
      .catch(() => {});
    return;
  }
  playNotes(ctx);
}

function playNotes(ctx: AudioContext): void {
  const start = ctx.currentTime;
  NOTES.forEach((frequency, i) => {
    const at = start + i * NOTE_START_S;
    const oscillator = ctx.createOscillator();
    const gain = ctx.createGain();
    oscillator.type = "sine";
    oscillator.frequency.value = frequency;
    // Fades in and out: a hard start or stop clicks.
    gain.gain.setValueAtTime(0, at);
    gain.gain.linearRampToValueAtTime(PEAK_GAIN, at + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.001, at + NOTE_DECAY_S);
    oscillator.connect(gain).connect(ctx.destination);
    oscillator.start(at);
    oscillator.stop(at + NOTE_DECAY_S + 0.02);
  });
}

/** The sound is a setting of the browser (on by default), not of the user. */
export function isAttentionSoundOn(): boolean {
  try {
    return window.localStorage.getItem(STORAGE_KEY) !== "off";
  } catch {
    return true;
  }
}

export function setAttentionSoundOn(on: boolean): void {
  try {
    if (on) window.localStorage.removeItem(STORAGE_KEY);
    else window.localStorage.setItem(STORAGE_KEY, "off");
  } catch {
    // Private mode without storage: stays on.
  }
}
