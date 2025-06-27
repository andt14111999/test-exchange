import { useEffect, useRef } from "react";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { AmmPosition } from "@/lib/api/positions";
import type { Subscription, Consumer } from "@rails/actioncable";

interface UseAmmPositionChannelProps {
  userId: number;
  onPositionUpdate: (position: AmmPosition) => void;
  onPositionCreated?: (position: AmmPosition) => void;
  onPositionClosed?: (positionId: number) => void;
}

interface ResponseWithData {
  status?: string;
  data?: AmmPosition | AmmPosition[];
  message?: string;
  action?: "created" | "updated" | "closed";
  position_id?: number;
  [key: string]: unknown;
}

export function useAmmPositionChannel({
  userId,
  onPositionUpdate,
  onPositionCreated,
  onPositionClosed,
}: UseAmmPositionChannelProps) {
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
            channel: "AmmPositionChannel",
            user_id: userId,
          },
          {
            connected() {
              console.log(
                `⚡ Connected to AmmPositionChannel for user ${userId}`,
              );
              startKeepAlive();
            },
            disconnected() {
              console.log(
                `💤 Disconnected from AmmPositionChannel for user ${userId}`,
              );
              stopKeepAlive();

              setTimeout(() => {
                setupConnection();
              }, 3000);
            },
            rejected() {
              console.log(
                `❌ AmmPositionChannel connection rejected for user ${userId}`,
              );
              stopKeepAlive();
            },
            received(response: ResponseWithData | string) {
              console.log("📥 AmmPositionChannel received:", response);
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
                    "AmmPositionChannel system message:",
                    processedResponse.message,
                  );
                  return;
                }

                if (
                  processedResponse.status === "success" &&
                  processedResponse.data
                ) {
                  const action = processedResponse.action || "updated";

                  if (Array.isArray(processedResponse.data)) {
                    // Handle multiple positions update
                    processedResponse.data.forEach((position) => {
                      console.log(
                        `AmmPositionChannel ${action} multiple positions:`,
                        position,
                      );
                      onPositionUpdate(position);
                    });
                  } else {
                    // Handle single position update
                    const position = processedResponse.data as AmmPosition;
                    console.log(
                      `AmmPositionChannel ${action} position:`,
                      position,
                    );

                    switch (action) {
                      case "created":
                        onPositionCreated?.(position);
                        break;
                      case "closed":
                        onPositionClosed?.(position.id);
                        break;
                      default:
                        onPositionUpdate(position);
                        break;
                    }
                  }
                }

                // Handle position closed by ID only
                if (
                  processedResponse.status === "success" &&
                  processedResponse.action === "closed" &&
                  processedResponse.position_id
                ) {
                  console.log(
                    "AmmPositionChannel position closed by ID:",
                    processedResponse.position_id,
                  );
                  onPositionClosed?.(processedResponse.position_id);
                }

                // Handle position closed in data object
                if (
                  processedResponse.status === "success" &&
                  processedResponse.action === "closed" &&
                  processedResponse.data &&
                  !Array.isArray(processedResponse.data)
                ) {
                  const position = processedResponse.data as AmmPosition;
                  console.log(
                    "AmmPositionChannel position closed with data:",
                    position,
                  );
                  onPositionClosed?.(position.id);
                }
              };

              try {
                processResponse();
              } catch (error) {
                console.error(
                  "Error processing AmmPositionChannel response:",
                  error,
                );
              }
            },
          } as unknown as Subscription,
        );
      } catch (error) {
        console.error("Error creating AmmPositionChannel subscription:", error);
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
  }, [userId, onPositionUpdate, onPositionCreated, onPositionClosed]);
}
