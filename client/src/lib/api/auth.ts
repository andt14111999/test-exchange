import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";

export async function loginWithGoogle() {
  const response = await apiClient.get(API_ENDPOINTS.auth.google);
  return response.data;
}
