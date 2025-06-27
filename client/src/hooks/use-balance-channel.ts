import { useEffect, useRef } from "react";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { BalanceData } from "@/lib/api/balance";
import type { Subscription, Consumer } from "@rails/actioncable";

interface UseBalanceChannelProps {
  userId: number;
  onBalanceUpdate: (balance: BalanceData) => void;
}

interface ResponseWithData {
  status?: string;
  data?: BalanceData;
  message?: string;
  [key: string]: unknown;
}

export function useBalanceChannel({
  userId,
  onBalanceUpdate,
}: UseBalanceChannelProps) {
  const subscriptionRef = useRef<Subscription | null>(null);
  const consumerRef = useRef<Consumer | null>(null);

  useEffect(() => {
    const setupConnection = () => {
      if (!userId) {
        return;
      }

      if (consumerRef.current) {
        try {
          if (subscriptionRef.current) {
            subscriptionRef.current.unsubscribe();
            subscriptionRef.current = null;
          }
          consumerRef.current.disconnect();
          consumerRef.current = null;
        } catch {}
      }

      const consumer = createActionCableConsumer();
      if (!consumer) {
        return;
      }

      consumerRef.current = consumer;

      try {
        subscriptionRef.current = consumer.subscriptions.create(
          {
            channel: "BalanceChannel",
          },
          {
            connected() {
              console.log(`⚡ Connected to BalanceChannel for user ${userId}`);
              startKeepAlive();
            },
            disconnected() {
              console.log(
                `💤 Disconnected from BalanceChannel for user ${userId}`,
              );
              stopKeepAlive();

              setTimeout(() => {
                setupConnection();
              }, 3000);
            },
            rejected() {
              console.log(
                `❌ BalanceChannel connection rejected for user ${userId}`,
              );
              stopKeepAlive();
            },
            received(response: ResponseWithData | string) {
              console.log("📥 BalanceChannel received:", response);
              const processResponse = () => {
                let processedResponse: ResponseWithData;

                if (typeof response === "string") {
                  try {
                    processedResponse = JSON.parse(
                      response,
                    ) as ResponseWithData;
                  } catch {
                    processedResponse = {} as ResponseWithData;
                  }
                } else {
                  processedResponse = response;
                }

                if (
                  processedResponse.status === "success" &&
                  processedResponse.message
                ) {
                  console.log(
                    "BalanceChannel system message:",
                    processedResponse.message,
                  );
                  return;
                }

                if (
                  processedResponse.status === "success" &&
                  processedResponse.data
                ) {
                  console.log(
                    "BalanceChannel data update:",
                    processedResponse.data,
                  );
                  onBalanceUpdate(processedResponse.data);
                }
              };

              try {
                processResponse();
              } catch (error) {
                console.error(
                  "Error processing BalanceChannel response:",
                  error,
                );
              }
            },
          } as unknown as Subscription,
        );
      } catch (error) {
        console.error("Error creating BalanceChannel subscription:", error);
      }
    };

    let keepAliveInterval: NodeJS.Timeout | null = null;

    const startKeepAlive = () => {
      stopKeepAlive();

      keepAliveInterval = setInterval(() => {
        if (subscriptionRef.current) {
          subscriptionRef.current.perform("keepalive", {
            timestamp: Date.now(),
          });
        } else {
          stopKeepAlive();
        }
      }, 30000);
    };

    const stopKeepAlive = () => {
      if (keepAliveInterval) {
        clearInterval(keepAliveInterval);
        keepAliveInterval = null;
      }
    };

    setupConnection();

    return () => {
      stopKeepAlive();
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
      if (consumerRef.current) {
        consumerRef.current.disconnect();
      }
    };
  }, [userId, onBalanceUpdate]);
}
