import json
from datetime import datetime, timedelta
from typing import List, Dict, Optional
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix

# ===================== Constants =====================
class BazaarConstants:
    RESPONSE_CODE = "RESPONSE_CODE"
    RESPONSE_RESULT_OK = 0
    RESPONSE_PURCHASE_DATA = "INAPP_PURCHASE_DATA"
    RESPONSE_SIGNATURE_DATA = "INAPP_DATA_SIGNATURE"
    RESPONSE_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN"
    RESPONSE_PURCHASE_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST"
    RESPONSE_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
    RESPONSE_DATA_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST"
    RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST"
    RESPONSE_CHECK_TRIAL_SUBSCRIPTION_DATA = "CHECK_TRIAL_SUBSCRIPTION_DATA"
    REQUEST_SKU_DETAILS_LIST = "ITEM_ID_LIST"
    RESPONSE_DYNAMIC_PRICE_TOKEN = "DYNAMIC_PRICE_TOKEN"
    RESPONSE_CUTOUT_MODE_IS_SHORT_EDGES = "CUTOUT_MODE_IS_SHORT_EDGES"
    IN_APP_BILLING_VERSION = 3
    BAZAAR_PACKAGE_NAME = "com.farsitel.bazaar"
    BAZAAR_PAYMENT_SERVICE_CLASS_NAME = "com.farsitel.bazaar.inappbilling.service.InAppBillingService"

# ===================== JSON Utilities =====================
class RawJson:
    ORDER_ID = "orderId"
    PURCHASE_TOKEN = "purchaseToken"
    DEVELOPER_PAYLOAD = "developerPayload"
    PACKAGE_NAME = "packageName"
    PURCHASE_STATE = "purchaseState"
    PURCHASE_TIME = "purchaseTime"
    PRODUCT_ID = "productId"

    @staticmethod
    def get_string(json_obj: dict, key: str) -> Optional[str]:
        return str(json_obj.get(key)) if key in json_obj else None

    @staticmethod
    def get_long(json_obj: dict, key: str) -> int:
        return int(json_obj.get(key, 0))

# ===================== Purchase Model =====================
class PurchaseInfo:
    def __init__(self, data: dict):
        self.order_id = RawJson.get_string(data, RawJson.ORDER_ID) or "N/A"
        self.product_id = RawJson.get_string(data, RawJson.PRODUCT_ID) or "N/A"
        self.purchase_state = (RawJson.get_string(data, RawJson.PURCHASE_STATE) or "UNKNOWN").upper()
        self.purchase_time = datetime.fromtimestamp(RawJson.get_long(data, RawJson.PURCHASE_TIME))
        self.purchase_token = RawJson.get_string(data, RawJson.PURCHASE_TOKEN) or ""
        self.raw_data = data

    def is_valid(self) -> bool:
        return self.purchase_state == "PURCHASED" and bool(self.purchase_token)

    def is_refunded(self) -> bool:
        return self.purchase_state == "REFUNDED"

    def pretty_display(self) -> str:
        token_status = "Valid" if self.purchase_token else "Missing"
        return (
            f"╔═════════ Purchase Info ═════════╗\n"
            f"║ Order ID   : {self.order_id}\n"
            f"║ Product ID : {self.product_id}\n"
            f"║ Status     : {self.purchase_state}\n"
            f"║ Purchased  : {self.purchase_time}\n"
            f"║ Token      : {token_status}\n"
            f"╚═════════════════════════════════╝"
        )

# ===================== Manager for Analytics & ML =====================
class SubscriptionManager:
    def __init__(self):
        self.purchases: List[PurchaseInfo] = []

    def add_purchase(self, purchase: PurchaseInfo):
        self.purchases.append(purchase)

    # ------------------- Data Analysis -------------------
    def purchase_frequency(self):
        df = pd.DataFrame([p.purchase_state for p in self.purchases], columns=["state"])
        return df.value_counts()

    def refund_ratio(self) -> float:
        if not self.purchases:
            return 0.0
        refunded = sum(p.is_refunded() for p in self.purchases)
        return refunded / len(self.purchases)

    def purchases_over_time(self, freq: str = "D") -> pd.DataFrame:
        if not self.purchases:
            return pd.DataFrame()
        df = pd.DataFrame([{"time": p.purchase_time} for p in self.purchases])
        df.set_index("time", inplace=True)
        return df.resample(freq).size().rename("count").to_frame()

    def plot_purchases_over_time(self, freq: str = "D"):
        df = self.purchases_over_time(freq)
        if df.empty:
            print("No purchases to plot.")
            return
        df.plot(kind="bar", figsize=(10, 5), color="skyblue")
        plt.title(f"Purchases Over Time ({freq})")
        plt.xlabel("Date")
        plt.ylabel("Count")
        plt.show()

    # ------------------- Machine Learning -------------------
    def train_refund_model(self):
        if not self.purchases:
            print("No data to train.")
            return None
        df = pd.DataFrame([{
            "hour": p.purchase_time.hour,
            "day_of_week": p.purchase_time.weekday(),
            "is_refunded": int(p.is_refunded())
        } for p in self.purchases])
        X = df[["hour", "day_of_week"]]
        y = df["is_refunded"]
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        model = RandomForestClassifier(n_estimators=100, random_state=42)
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        print("=== Refund Prediction Report ===")
        print(classification_report(y_test, y_pred))
        print("=== Confusion Matrix ===")
        print(confusion_matrix(y_test, y_pred))
        return model

    def train_trial_conversion_model(self, trials_data: List[dict]):
        if not trials_data:
            print("No trial data to train.")
            return None
        df = pd.DataFrame([{
            "trial_days": t.get("trial_period_days", 0),
            "converted": int(t.get("converted", 0))
        } for t in trials_data])
        X = df[["trial_days"]]
        y = df["converted"]
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        model = LogisticRegression()
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        print("=== Trial Conversion Report ===")
        print(classification_report(y_test, y_pred))
        return model

    # ------------------- Extended Analytics & ML -------------------
    def revenue_estimation(self) -> float:
        total = 0.0
        for p in self.purchases:
            try:
                total += float(''.join(filter(str.isdigit, p.product_id)) or 0)
            except:
                continue
        return total

    def subscription_vs_one_time(self, subscription_ids: List[str]) -> dict:
        subs = sum(p.product_id in subscription_ids for p in self.purchases)
        one_time = len(self.purchases) - subs
        return {"subscription": subs, "one_time": one_time}

    def purchase_hour_distribution(self):
        if not self.purchases:
            return pd.Series(dtype=int)
        df = pd.DataFrame([p.purchase_time.hour for p in self.purchases], columns=["hour"])
        return df.value_counts().sort_index()

    def day_of_week_distribution(self):
        if not self.purchases:
            return pd.Series(dtype=int)
        df = pd.DataFrame([p.purchase_time.weekday() for p in self.purchases], columns=["weekday"])
        return df.value_counts().sort_index()

    def train_advanced_refund_model(self):
        if not self.purchases:
            print("No data to train.")
            return None
        df = pd.DataFrame([{
            "hour": p.purchase_time.hour,
            "day_of_week": p.purchase_time.weekday(),
            "is_refunded": int(p.is_refunded()),
            "subscription": int(p.product_id.startswith("sub"))
        } for p in self.purchases])
        X = df[["hour", "day_of_week", "subscription"]]
        y = df["is_refunded"]
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        model = RandomForestClassifier(n_estimators=200, random_state=42, class_weight="balanced")
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        print("=== Advanced Refund Prediction ===")
        print(classification_report(y_test, y_pred))
        print(confusion_matrix(y_test, y_pred))
        return model

    def export_purchases_csv(self, filepath: str):
        df = pd.DataFrame([{
            "order_id": p.order_id,
            "product_id": p.product_id,
            "status": p.purchase_state,
            "timestamp": p.purchase_time,
            "token_valid": bool(p.purchase_token)
        } for p in self.purchases])
        df.to_csv(filepath, index=False)
        print(f"Purchases exported to {filepath}")

    def plot_hour_distribution(self):
        dist = self.purchase_hour_distribution()
        if dist.empty:
            print("No data to plot.")
            return
        dist.plot(kind="bar", figsize=(10,5), color="orange")
        plt.title("Purchases by Hour")
        plt.xlabel("Hour of Day")
        plt.ylabel("Number of Purchases")
        plt.show()

# ===================== Example Usage =====================
if __name__ == "__main__":
    # Sample purchases
    sample_data = [
        {"orderId": "order1", "productId": "prod1", "purchaseState": "PURCHASED", "purchaseTime": 1697577600, "purchaseToken": "token1"},
        {"orderId": "order2", "productId": "prod2", "purchaseState": "REFUNDED", "purchaseTime": 1697664000, "purchaseToken": ""}
    ]
    manager = SubscriptionManager()
    for d in sample_data:
        manager.add_purchase(PurchaseInfo(d))

    # Display info
    for p in manager.purchases:
        print(p.pretty_display())

    # Analytics
    print("Purchase frequency:\n", manager.purchase_frequency())
    print("Refund ratio:", manager.refund_ratio())
    manager.plot_purchases_over_time()

    # ML training
    manager.train_refund_model()
