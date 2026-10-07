import { initializeApp, deleteApp } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js";
import {
  getAuth, onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword,
  signOut, updateProfile, deleteUser
} from "https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js";
import {
  getFirestore, collection, doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc,
  query, where
} from "https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyC0MZvrdLMUSRRpDj64zrig32dKtg5Qpok",
  authDomain: "elbuensabor-994af.firebaseapp.com",
  projectId: "elbuensabor-994af",
  storageBucket: "elbuensabor-994af.firebasestorage.app",
  messagingSenderId: "1035979130445"
};

const PRIMARY_ADMIN = "admin@elbuensabor.com";
const ROLE_ADMIN = "ADMIN";
const ROLE_CLIENT = "CLIENTE";
const CATEGORIES = ["Entradas", "Bebidas", "Platos"];
const MAX_IMAGE_BASE64 = 600000;

const firebaseApp = initializeApp(firebaseConfig);
const auth = getAuth(firebaseApp);
const db = getFirestore(firebaseApp);

const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];
const appEl = $("#app");
const navEl = $("#mainNav");
const toastEl = $("#toast");
const cartBadge = $("#cartBadge");

const state = {
  user: null,
  profile: null,
  route: "menu",
  category: "Entradas",
  products: [],
  users: [],
  orders: [],
  cart: loadCart(),
  editingProduct: null,
  pendingImageBase64: ""
};

function money(v) { return `S/ ${Number(v || 0).toFixed(2)}`; }
function escapeHtml(v = "") {
  return String(v).replace(/[&<>'"]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#039;",'"':"&quot;"}[c]));
}
function formatDate(v) {
  if (!v) return "—";
  const d = new Date(Number(v));
  if (Number.isNaN(d.getTime())) return "—";
  return new Intl.DateTimeFormat("es-PE", { dateStyle: "medium", timeStyle: "short" }).format(d);
}
function formatNowText() {
  return new Intl.DateTimeFormat("es-PE", { dateStyle: "short", timeStyle: "short" }).format(new Date());
}
function showToast(message, type = "") {
  toastEl.textContent = message;
  toastEl.className = `toast show ${type}`;
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toastEl.className = "toast", 3300);
}
function readableError(err) {
  const code = err?.code || "";
  const map = {
    "auth/invalid-credential": "Correo o contraseña incorrectos.",
    "auth/user-not-found": "Usuario no encontrado.",
    "auth/wrong-password": "Contraseña incorrecta.",
    "auth/email-already-in-use": "Ese correo ya está registrado.",
    "auth/weak-password": "La contraseña debe tener al menos 6 caracteres.",
    "auth/invalid-email": "El correo no es válido.",
    "auth/unauthorized-domain": "Este dominio no está autorizado en Firebase Authentication.",
    "permission-denied": "Firebase rechazó la operación. Revisa las reglas de Firestore."
  };
  return map[code] || err?.message || "Ocurrió un error inesperado.";
}
function imageSrc(product) {
  if (product?.imagenBase64) return `data:image/jpeg;base64,${product.imagenBase64}`;
  return product?.imagenUrl || "";
}
function loadCart() {
  try { return JSON.parse(localStorage.getItem("elbuensabor_cart") || "[]"); }
  catch { return []; }
}
function saveCart() {
  localStorage.setItem("elbuensabor_cart", JSON.stringify(state.cart));
  window.addEventListener("resize", () => {
  if (!state.user) return;
  if (window.matchMedia("(max-width: 900px)").matches) navEl.classList.add("hidden-mobile");
  else navEl.classList.remove("hidden-mobile");
});

updateCartBadge();
}
function updateCartBadge() {
  cartBadge.textContent = state.cart.reduce((s, x) => s + Number(x.cantidad || 0), 0);
}
function cartTotal() { return state.cart.reduce((s, x) => s + Number(x.precio) * Number(x.cantidad), 0); }
function isAdmin() { return state.profile?.rol === ROLE_ADMIN; }

async function ensureProfile(firebaseUser) {
  const ref = doc(db, "usuarios", firebaseUser.uid);
  const snap = await getDoc(ref);
  const email = (firebaseUser.email || "").trim().toLowerCase();
  const expectedRole = email === PRIMARY_ADMIN ? ROLE_ADMIN : ROLE_CLIENT;

  if (!snap.exists()) {
    const profile = {
      usuarioId: firebaseUser.uid,
      nombre: firebaseUser.displayName || (email === PRIMARY_ADMIN ? "Administrador" : "Cliente"),
      correo: email,
      rol: expectedRole
    };
    await setDoc(ref, profile);
    return profile;
  }

  const current = snap.data();
  const role = email === PRIMARY_ADMIN ? ROLE_ADMIN : (String(current.rol || "").toUpperCase() === ROLE_ADMIN ? ROLE_ADMIN : ROLE_CLIENT);
  const normalized = {
    usuarioId: firebaseUser.uid,
    nombre: current.nombre || firebaseUser.displayName || "Usuario",
    correo: email,
    rol: role
  };

  if (JSON.stringify(current) !== JSON.stringify(normalized)) await setDoc(ref, normalized);
  return normalized;
}

onAuthStateChanged(auth, async user => {
  state.user = user;
  if (!user) {
    state.profile = null;
    setNav(false);
    renderAuth();
    return;
  }
  try {
    state.profile = await ensureProfile(user);
    setNav(true);
    routeTo(location.hash.replace("#", "") || "menu", false);
  } catch (err) {
    showToast(readableError(err), "error");
    setNav(true);
    state.profile = { nombre: user.displayName || "Usuario", correo: user.email || "", rol: ROLE_CLIENT, usuarioId: user.uid };
    routeTo("menu", false);
  }
});

function setNav(logged) {
  navEl.classList.toggle("hidden", !logged);
  if (logged && window.matchMedia("(max-width: 900px)").matches) navEl.classList.add("hidden-mobile");
  else navEl.classList.remove("hidden-mobile");
  $$(".admin-only").forEach(el => el.classList.toggle("hidden", !isAdmin()));
  updateCartBadge();
}

function routeTo(route, pushHash = true) {
  if (!state.user) return renderAuth();
  const adminRoutes = new Set(["admin-products","admin-orders","admin-users"]);
  if (adminRoutes.has(route) && !isAdmin()) route = "menu";
  state.route = route || "menu";
  if (pushHash) location.hash = state.route;
  $$(".nav-link").forEach(el => el.classList.toggle("active", el.dataset.route === state.route));
  navEl.classList.add("hidden-mobile");
  renderRoute();
}

window.addEventListener("hashchange", () => {
  const r = location.hash.replace("#", "") || "menu";
  if (state.user) routeTo(r, false);
});

$("#brandBtn").addEventListener("click", () => state.user ? routeTo("menu") : renderAuth());
$("#mobileMenuBtn").addEventListener("click", () => navEl.classList.toggle("hidden-mobile"));
navEl.addEventListener("click", e => {
  const btn = e.target.closest("[data-route]");
  if (btn) routeTo(btn.dataset.route);
});
$("#logoutBtn").addEventListener("click", async () => {
  await signOut(auth);
  location.hash = "";
});

function renderAuth(mode = "login") {
  const register = mode === "register";
  appEl.innerHTML = `
    <section class="auth-wrap">
      <div class="auth-hero">
        <span class="eyebrow">Sabor peruano • pedido fácil</span>
        <h1>Tu restaurante también vive en la web.</h1>
        <p>Ingresa para explorar el menú, realizar pedidos y consultar su estado. Administradores y clientes usan el mismo Firebase que la app Android.</p>
      </div>
      <div class="auth-card">
        <span class="eyebrow">${register ? "Nueva cuenta" : "Bienvenido"}</span>
        <h2>${register ? "Crear cuenta" : "Iniciar sesión"}</h2>
        <p class="subtitle">${register ? "Las cuentas creadas aquí se registran como CLIENTE." : "Usa las mismas credenciales de la aplicación Android."}</p>
        <form id="authForm" class="stack">
          ${register ? `<div class="field"><label>Nombre</label><input id="authName" class="input" required maxlength="80" autocomplete="name"></div>` : ""}
          <div class="field"><label>Correo electrónico</label><input id="authEmail" class="input" type="email" required autocomplete="email"></div>
          <div class="field"><label>Contraseña</label><input id="authPass" class="input" type="password" minlength="6" required autocomplete="${register ? "new-password" : "current-password"}"></div>
          <button class="btn btn-primary" type="submit">${register ? "CREAR CUENTA" : "INICIAR SESIÓN"}</button>
        </form>
        <button id="switchAuth" class="btn btn-light" style="margin-top:10px">${register ? "Ya tengo cuenta" : "Crear cuenta de cliente"}</button>
        <p style="color:var(--muted);font-size:.8rem;margin-top:18px">Administrador principal: <strong>${PRIMARY_ADMIN}</strong></p>
      </div>
    </section>`;

  $("#switchAuth").onclick = () => renderAuth(register ? "login" : "register");
  $("#authForm").onsubmit = async e => {
    e.preventDefault();
    const email = $("#authEmail").value.trim().toLowerCase();
    const pass = $("#authPass").value;
    const submit = e.submitter;
    submit.disabled = true;
    try {
      if (register) {
        if (email === PRIMARY_ADMIN) throw new Error("Ese correo está reservado para el administrador principal.");
        const name = $("#authName").value.trim();
        const cred = await createUserWithEmailAndPassword(auth, email, pass);
        await updateProfile(cred.user, { displayName: name });
        await setDoc(doc(db, "usuarios", cred.user.uid), {
          usuarioId: cred.user.uid, nombre: name, correo: email, rol: ROLE_CLIENT
        });
        showToast("Cuenta creada correctamente.", "success");
      } else {
        await signInWithEmailAndPassword(auth, email, pass);
      }
    } catch (err) {
      showToast(readableError(err), "error");
      submit.disabled = false;
    }
  };
}

async function renderRoute() {
  appEl.innerHTML = `<div class="loader"></div>`;
  try {
    switch (state.route) {
      case "menu": await renderMenu(); break;
      case "cart": renderCart(); break;
      case "orders": await renderMyOrders(); break;
      case "profile": renderProfile(); break;
      case "admin-products": await renderAdminProducts(); break;
      case "admin-orders": await renderAdminOrders(); break;
      case "admin-users": await renderAdminUsers(); break;
      default: routeTo("menu");
    }
  } catch (err) {
    appEl.innerHTML = `<div class="empty"><strong>No se pudo cargar esta sección</strong>${escapeHtml(readableError(err))}</div>`;
    showToast(readableError(err), "error");
  }
}

async function fetchProducts(category = null) {
  let q = collection(db, "productos");
  if (category) q = query(q, where("categoria", "==", category));
  const snap = await getDocs(q);
  return snap.docs.map(d => ({ ...d.data(), productoId: d.data().productoId || d.id }));
}

function productCard(p, admin = false) {
  const src = imageSrc(p);
  return `<article class="product-card">
    ${src ? `<img class="product-image" src="${escapeHtml(src)}" alt="${escapeHtml(p.nombre)}" onerror="this.outerHTML='<div class=&quot;image-fallback&quot;>Sin imagen</div>'">` : `<div class="image-fallback">Sin imagen</div>`}
    <div class="product-body">
      <div><span class="eyebrow">${escapeHtml(p.categoria || "Producto")}</span><h3 class="product-title">${escapeHtml(p.nombre)}</h3></div>
      <div class="product-description">${escapeHtml(p.descripcion)}</div>
      <div class="product-footer">
        <span class="price">${money(p.precio)}</span>
        ${admin
          ? `<span><button class="btn btn-light btn-sm" data-edit-product="${p.productoId}">Editar</button> <button class="btn btn-danger btn-sm" data-delete-product="${p.productoId}">Eliminar</button></span>`
          : `<button class="btn btn-primary btn-sm" data-add-cart="${p.productoId}">Agregar</button>`}
      </div>
    </div>
  </article>`;
}

async function renderMenu() {
  state.products = await fetchProducts(state.category);
  const total = state.products.length;
  appEl.innerHTML = `
    <section class="hero">
      <span class="eyebrow">Hola, ${escapeHtml(state.profile?.nombre || "Cliente")}</span>
      <h1>Comida que se antoja antes del primer bocado.</h1>
      <p>Explora el menú de El Buen Sabor. Los productos se sincronizan con la aplicación Android mediante el mismo Firestore.</p>
      <div class="hero-actions"><button class="btn btn-primary" data-scroll-menu>Ver menú</button><button class="btn btn-light" data-route-inline="cart">Ver carrito (${state.cart.reduce((s,x)=>s+x.cantidad,0)})</button></div>
    </section>
    <div id="menuSection" class="section-head"><div><span class="eyebrow">Menú</span><h2>¿Qué deseas pedir hoy?</h2><p>${total} producto${total === 1 ? "" : "s"} en ${escapeHtml(state.category)}.</p></div></div>
    <div class="tabs">${CATEGORIES.map(c => `<button class="tab ${c === state.category ? "active" : ""}" data-category="${c}">${c}</button>`).join("")}</div>
    ${state.products.length ? `<div class="grid">${state.products.map(p => productCard(p)).join("")}</div>` : `<div class="empty"><strong>No hay productos en esta categoría.</strong>Cuando un administrador agregue productos, aparecerán aquí automáticamente.</div>`}`;

  $("[data-scroll-menu]")?.addEventListener("click", () => $("#menuSection").scrollIntoView({behavior:"smooth"}));
  $$('[data-route-inline]').forEach(b => b.onclick = () => routeTo(b.dataset.routeInline));
  $$('[data-category]').forEach(b => b.onclick = async () => { state.category = b.dataset.category; await renderMenu(); });
  $$('[data-add-cart]').forEach(b => b.onclick = () => addToCart(b.dataset.addCart));
}

function addToCart(productId) {
  const p = state.products.find(x => x.productoId === productId);
  if (!p) return;
  const existing = state.cart.find(x => x.productoId === productId);
  if (existing) existing.cantidad += 1;
  else state.cart.push({ productoId: p.productoId, nombre: p.nombre, precio: Number(p.precio), cantidad: 1, imagenBase64: p.imagenBase64 || "", imagenUrl: p.imagenUrl || "" });
  saveCart();
  showToast(`${p.nombre} agregado al carrito.`, "success");
}

function renderCart() {
  const rows = state.cart.map((x, i) => {
    const src = imageSrc(x);
    return `<div class="cart-row">
      ${src ? `<img class="cart-thumb" src="${escapeHtml(src)}" alt="">` : `<div class="cart-thumb image-fallback">—</div>`}
      <div><strong>${escapeHtml(x.nombre)}</strong><div style="color:var(--muted);font-size:.86rem">${money(x.precio)} c/u</div></div>
      <div class="qty"><button data-qty="${i}" data-delta="-1">−</button><strong>${x.cantidad}</strong><button data-qty="${i}" data-delta="1">+</button></div>
      <div class="line-total"><strong>${money(x.precio * x.cantidad)}</strong><br><button class="btn btn-danger btn-sm" data-remove="${i}" style="margin-top:6px">Quitar</button></div>
    </div>`;
  }).join("");
  appEl.innerHTML = `<div class="section-head"><div><span class="eyebrow">Carrito</span><h2>Tu pedido</h2><p>Revisa cantidades antes de confirmar.</p></div></div>
    ${state.cart.length ? `<div class="two-col"><div class="card">${rows}</div><aside class="summary"><h3>Resumen</h3><div class="summary-row"><span>Productos</span><span>${state.cart.reduce((s,x)=>s+x.cantidad,0)}</span></div><div class="summary-row summary-total"><span>Total</span><span>${money(cartTotal())}</span></div><button id="checkoutBtn" class="btn btn-primary" style="width:100%;margin-top:16px">Continuar con pedido</button></aside></div>` : `<div class="empty"><strong>Tu carrito está vacío.</strong>Agrega productos desde el menú.<br><button class="btn btn-primary" data-go-menu style="margin-top:15px">Ver menú</button></div>`}`;
  $$('[data-qty]').forEach(b => b.onclick = () => {
    const i = Number(b.dataset.qty), d = Number(b.dataset.delta);
    state.cart[i].cantidad += d;
    if (state.cart[i].cantidad <= 0) state.cart.splice(i, 1);
    saveCart(); renderCart();
  });
  $$('[data-remove]').forEach(b => b.onclick = () => { state.cart.splice(Number(b.dataset.remove),1); saveCart(); renderCart(); });
  $('[data-go-menu]')?.addEventListener('click', () => routeTo('menu'));
  $('#checkoutBtn')?.addEventListener('click', openCheckout);
}

function openCheckout() {
  const modal = document.createElement("div");
  modal.className = "modal-backdrop";
  modal.innerHTML = `<div class="modal"><div class="modal-head"><div><span class="eyebrow">Finalizar pedido</span><h3>Datos de entrega</h3></div><button class="close-btn" data-close>×</button></div>
    <form id="checkoutForm" class="stack">
      <div class="field"><label>Dirección de entrega</label><textarea id="deliveryAddress" class="input" required placeholder="Ej.: Av. Principal 123, San Juan de Miraflores"></textarea></div>
      <div class="two-col"><div class="field"><label>Latitud</label><input id="lat" class="input" type="number" step="any" value="0"></div><div class="field"><label>Longitud</label><input id="lon" class="input" type="number" step="any" value="0"></div></div>
      <button id="geoBtn" type="button" class="btn btn-light">Usar mi ubicación actual</button>
      <div class="card" style="background:#fafafa"><div style="display:flex;justify-content:space-between"><strong>Total</strong><strong>${money(cartTotal())}</strong></div></div>
      <button type="submit" class="btn btn-primary">Confirmar pedido</button>
    </form></div>`;
  document.body.appendChild(modal);
  $('[data-close]', modal).onclick = () => modal.remove();
  modal.onclick = e => { if (e.target === modal) modal.remove(); };
  $('#geoBtn', modal).onclick = () => {
    if (!navigator.geolocation) return showToast("El navegador no ofrece geolocalización.", "error");
    const btn = $('#geoBtn', modal); btn.disabled = true; btn.textContent = "Obteniendo ubicación...";
    navigator.geolocation.getCurrentPosition(pos => {
      $('#lat', modal).value = pos.coords.latitude;
      $('#lon', modal).value = pos.coords.longitude;
      btn.textContent = "Ubicación obtenida";
    }, () => { btn.disabled = false; btn.textContent = "Usar mi ubicación actual"; showToast("No se pudo obtener la ubicación.", "error"); }, {enableHighAccuracy:true, timeout:10000});
  };
  $('#checkoutForm', modal).onsubmit = async e => {
    e.preventDefault();
    const submit = e.submitter; submit.disabled = true;
    try {
      const ref = doc(collection(db, "pedidos"));
      const now = Date.now();
      const items = state.cart.map(x => ({ nombre:x.nombre, precio:Number(x.precio), cantidad:Number(x.cantidad) }));
      await setDoc(ref, {
        pedidoId: ref.id,
        usuarioId: state.user.uid,
        total: cartTotal(),
        direccionEntrega: $('#deliveryAddress', modal).value.trim(),
        latitud: Number($('#lat', modal).value) || 0,
        longitud: Number($('#lon', modal).value) || 0,
        fecha: formatNowText(),
        fechaMillis: now,
        estado: "PENDIENTE",
        items
      });
      state.cart = []; saveCart(); modal.remove(); showToast("Pedido registrado correctamente.", "success"); routeTo("orders");
    } catch (err) { showToast(readableError(err), "error"); submit.disabled = false; }
  };
}

async function renderMyOrders() {
  const snap = await getDocs(query(collection(db,"pedidos"), where("usuarioId","==",state.user.uid)));
  const orders = snap.docs.map(d => ({...d.data(), pedidoId:d.data().pedidoId || d.id})).sort((a,b)=>(b.fechaMillis||0)-(a.fechaMillis||0));
  appEl.innerHTML = `<div class="section-head"><div><span class="eyebrow">Historial</span><h2>Mis pedidos</h2><p>Consulta el estado de tus pedidos.</p></div></div>
    ${orders.length ? `<div class="stack">${orders.map(o => `<div class="card"><div style="display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap"><div><strong>Pedido ${escapeHtml(o.pedidoId.slice(0,8))}</strong><div style="color:var(--muted);font-size:.86rem;margin-top:4px">${escapeHtml(o.direccionEntrega || "Sin dirección")} • ${escapeHtml(o.fecha || formatDate(o.fechaMillis))}</div></div><div style="text-align:right"><span class="status ${escapeHtml(o.estado)}">${escapeHtml(o.estado)}</span><div class="price" style="margin-top:6px">${money(o.total)}</div></div></div><div style="margin-top:12px;color:var(--muted);font-size:.9rem">${(o.items||[]).map(i=>`${i.cantidad}× ${escapeHtml(i.nombre)}`).join(" · ")}</div></div>`).join("")}</div>` : `<div class="empty"><strong>Aún no tienes pedidos.</strong>Cuando confirmes uno, aparecerá aquí.</div>`}`;
}

function renderProfile() {
  appEl.innerHTML = `<div class="section-head"><div><span class="eyebrow">Cuenta</span><h2>Mi perfil</h2><p>Actualiza tu nombre. El correo y el rol están protegidos.</p></div></div>
    <div class="card" style="max-width:680px"><form id="profileForm" class="stack">
      <div class="field"><label>Nombre</label><input id="profileName" class="input" value="${escapeHtml(state.profile.nombre)}" required></div>
      <div class="field"><label>Correo</label><input class="input" value="${escapeHtml(state.profile.correo)}" disabled></div>
      <div class="field"><label>Rol</label><input class="input" value="${escapeHtml(state.profile.rol)}" disabled></div>
      <div class="form-actions"><button class="btn btn-primary" type="submit">Guardar cambios</button></div>
    </form></div>`;
  $('#profileForm').onsubmit = async e => {
    e.preventDefault(); const btn = e.submitter; btn.disabled = true;
    try {
      const name = $('#profileName').value.trim();
      await updateProfile(state.user, {displayName:name});
      await updateDoc(doc(db,"usuarios",state.user.uid), {nombre:name});
      state.profile.nombre = name; showToast("Perfil actualizado.", "success"); renderProfile();
    } catch(err) { showToast(readableError(err),"error"); btn.disabled=false; }
  };
}

async function renderAdminProducts() {
  state.products = await fetchProducts();
  appEl.innerHTML = `<div class="section-head"><div><span class="eyebrow">Administración</span><h2>Productos</h2><p>Agrega, edita o elimina productos. Las imágenes se guardan compatibles con Android.</p></div><button id="newProductBtn" class="btn btn-primary">+ Nuevo producto</button></div>
    ${state.products.length ? `<div class="grid">${state.products.map(p=>productCard(p,true)).join("")}</div>` : `<div class="empty"><strong>No hay productos.</strong>Crea el primero desde “Nuevo producto”.</div>`}`;
  $('#newProductBtn').onclick = () => openProductModal();
  $$('[data-edit-product]').forEach(b => b.onclick = () => openProductModal(state.products.find(p=>p.productoId===b.dataset.editProduct)));
  $$('[data-delete-product]').forEach(b => b.onclick = async () => {
    const p = state.products.find(x=>x.productoId===b.dataset.deleteProduct); if (!p) return;
    if (!confirm(`¿Eliminar “${p.nombre}”?`)) return;
    try { await deleteDoc(doc(db,"productos",p.productoId)); showToast("Producto eliminado.","success"); await renderAdminProducts(); }
    catch(err){ showToast(readableError(err),"error"); }
  });
}

function openProductModal(product = null) {
  state.pendingImageBase64 = product?.imagenBase64 || "";
  const modal = document.createElement("div"); modal.className="modal-backdrop";
  modal.innerHTML = `<div class="modal"><div class="modal-head"><div><span class="eyebrow">${product?"Editar":"Nuevo"} producto</span><h3>${product?"Actualizar producto":"Agregar al menú"}</h3></div><button class="close-btn" data-close>×</button></div>
    <form id="productForm" class="stack">
      <div class="field"><label>Nombre</label><input id="pName" class="input" required maxlength="100" value="${escapeHtml(product?.nombre||"")}"></div>
      <div class="field"><label>Descripción</label><textarea id="pDesc" class="input" required>${escapeHtml(product?.descripcion||"")}</textarea></div>
      <div class="form-grid"><div class="field"><label>Precio (S/)</label><input id="pPrice" class="input" type="number" step="0.01" min="0.01" required value="${product?.precio??""}"></div><div class="field"><label>Categoría</label><select id="pCategory" class="input">${CATEGORIES.map(c=>`<option ${product?.categoria===c?"selected":""}>${c}</option>`).join("")}</select></div></div>
      <div class="field"><label>Imagen del producto</label><input id="pImage" class="input" type="file" accept="image/*"><small style="color:var(--muted)">Se redimensiona y comprime antes de guardar en Firestore.</small></div>
      <img id="pPreview" class="preview ${imageSrc(product)?"":"hidden"}" src="${escapeHtml(imageSrc(product))}" alt="Vista previa">
      <div id="imageStatus" style="font-size:.85rem;color:var(--muted)">${state.pendingImageBase64 ? "Imagen actual cargada." : "Selecciona una foto."}</div>
      <div class="form-actions"><button type="button" class="btn btn-light" data-close>Cancelar</button><button type="submit" class="btn btn-primary">Guardar</button></div>
    </form></div>`;
  document.body.appendChild(modal);
  $$('[data-close]',modal).forEach(b=>b.onclick=()=>modal.remove());
  modal.onclick=e=>{if(e.target===modal)modal.remove();};
  $('#pImage',modal).onchange = async e => {
    const file=e.target.files?.[0]; if(!file)return;
    const status=$('#imageStatus',modal); status.textContent="Procesando imagen...";
    try {
      const base64=await compressImage(file); state.pendingImageBase64=base64;
      const prev=$('#pPreview',modal); prev.src=`data:image/jpeg;base64,${base64}`; prev.classList.remove('hidden');
      status.textContent=`Imagen lista (${Math.round(base64.length/1024)} KB en Base64).`;
    } catch(err) { state.pendingImageBase64=""; status.textContent=readableError(err); status.style.color="var(--danger)"; }
  };
  $('#productForm',modal).onsubmit=async e=>{
    e.preventDefault(); const btn=e.submitter; btn.disabled=true;
    try {
      if(!state.pendingImageBase64 && !product?.imagenUrl) throw new Error("Selecciona una imagen para el producto.");
      const ref = product ? doc(db,"productos",product.productoId) : doc(collection(db,"productos"));
      const data={productoId:ref.id,nombre:$('#pName',modal).value.trim(),descripcion:$('#pDesc',modal).value.trim(),precio:Number($('#pPrice',modal).value),categoria:$('#pCategory',modal).value,imagenUrl: product?.imagenUrl || "", imagenBase64:state.pendingImageBase64};
      await setDoc(ref,data); modal.remove(); showToast(product?"Producto actualizado.":"Producto creado.","success"); await renderAdminProducts();
    }catch(err){showToast(readableError(err),"error");btn.disabled=false;}
  };
}

async function compressImage(file) {
  if (!file.type.startsWith("image/")) throw new Error("Selecciona un archivo de imagen válido.");
  const dataUrl = await new Promise((resolve,reject)=>{ const r=new FileReader(); r.onload=()=>resolve(r.result); r.onerror=reject; r.readAsDataURL(file); });
  const img = await new Promise((resolve,reject)=>{ const im=new Image(); im.onload=()=>resolve(im); im.onerror=()=>reject(new Error("No se pudo leer la imagen.")); im.src=dataUrl; });
  const max=640; let w=img.naturalWidth, h=img.naturalHeight; const scale=Math.min(1,max/Math.max(w,h)); w=Math.max(1,Math.round(w*scale)); h=Math.max(1,Math.round(h*scale));
  const canvas=document.createElement('canvas'); canvas.width=w; canvas.height=h; const ctx=canvas.getContext('2d'); ctx.drawImage(img,0,0,w,h);
  for(let q=.72;q>=.38;q-=.1){ const url=canvas.toDataURL('image/jpeg',q); const b64=url.split(',')[1]; if(b64.length<=MAX_IMAGE_BASE64)return b64; }
  throw new Error("La imagen es demasiado pesada. Elige otra de menor tamaño.");
}

async function renderAdminOrders() {
  const snap=await getDocs(collection(db,"pedidos"));
  state.orders=snap.docs.map(d=>({...d.data(),pedidoId:d.data().pedidoId||d.id})).sort((a,b)=>(b.fechaMillis||0)-(a.fechaMillis||0));
  const userSnap=await getDocs(collection(db,"usuarios")); const names=new Map(userSnap.docs.map(d=>[d.id,d.data()]));
  appEl.innerHTML=`<div class="section-head"><div><span class="eyebrow">Administración</span><h2>Pedidos</h2><p>${state.orders.length} pedido${state.orders.length===1?"":"s"} registrado${state.orders.length===1?"":"s"}.</p></div></div>
    ${state.orders.length?`<div class="table-wrap"><table><thead><tr><th>Pedido</th><th>Cliente</th><th>Fecha</th><th>Dirección</th><th>Total</th><th>Estado</th></tr></thead><tbody>${state.orders.map(o=>{const u=names.get(o.usuarioId)||{};return `<tr><td><strong>${escapeHtml(o.pedidoId.slice(0,8))}</strong><br><small>${(o.items||[]).map(i=>`${i.cantidad}× ${escapeHtml(i.nombre)}`).join('<br>')}</small></td><td>${escapeHtml(u.nombre||o.usuarioId)}<br><small>${escapeHtml(u.correo||"")}</small></td><td>${escapeHtml(o.fecha||formatDate(o.fechaMillis))}</td><td>${escapeHtml(o.direccionEntrega||"—")}</td><td><strong>${money(o.total)}</strong></td><td><select class="input" data-order-status="${o.pedidoId}" style="min-width:145px">${["PENDIENTE","CONFIRMADO","PREPARANDO","ENTREGADO","CANCELADO"].map(s=>`<option ${o.estado===s?"selected":""}>${s}</option>`).join('')}</select></td></tr>`}).join('')}</tbody></table></div>`:`<div class="empty"><strong>No hay pedidos.</strong>Los nuevos pedidos aparecerán aquí.</div>`}`;
  $$('[data-order-status]').forEach(s=>s.onchange=async()=>{try{await updateDoc(doc(db,"pedidos",s.dataset.orderStatus),{estado:s.value});showToast("Estado actualizado.","success");}catch(err){showToast(readableError(err),"error");}});
}

async function renderAdminUsers() {
  const snap=await getDocs(collection(db,"usuarios"));
  state.users=snap.docs.map(d=>({...d.data(),usuarioId:d.data().usuarioId||d.id})).sort((a,b)=>(a.rol===ROLE_ADMIN?-1:1)-(b.rol===ROLE_ADMIN?-1:1)||String(a.nombre).localeCompare(String(b.nombre)));
  appEl.innerHTML=`<div class="section-head"><div><span class="eyebrow">Administración</span><h2>Usuarios y administradores</h2><p>Los clientes se registran por sí mismos; los administradores pueden crear otros administradores.</p></div><button id="newAdminBtn" class="btn btn-primary">+ Crear administrador</button></div>
    <div class="stats"><div class="stat"><strong>${state.users.length}</strong><span>Usuarios</span></div><div class="stat"><strong>${state.users.filter(u=>u.rol===ROLE_ADMIN).length}</strong><span>Administradores</span></div><div class="stat"><strong>${state.users.filter(u=>u.rol===ROLE_CLIENT).length}</strong><span>Clientes</span></div></div>
    <div class="table-wrap"><table><thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Acción</th></tr></thead><tbody>${state.users.map(u=>{const primary=(u.correo||'').toLowerCase()===PRIMARY_ADMIN;const self=u.usuarioId===state.user.uid;return `<tr><td><strong>${escapeHtml(u.nombre||'—')}</strong></td><td>${escapeHtml(u.correo||'—')}${primary?'<br><small>Administrador principal</small>':''}</td><td><span class="status">${escapeHtml(u.rol)}</span></td><td>${primary||self?'<span style="color:var(--muted)">Protegido</span>':`<button class="btn btn-light btn-sm" data-role-user="${u.usuarioId}" data-role-next="${u.rol===ROLE_ADMIN?ROLE_CLIENT:ROLE_ADMIN}">${u.rol===ROLE_ADMIN?'Convertir en cliente':'Convertir en admin'}</button>`}</td></tr>`}).join('')}</tbody></table></div>`;
  $('#newAdminBtn').onclick=openAdminModal;
  $$('[data-role-user]').forEach(b=>b.onclick=async()=>{try{await updateDoc(doc(db,"usuarios",b.dataset.roleUser),{rol:b.dataset.roleNext});showToast("Rol actualizado.","success");await renderAdminUsers();}catch(err){showToast(readableError(err),"error");}});
}

function openAdminModal() {
  const modal=document.createElement('div');modal.className='modal-backdrop';modal.innerHTML=`<div class="modal"><div class="modal-head"><div><span class="eyebrow">Administración</span><h3>Crear nuevo administrador</h3></div><button class="close-btn" data-close>×</button></div><form id="adminForm" class="stack"><div class="field"><label>Nombre</label><input id="aName" class="input" required></div><div class="field"><label>Correo</label><input id="aEmail" class="input" type="email" required></div><div class="field"><label>Contraseña inicial</label><input id="aPass" class="input" type="password" minlength="6" required></div><div class="form-actions"><button type="button" class="btn btn-light" data-close>Cancelar</button><button type="submit" class="btn btn-primary">Crear administrador</button></div></form></div>`;document.body.appendChild(modal);$$('[data-close]',modal).forEach(b=>b.onclick=()=>modal.remove());
  $('#adminForm',modal).onsubmit=async e=>{e.preventDefault();const btn=e.submitter;btn.disabled=true;let secondaryApp=null;let secondaryAuth=null;let created=null;try{const name=$('#aName',modal).value.trim(),email=$('#aEmail',modal).value.trim().toLowerCase(),pass=$('#aPass',modal).value;if(email===PRIMARY_ADMIN)throw new Error("El administrador principal ya existe.");secondaryApp=initializeApp(firebaseConfig,`adminCreator-${Date.now()}`);secondaryAuth=getAuth(secondaryApp);const cred=await createUserWithEmailAndPassword(secondaryAuth,email,pass);created=cred.user;await updateProfile(created,{displayName:name});await setDoc(doc(db,"usuarios",created.uid),{usuarioId:created.uid,nombre:name,correo:email,rol:ROLE_ADMIN});await signOut(secondaryAuth);await deleteApp(secondaryApp);modal.remove();showToast("Administrador creado.","success");await renderAdminUsers();}catch(err){if(created){try{await deleteUser(created);}catch{}}if(secondaryAuth){try{await signOut(secondaryAuth);}catch{}}if(secondaryApp){try{await deleteApp(secondaryApp);}catch{}}showToast(readableError(err),"error");btn.disabled=false;}};
}

updateCartBadge();
