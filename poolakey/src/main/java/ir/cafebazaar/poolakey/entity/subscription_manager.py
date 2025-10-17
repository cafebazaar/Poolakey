from sklearn.linear_model import LogisticRegression

class SubscriptionManager:
    def __init__(self):
        self.purchases: List[PurchaseInfo] = []
        self.skus: List[SkuDetails] = []
        self.trials: List[TrialSubscriptionInfo] = []

    # ------------------- Adders -------------------
    def add_purchase(self, purchase: PurchaseInfo):
        self.purchases.append(purchase)

    def add_sku(self, sku: SkuDetails):
        self.skus.append(sku)

    def add_trial(self, trial: TrialSubscriptionInfo):
        self.trials.append(trial)

    # ------------------- Purchase Analysis -------------------
    def purchase_frequency(self):
        df = pd.DataFrame([p.purchase_state for p in self.purchases], columns=["state"])
        return df.value_counts()

    def refund_ratio(self) -> float:
        if not self.purchases:
            return 0.0
        refunded = sum(p.is_refunded() for p in self.purchases)
        return refunded / len(self.purchases)

    def revenue_estimation(self) -> float:
        total = 0.0
        for p in self.purchases:
            try:
                total += float(''.join(filter(lambda x: x.isdigit() or x=='.', p.product_id)))
            except:
                continue
        return total

    def purchases_over_time(self, freq: str = "D"):
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

    # ------------------- SKU Analysis -------------------
    def most_expensive_sku(self) -> Optional[SkuDetails]:
        if not self.skus:
            return None
        return max(self.skus, key=lambda s: float(''.join(filter(str.isdigit, s.price)) or 0))

    def least_expensive_sku(self) -> Optional[SkuDetails]:
        if not self.skus:
            return None
        return min(self.skus, key=lambda s: float(''.join(filter(str.isdigit, s.price)) or float('inf')))

    def subscription_vs_one_time(self) -> dict:
        subs = sum(s.is_subscription() for s in self.skus)
        one_time = len(self.skus) - subs
        return {"subscription": subs, "one_time": one_time}

    def refund_probability_by_type(self):
        if not self.purchases or not self.skus:
            return {}
        data = []
        sku_dict = {s.sku: s.type for s in self.skus}
        for p in self.purchases:
            sku_type = sku_dict.get(p.product_id, "unknown")
            data.append({"type": sku_type, "refunded": int(p.is_refunded())})
        df = pd.DataFrame(data)
        return df.groupby("type")["refunded"].mean().to_dict()

    # ------------------- Trial Analysis -------------------
    def available_trials_count(self) -> int:
        return sum(t.can_use_trial() for t in self.trials)

    def average_trial_days(self) -> float:
        if not self.trials:
            return 0.0
        return sum(t.trial_period_days for t in self.trials) / len(self.trials)

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
        print("=== Classification Report ===")
        print(classification_report(y_test, y_pred))
        print("=== Confusion Matrix ===")
        print(confusion_matrix(y_test, y_pred))
        return model

    def train_trial_conversion_model(self):
        if not self.trials:
            print("No trial data to train.")
            return None
        # Example: create a synthetic feature set for ML
        df = pd.DataFrame([{
            "trial_days": t.trial_period_days,
            "converted": int(t.can_use_trial())  # placeholder: in reality, track real conversion
        } for t in self.trials])
        X = df[["trial_days"]]
        y = df["converted"]
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        model = LogisticRegression()
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        print("=== Trial Conversion Report ===")
        print(classification_report(y_test, y_pred))
        return model

    # ------------------- Export Utilities -------------------
    def export_purchases_csv(self, filepath: str):
        df = pd.DataFrame([{
            "order_id": p.order_id,
            "product_id": p.product_id,
            "state": p.purchase_state,
            "timestamp": p.purchase_time
        } for p in self.purchases])
        df.to_csv(filepath, index=False)
        print(f"Purchases exported to {filepath}")

    def export_trials_csv(self, filepath: str):
        df = pd.DataFrame([{
            "available": t.is_available,
            "days": t.trial_period_days,
            "ends_on": t.trial_end_date()
        } for t in self.trials])
        df.to_csv(filepath, index=False)
        print(f"Trials exported to {filepath}")
