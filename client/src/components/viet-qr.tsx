import { QRCodeSVG } from "qrcode.react";
import Image from "next/image";
import { useToast } from "@/components/ui/use-toast";
import { Button } from "@/components/ui/button";
import { CopyIcon } from "lucide-react";

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

// Get bank bin code for Vietnamese banks
const getBankBIN = (bankName: string): string => {
  const bankMap: Record<string, string> = {
    // Common Vietnamese banks
    VIETCOMBANK: "970436",
    VIETINBANK: "970415",
    BIDV: "970418",
    TECHCOMBANK: "970407",
    MBBANK: "970422",
    TPBANK: "970423",
    ACB: "970416",
    VPBANK: "970432",
    SACOMBANK: "970403",
    VIETCAPITALBANK: "970454",
    EXIMBANK: "970431",
    OCB: "970448",
    AGRIBANK: "970405",
    HDBANK: "970437",
    SHB: "970443",
    SEABANK: "970440",
    BAOVIETBANK: "970438",
    PVCOMBANK: "970412",
    VIETABANK: "970427",
    NAMABANK: "970428",
    ABBANK: "970425",
    VIB: "970441",
    OCEANBANK: "970414",
  };

  // Normalize bank name for comparison
  const normalizedBankName = bankName.trim().toUpperCase().replace(/\s+/g, "");

  // Return the BIN if found, otherwise a default value
  return bankMap[normalizedBankName] || "970436"; // Default to Vietcombank if not found
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
}: VietQRProps) => {
  const { toast } = useToast();

  // Only consider as Vietnamese bank if we have both bank name and account number
  const isVietnameseBank =
    bankName?.trim().length > 0 && accountNumber?.trim().length > 0;

  // Generate the VietQR data
  const qrData = isVietnameseBank
    ? generateVietQRData(getBankBIN(bankName), accountNumber, amount, content)
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
    if (!isVietnameseBank) return null;

    const bankId = getBankBIN(bankName);
    const templateType = "compact2";
    const encodedAccountName = encodeURIComponent(accountName || "");
    const numericAmount = parseFloat(amount.replace(/[^\d.]/g, "")).toString();

    return `https://img.vietqr.io/image/${bankId}-${accountNumber}-${templateType}.png?amount=${numericAmount}&addInfo=${encodeURIComponent(content)}&accountName=${encodedAccountName}`;
  };

  const vietQRImageUrl = useImageAPI ? getVietQRImageUrl() : null;

  const handleCopy = () => {
    navigator.clipboard.writeText(qrData);
    toast({
      title: "Copied to clipboard",
      description: "Payment information copied",
    });
  };

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

        {isVietnameseBank && (
          <div className="flex justify-center mt-1">
            <div className="text-xs font-medium px-1.5 py-0.5 bg-blue-50 text-blue-700 rounded-full">
              VietQR
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
