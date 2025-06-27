import { useToast } from "./ui/use-toast";
import { Button } from "./ui/button";

interface CopyableFieldProps {
  content: React.ReactNode;
  value: string | undefined;
  copyMessage?: string;
}

export function CopyableField({
  content,
  value = "",
  copyMessage = "Information copied",
}: CopyableFieldProps) {
  const { toast } = useToast();

  const handleCopy = () => {
    navigator.clipboard.writeText(value || "");
    toast({
      title: "Copied to clipboard",
      description: copyMessage,
    });
  };

  return (
    <Button
      variant="ghost"
      size="sm"
      className="h-8 w-8 p-0 ml-2 flex-shrink-0"
      onClick={handleCopy}
      aria-label="Copy to clipboard"
    >
      {content}
    </Button>
  );
}
