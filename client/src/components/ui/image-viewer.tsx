"use client";

import { useState, useRef, useEffect } from "react";
import Image from "next/image";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { X, ZoomIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VisuallyHidden } from "@radix-ui/react-visually-hidden";

interface ImageViewerProps {
  src: string;
  alt: string;
  className?: string;
  maxHeight?: number;
  showZoomIcon?: boolean;
}

export function ImageViewer({
  src,
  alt,
  className = "",
  maxHeight = 400,
  showZoomIcon = true,
}: ImageViewerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [dimensions, setDimensions] = useState<{
    width: number;
    height: number;
  } | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const img = new window.Image();
    img.onload = () => {
      const aspectRatio = img.naturalWidth / img.naturalHeight;
      let displayWidth = img.naturalWidth;
      let displayHeight = img.naturalHeight;

      // If image is too tall, constrain by maxHeight
      if (displayHeight > maxHeight) {
        displayHeight = maxHeight;
        displayWidth = displayHeight * aspectRatio;
      }

      // If image is too wide, constrain by container width (assuming max 400px for mobile-first)
      const maxWidth = Math.min(400, window.innerWidth - 64); // 32px padding on each side
      if (displayWidth > maxWidth) {
        displayWidth = maxWidth;
        displayHeight = displayWidth / aspectRatio;
      }

      setDimensions({
        width: Math.round(displayWidth),
        height: Math.round(displayHeight),
      });
      setIsLoading(false);
    };

    img.onerror = () => {
      setIsLoading(false);
    };

    img.src = src;
  }, [src, maxHeight]);

  if (isLoading || !dimensions) {
    return (
      <div
        data-testid="loading-skeleton"
        className={`animate-pulse bg-gray-200 rounded-lg ${className}`}
        style={{ width: "100%", height: `${maxHeight}px` }}
      />
    );
  }

  return (
    <>
      <div
        data-testid="image-container"
        className={`relative group cursor-pointer rounded-lg overflow-hidden border hover:shadow-md transition-shadow max-w-full ${className}`}
        onClick={() => setIsOpen(true)}
        style={{
          width: "100%",
          maxWidth: dimensions.width,
          height: dimensions.height,
        }}
      >
        <Image
          ref={imgRef}
          src={src}
          alt={alt}
          fill
          className="object-contain"
          sizes="(max-width: 768px) 100vw, 400px"
        />

        {showZoomIcon && (
          <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
            <div className="opacity-0 group-hover:opacity-100 transition-opacity">
              <div className="bg-white/90 backdrop-blur-sm rounded-full p-2">
                <ZoomIn
                  data-testid="zoom-icon"
                  className="h-5 w-5 text-gray-700"
                />
              </div>
            </div>
          </div>
        )}
      </div>

      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <DialogContent
          className="p-0 border-0 bg-black/95 shadow-none max-w-none max-h-none w-auto h-auto overflow-auto"
          onPointerDownOutside={() => setIsOpen(false)}
          onEscapeKeyDown={() => setIsOpen(false)}
        >
          <VisuallyHidden>
            <DialogTitle>Image Viewer</DialogTitle>
          </VisuallyHidden>
          <div data-testid="dialog-container" className="relative">
            <Button
              data-testid="close-button"
              variant="ghost"
              size="icon"
              className="absolute top-4 right-4 z-50 bg-white/10 text-white hover:bg-white/20 border border-white/20 backdrop-blur-sm"
              onClick={() => setIsOpen(false)}
            >
              <X className="h-4 w-4" />
            </Button>
            <Image
              data-testid="dialog-image"
              src={src}
              alt={alt}
              width={0}
              height={0}
              className="block max-w-[90vw] max-h-[90vh] w-auto h-auto"
              sizes="90vw"
              style={{
                width: "auto",
                height: "auto",
              }}
            />
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
