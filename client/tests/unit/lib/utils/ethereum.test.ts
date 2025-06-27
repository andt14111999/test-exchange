import { toChecksumAddress, isEthereumLikeNetwork } from "@/lib/utils/ethereum";

describe("Ethereum Utils", () => {
  describe("isEthereumLikeNetwork", () => {
    it("should return true for ERC20 network", () => {
      expect(isEthereumLikeNetwork("erc20")).toBe(true);
    });

    it("should return true for BEP20 network", () => {
      expect(isEthereumLikeNetwork("bep20")).toBe(true);
    });

    it("should return true for ERC20 network with uppercase", () => {
      expect(isEthereumLikeNetwork("ERC20")).toBe(true);
    });

    it("should return true for BEP20 network with uppercase", () => {
      expect(isEthereumLikeNetwork("BEP20")).toBe(true);
    });

    it("should return true for mixed case networks", () => {
      expect(isEthereumLikeNetwork("ErC20")).toBe(true);
      expect(isEthereumLikeNetwork("BeP20")).toBe(true);
    });

    it("should return false for TRC20 network", () => {
      expect(isEthereumLikeNetwork("trc20")).toBe(false);
    });

    it("should return false for Solana network", () => {
      expect(isEthereumLikeNetwork("solana")).toBe(false);
    });

    it("should return false for unknown networks", () => {
      expect(isEthereumLikeNetwork("bitcoin")).toBe(false);
      expect(isEthereumLikeNetwork("")).toBe(false);
      expect(isEthereumLikeNetwork("unknown")).toBe(false);
    });

    it("should handle edge cases", () => {
      expect(isEthereumLikeNetwork("erc21")).toBe(false);
      expect(isEthereumLikeNetwork("bep21")).toBe(false);
      expect(isEthereumLikeNetwork("erc")).toBe(false);
      expect(isEthereumLikeNetwork("bep")).toBe(false);
    });
  });

  describe("toChecksumAddress", () => {
    describe("EIP-55 compliance tests", () => {
      it("should correctly checksum the reported issue address", async () => {
        const address = "0x36f6b28c9d827962b582073d427bc4d1140fcf55";
        const result = await toChecksumAddress(address);

        // This is the correct checksum according to EIP-55 and MetaMask
        expect(result).toBe("0x36f6B28c9D827962B582073d427bC4D1140fCF55");
      });

      it("should correctly checksum various Ethereum addresses", async () => {
        const testCases = [
          {
            input: "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            expected: "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
          },
          {
            input: "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
            expected: "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
          },
          {
            input: "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
            expected: "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
          },
          {
            input: "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
            expected: "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
          },
          {
            input: "0x52908400098527886e0f7030069857d2e4169ee7",
            expected: "0x52908400098527886E0F7030069857D2E4169EE7",
          },
          {
            input: "0x8617e340b3d01fa5f11f306f4090fd50e238070d",
            expected: "0x8617E340B3D01FA5F11F306F4090FD50E238070D",
          },
          {
            input: "0xde709f2102306220921060314715629080e2fb77",
            expected: "0xde709f2102306220921060314715629080e2fb77",
          },
          {
            input: "0x27b1fdb04752bbc536007a920d24acb045561c26",
            expected: "0x27b1fdb04752bbc536007a920d24acb045561c26",
          },
        ];

        for (const testCase of testCases) {
          const result = await toChecksumAddress(testCase.input);
          expect(result).toBe(testCase.expected);
        }
      });

      it("should handle lowercase input addresses", async () => {
        const address = "0x36f6b28c9d827962b582073d427bc4d1140fcf55";
        const result = await toChecksumAddress(address);
        expect(result).toBe("0x36f6B28c9D827962B582073d427bC4D1140fCF55");
      });

      it("should handle uppercase input addresses", async () => {
        const address = "0X36F6B28C9D827962B582073D427BC4D1140FCF55";
        const result = await toChecksumAddress(address);
        expect(result).toBe("0x36f6B28c9D827962B582073d427bC4D1140fCF55");
      });

      it("should handle mixed case input addresses", async () => {
        const address = "0x36F6b28C9d827962b582073d427bC4D1140FcF55";
        const result = await toChecksumAddress(address);
        expect(result).toBe("0x36f6B28c9D827962B582073d427bC4D1140fCF55");
      });
    });

    describe("edge cases", () => {
      it("should handle address without 0x prefix", async () => {
        const address = "36f6b28c9d827962b582073d427bc4d1140fcf55";
        const result = await toChecksumAddress(address);
        expect(result).toBe("0x36f6B28c9D827962B582073d427bC4D1140fCF55");
      });

      it("should return empty string for empty input", async () => {
        const result = await toChecksumAddress("");
        expect(result).toBe("");
      });

      it("should return original address for invalid Ethereum address format", async () => {
        const invalidAddresses = [
          "invalid_address",
          "0x123", // Too short
          "0x36f6b28c9d827962b582073d427bc4d1140fcf55extra", // Too long
          "0xZ6f6b28c9d827962b582073d427bc4d1140fcf55", // Invalid character
          "not_hex_at_all",
        ];

        for (const address of invalidAddresses) {
          const result = await toChecksumAddress(address);
          expect(result).toBe(address);
        }
      });

      it("should handle null and undefined inputs", async () => {
        const result1 = await toChecksumAddress(null as any);
        const result2 = await toChecksumAddress(undefined as any);

        expect(result1).toBe(null);
        expect(result2).toBe(undefined);
      });

      it("should preserve 0x prefix in output", async () => {
        const addresses = [
          "0x36f6b28c9d827962b582073d427bc4d1140fcf55",
          "36f6b28c9d827962b582073d427bc4d1140fcf55",
        ];

        for (const address of addresses) {
          const result = await toChecksumAddress(address);
          expect(result.startsWith("0x")).toBe(true);
        }
      });

      it("should generate deterministic results", async () => {
        const address = "0x36f6b28c9d827962b582073d427bc4d1140fcf55";

        const result1 = await toChecksumAddress(address);
        const result2 = await toChecksumAddress(address);

        expect(result1).toBe(result2);
        expect(result1).toBe("0x36f6B28c9D827962B582073d427bC4D1140fCF55");
      });

      it("should normalize input addresses consistently", async () => {
        const variations = [
          "0x36f6b28c9d827962b582073d427bc4d1140fcf55",
          "0X36F6B28C9D827962B582073D427BC4D1140FCF55",
          "36f6b28c9d827962b582073d427bc4d1140fcf55",
          "36F6B28C9D827962B582073D427BC4D1140FCF55",
        ];

        const results = await Promise.all(
          variations.map((addr) => toChecksumAddress(addr)),
        );

        // All variations should produce the same result
        expect(results[0]).toBe(results[1]);
        expect(results[0]).toBe(results[2]);
        expect(results[0]).toBe(results[3]);
        expect(results[0]).toBe("0x36f6B28c9D827962B582073d427bC4D1140fCF55");
      });
    });

    describe("error handling", () => {
      it("should handle invalid hex characters gracefully", async () => {
        const address = "0x36g6b28c9d827962b582073d427bc4d1140fcf55"; // 'g' is invalid hex
        const result = await toChecksumAddress(address);
        expect(result).toBe(address); // Should return original for invalid format
      });

      it("should handle addresses with wrong length", async () => {
        const shortAddress = "0x36f6b28c";
        const longAddress = "0x36f6b28c9d827962b582073d427bc4d1140fcf55abc";

        expect(await toChecksumAddress(shortAddress)).toBe(shortAddress);
        expect(await toChecksumAddress(longAddress)).toBe(longAddress);
      });
    });
  });
});
