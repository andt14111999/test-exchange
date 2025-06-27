#!/usr/bin/env node

// Shim file to execute check-translations.ts with proper ESM handling
import { execSync } from "child_process";
import { fileURLToPath } from "url";
import { dirname, resolve } from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Pass all arguments to the tsx command
const args = process.argv.slice(2);
const tsFilePath = resolve(__dirname, "check-translations.ts");

try {
  // Execute the TypeScript file using tsx
  execSync(`npx tsx "${tsFilePath}" ${args.join(" ")}`, {
    stdio: "inherit",
    env: { ...process.env, NODE_ENV: "development" },
  });
} catch {
  // Ensure non-zero exit code is propagated
  process.exit(1);
}
