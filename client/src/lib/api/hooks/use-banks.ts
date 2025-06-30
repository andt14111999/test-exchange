import { useQuery } from "@tanstack/react-query";
import { getBanks, GetBanksResponse } from "../banks";

export const useBanks = (countryCode?: string) => {
  return useQuery<GetBanksResponse, Error>({
    queryKey: ["banks", countryCode],
    queryFn: () => getBanks(countryCode),
  });
};
