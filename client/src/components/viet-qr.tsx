import { QRCodeSVG } from "qrcode.react";
import Image from "next/image";
import { useToast } from "@/components/ui/use-toast";
import { Button } from "@/components/ui/button";
import { CopyIcon } from "lucide-react";
import { Bank } from "@/lib/api/banks";
import { useBanks } from "@/lib/api/hooks/use-banks";

// Function to calculate CRC-16 for VietQR
const calculateCRC16 = (str: string): string => {
  let crc = 0xffff;
  const polynomial = 0x1021;

  for (let i = 0; i < str.length; i++) {
    const c = str.charCodeAt(i);
    crc ^= c << 8;

    for (let j = 0; j < 8; j++) {
      if (crc & 0x8000) {
        crc = ((crc << 1) ^ polynomial) & 0xffff;
      } else {
        crc = (crc << 1) & 0xffff;
      }
    }
  }

  // Convert to hexadecimal and ensure it's 4 characters
  return crc.toString(16).toUpperCase().padStart(4, "0");
};

// Generate VietQR code data for Vietnamese bank transfers
const generateVietQRData = (
  bankBIN: string,
  accountNumber: string,
  amount: string,
  content: string,
): string => {
  try {
    // Remove non-numeric characters from amount and convert to string
    const numericAmount = amount.replace(/[^\d.]/g, "");
    const amountString = parseFloat(numericAmount).toString();

    // Format each field with ID, length, and value
    const formatField = (id: string, value: string) => {
      return `${id}${value.length.toString().padStart(2, "0")}${value}`;
    };

    // 1. Payload Format Indicator
    const payloadFormatIndicator = formatField("00", "01");

    // 2. Point of Initiation Method - Dynamic QR
    const initiationMethod = formatField("01", "12");

    // 3. Consumer Account Information
    // 3.1 GUID
    const guid = formatField("00", "A000000727");

    // 3.2 Bank BIN and Account Number
    const bankBinField = formatField("00", bankBIN);
    const accountField = formatField("01", accountNumber);
    const serviceCode = formatField("02", "QRIBFTTA");

    const merchantInfo = formatField(
      "01",
      bankBinField + accountField + serviceCode,
    );

    // Combine all Consumer Account Information
    const consumerAccountInfo = formatField("38", guid + merchantInfo);

    // 4. Transaction Currency (VND = 704)
    const currency = formatField("53", "704");

    // 5. Transaction Amount
    const transactionAmount = formatField("54", amountString);

    // 6. Country Code
    const countryCode = formatField("58", "VN");

    // 7. Additional Data Field Template - For transaction purpose
    const purposeOfTransaction = formatField("08", content);
    const additionalDataField = formatField("62", purposeOfTransaction);

    // Combine all fields except CRC
    const dataWithoutCRC =
      payloadFormatIndicator +
      initiationMethod +
      consumerAccountInfo +
      currency +
      transactionAmount +
      countryCode +
      additionalDataField +
      "6304";

    // Calculate CRC
    const crc = calculateCRC16(dataWithoutCRC);

    // Complete QR code data with CRC
    return dataWithoutCRC + crc;
  } catch (error) {
    console.error("Error generating VietQR data:", error);
    return "";
  }
};

// Normalize string for comparison (remove diacritics, spaces, and convert to lowercase)
const normalizeString = (str: string): string => {
  return str
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "") // Remove diacritics
    .replace(/[^\w]/g, ""); // Remove spaces and special characters
};

// Find bank by name with fuzzy matching
const findBankByName = (bankName: string, banks: Bank[]): Bank | null => {
  if (!bankName?.trim() || !banks?.length) return null;

  const normalizedInput = normalizeString(bankName);

  // First try exact matches with short names and codes
  for (const bank of banks) {
    const normalizedShortName = normalizeString(bank.shortName);
    const normalizedCode = normalizeString(bank.code);

    if (
      normalizedInput === normalizedShortName ||
      normalizedInput === normalizedCode
    ) {
      return bank;
    }
  }

  // Then try full name matches
  for (const bank of banks) {
    const normalizedFullName = normalizeString(bank.name);
    if (normalizedInput === normalizedFullName) {
      return bank;
    }
  }

  // Finally try partial matches
  for (const bank of banks) {
    const normalizedShortName = normalizeString(bank.shortName);
    const normalizedCode = normalizeString(bank.code);
    const normalizedFullName = normalizeString(bank.name);

    // Check if input contains the bank identifier or vice versa
    if (
      normalizedInput.includes(normalizedShortName) ||
      normalizedShortName.includes(normalizedInput) ||
      normalizedInput.includes(normalizedCode) ||
      normalizedCode.includes(normalizedInput) ||
      normalizedFullName.includes(normalizedInput) ||
      normalizedInput.includes(normalizedFullName)
    ) {
      return bank;
    }
  }

  return null;
};

// Get bank BIN code with smart matching
const getBankBIN = (bankName: string, banks: Bank[]): string | null => {
  const foundBank = findBankByName(bankName, banks);
  return foundBank?.bin || null;
};

export interface VietQRProps {
  bankName: string;
  accountName: string;
  accountNumber: string;
  amount: string;
  content: string;
  currency?: string;
  copyButtonText?: string;
  scanQRText?: string;
  qrSize?: number;
  useImageAPI?: boolean;
  banks?: Bank[]; // Optional banks data from API - if not provided, will use internal hook
}

export const VietQR = ({
  bankName,
  accountName,
  accountNumber,
  amount,
  content,
  currency = "VND",
  copyButtonText = "Copy Payment Info",
  scanQRText = "Scan QR Code",
  qrSize = 200,
  useImageAPI = true,
  banks: propsBanks,
}: VietQRProps) => {
  const { toast } = useToast();

  // Use banks from props or fetch from API
  const { data: banksResponse, isLoading } = useBanks();
  const apiBanks = banksResponse?.data || [];
  const banks = propsBanks || apiBanks;

  // Only consider as Vietnamese bank if we have both bank name, account number, and banks data
  const isVietnameseBank =
    bankName?.trim().length > 0 &&
    accountNumber?.trim().length > 0 &&
    banks?.length > 0;

  // Get bank BIN
  const bankBIN = isVietnameseBank ? getBankBIN(bankName, banks) : null;

  // Generate the VietQR data
  const qrData =
    isVietnameseBank && bankBIN
      ? generateVietQRData(bankBIN, accountNumber, amount, content)
      : JSON.stringify({
          bankName,
          accountName,
          accountNumber,
          amount,
          currency,
          content,
        });

  // Generate VietQR image URL using the VietQR.io API
  const getVietQRImageUrl = () => {
    if (!isVietnameseBank || !bankBIN) return null;

    const templateType = "compact2";
    const encodedAccountName = encodeURIComponent(accountName || "");
    const numericAmount = parseFloat(amount.replace(/[^\d.]/g, "")).toString();

    return `https://img.vietqr.io/image/${bankBIN}-${accountNumber}-${templateType}.png?amount=${numericAmount}&addInfo=${encodeURIComponent(content)}&accountName=${encodedAccountName}`;
  };

  const vietQRImageUrl = useImageAPI ? getVietQRImageUrl() : null;

  const handleCopy = () => {
    navigator.clipboard.writeText(qrData);
    toast({
      title: "Copied to clipboard",
      description: "Payment information copied",
    });
  };

  // Show loading state if banks are being fetched
  if (isLoading && !propsBanks) {
    return (
      <div className="space-y-4">
        <div className="bg-slate-50 p-4 rounded space-y-4">
          <div className="text-gray-500 text-sm text-center">
            Loading banks data...
          </div>
          <div className="flex justify-center">
            <div className="w-[200px] h-[200px] bg-gray-200 animate-pulse rounded-lg"></div>
          </div>
        </div>
      </div>
    );
  }

  // Show error state if no banks data available
  if (!banks?.length) {
    return (
      <div className="space-y-4">
        <div className="bg-red-50 p-4 rounded space-y-4">
          <div className="text-red-600 text-sm text-center">
            Unable to load banks data. Please try again later.
          </div>
          <div className="flex justify-center">
            <div className="w-[200px] h-[200px] bg-red-100 rounded-lg flex items-center justify-center">
              <span className="text-red-500 text-xs">No QR Code</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="bg-slate-50 p-4 rounded space-y-4">
        <div className="text-gray-500 text-sm mb-2">{scanQRText}</div>

        <div className="flex justify-center">
          <div className="bg-white p-4 rounded-lg shadow-md relative group">
            {vietQRImageUrl ? (
              <div className="relative">
                <Image
                  src={vietQRImageUrl}
                  alt="VietQR Payment Code"
                  width={qrSize}
                  height={qrSize}
                  className="rounded"
                />
              </div>
            ) : (
              <QRCodeSVG
                value={qrData}
                width={qrSize}
                height={qrSize}
                level="H"
                className="rounded"
              />
            )}

            {/* Hover overlay */}
            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/5 flex items-center justify-center transition-all duration-200 rounded-lg opacity-0 group-hover:opacity-100">
              <Button
                variant="secondary"
                size="sm"
                className="shadow-lg"
                onClick={handleCopy}
              >
                <CopyIcon size={16} className="mr-2" />
                Copy
              </Button>
            </div>
          </div>
        </div>

        {isVietnameseBank && bankBIN && (
          <div className="flex justify-center mt-1">
            <div className="text-xs font-medium px-1.5 py-0.5 bg-blue-50 text-blue-700 rounded-full">
              VietQR
            </div>
          </div>
        )}

        {/* Show warning if bank not found */}
        {bankName && !bankBIN && banks?.length > 0 && (
          <div className="flex justify-center mt-1">
            <div className="text-xs font-medium px-1.5 py-0.5 bg-yellow-50 text-yellow-700 rounded-full">
              Bank not recognized - using generic QR
            </div>
          </div>
        )}
      </div>

      <div className="flex justify-center">
        <Button
          variant="outline"
          size="sm"
          className="shadow-sm"
          onClick={handleCopy}
        >
          <CopyIcon size={14} className="mr-2" />
          {copyButtonText}
        </Button>
      </div>
    </div>
  );
};
