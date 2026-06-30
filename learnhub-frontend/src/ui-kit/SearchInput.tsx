import React, { useEffect, useState } from 'react';
import { cn } from '../lib/cn';

export interface SearchInputProps {
  value?: string;
  onSearch: (query: string) => void;
  placeholder?: string;
  debounceMs?: number;
  className?: string;
}

/**
 * Debounced search box. Fires onSearch after the user stops typing.
 */
export const SearchInput: React.FC<SearchInputProps> = ({
  value,
  onSearch,
  placeholder = 'Search…',
  debounceMs = 300,
  className,
}) => {
  const [query, setQuery] = useState(value ?? '');

  useEffect(() => {
    const t = setTimeout(() => onSearch(query), debounceMs);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, debounceMs]);

  return (
    <div className={cn('relative', className)}>
      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
        ⌕
      </span>
      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-lg border border-gray-300 py-2 pl-9 pr-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
      />
    </div>
  );
};

export default SearchInput;
