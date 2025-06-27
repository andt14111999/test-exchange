import { cn } from "@/lib/utils";

interface SkeletonProps extends React.HTMLAttributes<HTMLSpanElement> {
  isInline?: boolean;
}

function Skeleton({ className, isInline = false, ...props }: SkeletonProps) {
  return (
    <span
      className={cn(
        "animate-pulse rounded-md bg-muted",
        isInline ? "inline-block" : "block",
        className,
      )}
      {...props}
    />
  );
}

export { Skeleton };
