import { createConsumer } from "@rails/actioncable";
import { WS_URL } from "./config";

export const createActionCableConsumer = () => {
  const token =
    typeof window !== "undefined" ? localStorage.getItem("token") : null;

  if (!token) {
    return null;
  }

  return createConsumer(`${WS_URL}/cable?token=${token}`);
};
