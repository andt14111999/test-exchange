import { useEffect, useRef, useCallback } from "react";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { ApiTrade } from "@/lib/api/trades";
import type { Subscription, Consumer } from "@rails/actioncable";

interface UseTradeChannelProps {
  tradeId: string;
  onTradeUpdated: (trade: ApiTrade) => void;
}

interface ResponseWithData {
  status?: string;
  data?: ApiTrade;
  message?: string;
  [key: string]: unknown;
}

export function useTradeChannel({
  tradeId,
  onTradeUpdated,
}: UseTradeChannelProps) {
  const subscriptionRef = useRef<Subscription | null>(null);
  const consumerRef = useRef<Consumer | null>(null);

  const testPing = useCallback(() => {
    if (subscriptionRef.current) {
      try {
        setTimeout(() => {
          if (subscriptionRef.current) {
            try {
              subscriptionRef.current.perform("ping", {
                message: "Hello from client",
                timestamp: Date.now(),
                client_id: Math.random().toString(36).substring(2, 9),
              });
            } catch (error) {
              console.error("Error in testPing:", error);
            }
          }
        }, 100);
      } catch (error) {
        console.error("Error setting up testPing timeout:", error);
      }
    }
  }, []);

  useEffect(() => {
    // Function to set up connection
    const setupConnection = () => {
      if (!tradeId) {
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
        } catch (error) {
          console.error("Error cleaning up existing connection:", error);
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
            channel: "TradeChannel",
            trade_id: tradeId,
          },
          {
            connected() {
              console.log(`✅ Connected to TradeChannel for trade ${tradeId}`);
              startKeepAlive();
            },
            disconnected() {
              console.log(
                `💤 Disconnected from TradeChannel for trade ${tradeId}`,
              );
              stopKeepAlive();
              setTimeout(() => {
                setupConnection();
              }, 3000);
            },
            rejected() {
              console.log(
                `❌ TradeChannel connection rejected for trade ${tradeId}`,
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
                  processedResponse.data
                ) {
                  onTradeUpdated(processedResponse.data);
                }
              };

              try {
                processResponse();
              } catch (error) {
                console.error("Error processing trade update:", error);
              }
            },
          } as unknown as Subscription,
        );
      } catch (error) {
        console.error("Error creating subscription:", error);
      }
    };

    let keepAliveInterval: NodeJS.Timeout | null = null;

    const startKeepAlive = () => {
      stopKeepAlive();
      keepAliveInterval = setInterval(() => {
        if (subscriptionRef.current) {
          try {
            subscriptionRef.current.perform("keepalive", {
              timestamp: Date.now(),
            });
          } catch (error) {
            console.error("Error in keepalive:", error);
            stopKeepAlive();
          }
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
      try {
        if (subscriptionRef.current) {
          subscriptionRef.current.unsubscribe();
        }
        if (consumerRef.current) {
          consumerRef.current.disconnect();
        }
      } catch (error) {
        console.error("Error in cleanup:", error);
      }
    };
  }, [tradeId, onTradeUpdated]);

  return {
    testPing,
  };
}
