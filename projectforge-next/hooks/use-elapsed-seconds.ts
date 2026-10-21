import { useEffect, useState } from "react";

/** Seconds since `running` became true, 0 while not running. */
export function useElapsedSeconds(running: boolean): number {
  const [elapsed, setElapsed] = useState(0);
  useEffect(() => {
    if (!running) return;
    const start = Date.now();
    const timer = window.setInterval(
      () => setElapsed(Math.floor((Date.now() - start) / 1000)),
      1_000
    );
    return () => {
      window.clearInterval(timer);
      setElapsed(0);
    };
  }, [running]);
  return elapsed;
}
