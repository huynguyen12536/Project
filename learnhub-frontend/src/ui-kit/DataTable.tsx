import React, { useMemo, useState } from 'react';
import { cn } from '../lib/cn';

export interface Column<T> {
  key: string;
  header: string;
  /** Value accessor; defaults to row[key]. */
  accessor?: (row: T) => React.ReactNode;
  /** Comparable value for sorting; defaults to the accessor/raw value. */
  sortValue?: (row: T) => string | number;
  sortable?: boolean;
  className?: string;
}

export interface DataTableProps<T> {
  data: T[];
  columns: Column<T>[];
  rowKey: (row: T) => string;
  /** Enables checkbox selection column. */
  selectable?: boolean;
  selectedIds?: string[];
  onSelectionChange?: (ids: string[]) => void;
  /** Rendered above the table when there is a selection. */
  bulkActions?: React.ReactNode;
  emptyMessage?: string;
  className?: string;
}

type SortDir = 'asc' | 'desc' | null;

export function DataTable<T>({
  data,
  columns,
  rowKey,
  selectable = false,
  selectedIds = [],
  onSelectionChange,
  bulkActions,
  emptyMessage = 'No data',
  className,
}: DataTableProps<T>) {
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<SortDir>(null);

  const sorted = useMemo(() => {
    if (!sortKey || !sortDir) return data;
    const col = columns.find((c) => c.key === sortKey);
    if (!col) return data;
    const getVal = (row: T): string | number => {
      if (col.sortValue) return col.sortValue(row);
      const v = (row as Record<string, unknown>)[col.key];
      return typeof v === 'number' ? v : String(v ?? '');
    };
    return [...data].sort((a, b) => {
      const av = getVal(a);
      const bv = getVal(b);
      if (av < bv) return sortDir === 'asc' ? -1 : 1;
      if (av > bv) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
  }, [data, columns, sortKey, sortDir]);

  const toggleSort = (key: string) => {
    if (sortKey !== key) {
      setSortKey(key);
      setSortDir('asc');
    } else {
      setSortDir((d) => (d === 'asc' ? 'desc' : d === 'desc' ? null : 'asc'));
      if (sortDir === 'desc') setSortKey(null);
    }
  };

  const allIds = sorted.map(rowKey);
  const allSelected = selectable && allIds.length > 0 && allIds.every((id) => selectedIds.includes(id));

  const toggleAll = () => {
    if (!onSelectionChange) return;
    onSelectionChange(allSelected ? [] : allIds);
  };

  const toggleOne = (id: string) => {
    if (!onSelectionChange) return;
    onSelectionChange(
      selectedIds.includes(id) ? selectedIds.filter((x) => x !== id) : [...selectedIds, id]
    );
  };

  return (
    <div className={cn('w-full', className)}>
      {selectable && selectedIds.length > 0 && bulkActions && (
        <div className="mb-2 flex items-center gap-3 rounded-lg bg-primary-50 px-3 py-2 text-sm">
          <span className="font-medium text-primary-700">{selectedIds.length} selected</span>
          {bulkActions}
        </div>
      )}
      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="w-full text-left text-sm">
          <thead className="bg-gray-50 text-gray-600">
            <tr>
              {selectable && (
                <th className="w-10 px-3 py-2">
                  <input type="checkbox" checked={allSelected} onChange={toggleAll} aria-label="Select all" />
                </th>
              )}
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={cn('px-3 py-2 font-medium', col.sortable && 'cursor-pointer select-none', col.className)}
                  onClick={() => col.sortable && toggleSort(col.key)}
                >
                  <span className="inline-flex items-center gap-1">
                    {col.header}
                    {col.sortable && sortKey === col.key && (
                      <span aria-hidden>{sortDir === 'asc' ? '▲' : sortDir === 'desc' ? '▼' : ''}</span>
                    )}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {sorted.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length + (selectable ? 1 : 0)}
                  className="px-3 py-6 text-center text-gray-400"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              sorted.map((row) => {
                const id = rowKey(row);
                return (
                  <tr key={id} className="hover:bg-gray-50">
                    {selectable && (
                      <td className="px-3 py-2">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(id)}
                          onChange={() => toggleOne(id)}
                          aria-label={`Select row ${id}`}
                        />
                      </td>
                    )}
                    {columns.map((col) => (
                      <td key={col.key} className={cn('px-3 py-2 text-gray-700', col.className)}>
                        {col.accessor
                          ? col.accessor(row)
                          : String((row as Record<string, unknown>)[col.key] ?? '')}
                      </td>
                    ))}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default DataTable;
