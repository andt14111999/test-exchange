/**
 * This config addresses the Haste module naming collision issue with package.json files
 */
import { fileURLToPath } from "url";
import { dirname, resolve } from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default {
  // This function tells Jest which module names should be excluded from the Haste map
  hasteImplModulePath: resolve(__dirname, "./hasteImpl.js"),
};
