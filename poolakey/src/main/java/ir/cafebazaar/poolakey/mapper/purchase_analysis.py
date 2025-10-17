import json
from datetime import datetime
from enum import Enum
from typing import List, Optional, Dict, Any

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.pipeline import make_pipeline
from sklearn.feature_extraction.text import CountVectorizer
from xgboost import XGBClassifier
from statsmodels.tsa.arima.model import ARIMA
from sklearn.cluster import KMeans


# ======================================================
#                PURCHASE CLASSES & MAPPER
# ======================================================

class PurchaseState(Enum):
    PURCHASED = "PURCHASED"
    REFUNDED = "REFUNDED"


class PurchaseInfo:
    def __init__(self,
                 order_id: str,
                 purchase_token: str,
                 payload: str,
                 package_name: str,
                 purchase_state: PurchaseState,
                 purchase_time: int,
                 product_id: str,
                 data_signature: str,
                 original_json: str):
        self.order_id = order_id
        self.purchase_token = purchase_token
        self.payload = payload
        self.package_name = package_name
        self.purchase_state = purchase_state
        self.purchase_time = purchase_time
        self.product_id = product_id
        self.data_signature = data_signature
        self.original_json = original_json

    def __repr__(self):
        return f"<PurchaseInfo {self.product_id} ({self.purchase_state.value})>"


class PurchaseMapper:

    @staticmethod
    def map_to_purchase_info(purchase_data: str, data_signature: str) -> PurchaseInfo:
        data = json.loads(purchase_data)
        purchase_state = (
            PurchaseState.PURCHASED if data.get("purchaseState") == 0 else PurchaseState.REFUNDED
        )

        return PurchaseInfo(
            order_id=data.get("orderId"),
            purchase_token=data.get("purchaseToken"),
            payload=data.get("developerPayload"),
            package_name=data.get("packageName"),
            purchase_state=purchase_state,
            purchase_time=data.get("purchaseTime", 0),
            product_id=data.get("productId"),
            data_signature=data_signature,
            original_json=purchase_data
        )

    @staticmethod
    def map_list(purchases: List[tuple[str, str]]) -> List[PurchaseInfo]:
        return [PurchaseMapper.map_to_purchase_info(data, sig) for data, sig in purchases]

    @staticmethod
    def get_purchase_summary(purchase_data: str) -> str:
        data = json.loads(purchase_data)
        state = "✅ PURCHASED" if data.get("purchaseState") == 0 else "❌ REFUNDED"
        formatted_time = PurchaseMapper.format_purchase_time(data.get("purchaseTime", 0))

        return (
            "━━━━━━━━━ Purchase Summary ━━━━━━━━━\n"
            f"Order ID      : {data.get('orderId')}\n"
            f"Product ID    : {data.get('productId')}\n"
            f"Purchase State: {state}\n"
            f"Purchase Time : {formatted_time}\n"
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )

    @staticmethod
    def verify_purchase_signature(purchase_data: str, signature: str, public_key: str) -> bool:
        if not signature.strip() or not purchase_data.strip():
            print("⚠️ Signature verification placeholder - not implemented!")
            return False
        return True

    @staticmethod
    def filter_by_state(purchases: List[PurchaseInfo], state: PurchaseState) -> List[PurchaseInfo]:
        return [p for p in purchases if p.purchase_state == state]

    @staticmethod
    def get_most_recent_purchase(purchases: List[PurchaseInfo]) -> Optional[PurchaseInfo]:
        return max(purchases, key=lambda p: p.purchase_time, default=None)

    @staticmethod
    def is_refunded(purchase_data: str) -> bool:
        return json.loads(purchase_data).get("purchaseState") != 0

    @staticmethod
    def get_field(purchase_data: str, field: str) -> Optional[Any]:
        return json.loads(purchase_data).get(field)

    @staticmethod
    def format_purchase_time(timestamp: int) -> str:
        if timestamp > 0:
            return datetime.fromtimestamp(timestamp / 1000).strftime("%Y-%m-%d %H:%M:%S")
        return "N/A"

    @staticmethod
    def is_purchase_for_product(purchase_data: str, product_id: str) -> bool:
        return json.loads(purchase_data).get("productId") == product_id


# ======================================================
#                   DATA ANALYSIS FUNCTIONS
# ======================================================

def purchases_to_dataframe(purchases):
    df = pd.DataFrame([
        {
            "order_id": p.order_id,
            "product_id": p.product_id,
            "package_name": p.package_name,
            "purchase_state": p.purchase_state.value,
            "purchase_time": datetime.fromtimestamp(p.purchase_time / 1000),
            "payload": p.payload,
            "signature": p.data_signature,
        }
        for p in purchases
    ])
    df["is_refunded"] = (df["purchase_state"] == "REFUNDED").astype(int)
    df["hour"] = df["purchase_time"].dt.hour
    df["day_of_week"] = df["purchase_time"].dt.day_name()
    df["date"] = df["purchase_time"].dt.date
    return df


# ======================================================
#               MAIN EXECUTION / ANALYSIS
# ======================================================

if __name__ == "__main__":

    # Example Raw JSON Purchase
    raw_json = '{"orderId":"12345","productId":"com.app.gold","purchaseState":0,"purchaseTime":1691234567890,"packageName":"com.app.demo","purchaseToken":"abc123","developerPayload":"optional"}'
    signature = "FAKE_SIGNATURE"

    mapper = PurchaseMapper()
    info = mapper.map_to_purchase_info(raw_json, signature)
    print(info)
    print(mapper.get_purchase_summary(raw_json))

    # Example purchase list for analysis
    purchases = [
        mapper.map_to_purchase_info(raw_json, signature),
        mapper.map_to_purchase_info(raw_json.replace("purchaseState\":0", "purchaseState\":1"), signature),
        mapper.map_to_purchase_info(raw_json, signature)
    ]

    df = purchases_to_dataframe(purchases)
    print("\n--- DataFrame ---\n", df)

    # Descriptive analysis
    print("\n--- Descriptive Stats ---")
    print(df.describe(include='all'))

    print("\n--- Purchase State Distribution ---")
    print(df['purchase_state'].value_counts(normalize=True) * 100)

    # Visualization
    plt.figure(figsize=(10, 4))
    df.groupby('hour')['order_id'].count().plot(kind='bar')
    plt.title("تعداد خرید بر اساس ساعت روز")
    plt.xlabel("ساعت")
    plt.ylabel("تعداد خرید")
    plt.show()

    plt.figure(figsize=(5, 5))
    df['purchase_state'].value_counts().plot(kind='pie', autopct='%1.1f%%', startangle=90)
    plt.title("درصد خریدهای موفق در برابر برگشتی")
    plt.ylabel("")
    plt.show()

    # Machine Learning Preparation
    le_product = LabelEncoder()
    df["product_id_encoded"] = le_product.fit_transform(df["product_id"])
    le_day = LabelEncoder()
    df["day_of_week_encoded"] = le_day.fit_transform(df["day_of_week"])

    X = df[["hour", "product_id_encoded", "day_of_week_encoded"]]
    y = df["is_refunded"]

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    # Random Forest Model
    model = RandomForestClassifier(n_estimators=200, random_state=42)
    model.fit(X_train, y_train)
    y_pred = model.predict(X_test)
    print("\n--- RandomForest Results ---")
    print(classification_report(y_test, y_pred))
    print("Confusion Matrix:\n", confusion_matrix(y_test, y_pred))

    # Feature Importance
    importances = model.feature_importances_
    features = X.columns
    sorted_indices = np.argsort(importances)[::-1]
    print("\n--- Feature Importance ---")
    for idx in sorted_indices:
        print(f"{features[idx]}: {importances[idx]*100:.2f}%")

    # XGBoost Model
    xgb = XGBClassifier(use_label_encoder=False, eval_metric='logloss')
    xgb.fit(X_train, y_train)
    print("\nXGBoost Accuracy:", xgb.score(X_test, y_test))

    # Time Series Forecasting
    daily = df.groupby('date')['order_id'].count().reset_index()
    if len(daily) > 2:
        model_arima = ARIMA(daily['order_id'], order=(1, 1, 0))
        model_fit = model_arima.fit()
        forecast = model_fit.forecast(steps=7)
        print("\nForecast (Next 7 days):", forecast)

    # User Behavior Clustering
    X_features = df[["hour", "is_refunded"]]
    kmeans = KMeans(n_clusters=2, random_state=42)
    df["user_cluster"] = kmeans.fit_predict(X_features)
    print("\n--- Cluster Refund Rates ---")
    print(df.groupby("user_cluster")["is_refunded"].mean())
