"use client";

import { useEffect, useRef } from "react";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { ApiPool } from "@/lib/api/pools";
import type { Subscription, Consumer } from "@rails/actioncable";

interface UseAmmPoolChannelProps {
  onPoolUpdate: (pool: ApiPool) => void;
}

interface ResponseWithData {
  status?: string;
  data?: ApiPool;
  message?: string;
  action?: string;
  [key: string]: unknown;
}

export function useAmmPoolChannel({ onPoolUpdate }: UseAmmPoolChannelProps) {
  const subscriptionRef = useRef<Subscription | null>(null);
  const consumerRef = useRef<Consumer | null>(null);

  useEffect(() => {
    const setupConnection = () => {
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
            channel: "AmmPoolChannel",
          },
          {
            connected() {
              // console.log("⚡ Connected to AmmPoolChannel");
              startKeepAlive();
            },
            disconnected() {
              // console.log("💤 Disconnected from AmmPoolChannel");
              stopKeepAlive();

              setTimeout(() => {
                setupConnection();
              }, 3000);
            },
            rejected() {
              // console.log("❌ AmmPoolChannel connection rejected");
              stopKeepAlive();
            },
            received(response: ResponseWithData | string) {
              const processResponse = () => {
                let processedResponse: ResponseWithData;

                if (typeof response === "string") {
                  try {
                    processedResponse = JSON.parse(
                      response,
                    ) as ResponseWithData;
                  } catch {
                    processedResponse = {} as ResponseWithData;
                    console.error("Failed to parse response:", response);
                    return;
                  }
                } else {
                  processedResponse = response;
                }

                // Xử lý thông báo hệ thống
                if (
                  processedResponse.status === "success" &&
                  processedResponse.message &&
                  !processedResponse.data
                ) {
                  console.log(
                    "AmmPoolChannel system message:",
                    processedResponse.message,
                  );
                  return;
                }

                // Xử lý update/create pool
                if (
                  processedResponse.status === "success" &&
                  processedResponse.data
                ) {
                  const actionType = processedResponse.action || "updated";

                  console.log(
                    `AmmPoolChannel: Pool ${actionType}`,
                    processedResponse.data,
                  );

                  onPoolUpdate(processedResponse.data);
                }
              };

              try {
                processResponse();
              } catch (error) {
                console.error(
                  "Error processing AmmPoolChannel response:",
                  error,
                );
              }
            },
          } as unknown as Subscription,
        );
      } catch (error) {
        console.error("Error creating AmmPoolChannel subscription:", error);
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
  }, [onPoolUpdate]);
}
