import { keccak256 } from "js-sha3";

/**
 * Converts an Ethereum address to EIP-55 checksum format
 * @param address - The Ethereum address to checksum (with or without 0x prefix)
 * @returns The checksummed address
 */
export async function toChecksumAddress(address: string): Promise<string> {
  if (!address) return address;

  // Remove 0x prefix if present and convert to lowercase
  const cleanAddress = address.replace(/^0x/i, "").toLowerCase();

  // Validate that it's a valid Ethereum address format (40 hex characters)
  if (!/^[a-f0-9]{40}$/i.test(cleanAddress)) {
    return address; // Return original if not a valid Ethereum address
  }

  try {
    // Create Keccak-256 hash of the lowercase address (without 0x prefix)
    const hash = keccak256(cleanAddress);

    let checksumAddress = "0x";

    for (let i = 0; i < cleanAddress.length; i++) {
      if (parseInt(hash[i], 16) >= 8) {
        checksumAddress += cleanAddress[i].toUpperCase();
      } else {
        checksumAddress += cleanAddress[i];
      }
    }

    return checksumAddress;
  } catch (error) {
    console.warn("Failed to checksum address:", error);
    return "0x" + cleanAddress; // Return with 0x prefix if checksumming fails
  }
}

/**
 * Checks if an address is an Ethereum-type address (ERC20, BEP20)
 * @param networkId - The network identifier
 * @returns True if the network uses Ethereum-style addresses
 */
export function isEthereumLikeNetwork(networkId: string): boolean {
  return ["erc20", "bep20"].includes(networkId.toLowerCase());
}
