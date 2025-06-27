import { useEffect, useRef, useCallback } from "react";
import { createActionCableConsumer } from "@/lib/api/action-cable";
import type { FiatMint } from "@/lib/api/merchant";
import type { Subscription, Consumer } from "@rails/actioncable";

interface UseFiatMintChannelProps {
  fiatMintId: string;
  onFiatMintUpdated: (fiatMint: FiatMint) => void;
}

interface ResponseWithData {
  status?: string;
  data?: FiatMint;
  message?: string;
  [key: string]: unknown;
}

export function useFiatMintChannel({
  fiatMintId,
  onFiatMintUpdated,
}: UseFiatMintChannelProps) {
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
    const setupConnection = () => {
      if (!fiatMintId) {
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
            channel: "MerchantFiatMintChannel",
            fiat_mint_id: fiatMintId,
          },
          {
            connected() {
              startKeepAlive();
            },
            disconnected() {
              stopKeepAlive();
              setTimeout(() => {
                setupConnection();
              }, 3000);
            },
            rejected() {
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
                  } catch (error) {
                    console.error("Failed to parse string response:", error);
                    processedResponse = {} as ResponseWithData;
                  }
                } else {
                  processedResponse = response;
                }

                if (processedResponse.data) {
                  onFiatMintUpdated(processedResponse.data);
                }
              };

              try {
                processResponse();
              } catch (error) {
                console.error("Error processing fiat mint update:", error);
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
  }, [fiatMintId, onFiatMintUpdated]);

  return {
    testPing,
  };
}
