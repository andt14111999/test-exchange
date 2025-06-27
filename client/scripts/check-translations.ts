import fs from "fs";
import path from "path";
import { execSync } from "child_process";
import readline from "readline";

const translationDirs = [
  path.join(process.cwd(), "src/i18n"),
  path.join(process.cwd(), "src/app/[locale]"),
  path.join(process.cwd(), "src/components"),
];

// Các ngôn ngữ được hỗ trợ
const supportedLocales = ["en", "vi", "fil"];

// Các thư mục chứa code cần được quét để tìm các key dịch đang sử dụng
const codeDirs = [
  path.join(process.cwd(), "src/app"),
  path.join(process.cwd(), "src/components"),
  path.join(process.cwd(), "src/lib"),
];

// Các pattern để nhận dạng việc sử dụng key dịch
// Cải thiện các pattern để bắt được nhiều cách sử dụng i18n hơn
const translationPatterns = [
  // Các pattern cơ bản
  /t\(['"]([\w.-]+)['"](?:[),]|$)/g, // t('key.name')
  /useTranslations\(\)[^]*?(?:\.[a-zA-Z0-9_]+)+\(['"]([\w.-]+)['"](?:[),]|$)/g, // useTranslations().namespace.t('key.name')
  /(?:\bt|i18n\.t|i18next\.t)\(['"]([\w.-]+)['"](?:[),]|$)/g, // t('key') variants
  /\{\s*t\(['"]([\w.-]+)['"](?:[),]|$)/g, // {t('key.name')}
  /(?:formatMessage|intl\.formatMessage)\(\{\s*id:\s*['"]([\w.-]+)['"](?:[),]|$)/g, // formatMessage({id: 'key.name'})

  // Pattern cho trường hợp JSON
  /["']([\w.-]+)["']\s*:\s*["'].*?["']/g, // "key.name": "value" in JSON files

  // Pattern đơn giản cho string
  /['"]([\w.-]+)['"](?:\s*[),]|$)/g, // Simple key pattern to catch various cases

  // Pattern cho JSX và các trường hợp đặc biệt
  />\s*\{\s*t\(['"]([\w.-]+)['"]/g, // JSX content with translation: >{t('key')}
  /['"]([\w.-]+)['"]\s*\}/g, // Translation key in object: {'key.name'}
  /translate\(['"]([\w.-]+)['"]/g, // translate('key.name')

  // Thêm pattern cho biến và object destructuring
  /\b(?:const|let|var)\s+\w+\s*=\s*['"]([\w.-]+)['"]/g, // const key = 'translation.key'

  // Pattern cho dynamic construction
  /['"]([\w.-]+)['"](?:\s*\+\s*["'][\w.-]+["'])/g, // 'part1.' + 'part2'
  /(?:["'][\w.-]+["']\s*\+\s*)["']([\w.-]+)["']/g, // 'part1' + '.part2'

  // Pattern cho prop truyền vào component
  /\b(?:translationKey|i18nKey|id|textKey)=["']([\w.-]+)["']/g, // translationKey="key.name"

  // Pattern cho template literals
  /`([\w.-]+(?:\${[^}]+}[\w.-]*)*)(?:`)/g, // `key.${dynamic}.value`

  // Pattern cho truyền key qua hàm
  /\(\s*['"]([\w.-]+)['"]\s*(?:,|\))/g, // functionCall('key.name')

  // Pattern cho khai báo trong object
  /[\w.]+\[['"]([\w.-]+)['"]\]/g, // obj['key.name'] hoặc translations['key']
];

// Phát hiện chuỗi có dạng translation key
function looksLikeTranslationKey(key: string): boolean {
  // Kiểm tra xem key có hợp lệ không (loại bỏ các trường hợp như 'version.0.0.1')
  if (!/^[a-z0-9]+(\.[a-z0-9_-]+)+$/i.test(key)) {
    return false;
  }

  // Loại trừ các URL và đường dẫn
  if (key.includes("http") || key.includes("www.") || key.includes(".com")) {
    return false;
  }

  // Loại trừ các version number
  if (/\d+\.\d+\.\d+/.test(key)) {
    return false;
  }

  // Kiểm tra độ dài hợp lý
  return key.length > 3 && key.length < 50;
}

type TranslationObject = {
  [key: string]: string | TranslationObject;
};

type MessageFile = {
  locale: string;
  content: TranslationObject;
  filePath: string;
  keys: string[];
};

type MissingTranslation = {
  locale: string;
  key: string;
  presentIn: string[];
  filePath: string;
};

type UsedKey = {
  key: string;
  files: string[];
};

// Kết quả so sánh
type ComparisonResult = {
  usedButMissing: Record<string, string[]>;
  unusedKeys: string[];
  missingBetweenLocales: Record<string, MissingTranslation[]>;
  allKeys: Set<string>;
  usedKeys: UsedKey[];
};

function getAllKeys(obj: TranslationObject, prefix = ""): string[] {
  let keys: string[] = [];
  for (const key in obj) {
    const newKey = prefix ? `${prefix}.${key}` : key;
    keys.push(newKey);
    if (typeof obj[key] === "object" && obj[key] !== null) {
      keys = keys.concat(getAllKeys(obj[key] as TranslationObject, newKey));
    }
  }
  return keys;
}

function getValueByPath(obj: TranslationObject, path: string): string | null {
  const parts = path.split(".");
  let current: TranslationObject | string = obj;

  for (const part of parts) {
    if (
      typeof current !== "object" ||
      current === null ||
      current[part] === undefined
    ) {
      return null;
    }
    current = current[part];
  }

  return typeof current === "string" ? current : null;
}

function setValueByPath(
  obj: TranslationObject,
  path: string,
  value: string,
): void {
  const parts = path.split(".");
  let current: TranslationObject = obj;

  for (let i = 0; i < parts.length - 1; i++) {
    const part = parts[i];
    if (current[part] === undefined) {
      current[part] = {};
    } else if (typeof current[part] !== "object") {
      return;
    }
    current = current[part] as TranslationObject;
  }

  current[parts[parts.length - 1]] = value;
}

function removeKeyFromObject(obj: TranslationObject, path: string): boolean {
  const parts = path.split(".");
  let current: TranslationObject | Record<string, unknown> = obj;

  for (let i = 0; i < parts.length - 1; i++) {
    const part = parts[i];
    if (typeof current[part] !== "object" || current[part] === null) {
      return false;
    }
    current = current[part] as TranslationObject;
  }

  const lastPart = parts[parts.length - 1];
  if (current[lastPart] === undefined) {
    return false;
  }

  delete current[lastPart];
  return true;
}

function cleanupEmptyObjects(obj: TranslationObject): TranslationObject {
  const result: TranslationObject = { ...obj };

  for (const key in result) {
    if (typeof result[key] === "object" && result[key] !== null) {
      const cleaned = cleanupEmptyObjects(result[key] as TranslationObject);

      if (Object.keys(cleaned).length === 0) {
        delete result[key];
      } else {
        result[key] = cleaned;
      }
    }
  }

  return result;
}

function findTranslationFiles(dir: string): string[] {
  const files: string[] = [];

  function scan(directory: string) {
    if (!fs.existsSync(directory)) return;

    const items = fs.readdirSync(directory);

    for (const item of items) {
      const fullPath = path.join(directory, item);
      const stat = fs.statSync(fullPath);

      if (stat.isDirectory()) {
        scan(fullPath);
      } else if (item.endsWith(".json")) {
        files.push(fullPath);
      }
    }
  }

  scan(dir);
  return files;
}

function findCodeFiles(dirs: string[]): string[] {
  const files: string[] = [];
  const extensions = [".ts", ".tsx", ".js", ".jsx", ".json"];

  function scan(directory: string) {
    if (!fs.existsSync(directory)) return;

    const items = fs.readdirSync(directory);

    for (const item of items) {
      const fullPath = path.join(directory, item);
      const stat = fs.statSync(fullPath);

      if (stat.isDirectory()) {
        if (!item.startsWith(".") && item !== "node_modules") {
          scan(fullPath);
        }
      } else if (extensions.some((ext) => item.endsWith(ext))) {
        files.push(fullPath);
      }
    }
  }

  dirs.forEach((dir) => scan(dir));
  return files;
}

function findKeysInCode(content: string): string[] {
  const foundKeys = new Set<string>();

  // Kiểm tra với pattern tiêu chuẩn
  translationPatterns.forEach((pattern) => {
    pattern.lastIndex = 0;
    let match;
    while ((match = pattern.exec(content)) !== null) {
      const key = match[1];
      if (key && looksLikeTranslationKey(key)) {
        foundKeys.add(key);
      }
    }
  });

  // Kiểm tra bằng cách tìm tất cả các chuỗi có dạng key
  const potentialKeyPattern = /'([^']+\.[^']+)'|"([^"]+\.[^"]+)"/g;
  let keyMatch;
  while ((keyMatch = potentialKeyPattern.exec(content)) !== null) {
    const key = keyMatch[1] || keyMatch[2];
    if (key && looksLikeTranslationKey(key)) {
      foundKeys.add(key);
    }
  }

  return Array.from(foundKeys);
}

function extractTranslationKeysFromCode(files: string[]): UsedKey[] {
  const keysMap = new Map<string, Set<string>>();

  // 1. Trích xuất các key từ file code thông thường
  console.log("  🔍 Đang quét các file code...");
  files.forEach((file) => {
    try {
      const content = fs.readFileSync(file, "utf-8");
      const keys = findKeysInCode(content);

      keys.forEach((key) => {
        if (!keysMap.has(key)) {
          keysMap.set(key, new Set());
        }
        keysMap.get(key)!.add(file);
      });
    } catch (error) {
      console.error(`❌ Lỗi khi đọc file ${file}:`, error);
    }
  });

  // 2. Tìm kiếm trong các file constants
  console.log("  🔍 Đang tìm kiếm trong các file constants...");
  const constantsKeys = findKeysInConstantsFiles();
  constantsKeys.forEach(({ key, files }) => {
    if (!keysMap.has(key)) {
      keysMap.set(key, new Set());
    }
    files.forEach((file) => keysMap.get(key)!.add(file));
  });

  // 3. Grep toàn bộ source code
  console.log("  🔍 Đang tìm kiếm trong toàn bộ source code bằng grep...");
  try {
    // Tìm key bằng grep để bổ sung
    const allTranslationFiles: string[] = [];
    translationDirs.forEach((dir) => {
      if (fs.existsSync(dir)) {
        allTranslationFiles.push(...findTranslationFiles(dir));
      }
    });

    // Lấy tất cả key từ các file dịch
    allTranslationFiles.forEach((file) => {
      try {
        const content = JSON.parse(fs.readFileSync(file, "utf-8"));
        const keys = getAllKeys(content);

        keys.forEach((key) => {
          // Nếu key này đã được tìm thấy trong code
          if (keysMap.has(key)) return;

          // Kiểm tra thêm với grep để đảm bảo
          try {
            const grepResult = execSync(
              `grep -r '${key.replace(/'/g, "'\\''")}' --include="*.{ts,tsx,js,jsx}" src 2>/dev/null`,
              { encoding: "utf-8" },
            );
            if (grepResult && grepResult.trim().length > 0) {
              keysMap.set(key, new Set(["found-via-grep"]));
            }
          } catch {
            // Không tìm thấy key trong code - bỏ qua lỗi
          }
        });
      } catch {
        // Bỏ qua lỗi khi đọc file translation
      }
    });
  } catch {}

  // 4. Thử quét file compiled
  console.log("  🔍 Đang kiểm tra các file build...");
  const compiledKeys = scanCompiledFiles();
  compiledKeys.forEach(({ key, files }) => {
    if (!keysMap.has(key)) {
      keysMap.set(key, new Set());
    }
    files.forEach((file) => keysMap.get(key)!.add(file));
  });

  return Array.from(keysMap.entries()).map(([key, files]) => ({
    key,
    files: Array.from(files).map((file) => path.relative(process.cwd(), file)),
  }));
}

function writeJsonFile(filePath: string, data: unknown): void {
  const dirPath = path.dirname(filePath);
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2), "utf-8");
}

function generateReport(
  messages: MessageFile[],
  comparison: ComparisonResult,
): void {
  const reportFile = path.join(process.cwd(), "translation-report.json");

  const missingByLocale: Record<
    string,
    { keys: string[]; presentIn: Record<string, number> }
  > = {};

  Object.entries(comparison.missingBetweenLocales).forEach(
    ([locale, missingItems]) => {
      missingByLocale[locale] = {
        keys: missingItems.map((item) => item.key),
        presentIn: {},
      };

      missingItems.forEach((item) => {
        item.presentIn.forEach((otherLocale) => {
          if (!missingByLocale[locale].presentIn[otherLocale]) {
            missingByLocale[locale].presentIn[otherLocale] = 0;
          }
          missingByLocale[locale].presentIn[otherLocale]++;
        });
      });
    },
  );

  const usedButMissingByLocale: Record<string, string[]> = {};
  Object.entries(comparison.usedButMissing).forEach(([key, locales]) => {
    locales.forEach((locale) => {
      if (!usedButMissingByLocale[locale]) {
        usedButMissingByLocale[locale] = [];
      }
      usedButMissingByLocale[locale].push(key);
    });
  });

  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      totalFiles: messages.length,
      locales: messages.map((m) => m.locale),
      totalUsedKeys: comparison.usedKeys.length,
      totalUnusedKeys: comparison.unusedKeys.length,
      totalUsedButMissingKeys: Object.keys(comparison.usedButMissing).length,
      totalAllPossibleKeys: comparison.allKeys.size,
      missingKeysByLocale: Object.fromEntries(
        Object.entries(missingByLocale).map(([locale, info]) => [
          locale,
          info.keys.length,
        ]),
      ),
      usedButMissingByLocale: Object.fromEntries(
        Object.entries(usedButMissingByLocale).map(([locale, keys]) => [
          locale,
          keys.length,
        ]),
      ),
    },
    details: {
      missingByLocale,
      usedButMissingByLocale,
      unusedKeys: comparison.unusedKeys,
      usedKeys: comparison.usedKeys.map(({ key, files }) => ({
        key,
        usedInFiles: files.length,
        files:
          files.length <= 5
            ? files
            : files.slice(0, 5).concat([`... and ${files.length - 5} more`]),
      })),
    },
  };

  writeJsonFile(reportFile, report);
  console.log(`\nBáo cáo chi tiết đã được lưu tại: ${reportFile}`);
}

function compareTranslations(
  messages: MessageFile[],
  usedKeysFromCode: UsedKey[],
): ComparisonResult {
  const allTranslationKeys = new Set<string>();
  const localeKeyMap: Record<string, Set<string>> = {};

  const result: ComparisonResult = {
    usedButMissing: {},
    unusedKeys: [],
    missingBetweenLocales: {},
    allKeys: new Set<string>(),
    usedKeys: usedKeysFromCode,
  };

  messages.forEach(({ locale, keys }) => {
    localeKeyMap[locale] = new Set(keys);
    keys.forEach((key) => allTranslationKeys.add(key));
  });

  const usedKeysSet = new Set(usedKeysFromCode.map((uk) => uk.key));
  const allPossibleKeys = new Set<string>();
  // Add all keys from allTranslationKeys to allPossibleKeys
  allTranslationKeys.forEach((key) => allPossibleKeys.add(key));
  // Add all keys from usedKeysSet to allPossibleKeys
  usedKeysSet.forEach((key) => allPossibleKeys.add(key));
  result.allKeys = allPossibleKeys;

  usedKeysFromCode.forEach(({ key }) => {
    const missingInLocales: string[] = [];

    for (const locale of Object.keys(localeKeyMap)) {
      if (!localeKeyMap[locale].has(key)) {
        missingInLocales.push(locale);
      }
    }

    if (missingInLocales.length > 0) {
      result.usedButMissing[key] = missingInLocales;
    }
  });

  result.unusedKeys = Array.from(allTranslationKeys).filter(
    (key) => !usedKeysSet.has(key),
  );

  for (const locale of Object.keys(localeKeyMap)) {
    result.missingBetweenLocales[locale] = [];

    allTranslationKeys.forEach((key) => {
      if (!localeKeyMap[locale].has(key)) {
        const presentIn = Object.keys(localeKeyMap).filter(
          (otherLocale) =>
            otherLocale !== locale && localeKeyMap[otherLocale].has(key),
        );

        if (presentIn.length > 0) {
          const filePath =
            messages.find((m) => m.locale === locale)?.filePath || "";

          result.missingBetweenLocales[locale].push({
            locale,
            key,
            presentIn,
            filePath,
          });
        }
      }
    });
  }

  return result;
}

async function askForConfirmation(question: string): Promise<boolean> {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });

  return new Promise((resolve) => {
    rl.question(`${question} (y/N): `, (answer) => {
      rl.close();
      answer = answer.toLowerCase();
      resolve(answer === "y" || answer === "yes");
    });
  });
}

async function fixMissingTranslations(
  messages: MessageFile[],
  comparison: ComparisonResult,
  forceConfirm: boolean = false,
): Promise<boolean> {
  const totalKeysToAdd = Object.values(comparison.usedButMissing).reduce(
    (total, locales) => total + locales.length,
    0,
  );

  if (totalKeysToAdd === 0) return false;

  // Yêu cầu xác nhận nếu số lượng key cần thêm quá lớn
  if (totalKeysToAdd > 50 && !forceConfirm) {
    const confirmed = await askForConfirmation(
      `⚠️ Chuẩn bị thêm ${totalKeysToAdd} key vào các file dịch. Tiếp tục?`,
    );
    if (!confirmed) {
      console.log("❌ Đã hủy thao tác thêm key.");
      return false;
    }
  }

  const localeContents: Record<string, TranslationObject> = {};
  const localeFiles: Record<string, string> = {};

  messages.forEach(({ locale, filePath }) => {
    try {
      localeContents[locale] = JSON.parse(fs.readFileSync(filePath, "utf-8"));
      localeFiles[locale] = filePath;
    } catch (error) {
      console.error(`Lỗi khi đọc file ${filePath}:`, error);
    }
  });

  let filesUpdated = 0;
  let keysAdded = 0;

  // Fix keys missing between locales
  Object.entries(comparison.missingBetweenLocales).forEach(
    ([locale, missingItems]) => {
      if (!localeContents[locale]) return;

      let updated = false;

      missingItems.forEach((item) => {
        const sourceLocale = item.presentIn[0];
        if (!sourceLocale || !localeContents[sourceLocale]) return;

        const value = getValueByPath(localeContents[sourceLocale], item.key);
        if (value) {
          setValueByPath(localeContents[locale], item.key, value);
          updated = true;
          keysAdded++;
        }
      });

      if (updated) {
        writeJsonFile(localeFiles[locale], localeContents[locale]);
        filesUpdated++;
      }
    },
  );

  // Fix keys used in code but missing in some locales
  Object.entries(comparison.usedButMissing).forEach(([key, missingLocales]) => {
    // Find a locale that has this key to use as source
    const sourceLocale = Object.keys(localeContents).find(
      (locale) =>
        !missingLocales.includes(locale) &&
        getValueByPath(localeContents[locale], key) !== null,
    );

    // If no locale has this key, create a default value
    let value: string | null = null;
    if (sourceLocale) {
      value = getValueByPath(localeContents[sourceLocale], key);
    }

    if (!value) {
      // Create a placeholder value based on the key
      const lastPart = key.split(".").pop() || key;
      value =
        lastPart.charAt(0).toUpperCase() +
        lastPart.slice(1).replace(/[-_.]/g, " ");
    }

    // Add the key to all missing locales
    missingLocales.forEach((locale) => {
      if (!localeContents[locale] || !localeFiles[locale]) return;

      setValueByPath(localeContents[locale], key, value as string);
      writeJsonFile(localeFiles[locale], localeContents[locale]);
      keysAdded++;
      if (!filesUpdated) filesUpdated++;
    });
  });

  if (filesUpdated > 0) {
    console.log(`\n✅ Đã cập nhật ${filesUpdated} file dịch`);
    console.log(`✅ Đã thêm ${keysAdded} key còn thiếu vào các file dịch`);
    return true;
  }

  return false;
}

async function removeUnusedTranslations(
  messages: MessageFile[],
  unusedKeys: string[],
  forceConfirm: boolean = false,
): Promise<boolean> {
  if (unusedKeys.length === 0) return false;

  // Yêu cầu xác nhận khi số lượng key cần xóa lớn
  if (unusedKeys.length > 20 && !forceConfirm) {
    const confirmed = await askForConfirmation(
      `⚠️ Chuẩn bị xóa ${unusedKeys.length} key không sử dụng từ các file dịch. Tiếp tục?`,
    );
    if (!confirmed) {
      console.log("❌ Đã hủy thao tác xóa key.");
      return false;
    }
  }

  let filesUpdated = 0;
  let keysRemoved = 0;

  // Process each locale file
  messages.forEach(({ filePath, content }) => {
    let updated = false;
    let updatedContent = { ...content };

    // Remove each unused key
    unusedKeys.forEach((key) => {
      if (removeKeyFromObject(updatedContent, key)) {
        updated = true;
        keysRemoved++;
      }
    });

    if (updated) {
      // Clean up any empty objects left after removing keys
      updatedContent = cleanupEmptyObjects(updatedContent);
      writeJsonFile(filePath, updatedContent);
      filesUpdated++;
    }
  });

  if (filesUpdated > 0) {
    console.log(
      `\n🧹 Đã xóa ${keysRemoved} key không sử dụng khỏi ${filesUpdated} file dịch`,
    );
    return true;
  }

  return false;
}

// Thêm hàm quét các file build/compiled
function scanCompiledFiles(): UsedKey[] {
  const keysMap = new Map<string, Set<string>>();

  // Kiểm tra thư mục build hoặc dist
  const buildDirs = [
    path.join(process.cwd(), "build"),
    path.join(process.cwd(), "dist"),
    path.join(process.cwd(), ".next"),
  ];

  for (const buildDir of buildDirs) {
    if (!fs.existsSync(buildDir)) continue;

    console.log(
      `  🔍 Đang kiểm tra thư mục build: ${path.relative(process.cwd(), buildDir)}`,
    );

    try {
      const files = findCodeFiles([buildDir]);

      // Chỉ quét một số lượng file hợp lý
      const filesToScan = files
        .filter((f) => f.endsWith(".js") || f.endsWith(".json"))
        .slice(0, 100);

      filesToScan.forEach((file) => {
        try {
          const content = fs.readFileSync(file, "utf-8");
          const keys = findKeysInCode(content);

          keys.forEach((key) => {
            if (!keysMap.has(key)) {
              keysMap.set(key, new Set());
            }
            keysMap.get(key)!.add(file);
          });
        } catch {}
      });
    } catch {}
  }

  return Array.from(keysMap.entries()).map(([key, files]) => ({
    key,
    files: Array.from(files).map(
      (file) => `compiled:${path.relative(process.cwd(), file)}`,
    ),
  }));
}

// Thêm hàm đọc file constants để tìm key
function findKeysInConstantsFiles(): UsedKey[] {
  const keysMap = new Map<string, Set<string>>();
  const potentialFiles = [
    "src/constants/translations.ts",
    "src/constants/i18n.ts",
    "src/i18n/constants.ts",
    "src/utils/i18n.ts",
    "src/lib/i18n.ts",
    "src/lib/translations.ts",
    "src/config/i18n.ts",
  ];

  potentialFiles.forEach((filePath) => {
    const fullPath = path.join(process.cwd(), filePath);
    if (!fs.existsSync(fullPath)) return;

    try {
      const content = fs.readFileSync(fullPath, "utf-8");
      const keys = findKeysInCode(content);

      keys.forEach((key) => {
        if (!keysMap.has(key)) {
          keysMap.set(key, new Set());
        }
        keysMap.get(key)!.add(fullPath);
      });
    } catch {}
  });

  return Array.from(keysMap.entries()).map(([key, files]) => ({
    key,
    files: Array.from(files).map((file) => path.relative(process.cwd(), file)),
  }));
}

async function main() {
  const args = process.argv.slice(2);
  const generateReportFlag = args.includes("--report");
  const fixMissingFlag = args.includes("--fix");
  const verbose = args.includes("--verbose");
  const onlyShowUsedFlag = args.includes("--only-used");
  const removeUnusedFlag = args.includes("--remove-unused");
  const forceFlag = args.includes("--force");
  const addKeyFlag = args.includes("--add-key");
  const deepScanFlag = args.includes("--deep-scan");

  // Thêm key thủ công vào các file dịch
  if (addKeyFlag) {
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout,
    });

    const keyToAdd = await new Promise<string>((resolve) => {
      rl.question(
        "Nhập key cần thêm (ví dụ: liquidity.noPoolsAvailable): ",
        resolve,
      );
    });

    if (!keyToAdd || !keyToAdd.includes(".")) {
      console.error(
        "❌ Key không hợp lệ. Key phải có format dạng: namespace.keyName",
      );
      rl.close();
      return;
    }

    let defaultValue = await new Promise<string>((resolve) => {
      rl.question(`Nhập giá trị mặc định cho key '${keyToAdd}': `, resolve);
    });

    if (!defaultValue) {
      // Tạo giá trị mặc định từ key
      const lastPart = keyToAdd.split(".").pop() || keyToAdd;
      defaultValue =
        lastPart.charAt(0).toUpperCase() +
        lastPart.slice(1).replace(/[-_.]/g, " ");
    }

    rl.close();

    // Tìm các file dịch và thêm key vào
    const translationFiles: string[] = [];
    translationDirs.forEach((dir) => {
      if (fs.existsSync(dir)) {
        translationFiles.push(...findTranslationFiles(dir));
      }
    });

    // Lọc theo ngôn ngữ được hỗ trợ
    const supportedFiles = translationFiles.filter((file) => {
      const locale = path.parse(file).name;
      return supportedLocales.includes(locale);
    });

    if (supportedFiles.length === 0) {
      console.error(
        "❌ Không tìm thấy file dịch nào thuộc các ngôn ngữ được hỗ trợ!",
      );
      return;
    }

    // Thêm key vào các file
    let updatedCount = 0;
    for (const file of supportedFiles) {
      try {
        const content = JSON.parse(fs.readFileSync(file, "utf-8"));
        setValueByPath(content, keyToAdd, defaultValue);
        writeJsonFile(file, content);
        updatedCount++;
      } catch (error) {
        console.error(`❌ Lỗi khi cập nhật file ${file}:`, error);
      }
    }

    console.log(
      `\n✅ Đã thêm key '${keyToAdd}' vào ${updatedCount}/${supportedFiles.length} file dịch.`,
    );
    return;
  }

  console.log("🔍 Đang kiểm tra các bản dịch...");
  const messages: MessageFile[] = [];

  // Collect all translation files
  translationDirs.forEach((dir) => {
    if (!fs.existsSync(dir)) {
      console.warn(`⚠️ Cảnh báo: Thư mục ${dir} không tồn tại`);
      return;
    }

    const files = findTranslationFiles(dir);

    // Only process files for supported locales
    files.forEach((file) => {
      const locale = path.parse(file).name;
      if (!supportedLocales.includes(locale)) return;

      try {
        const content = JSON.parse(fs.readFileSync(file, "utf-8"));
        const keys = getAllKeys(content);

        messages.push({
          locale,
          content,
          filePath: file,
          keys,
        });
      } catch (error) {
        console.error(`❌ Lỗi khi đọc file ${file}:`, error);
      }
    });
  });

  if (messages.length === 0) {
    console.error("❌ Không tìm thấy file dịch nào!");
    return;
  }

  // Scan code for translation keys
  console.log("\n🔍 Đang quét code để tìm các key dịch được sử dụng...");
  const codeFiles = findCodeFiles(codeDirs);
  console.log(`  Đã tìm thấy ${codeFiles.length} files code để kiểm tra`);

  // Kiểm tra deepScan flag
  if (deepScanFlag) {
    console.log(
      "  🔍 Đang thực hiện quét sâu (deep scan) - có thể mất nhiều thời gian hơn...",
    );
  }

  const usedKeys = extractTranslationKeysFromCode(codeFiles);
  console.log(
    `  Đã tìm thấy ${usedKeys.length} keys dịch đang được sử dụng trong code`,
  );

  // Compare translations with each other and with code
  const comparison = compareTranslations(messages, usedKeys);

  // Print results
  console.log("\n📊 Kết quả so sánh các file dịch:");

  // 1. Print stats for each locale
  messages.forEach(({ locale, keys, filePath }) => {
    const missingFromOthers =
      comparison.missingBetweenLocales[locale]?.length || 0;
    const missingFromCode = Object.entries(comparison.usedButMissing).filter(
      ([, locales]) => locales.includes(locale),
    ).length;

    console.log(`\n${locale.toUpperCase()}:`);
    console.log(`  📂 File: ${path.relative(process.cwd(), filePath)}`);
    console.log(`  📝 Tổng số key: ${keys.length}`);
    console.log(
      `  🔴 Key thiếu (so với các locale khác): ${missingFromOthers}`,
    );
    console.log(`  🟠 Key thiếu (có trong code): ${missingFromCode}`);

    // Show detailed missing keys in verbose mode
    if (verbose) {
      if (missingFromOthers > 0) {
        console.log(
          `\n  Key thiếu trong ${locale} (có trong các locale khác):`,
        );
        comparison.missingBetweenLocales[locale].forEach(
          ({ key, presentIn }) => {
            console.log(`  - ${key} (có trong: ${presentIn.join(", ")})`);
          },
        );
      }

      const missingFromCodeKeys = Object.entries(comparison.usedButMissing)
        .filter(([, locales]) => locales.includes(locale))
        .map(([key]) => key);

      if (missingFromCodeKeys.length > 0) {
        console.log(`\n  Key sử dụng trong code nhưng thiếu trong ${locale}:`);
        missingFromCodeKeys.forEach((key) => console.log(`  - ${key}`));
      }
    }
  });

  // 2. Print unused keys
  if (!onlyShowUsedFlag && comparison.unusedKeys.length > 0) {
    console.log(
      `\n🔵 Key không sử dụng trong code nhưng có trong các file dịch: ${comparison.unusedKeys.length}`,
    );
    if (verbose) {
      console.log("\n  Danh sách key không sử dụng:");
      comparison.unusedKeys.forEach((key) => {
        console.log(`  - ${key}`);
      });
    }
  }

  // 3. Print summary
  console.log("\n📝 Tổng kết:");
  console.log(`  Số file dịch: ${messages.length}`);
  console.log(`  Số key đang sử dụng trong code: ${usedKeys.length}`);
  console.log(
    `  Số key không sử dụng trong code: ${comparison.unusedKeys.length}`,
  );
  console.log(
    `  Tổng số key thiếu (so với code): ${Object.keys(comparison.usedButMissing).length}`,
  );

  // Generate detailed report if requested
  if (generateReportFlag) {
    generateReport(messages, comparison);
  }

  // Fix missing translations if requested
  if (fixMissingFlag) {
    const updated = await fixMissingTranslations(
      messages,
      comparison,
      forceFlag,
    );
    if (updated) {
      console.log("\n✅ Đã thêm các bản dịch còn thiếu");
      console.log("⚠️ Vui lòng kiểm tra và cập nhật lại các bản dịch tự động");
    } else {
      console.log("\n⚠️ Không thể tự động thêm các bản dịch còn thiếu");
    }
  }

  // Remove unused keys if requested
  if (removeUnusedFlag && comparison.unusedKeys.length > 0) {
    if (verbose) {
      console.log("\n⚠️ Chuẩn bị xóa các key không sử dụng sau đây:");
      comparison.unusedKeys.forEach((key) => console.log(`  - ${key}`));
    }

    const removed = await removeUnusedTranslations(
      messages,
      comparison.unusedKeys,
      forceFlag,
    );
    if (removed) {
      console.log("\n✅ Đã xóa tất cả key không sử dụng từ các file dịch");
      console.log(`🔢 Tổng số key đã xóa: ${comparison.unusedKeys.length}`);
    } else {
      console.log("\n⚠️ Không thể xóa các key không sử dụng");
    }
  }

  // Provide usage instructions
  console.log("\n💡 Cách sử dụng:");
  console.log("  --verbose      : Hiển thị chi tiết các keys thiếu và thừa");
  console.log("  --report       : Tạo báo cáo JSON chi tiết");
  console.log("  --fix          : Tự động thêm các bản dịch còn thiếu");
  console.log(
    "  --only-used    : Chỉ hiển thị thông tin về các key đang sử dụng",
  );
  console.log("  --remove-unused: Xóa tất cả key không sử dụng trong code");
  console.log(
    "  --force        : Bỏ qua các xác nhận khi thực hiện thay đổi lớn",
  );
  console.log("  --add-key      : Thêm key thủ công vào các file dịch");
  console.log("  --deep-scan    : Quét sâu hơn để tìm kiếm các key dịch");
}

main();
