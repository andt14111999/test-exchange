import { useEffect, useRef } from "react";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { Subscription, Consumer } from "@rails/actioncable";

interface UseAmmOrderChannelProps {
  userId: number;
  onOrderCreated?: (orderId: number) => void;
  onOrderUpdated?: (orderId: number) => void;
  onOrderCompleted?: (orderId: number) => void;
  onOrderFailed?: (orderId: number) => void;
  onError?: (error: string) => void;
}

interface AmmOrderChannelResponse {
  status?: string;
  action?: string;
  order_id?: number;
  data?: Record<string, unknown>;
  message?: string;
  [key: string]: unknown;
}

export function useAmmOrderChannel({
  userId,
  onOrderCreated,
  onOrderUpdated,
  onOrderCompleted,
  onOrderFailed,
  onError,
}: UseAmmOrderChannelProps) {
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
        const subscriptionParams = {
          channel: "AmmOrderChannel",
          user_id: userId,
        };

        subscriptionRef.current = consumer.subscriptions.create(
          subscriptionParams,
          {
            connected() {
              console.log(`⚡ Connected to AmmOrderChannel for user ${userId}`);
              startKeepAlive();
            },
            disconnected() {
              console.log(
                `💤 Disconnected from AmmOrderChannel for user ${userId}`,
              );
              stopKeepAlive();

              setTimeout(() => {
                setupConnection();
              }, 3000);
            },
            rejected() {
              console.log(
                `❌ AmmOrderChannel connection rejected for user ${userId}`,
              );
              stopKeepAlive();
            },
            received(response: AmmOrderChannelResponse | string) {
              const processResponse = () => {
                let processedResponse: AmmOrderChannelResponse;

                if (typeof response === "string") {
                  try {
                    processedResponse = JSON.parse(
                      response,
                    ) as AmmOrderChannelResponse;
                  } catch {
                    processedResponse = {} as AmmOrderChannelResponse;
                  }
                } else {
                  processedResponse = response;
                }

                if (
                  processedResponse.status === "success" &&
                  processedResponse.message
                ) {
                  return;
                }

                // Handle order created
                if (
                  processedResponse.status === "success" &&
                  processedResponse.action === "created" &&
                  processedResponse.order_id
                ) {
                  onOrderCreated?.(processedResponse.order_id);
                }

                // Handle order updated
                if (
                  processedResponse.status === "success" &&
                  processedResponse.action === "updated" &&
                  processedResponse.order_id
                ) {
                  onOrderUpdated?.(processedResponse.order_id);
                }

                // Handle order completed
                if (
                  processedResponse.status === "success" &&
                  processedResponse.action === "completed" &&
                  processedResponse.order_id
                ) {
                  onOrderCompleted?.(processedResponse.order_id);
                }

                // Handle order failed
                if (
                  processedResponse.status === "error" &&
                  processedResponse.action === "failed" &&
                  processedResponse.order_id
                ) {
                  onOrderFailed?.(processedResponse.order_id);
                }

                // Handle generic error
                if (processedResponse.status === "error") {
                  onError?.(processedResponse.message || "Unknown error");
                }
              };

              try {
                processResponse();
              } catch (error) {
                console.error(
                  "Error processing AmmOrderChannel response:",
                  error,
                );
              }
            },
          } as unknown as Subscription,
        );
      } catch (error) {
        console.error("Error creating AmmOrderChannel subscription:", error);
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
  }, [
    userId,
    onOrderCreated,
    onOrderUpdated,
    onOrderCompleted,
    onOrderFailed,
    onError,
  ]);
}
