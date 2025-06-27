/**
 * Custom Haste implementation to resolve package.json collisions
 */
import { HasteMap } from "jest-haste-map";

class CustomHasteMap extends HasteMap {
  constructor(options) {
    super(options);
  }

  // This method is called for each file to decide if it should be included in the Haste map
  shouldIncludeFile(file) {
    // Skip .next directory contents to avoid collisions
    if (file.includes("/.next/") || file.includes("\\.next\\")) {
      return false;
    }

    return super.shouldIncludeFile(file);
  }
}

export default CustomHasteMap;
