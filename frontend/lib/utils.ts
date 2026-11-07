import { clsx, type ClassValue } from "clsx"
import { extendTailwindMerge } from "tailwind-merge"

// the yard ops type scale and shadows from globals.css, otherwise e.g. "text-body" counts as a color and gets dropped next to "text-ink"
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: ["kpi", "heading", "title", "body", "label", "caption"],
      shadow: ["card", "float"],
    },
  },
})

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
