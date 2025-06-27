import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MOCK_OFFERS, MOCK_TRADES } from "@/lib/constants/mock-data";

// Update the color mappings to match marketplace styling
const STATUS_COLORS = {
  completed: "bg-green-100 text-green-800",
  pending: "bg-yellow-100 text-yellow-800",
  failed: "bg-red-100 text-red-800",
  cancelled: "bg-gray-100 text-gray-800",
} as const;

const TYPE_COLORS = {
  buy: "bg-green-100 text-green-800",
  sell: "bg-red-100 text-red-800",
} as const;

export default function MerchantDashboard() {
  const activeOffers = MOCK_OFFERS.filter((offer) => offer.isActive);
  const recentTrades = MOCK_TRADES.slice(0, 5);

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      <Card className="col-span-full">
        <CardHeader>
          <CardTitle>Merchant Dashboard</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                Active Offers
              </p>
              <p className="text-2xl font-bold">{activeOffers.length}</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                Total Transactions
              </p>
              <p className="text-2xl font-bold">{MOCK_TRADES.length}</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                Total Volume
              </p>
              <p className="text-2xl font-bold">
                {MOCK_TRADES.reduce((acc, trade) => acc + trade.amount, 0)} USDT
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className="col-span-full md:col-span-1">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Active Offers</CardTitle>
          <Link href="/merchant/create-offer">
            <Button variant="outline" size="sm">
              Create Offer
            </Button>
          </Link>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {activeOffers.map((offer) => (
              <div
                key={offer.id}
                className="flex items-center justify-between space-x-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        TYPE_COLORS[
                          offer.type.toLowerCase() as keyof typeof TYPE_COLORS
                        ]
                      }`}
                    >
                      {offer.type} {offer.fiatCurrency}
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    Amount: {offer.amount.toLocaleString()} {offer.fiatCurrency}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Limit: {offer.minAmount.toLocaleString()} -{" "}
                    {offer.maxAmount.toLocaleString()} {offer.fiatCurrency}
                  </p>
                </div>
                <Link href={`/merchant/create-offer?id=${offer.id}`}>
                  <Button variant="ghost" size="sm">
                    Edit
                  </Button>
                </Link>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card className="col-span-full md:col-span-1">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">
            Recent Transactions
          </CardTitle>
          <Link href="/merchant/transactions">
            <Button variant="outline" size="sm">
              View All
            </Button>
          </Link>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {recentTrades.map((trade) => (
              <div
                key={trade.id}
                className="flex items-center justify-between space-x-4"
              >
                <div className="space-y-1">
                  <p className="text-sm font-medium leading-none">
                    {trade.amount.toLocaleString()} {trade.fiatCurrency}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Status:{" "}
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        STATUS_COLORS[
                          trade.status.toLowerCase() as keyof typeof STATUS_COLORS
                        ]
                      }`}
                    >
                      {trade.status}
                    </span>
                  </p>
                </div>
                <Link href={`/merchant/transactions/${trade.id}`}>
                  <Button variant="ghost" size="sm">
                    View
                  </Button>
                </Link>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
