import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./button";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}: PaginationProps) {
  const canGoPrevious = currentPage > 1;
  const canGoNext = currentPage < totalPages;

  return (
    <div className="flex items-center gap-1">
      <Button
        variant="outline"
        size="icon"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={!canGoPrevious}
      >
        <ChevronLeft className="h-4 w-4" />
        <span className="sr-only">Previous page</span>
      </Button>
      <div className="flex items-center gap-1 mx-2">
        {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
          // Logic to determine which page numbers to show
          let pageNum;
          if (totalPages <= 5) {
            // Show all pages if 5 or fewer
            pageNum = i + 1;
          } else {
            // Show a window of pages centered around current page
            const startPage = Math.max(
              1,
              Math.min(currentPage - 2, totalPages - 4),
            );
            pageNum = startPage + i;
          }

          return (
            <Button
              key={pageNum}
              variant={currentPage === pageNum ? "default" : "outline"}
              size="sm"
              onClick={() => onPageChange(pageNum)}
              className="w-8 h-8"
            >
              {pageNum}
            </Button>
          );
        })}
        {totalPages > 5 && currentPage < totalPages - 2 && (
          <>
            <span className="mx-1">...</span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(totalPages)}
              className="w-8 h-8"
            >
              {totalPages}
            </Button>
          </>
        )}
      </div>
      <Button
        variant="outline"
        size="icon"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={!canGoNext}
      >
        <ChevronRight className="h-4 w-4" />
        <span className="sr-only">Next page</span>
      </Button>
    </div>
  );
}
