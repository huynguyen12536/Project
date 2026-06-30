import React, { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
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
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9CA3AF]" />
      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-xl border border-[#E5E7EB] bg-white py-2.5 pl-9 pr-3 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
      />
    </div>
  );
};

export default SearchInput;
