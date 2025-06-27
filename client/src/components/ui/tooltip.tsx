import * as React from "react";
import { createContext, forwardRef, useState } from "react";

// Context for the tooltip provider
const TooltipContext = createContext<{
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
}>({
  open: false,
  setOpen: () => {},
});

interface TooltipProviderProps {
  children: React.ReactNode;
}

export function TooltipProvider({ children }: TooltipProviderProps) {
  const [open, setOpen] = useState(false);

  return (
    <TooltipContext.Provider value={{ open, setOpen }}>
      {children}
    </TooltipContext.Provider>
  );
}

interface TooltipProps {
  children: React.ReactNode;
}

export function Tooltip({ children }: TooltipProps) {
  return <div className="relative inline-block">{children}</div>;
}

interface TooltipTriggerProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  asChild?: boolean;
}

export const TooltipTrigger = forwardRef<
  HTMLButtonElement,
  TooltipTriggerProps
>(({ asChild, ...props }, ref) => {
  const { setOpen } = React.useContext(TooltipContext);

  const handleMouseEnter = () => {
    setOpen(true);
  };

  const handleMouseLeave = () => {
    setOpen(false);
  };

  if (asChild) {
    return React.cloneElement(
      props.children as React.ReactElement<{
        onMouseEnter?: () => void;
        onMouseLeave?: () => void;
        ref?: React.Ref<HTMLElement>;
      }>,
      {
        onMouseEnter: handleMouseEnter,
        onMouseLeave: handleMouseLeave,
        ref,
      },
    );
  }

  return (
    <button
      type="button"
      {...props}
      ref={ref}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    />
  );
});

TooltipTrigger.displayName = "TooltipTrigger";

interface TooltipContentProps {
  children: React.ReactNode;
}

export function TooltipContent({ children }: TooltipContentProps) {
  const { open } = React.useContext(TooltipContext);

  if (!open) return null;

  return (
    <div className="absolute z-50 px-3 py-1.5 text-xs rounded shadow-md bg-gray-900 text-white -translate-x-1/2 left-1/2 -top-8">
      {children}
      <div className="tooltip-arrow absolute w-2 h-2 bg-gray-900 rotate-45 -bottom-1 left-1/2 -translate-x-1/2"></div>
    </div>
  );
}
