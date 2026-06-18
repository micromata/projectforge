"use client";

import { useCallback, useRef, useState } from "react";

interface UseScrollSpyOptions {
  offset?: number;
  scrollInset?: number;
}

export function useScrollSpy(
  sectionCount: number,
  opts?: UseScrollSpyOptions
) {
  const offset = opts?.offset ?? 80;
  const scrollInset = opts?.scrollInset ?? 6;

  const scrollRef = useRef<HTMLDivElement | null>(null);
  const sectionRefs = useRef<(HTMLDivElement | null)[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);

  const sectionRef = useCallback(
    (index: number) => (el: HTMLDivElement | null) => {
      sectionRefs.current[index] = el;
    },
    []
  );

  const onScroll = useCallback(() => {
    const container = scrollRef.current;
    if (!container) return;
    const scrollTop = container.scrollTop;
    let active = 0;
    for (let i = 0; i < sectionCount; i++) {
      const el = sectionRefs.current[i];
      if (el && el.offsetTop - offset <= scrollTop) {
        active = i;
      }
    }
    setActiveIndex(active);
  }, [sectionCount, offset]);

  const scrollToSection = useCallback(
    (index: number) => {
      setActiveIndex(index);
      const el = sectionRefs.current[index];
      if (el && scrollRef.current) {
        scrollRef.current.scrollTop = el.offsetTop - scrollInset;
      }
    },
    [scrollInset]
  );

  return { scrollRef, sectionRef, activeIndex, scrollToSection, onScroll };
}
