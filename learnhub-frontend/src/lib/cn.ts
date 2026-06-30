/**
 * Tiny classnames joiner (no external deps).
 * Filters out falsy values and joins with a space.
 */
export type ClassValue = string | number | false | null | undefined;

export function cn(...classes: ClassValue[]): string {
  return classes.filter(Boolean).join(' ');
}
