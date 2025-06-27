import { useQuery } from "@tanstack/react-query";
import { getBanks, GetBanksResponse } from "../banks";

export const useBanks = () => {
  return useQuery<GetBanksResponse, Error>({
    queryKey: ["banks"],
    queryFn: () => getBanks(),
  });
};
