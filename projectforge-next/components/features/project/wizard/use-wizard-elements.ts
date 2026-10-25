import { useState } from "react";
import type { ElementMode } from "./types";

/** The choice of one structure element, see WizardElementPick. */
export interface ElementChoice {
  mode: ElementMode;
  taskId: number | null;
}

const NEW: ElementChoice = { mode: "new", taskId: null };

/**
 * The structure elements of customer and project as the user chose them: new ones (by default), or
 * existing ones picked from the tree. What the request takes of them depends on the rest of the wizard.
 */
export function useWizardElements() {
  const [customerElement, setCustomerElement] = useState<ElementChoice>(NEW);
  const [projectElement, setProjectElement] = useState<ElementChoice>(NEW);
  const reset = () => {
    setCustomerElement(NEW);
    setProjectElement(NEW);
  };
  return {
    customerElement,
    setCustomerElement,
    projectElement,
    setProjectElement,
    reset,
  };
}
