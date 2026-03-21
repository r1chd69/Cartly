from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from webdriver_manager.chrome import ChromeDriverManager
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import time
import json

def init_firebase():
    cred = credentials.Certificate("C:\\Users\\Richard\\cartly-firebase.json")
    firebase_admin.initialize_app(cred)
    return firestore.client()

def categorize(name):
    name_lower = name.lower()
    if any(word in name_lower for word in ["milk", "cheese", "yoghurt", "butter", "cream", "dairy"]):
        return "Dairy"
    elif any(word in name_lower for word in ["bread", "roll", "bun", "bagel", "toast"]):
        return "Bread & Bakery"
    elif any(word in name_lower for word in ["chicken", "beef", "lamb", "pork", "mince", "braai", "meat", "sausage", "wors"]):
        return "Meat & Poultry"
    elif any(word in name_lower for word in ["apple", "banana", "orange", "tomato", "potato", "vegetable", "fruit", "lettuce"]):
        return "Fruit & Veg"
    elif any(word in name_lower for word in ["cola", "juice", "water", "drink", "soda", "beer", "wine", "cider", "seltzer", "cooler", "spritzer", "guarana"]):
        return "Drinks"
    elif any(word in name_lower for word in ["oil", "cooking", "sunflower", "olive"]):
        return "Cooking & Oils"
    elif any(word in name_lower for word in ["chips", "chocolate", "sweets", "biscuit", "snack", "cookie"]):
        return "Snacks & Treats"
    elif any(word in name_lower for word in ["washing", "cleaning", "soap", "detergent", "bleach"]):
        return "Household"
    else:
        return "Other"

def scrape_checkers(db):
    print("Starting Checkers scraper...")
    options = webdriver.ChromeOptions()
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-blink-features=AutomationControlled")
    options.add_experimental_option("excludeSwitches", ["enable-automation"])
    options.add_experimental_option("useAutomationExtension", False)
    options.add_argument("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")

    driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=options)
    driver.execute_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

    try:
        print("Opening Checkers specials page...")
        driver.get("https://www.checkers.co.za/specials")
        print("Waiting 20 seconds for products to load...")
        time.sleep(20)

        print("Page title: " + driver.title)

        products = driver.find_elements(By.CSS_SELECTOR, "div[class*='DsB3']")
        print("Found " + str(len(products)) + " product cards")

        deals = []
        for product in products:
            try:
                name = ""
                price_now = ""
                price_was = ""

                try:
                    name_elem = product.find_element(By.CSS_SELECTOR, "a[data-testid='product-card-link']")
                    name = name_elem.get_attribute("aria-label") or name_elem.text
                except:
                    pass

                if not name:
                    try:
                        name = product.find_element(By.CSS_SELECTOR, "p[class*='name']").text
                    except:
                        pass

                if not name:
                    try:
                        name = product.find_element(By.CSS_SELECTOR, "h3").text
                    except:
                        pass

                try:
                    price_elems = product.find_elements(By.CSS_SELECTOR, "span[class*='price']")
                    if price_elems:
                        price_now = price_elems[0].text
                    if len(price_elems) > 1:
                        price_was = price_elems[1].text
                except:
                    pass

                if not price_now:
                    try:
                        price_now = product.find_element(By.CSS_SELECTOR, "span[class*='Price']").text
                    except:
                        pass

                if name and len(name) > 3 and price_now:
                    category = categorize(name)
                    deals.append({
                        "name": name,
                        "price_now": price_now,
                        "price_was": price_was,
                        "store": "Checkers",
                        "category": category,
                        "emoji": "🛒",
                        "distance": "Nearby"
                    })
                    print("- " + name + " | " + price_now + " | " + category)

            except Exception as e:
                pass

        print("Total deals scraped: " + str(len(deals)))

        print("Pushing to Firebase...")
        batch = db.batch()

        existing = db.collection("deals").where("store", "==", "Checkers").get()
        for doc in existing:
            batch.delete(doc.reference)
        batch.commit()

        for i, deal in enumerate(deals):
            doc_ref = db.collection("deals").document("checkers_" + str(i))
            db.collection("deals").document("checkers_" + str(i)).set(deal)

        print("Successfully pushed " + str(len(deals)) + " deals to Firebase!")

    except Exception as e:
        print("Error: " + str(e))
    finally:
        driver.quit()

print("Initialising Firebase...")
db = init_firebase()
scrape_checkers(db)
print("All done!")
