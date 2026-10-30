const API = "/api";
const HOME_URL = "http://localhost:8081/";
const CATEGORIES = [
  ["Electronics", "⌁", "#e5edf4"], ["Computers & Laptops", "▤", "#e9e8f1"],
  ["Mobile Accessories", "▯", "#f4e9e5"], ["Audio & Headphones", "♫", "#eee9f5"],
  ["Fashion & Clothing", "✧", "#f4e8e3"], ["Shoes & Footwear", "◒", "#e6eef0"],
  ["Watches & Wearables", "◷", "#f1eddf"], ["Home & Kitchen", "⌂", "#e8efe8"],
  ["Furniture & Home Decor", "⌑", "#f3e9dd"], ["Beauty & Personal Care", "✿", "#f4e5e9"],
  ["Sports & Fitness", "↗", "#e4eeea"], ["Travel Accessories", "✈", "#e7eef5"],
  ["Books & Stationery", "▧", "#f0ede4"], ["Toys & Games", "✳", "#f4eee0"]
];
const PALETTES = ["#eaf0f3", "#eeedf4", "#f5ece7", "#eeeaf3", "#f2eae5", "#e7edef", "#f2f0e7", "#e9f0e9", "#f2ece5", "#f4e9ec", "#e7efeb", "#e9eef3", "#f1eee7", "#f4f0e5"];
const categoryMeta = name => CATEGORIES.find(category => category[0] === name) || ["Everyday finds", "✳", "#edf0f1"];
const API_ERROR = "Something went wrong. Please try again.";
const productList = document.getElementById("product-list");
const searchInput = document.getElementById("search");
const categorySelect = document.getElementById("category-filter");
const cartDrawer = document.getElementById("cart-drawer");
const scrim = document.getElementById("scrim");
let allProducts = [];
let cart = [];
let page = 0;
let totalResults = 0;
let toastTimer;
let searchTimer;
let browseSequence = 0;
let wishlist = loadWishlist();

async function request(url, options = {}) {
  const response = await fetch(url, { credentials: "same-origin", ...options,
    headers: { ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers } });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try { const body = await response.json(); message = body.error || body.message || message; } catch { /* keep status message */ }
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return response.json();
}
function money(value) { return Number(value || 0).toLocaleString("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }); }
function toast(message) {
  const el = document.getElementById("toast"); el.textContent = message; el.classList.add("visible");
  clearTimeout(toastTimer); toastTimer = setTimeout(() => el.classList.remove("visible"), 2600);
}
function safeText(value) { return String(value ?? ""); }
function productArt(product, extraClass = "") {
  const meta = categoryMeta(product.category); const categoryIndex = Math.max(0, CATEGORIES.findIndex(item => item[0] === product.category));
  const art = document.createElement("div"); art.className = `product-art ${extraClass}`; art.style.setProperty("--art-bg", PALETTES[categoryIndex] || "#edf0f1");
  const nameHash = [...safeText(product.name)].reduce((hash, character) => (hash * 31 + character.charCodeAt(0)) >>> 0, 7);
  art.style.setProperty("--art-turn", `${nameHash % 31 - 15}deg`);
  const fallback = document.createElement("div"); fallback.className = "art-fallback"; fallback.style.setProperty("--art-ink", ["#587383", "#716d88", "#ac7965", "#73658a", "#9b6c60", "#52717c", "#887846", "#587b65", "#92765a", "#a46f80", "#597e70", "#607b96", "#82785d", "#9c8450"][categoryIndex] || "#68798a");
  const emoji = document.createElement("span"); emoji.className = "art-emoji"; emoji.textContent = meta[1];
  const label = document.createElement("span"); label.className = "art-label-product"; label.textContent = safeText(product.name).split(" ").slice(0, 3).join(" ");
  fallback.append(emoji, label); art.append(fallback);
  if (product.imageUrl) {
    const image = document.createElement("img"); image.src = product.imageUrl; image.alt = product.name; image.loading = "lazy";
    image.addEventListener("error", () => image.remove(), { once: true }); art.append(image);
  }
  return art;
}
function renderCard(product) {
  const card = document.createElement("article"); card.className = "product-card";
  card.append(productArt(product));
  const info = document.createElement("div"); info.className = "product-info";
  const brand = document.createElement("div"); brand.className = "product-brand"; brand.textContent = product.brand || product.category || "ShopEase";
  const name = document.createElement("h3"); name.textContent = product.name;
  const rating = document.createElement("div"); rating.className = "product-rating";
  rating.innerHTML = product.rating ? `<span aria-label="Demo rating">★</span> ${Number(product.rating).toFixed(1)} <span class="rating-note">· demo rating</span>` : "";
  const priceLine = document.createElement("div"); priceLine.className = "price-line";
  const price = document.createElement("strong"); price.className = "product-price"; price.textContent = money(product.price); priceLine.append(price);
  if (product.originalPrice > product.price && product.price > 0) {
    const discount = Math.round((1 - product.price / product.originalPrice) * 100);
    const oldPrice = document.createElement("del"); oldPrice.className = "original-price"; oldPrice.textContent = money(product.originalPrice);
    const badge = document.createElement("span"); badge.className = "sale-badge"; badge.textContent = `${discount}% off · demo`;
    priceLine.append(oldPrice, badge);
  }
  const stock = document.createElement("div"); stock.className = `stock${product.stock > 0 && product.stock < 5 ? " low" : ""}`; stock.textContent = product.stock > 0 ? `${product.stock < 5 ? "Only " : ""}${product.stock} in stock` : "Currently unavailable";
  const actions = document.createElement("div"); actions.className = "card-actions";
  const add = document.createElement("button"); add.className = "add-btn"; add.textContent = product.stock > 0 ? "Add to cart" : "Out of stock"; add.disabled = product.stock <= 0; add.addEventListener("click", () => addToCart(product.id));
  const details = document.createElement("button"); details.className = "details-btn"; details.textContent = "Details"; details.setAttribute("aria-label", `View details for ${product.name}`); details.addEventListener("click", () => showProduct(product));
  const wish = document.createElement("button"); wish.className = `wish-btn${wishlist.includes(product.id) ? " active" : ""}`; wish.textContent = wishlist.includes(product.id) ? "♥" : "♡"; wish.setAttribute("aria-label", wishlist.includes(product.id) ? "Remove from wish list" : "Add to wish list"); wish.addEventListener("click", () => toggleWishlist(product.id));
  actions.append(add, details, wish); info.append(brand, name, rating, priceLine, stock, actions); card.append(info); return card;
}
function renderInto(id, items) {
  const target = document.getElementById(id); target.replaceChildren();
  items.forEach(product => target.append(renderCard(product)));
  if (!items.length && id === "wishlist-grid") target.innerHTML = '<p class="section-note">Your wish list is waiting for a favourite.</p>';
}
function renderCategories() {
  categorySelect.innerHTML = '<option value="">All</option>';
  const categories = [...new Set(allProducts.map(product => product.category).filter(Boolean))].sort((a, b) => a.localeCompare(b));
  categories.forEach(category => { const option = document.createElement("option"); option.value = category; option.textContent = category; categorySelect.append(option); });
  const suggestions = document.getElementById("search-suggestions"); suggestions.replaceChildren();
  [...new Set(allProducts.map(product => product.name))].forEach(name => { const option = document.createElement("option"); option.value = name; suggestions.append(option); });
  const tiles = document.getElementById("category-tiles"); tiles.replaceChildren();
  categories.forEach((category, index) => {
    const meta = categoryMeta(category); const button = document.createElement("button"); button.className = "category-tile"; button.style.setProperty("--tile", meta[2]);
    button.innerHTML = `<span class="tile-icon" aria-hidden="true">${meta[1]}</span><strong></strong>`; button.querySelector("strong").textContent = category;
    button.addEventListener("click", () => { categorySelect.value = category; page = 0; browse(); document.getElementById("products").scrollIntoView({ behavior: "smooth" }); }); tiles.append(button);
  });
}
function renderHomepage() {
  const featured = [...allProducts].sort((a, b) => (b.rating || 0) - (a.rating || 0));
  renderInto("deal-grid", featured.slice(0, 4));
  renderInto("best-grid", featured.slice(4, 8));
  renderInto("recommended-grid", featured.slice(8, 12));
  renderInto("new-grid", [...allProducts].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)).slice(0, 4));
  const brandStrip = document.getElementById("brand-strip"); brandStrip.replaceChildren();
  [...new Set(allProducts.map(product => product.brand).filter(Boolean))].slice(0, 8).forEach(brand => { const badge = document.createElement("span"); badge.className = "brand-chip"; badge.textContent = brand; brandStrip.append(badge); });
  const shelves = document.getElementById("category-shelves"); shelves.replaceChildren();
  const visibleCategories = ["Electronics", "Computers & Laptops", "Mobile Accessories", "Audio & Headphones", "Fashion & Clothing", "Shoes & Footwear", "Watches & Wearables", "Home & Kitchen", "Furniture & Home Decor", "Beauty & Personal Care", "Sports & Fitness", "Travel Accessories", "Books & Stationery", "Toys & Games"];
  visibleCategories.forEach((category, index) => {
    const items = allProducts.filter(product => product.category === category).slice(0, 4); if (!items.length) return;
    const section = document.createElement("section"); section.className = `wrap shelf category-shelf${index % 2 === 1 ? " tinted" : ""}`;
    section.innerHTML = `<div class="section-head"><div><span class="eyebrow">A LITTLE SOMETHING FOR EVERY DAY</span><h2></h2></div><a href="#products" class="text-link">Explore category <span>→</span></a></div><div class="product-strip"></div>`;
    section.querySelector("h2").textContent = category; section.querySelector(".text-link").addEventListener("click", () => { categorySelect.value = category; page = 0; browse(); });
    const grid = section.querySelector(".product-strip"); items.forEach(product => grid.append(renderCard(product))); shelves.append(section);
  });
}
function browseParams(nextPage = 0) {
  const params = new URLSearchParams({ q: searchInput.value.trim(), category: categorySelect.value, sort: document.getElementById("sort").value, page: String(nextPage), size: "12", inStock: String(document.getElementById("in-stock").checked) });
  const min = document.getElementById("min-price").value, max = document.getElementById("max-price").value;
  if (min) params.set("minPrice", min); if (max) params.set("maxPrice", max); return params;
}
function showSkeletons(append = false) {
  if (!append) productList.replaceChildren();
  const skeleton = document.getElementById("skeleton-template");
  for (let i = 0; i < 8; i++) productList.append(skeleton.content.cloneNode(true));
}
async function browse(append = false) {
  const call = ++browseSequence; const nextPage = append ? page + 1 : 0; if (!append) showSkeletons();
  try {
    const result = await request(`${API}/products/browse?${browseParams(nextPage)}`); if (call !== browseSequence) return;
    if (!append) productList.replaceChildren(); else [...productList.querySelectorAll(".skeleton-card")].forEach(node => node.remove());
    result.products.forEach(product => productList.append(renderCard(product)));
    page = result.page; totalResults = result.total;
    document.getElementById("result-count").textContent = `${result.total} ${result.total === 1 ? "find" : "finds"} in the collection`;
    document.getElementById("browse-empty").classList.toggle("hidden", result.total !== 0);
    document.getElementById("load-more").classList.toggle("hidden", (page + 1) * result.size >= result.total || result.total === 0);
    renderActiveFilters();
  } catch (error) {
    if (call !== browseSequence) return;
    productList.replaceChildren(); document.getElementById("result-count").textContent = error.message || API_ERROR;
    document.getElementById("browse-empty").classList.remove("hidden");
  }
}
function renderActiveFilters() {
  const target = document.getElementById("active-filters"); target.replaceChildren();
  const labels = [];
  if (searchInput.value.trim()) labels.push(`Search: ${searchInput.value.trim()}`);
  if (categorySelect.value) labels.push(categorySelect.value);
  if (document.getElementById("min-price").value) labels.push(`From ${money(document.getElementById("min-price").value)}`);
  if (document.getElementById("max-price").value) labels.push(`To ${money(document.getElementById("max-price").value)}`);
  if (document.getElementById("in-stock").checked) labels.push("In stock");
  labels.forEach(label => { const chip = document.createElement("span"); chip.className = "filter-chip"; chip.textContent = label; target.append(chip); });
}
function resetFilters() { searchInput.value = ""; categorySelect.value = ""; document.getElementById("min-price").value = ""; document.getElementById("max-price").value = ""; document.getElementById("in-stock").checked = false; document.getElementById("sort").value = "featured"; page = 0; browse(); }
function loadWishlist() { try { return JSON.parse(localStorage.getItem("shopease-wishlist") || "[]"); } catch { return []; } }
function toggleWishlist(id) {
  wishlist = wishlist.includes(id) ? wishlist.filter(item => item !== id) : [...wishlist, id];
  try { localStorage.setItem("shopease-wishlist", JSON.stringify(wishlist)); } catch { /* Wishlist remains available for this page view. */ }
  document.getElementById("wishlist-count").textContent = wishlist.length; renderInto("wishlist-grid", allProducts.filter(product => wishlist.includes(product.id)));
  document.querySelectorAll(".wish-btn").forEach(button => { const card = button.closest(".product-card"); const name = card?.querySelector("h3")?.textContent; const product = allProducts.find(item => item.name === name); if (!product) return; const saved = wishlist.includes(product.id); button.classList.toggle("active", saved); button.textContent = saved ? "♥" : "♡"; button.setAttribute("aria-label", saved ? "Remove from wish list" : "Add to wish list"); });
}
function syncWishlist() { document.getElementById("wishlist-count").textContent = wishlist.length; renderInto("wishlist-grid", allProducts.filter(product => wishlist.includes(product.id))); }
async function loadCart() {
  try { cart = await request(`${API}/cart`); const total = await request(`${API}/cart/total`); renderCart(total.total); }
  catch (error) { toast(`Could not load your cart: ${error.message}`); }
}
async function addToCart(id, quantity = 1) {
  try { await request(`${API}/cart/add`, { method: "POST", body: JSON.stringify({ productId: id, quantity }) }); await loadCart(); toast("Added to your cart."); }
  catch (error) { toast(`Could not add item: ${error.message}`); }
}
async function changeQuantity(item, quantity) {
  if (quantity < 1) return removeFromCart(item.productId);
  try { await request(`${API}/cart/${item.productId}`, { method: "PUT", body: JSON.stringify({ quantity }) }); await loadCart(); }
  catch (error) { toast(error.message); }
}
async function removeFromCart(id) {
  try { await request(`${API}/cart/${id}`, { method: "DELETE" }); await loadCart(); toast("Removed from your cart."); }
  catch (error) { toast(`Could not remove item: ${error.message}`); }
}
function renderCart(total) {
  const list = document.getElementById("cart-list"); list.replaceChildren(); const count = cart.reduce((sum, item) => sum + item.quantity, 0);
  document.getElementById("cart-count").textContent = count; document.getElementById("drawer-cart-count").textContent = `(${count})`; document.getElementById("cart-total").textContent = money(total);
  if (!cart.length) { list.innerHTML = '<div class="empty-cart">Your cart is ready for a good find.<br><br><a class="text-link" href="#products">Explore the collection →</a></div>'; return; }
  cart.forEach(item => {
    const product = allProducts.find(entry => entry.id === item.productId); const row = document.createElement("div"); row.className = "cart-row";
    const thumb = document.createElement("div"); thumb.className = "cart-thumb"; thumb.textContent = categoryMeta(product?.category)[1];
    const details = document.createElement("div"); const name = document.createElement("div"); name.className = "cart-name"; name.textContent = item.productName;
    const price = document.createElement("div"); price.className = "cart-price"; price.textContent = `${money(item.price)} each · ${money(item.subtotal)} subtotal`;
    const qty = document.createElement("div"); qty.className = "quantity-control";
    const minus = document.createElement("button"); minus.textContent = "−"; minus.setAttribute("aria-label", `Decrease ${item.productName} quantity`); minus.addEventListener("click", () => changeQuantity(item, item.quantity - 1));
    const number = document.createElement("span"); number.textContent = item.quantity;
    const plus = document.createElement("button"); plus.textContent = "+"; plus.setAttribute("aria-label", `Increase ${item.productName} quantity`); plus.addEventListener("click", () => changeQuantity(item, item.quantity + 1));
    qty.append(minus, number, plus); details.append(name, price, qty);
    const remove = document.createElement("button"); remove.className = "remove-cart"; remove.textContent = "Remove"; remove.addEventListener("click", () => removeFromCart(item.productId)); row.append(thumb, details, remove); list.append(row);
  });
}
function openCart() { cartDrawer.classList.add("open"); cartDrawer.setAttribute("aria-hidden", "false"); scrim.classList.add("visible"); document.body.style.overflow = "hidden"; document.getElementById("close-cart").focus(); }
function closeCart() { cartDrawer.classList.remove("open"); cartDrawer.setAttribute("aria-hidden", "true"); scrim.classList.remove("visible"); document.body.style.overflow = ""; }
function showProduct(product) {
  const target = document.getElementById("product-detail"); target.replaceChildren(); const layout = document.createElement("div"); layout.className = "detail-layout"; layout.append(productArt(product));
  const copy = document.createElement("div"); copy.className = "detail-copy"; copy.innerHTML = `<span class="eyebrow"></span><h2></h2><div class="product-rating"></div><div class="detail-price"></div><p class="detail-description"></p><p class="stock"></p><label class="detail-quantity">Quantity <input id="detail-quantity" type="number" min="1" value="1"></label><button class="button button-dark full-button"></button>`;
  copy.querySelector(".eyebrow").textContent = product.brand || product.category || "ShopEase find"; copy.querySelector("h2").textContent = product.name;
  copy.querySelector(".product-rating").textContent = product.rating ? `★ ${Number(product.rating).toFixed(1)} · demo rating (${product.reviewCount || 0} demo reviews)` : "";
  const detailPrice = copy.querySelector(".detail-price"); detailPrice.innerHTML = `<strong class="product-price">${money(product.price)}</strong>`;
  if (product.originalPrice > product.price) detailPrice.innerHTML += ` <del>${money(product.originalPrice)}</del>`;
  copy.querySelector(".detail-description").textContent = product.description || "A thoughtfully selected everyday essential.";
  copy.querySelector(".stock").textContent = product.stock > 0 ? `${product.stock} currently available` : "Currently unavailable";
  const quantity = copy.querySelector("#detail-quantity"); quantity.max = Math.max(1, product.stock); quantity.disabled = product.stock <= 0;
  const add = copy.querySelector("button"); add.textContent = product.stock > 0 ? "Add to cart" : "Out of stock"; add.disabled = product.stock <= 0; add.addEventListener("click", () => { const count = Number(quantity.value); if (!Number.isInteger(count) || count < 1 || count > product.stock) { toast("Choose a quantity within the available stock."); return; } addToCart(product.id, count); document.getElementById("product-dialog").close(); });
  layout.append(copy); target.append(layout); document.getElementById("product-dialog").showModal();
}
async function init() {
  showSkeletons();
  try {
    const result = await request(`${API}/products/browse?size=60&sort=featured`);
    allProducts = result.products;
    for (let nextPage = 1; allProducts.length < result.total; nextPage++) {
      const more = await request(`${API}/products/browse?size=60&sort=featured&page=${nextPage}`);
      allProducts.push(...more.products);
    }
    renderCategories(); renderHomepage(); syncWishlist(); browse();
  } catch (error) {
    productList.replaceChildren(); document.getElementById("result-count").textContent = `The catalogue could not load: ${error.message}`;
    document.getElementById("browse-empty").classList.remove("hidden");
    document.querySelector("#browse-empty h3").textContent = "We couldn't reach the catalogue";
    document.querySelector("#browse-empty p").textContent = error.message === "Failed to fetch"
      ? `Start Spring Boot and open ${HOME_URL} so ShopEase can connect to MySQL.`
      : `The catalogue request failed: ${error.message}`;
    document.getElementById("category-tiles").innerHTML = '<p class="section-note">Connect to the ShopEase catalogue to browse products.</p>';
  }
  await loadCart();
}

document.getElementById("search-form").addEventListener("submit", event => { event.preventDefault(); clearTimeout(searchTimer); page = 0; browse(); document.getElementById("products").scrollIntoView({ behavior: "smooth" }); });
searchInput.addEventListener("input", () => { document.getElementById("clear-search").classList.toggle("hidden", !searchInput.value); clearTimeout(searchTimer); searchTimer = setTimeout(() => { page = 0; browse(); }, 260); });
document.getElementById("clear-search").addEventListener("click", () => { searchInput.value = ""; document.getElementById("clear-search").classList.add("hidden"); page = 0; browse(); searchInput.focus(); });
categorySelect.addEventListener("change", () => { page = 0; browse(); });
["sort", "in-stock"].forEach(id => document.getElementById(id).addEventListener("change", () => { page = 0; browse(); }));
["min-price", "max-price"].forEach(id => document.getElementById(id).addEventListener("change", () => { page = 0; browse(); }));
document.getElementById("load-more").addEventListener("click", () => browse(true));
document.getElementById("clear-filters").addEventListener("click", resetFilters);
document.getElementById("empty-reset").addEventListener("click", resetFilters);
document.getElementById("cart-count").closest("a").addEventListener("click", event => { event.preventDefault(); openCart(); });
document.getElementById("close-cart").addEventListener("click", closeCart); scrim.addEventListener("click", closeCart);
document.getElementById("cart-list").addEventListener("click", event => { if (event.target.closest("a")) closeCart(); });
document.getElementById("clear-cart").addEventListener("click", async () => {
  if (!cart.length) return; if (!window.confirm("Clear all items from your cart?")) return;
  try { await request(`${API}/cart`, { method: "DELETE" }); await loadCart(); toast("Your cart is clear."); } catch (error) { toast(error.message); }
});
document.getElementById("checkout-open").addEventListener("click", () => {
  if (!cart.length) { toast("Add a find to your cart before checkout."); return; }
  const summary = document.getElementById("checkout-summary"); summary.className = "checkout-summary"; summary.textContent = `Demo order summary: ${cart.reduce((n, item) => n + item.quantity, 0)} item(s) · ${document.getElementById("cart-total").textContent}`;
  document.getElementById("checkout-message").textContent = ""; document.getElementById("checkout-dialog").showModal();
});
document.getElementById("checkout-form").addEventListener("submit", event => { event.preventDefault(); document.getElementById("checkout-message").textContent = "Your details passed this demo form's validation. No order was placed and no information was saved."; });
document.getElementById("newsletter-form").addEventListener("submit", event => { event.preventDefault(); const email = document.getElementById("newsletter-email"); if (!email.reportValidity()) return; document.getElementById("newsletter-message").textContent = "Thanks for your interest. This demo does not save or send email addresses."; event.currentTarget.reset(); });
document.getElementById("account-info").addEventListener("click", () => toast("Guest browsing is enabled. Sign-in is not available in this demo."));
document.querySelectorAll("[data-close]").forEach(button => button.addEventListener("click", () => document.getElementById(button.dataset.close).close()));
document.addEventListener("keydown", event => { if (event.key === "Escape") closeCart(); });
init();
