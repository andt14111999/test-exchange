"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatFiatAmount } from "@/lib/utils/index";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  getMerchantOffers,
  deleteOffer,
  Offer,
  setOfferOnlineStatus,
  enableOffer,
  disableOffer,
} from "@/lib/api/merchant";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { handleApiError } from "@/lib/utils/error-handler";
import { Badge } from "@/components/ui/badge";

export default function ManageOffers() {
  const router = useRouter();
  const t = useTranslations("merchant");
  const [offers, setOffers] = useState<Offer[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Helper function to check if an offer is disabled
  const isOfferDisabled = (offer: Offer): boolean => {
    return offer.disabled === true || offer.status === "disabled";
  };

  useEffect(() => {
    fetchOffers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function getOfferStatus(offer: Offer): string {
    if (offer.status) return offer.status;
    if (offer.deleted) return "deleted";
    if (offer.disabled) return "disabled";
    if (offer.online === false && offer.is_active === false) return "inactive";
    return "active";
  }

  async function fetchOffers() {
    try {
      setIsLoading(true);
      const response = await getMerchantOffers();

      // Process API response
      let apiData: Offer[] = [];
      if (Array.isArray(response)) {
        apiData = response;
      } else if (response.data) {
        apiData = Array.isArray(response.data)
          ? response.data
          : [response.data];
      }

      // Add missing statuses for display purposes if needed
      apiData = apiData.map((offer) => {
        // If the status isn't explicitly set, derive it from other fields
        if (!offer.status) {
          offer.status = getOfferStatus(offer);
        }
        return offer;
      });

      setOffers(apiData);
    } catch (error) {
      handleApiError(error, t("fetchOffersFailed"));
      setOffers([]);
    } finally {
      setIsLoading(false);
    }
  }

  // Helper function to convert string values to numbers
  const toNumber = (val: string | number | undefined): number => {
    if (val === undefined) return 0;
    if (typeof val === "number") return val;
    const num = parseFloat(val);
    return isNaN(num) ? 0 : num;
  };

  async function handleToggleOnlineStatus(offer: Offer) {
    try {
      // Toggle online status
      const newOnlineStatus = !offer.online;

      await setOfferOnlineStatus(offer.id, newOnlineStatus);
      toast.success(t("offerStatusUpdated"));
      await fetchOffers(); // Refresh the list
    } catch (error) {
      handleApiError(error, t("updateOfferFailed"));
    }
  }

  async function handleToggleActiveStatus(offer: Offer) {
    try {
      if (isOfferDisabled(offer)) {
        // Enable the offer
        await enableOffer(offer.id);
      } else {
        // Disable the offer
        await disableOffer(offer.id);
      }

      toast.success(t("offerStatusUpdated"));
      await fetchOffers(); // Refresh the list
    } catch (error) {
      handleApiError(error, t("updateOfferFailed"));
    }
  }

  async function handleDeleteOffer(id: number) {
    try {
      await deleteOffer(id);
      toast.success(t("offerDeleted"));
      fetchOffers(); // Refresh the list
    } catch (error) {
      handleApiError(error, t("deleteOfferFailed"));
    }
  }

  const TYPE_COLORS = {
    buy: "bg-green-100 text-green-800",
    sell: "bg-red-100 text-red-800",
  } as const;

  const STATUS_COLORS = {
    active: "bg-emerald-100 text-emerald-800 border-emerald-200",
    inactive: "bg-gray-100 text-gray-800 border-gray-200",
    disabled: "bg-yellow-100 text-yellow-800 border-yellow-200",
    online: "bg-blue-100 text-blue-800 border-blue-200",
    offline: "bg-slate-100 text-slate-800 border-slate-200",
  } as const;

  // Filter out deleted offers
  const filteredOffers = offers.filter((offer) => {
    return offer.status !== "deleted" && !offer.deleted;
  });

  // Active offers (not disabled)
  const activeOffers = filteredOffers.filter(
    (offer) => !isOfferDisabled(offer),
  );

  // Disabled offers
  const inactiveOffers = filteredOffers.filter((offer) =>
    isOfferDisabled(offer),
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">
          {t("manageOffers")}
        </h1>
        <Link href="/merchant/create-offer">
          <Button>{t("createNewOffer")}</Button>
        </Link>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <p>{t("loading")}</p>
        </div>
      ) : (
        <>
          <Card>
            <CardHeader>
              <CardTitle>{t("activeOffers")}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {activeOffers.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    {t("noActiveOffers")}
                  </p>
                ) : (
                  activeOffers.map((offer) => (
                    <div
                      key={offer.id}
                      className="flex items-center justify-between rounded-lg border p-4"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span
                            className={`rounded-full px-2 py-1 text-xs ${
                              TYPE_COLORS[
                                offer.offer_type as keyof typeof TYPE_COLORS
                              ] || "bg-gray-100 text-gray-800"
                            }`}
                          >
                            {offer.offer_type.toUpperCase()} {offer.currency}
                          </span>
                          <Badge className={STATUS_COLORS.active}>
                            {t("active")}
                          </Badge>
                          <Badge
                            className={
                              offer.online
                                ? STATUS_COLORS.online
                                : STATUS_COLORS.offline
                            }
                          >
                            {offer.online ? t("online") : t("offline")}
                          </Badge>
                        </div>
                        <p className="text-sm text-muted-foreground">
                          {t("amount")}:{" "}
                          {formatFiatAmount(
                            toNumber(
                              offer.available_amount || offer.total_amount,
                            ),
                            offer.currency,
                          )}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {t("limit")}:{" "}
                          {formatFiatAmount(
                            toNumber(offer.min_amount),
                            offer.currency,
                          )}{" "}
                          -{" "}
                          {formatFiatAmount(
                            toNumber(offer.max_amount),
                            offer.currency,
                          )}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            router.push(`/merchant/create-offer?id=${offer.id}`)
                          }
                        >
                          {t("edit")}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleToggleActiveStatus(offer)}
                        >
                          {isOfferDisabled(offer) ? t("enable") : t("disable")}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleToggleOnlineStatus(offer)}
                        >
                          {offer.online ? t("setOffline") : t("setOnline")}
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDeleteOffer(offer.id)}
                        >
                          {t("delete")}
                        </Button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>

          {inactiveOffers.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>{t("inactiveOffers")}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {inactiveOffers.map((offer) => (
                    <div
                      key={offer.id}
                      className="flex items-center justify-between rounded-lg border p-4 opacity-80"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span
                            className={`rounded-full px-2 py-1 text-xs ${
                              TYPE_COLORS[
                                offer.offer_type as keyof typeof TYPE_COLORS
                              ] || "bg-gray-100 text-gray-800"
                            }`}
                          >
                            {offer.offer_type.toUpperCase()} {offer.currency}
                          </span>
                          <Badge
                            className={
                              isOfferDisabled(offer)
                                ? STATUS_COLORS.disabled
                                : STATUS_COLORS.inactive
                            }
                          >
                            {isOfferDisabled(offer)
                              ? t("disabled")
                              : t("inactive")}
                          </Badge>
                          <Badge
                            className={
                              offer.online
                                ? STATUS_COLORS.online
                                : STATUS_COLORS.offline
                            }
                          >
                            {offer.online ? t("online") : t("offline")}
                          </Badge>
                        </div>
                        <p className="text-sm text-muted-foreground">
                          {t("amount")}:{" "}
                          {formatFiatAmount(
                            toNumber(
                              offer.available_amount || offer.total_amount,
                            ),
                            offer.currency,
                          )}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {t("limit")}:{" "}
                          {formatFiatAmount(
                            toNumber(offer.min_amount),
                            offer.currency,
                          )}{" "}
                          -{" "}
                          {formatFiatAmount(
                            toNumber(offer.max_amount),
                            offer.currency,
                          )}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            router.push(`/merchant/create-offer?id=${offer.id}`)
                          }
                        >
                          {t("edit")}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleToggleActiveStatus(offer)}
                        >
                          {isOfferDisabled(offer) ? t("enable") : t("disable")}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleToggleOnlineStatus(offer)}
                        >
                          {offer.online ? t("setOffline") : t("setOnline")}
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDeleteOffer(offer.id)}
                        >
                          {t("delete")}
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
