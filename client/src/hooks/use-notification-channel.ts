import { useEffect, useRef, useCallback } from "react";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { Notification } from "@/lib/api/notifications";
import type { Subscription, Consumer } from "@rails/actioncable";

interface UseNotificationChannelProps {
  userId: number;
  onNotificationReceived: (notification: Notification) => void;
}

interface ResponseWithData {
  status?: string;
  data?: Notification;
  title?: string;
  content?: string;
  type?: string;
  read?: boolean;
  created_at?: string;
  id?: number;
  message?: string;
  [key: string]: unknown;
}

export function useNotificationChannel({
  userId,
  onNotificationReceived,
}: UseNotificationChannelProps) {
  const subscriptionRef = useRef<Subscription | null>(null);
  const consumerRef = useRef<Consumer | null>(null);

  const testPing = useCallback(() => {
    if (subscriptionRef.current) {
      try {
        setTimeout(() => {
          if (subscriptionRef.current) {
            subscriptionRef.current.perform("ping", {
              message: "Hello from client",
              timestamp: Date.now(),
              client_id: Math.random().toString(36).substring(2, 9),
            });
          }
        }, 100);
      } catch {
        // Error handling
      }
    }
  }, []);

  useEffect(() => {
    // Function to set up connection
    const setupConnection = () => {
      if (!userId) {
        return;
      }

      // Clear existing connection if needed
      if (consumerRef.current) {
        try {
          if (subscriptionRef.current) {
            subscriptionRef.current.unsubscribe();
            subscriptionRef.current = null;
          }
          consumerRef.current.disconnect();
          consumerRef.current = null;
        } catch {
          // Error handling
        }
      }

      const consumer = createActionCableConsumer();
      if (!consumer) {
        return;
      }

      consumerRef.current = consumer;

      try {
        subscriptionRef.current = consumer.subscriptions.create(
          {
            channel: "NotificationChannel",
          },
          {
            connected() {
              startKeepAlive();
            },
            disconnected() {
              console.log(
                `💤 Disconnected from NotificationChannel for user ${userId}`,
              );
              stopKeepAlive();
              setTimeout(() => {
                setupConnection();
              }, 3000);
            },
            rejected() {
              console.log(
                `❌ NotificationChannel connection rejected for user ${userId}`,
              );
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
                if (
                  processedResponse.status === "success" &&
                  processedResponse.data
                ) {
                  onNotificationReceived(processedResponse.data);
                  return;
                }

                if (processedResponse.title || processedResponse.id) {
                  const notification: Notification = {
                    id:
                      processedResponse.id || Math.floor(Math.random() * 1000),
                    title: processedResponse.title || "Thông báo mới",
                    content: processedResponse.content || "",
                    type: processedResponse.type || "notification",
                    read: false,
                    created_at:
                      processedResponse.created_at || new Date().toISOString(),
                  };
                  onNotificationReceived(notification);
                }
              };

              try {
                processResponse();
              } catch {}
            },
          } as unknown as Subscription,
        );
      } catch {}
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
  }, [userId, onNotificationReceived]);

  return {
    testPing,
  };
}
