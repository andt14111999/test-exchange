import axios from "axios";
import { API_BASE_URL } from "./config";
import { getDeviceHeaders } from "@/lib/utils/device";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add request interceptor to add auth token and device headers
apiClient.interceptors.request.use((config) => {
  const token =
    typeof window !== "undefined" ? localStorage.getItem("token") : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Add device headers for all requests (only on client side)
  if (typeof window !== "undefined") {
    const deviceHeaders = getDeviceHeaders();
    Object.assign(config.headers, deviceHeaders);
  }

  return config;
});

// Add response interceptor to handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.log("API Error:", error?.response?.status, error?.response?.data);

    if (error.response?.status === 401) {
      console.log("Unauthorized error in API interceptor");
      localStorage.removeItem("token");
    }

    return Promise.reject(error);
  },
);
