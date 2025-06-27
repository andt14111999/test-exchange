import { toast } from "sonner";
import { AxiosError } from "axios";

/**
 * Extracts a detailed error message from different types of error objects
 * @param error The error object to extract message from
 * @returns A user-friendly error message
 */
export function extractErrorMessage(error: unknown): string {
  if (!error) return "An unknown error occurred";

  // Handle Axios errors
  if (error instanceof AxiosError) {
    // Try to get the error message from the response data
    const responseData = error.response?.data;

    if (responseData) {
      if (typeof responseData === "string") {
        return responseData;
      }

      // Check common error message formats
      if (responseData.error) {
        return typeof responseData.error === "string"
          ? responseData.error
          : JSON.stringify(responseData.error);
      }

      if (responseData.message) {
        return typeof responseData.message === "string"
          ? responseData.message
          : JSON.stringify(responseData.message);
      }

      if (responseData.errors && Array.isArray(responseData.errors)) {
        return responseData.errors
          .map((e: unknown) => {
            if (typeof e === "object" && e !== null && "message" in e) {
              return String(e.message);
            }
            return String(e);
          })
          .join(", ");
      }

      // If we can't find a specific error format, return the whole data
      return JSON.stringify(responseData);
    }

    // If no data in response, use the status text or message
    if (error.response?.statusText) {
      return `${error.response.statusText} (${error.response.status})`;
    }

    return error.message || "Network request failed";
  }

  // Handle standard Error objects
  if (error instanceof Error) {
    return error.message;
  }

  // Handle string errors
  if (typeof error === "string") {
    return error;
  }

  // Fallback for unknown error types
  return "An unexpected error occurred";
}

/**
 * Displays an error message using a toast notification and optionally logs to console
 * @param error The error that occurred
 * @param fallbackMessage A fallback message if no specific error can be extracted
 * @param shouldLog Whether to log the error to console (default: true)
 */
export function handleApiError(
  error: unknown,
  fallbackMessage = "An error occurred",
  shouldLog = process.env.NODE_ENV !== "test",
): string {
  if (shouldLog) {
    console.error("API Error:", error);
  }

  const errorMessage = extractErrorMessage(error) || fallbackMessage;

  // Show toast notification
  toast.error(errorMessage);

  return errorMessage;
}
