import { memo } from "react";
import { useTranslations } from "next-intl";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

export type TabStatus = "all" | "open" | "closed" | "pending" | "error";

interface StatusTabsProps {
  currentTab: TabStatus;
  onChange: (value: TabStatus) => void;
  children: React.ReactNode;
}

const StatusTabs = memo(
  ({ currentTab, onChange, children }: StatusTabsProps) => {
    const t = useTranslations("liquidity");

    return (
      <Tabs
        value={currentTab}
        className="mb-6"
        onValueChange={(value) => onChange(value as TabStatus)}
      >
        <TabsList className="w-full justify-start mb-4">
          <TabsTrigger
            value="all"
            className={
              currentTab === "all" ? "bg-primary text-primary-foreground" : ""
            }
          >
            {t("statusAll")}
          </TabsTrigger>
          <TabsTrigger
            value="open"
            className={
              currentTab === "open" ? "bg-primary text-primary-foreground" : ""
            }
          >
            {t("statusOpen")}
          </TabsTrigger>
          <TabsTrigger
            value="pending"
            className={
              currentTab === "pending"
                ? "bg-primary text-primary-foreground"
                : ""
            }
          >
            {t("statusPending")}
          </TabsTrigger>
          <TabsTrigger
            value="closed"
            className={
              currentTab === "closed"
                ? "bg-primary text-primary-foreground"
                : ""
            }
          >
            {t("statusClosed")}
          </TabsTrigger>
          <TabsTrigger
            value="error"
            className={
              currentTab === "error" ? "bg-primary text-primary-foreground" : ""
            }
          >
            {t("statusError")}
          </TabsTrigger>
        </TabsList>
        {children}
      </Tabs>
    );
  },
);

StatusTabs.displayName = "StatusTabs";

export default StatusTabs;
