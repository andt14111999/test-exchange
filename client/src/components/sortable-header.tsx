import { ArrowDownIcon, ArrowUpIcon } from "lucide-react";
import { TableHead } from "@/components/ui/table";

type SortKey = "type" | "amount" | "status" | "date";
type SortDirection = "asc" | "desc";

interface SortableHeaderProps {
  label: string;
  sortKey: SortKey;
  currentSort: {
    key: SortKey;
    direction: SortDirection;
  };
  onSort: (key: SortKey) => void;
}

export function SortableHeader({
  label,
  sortKey,
  currentSort,
  onSort,
}: SortableHeaderProps) {
  const isActive = currentSort.key === sortKey;
  const direction = currentSort.direction;

  return (
    <TableHead
      className="cursor-pointer select-none"
      onClick={() => onSort(sortKey)}
    >
      <div className="flex items-center gap-1">
        {label}
        {isActive &&
          (direction === "asc" ? (
            <ArrowUpIcon className="h-4 w-4" data-testid="arrow-up-icon" />
          ) : (
            <ArrowDownIcon className="h-4 w-4" data-testid="arrow-down-icon" />
          ))}
      </div>
    </TableHead>
  );
}
