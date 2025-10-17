import json
import datetime
import csv
from typing import List, Dict, Optional
from collections import Counter
import matplotlib.pyplot as plt
import pandas as pd
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix

class AndroidException:
    def __init__(self, name: str, message: str, type_: str):
        self.name = name
        self.message = message
        self.type = type_
        self.timestamp = datetime.datetime.now()

    def to_dict(self) -> Dict:
        return {
            "name": self.name,
            "message": self.message,
            "type": self.type,
            "timestamp": self.timestamp.isoformat()
        }

    def to_json(self, pretty: bool = True) -> str:
        return json.dumps(self.to_dict(), indent=4) if pretty else json.dumps(self.to_dict())

class ExceptionManager:
    def __init__(self):
        self.exceptions: List[AndroidException] = []

    def add_exception(self, exception: AndroidException):
        self.exceptions.append(exception)

    def generate_report(self) -> str:
        total = len(self.exceptions)
        types = Counter(ex.type for ex in self.exceptions)
        report_lines = [f"=== Android Exception Report ===", f"Total Exceptions: {total}", f"Unique Types: {len(types)}", "-"*35]
        for ex in self.exceptions:
            report_lines.extend([
                f"Name      : {ex.name}",
                f"Message   : {ex.message}",
                f"Type      : {ex.type}",
                f"Timestamp : {ex.timestamp}",
                "-"*35
            ])
        return "\n".join(report_lines)

    def save_to_json(self, filepath: str):
        data = [ex.to_dict() for ex in self.exceptions]
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4)

    def save_to_csv(self, filepath: str):
        if not self.exceptions:
            return
        keys = self.exceptions[0].to_dict().keys()
        with open(filepath, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=keys)
            writer.writeheader()
            for ex in self.exceptions:
                writer.writerow(ex.to_dict())

    # ================= Analysis =================
    def exception_frequency(self) -> Dict[str, int]:
        return dict(Counter(ex.type for ex in self.exceptions))

    def exceptions_over_time(self, freq: str = 'D') -> pd.DataFrame:
        if not self.exceptions:
            return pd.DataFrame(columns=['count'])
        timestamps = [ex.timestamp for ex in self.exceptions]
        df = pd.DataFrame({"timestamp": timestamps})
        df.set_index("timestamp", inplace=True)
        return df.resample(freq).size().rename("count").to_frame()

    def plot_exceptions_over_time(self, freq: str = 'D'):
        df = self.exceptions_over_time(freq)
        if df.empty:
            print("No data to plot.")
            return
        plt.figure(figsize=(12, 6))
        plt.bar(df.index, df['count'], color='skyblue', edgecolor='navy')
        plt.title("Android Exceptions Over Time", fontsize=16)
        plt.xlabel("Time")
        plt.ylabel("Number of Exceptions")
        plt.grid(axis='y', linestyle='--', alpha=0.7)
        plt.tight_layout()
        plt.show()

    def plot_exception_distribution(self):
        if not self.exceptions:
            print("No data to plot.")
            return
        freq = self.exception_frequency()
        plt.figure(figsize=(8, 6))
        plt.bar(freq.keys(), freq.values(), color='coral', edgecolor='black')
        plt.title("Exception Type Distribution", fontsize=14)
        plt.xlabel("Exception Type")
        plt.ylabel("Count")
        plt.xticks(rotation=45, ha='right')
        plt.grid(axis='y', linestyle='--', alpha=0.5)
        plt.tight_layout()
        plt.show()

    # ================= Machine Learning =================
    def prepare_ml_dataset(self) -> pd.DataFrame:
        if not self.exceptions:
            return pd.DataFrame(), None
        df = pd.DataFrame([{
            "name": ex.name,
            "message": ex.message,
            "type": ex.type,
            "hour": ex.timestamp.hour,
            "day_of_week": ex.timestamp.weekday(),
            "month": ex.timestamp.month
        } for ex in self.exceptions])
        le = LabelEncoder()
        df['type_encoded'] = le.fit_transform(df['type'])
        return df, le

    def train_predictive_model(self):
        df, le = self.prepare_ml_dataset()
        if df.empty:
            print("No data to train.")
            return None, None
        X = df[['hour', 'day_of_week', 'month']]
        y = df['type_encoded']

        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        model = RandomForestClassifier(n_estimators=100, random_state=42)
        model.fit(X_train, y_train)

        y_pred = model.predict(X_test)
        print("=== Classification Report ===")
        print(classification_report(y_test, y_pred, target_names=le.classes_))
        print("=== Confusion Matrix ===")
        print(confusion_matrix(y_test, y_pred))

        return model, le

# ================= Sample Usage =================
if __name__ == "__main__":
    manager = ExceptionManager()
    manager.add_exception(AndroidException("BazaarNotSupportedException", "Bazaar is not updated", "IllegalStateException"))
    manager.add_exception(AndroidException("ConsumeFailedException", "Consume request failed", "RemoteException"))
    manager.add_exception(AndroidException("DisconnectException", "Bazaar service disconnected", "IllegalStateException"))
    manager.add_exception(AndroidException("DynamicPriceNotSupportedException", "Dynamic pricing not supported", "IllegalStateException"))

    print(manager.generate_report())
    print("Frequency:", manager.exception_frequency())

    # Plots
    manager.plot_exceptions_over_time()
    manager.plot_exception_distribution()

    # Train ML model
    model, encoder = manager.train_predictive_model()
