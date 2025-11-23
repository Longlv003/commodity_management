var express = require("express");
var router = express.Router();
var mdw = require("../middleware/api.auth");
var accountCtrl = require("../controllers/account.controller");
var multer = require("multer");
var path = require("path");
var upload = multer({ dest: path.join(__dirname, "../public/upload") });
var bannerCtrl = require("../controllers/banner_sale.controller");
var catCtrl = require("../controllers/category.controller");
var pCtrl = require("../controllers/product.controller");
var cartCtrl = require("../controllers/cart.controller");
var orderCtrl = require("../controllers/order.controller");
var walletCtrl = require("../controllers/wallet.controller");
var userFavoriteCtrl = require("../controllers/userFavorite.controller");
var statisticsCtrl = require("../controllers/statistics.controller");
var customerCtrl = require("../controllers/customer.controller");
var pVariantCtrl = require("../controllers/product.variant.controller");

// User
router.post("/account/register", upload.single("image"), accountCtrl.doReg);
router.post("/account/login", upload.none(), accountCtrl.doLogin);
router.post("/account/login/web", upload.none(), accountCtrl.doLoginWeb);
router.post(
  "/account/upload-avatar",
  upload.single("image"),
  accountCtrl.UploadAvatar
);
router.get(
  "/account/list",
  mdw.api_auth,
  mdw.checkRole(["admin", "engineer"]),
  accountCtrl.GetAllAccount
);
router.put(
  "/account/update/:_id",
  mdw.api_auth,
  upload.single("image"),
  accountCtrl.UpdateUser
);
router.put(
  "/account/:_id/upload/avatar",
  mdw.api_auth,
  upload.single("image"),
  accountCtrl.UploadAvatar
);

// Banner
router.post(
  "/banner/sale/add",
  mdw.api_auth,
  mdw.checkRole(["admin", "engineer"]),
  upload.single("image"),
  bannerCtrl.AddBanner
);
router.get("/banner/sale/list", bannerCtrl.GetAllBanner);
router.get("/banner/sale/list/admin", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), bannerCtrl.GetAllBannersAdmin);
router.put("/banner/sale/update/:_id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), upload.single("image"), bannerCtrl.UpdateBanner);
router.delete("/banner/sale/delete/:_id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), bannerCtrl.DeleteBanner);

// Category
router.post(
  "/category/add",
  mdw.api_auth,
  mdw.checkRole(["admin", "engineer"]),
  catCtrl.addCat
);
router.put(
  "/category/edit/:_id",
  mdw.api_auth,
  mdw.checkRole(["admin", "engineer"]),
  catCtrl.updateCat
);
router.delete(
  "/category/delete/:_id",
  mdw.api_auth,
  mdw.checkRole(["admin", "engineer"]),
  catCtrl.deleteCat
);
router.get("/category/list", catCtrl.getListCat);
router.get("/categories/top4", catCtrl.GetTopCategories);

// Product
router.post(
  "/product/add",
  mdw.api_auth,
  mdw.checkRole(["admin", "engineer"]),
  upload.any(),
  pCtrl.AddProduct
);
router.put(
  "/product/edit/:_id",
  mdw.api_auth,
  mdw.checkRole(["admin", "engineer"]),
  upload.single("image"),
  pCtrl.EditProduct
);
router.delete("/product/delete/:_id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), pCtrl.DeleteProduct);
router.get("/product/list", pCtrl.GetListProduct);
router.get("/product/list/admin", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), pCtrl.GetAdminProducts);
router.get("/product/list-by-cat", pCtrl.GetProductByCat);
router.get("/product/list/top-selling", pCtrl.GetTopSellingProducts);
router.put(
  "/product/:_id/edit/favorite/:is_favorite",
  mdw.api_auth,
  pCtrl.UpdateFavorite
);
router.get("/product/list/favorite", mdw.api_auth, pCtrl.GetFavoriteProducts);
router.get("/product/:_id", pCtrl.GetProductDetail);

// Product Variant
router.put("/variant/edit/:_id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), upload.single("image"), pVariantCtrl.EditVariant);
router.delete("/variant/delete/:_id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), pVariantCtrl.DeleteVariant);

// User Favorite
// user_id bắt buộc phải truyền qua query (?user_id=xxx)
// Ví dụ: GET /api/favorite/list?user_id=xxx hoặc POST /api/favorite/add?user_id=xxx
router.post("/favorite/add", mdw.api_auth, userFavoriteCtrl.AddFavorite);
router.delete("/favorite/remove/:product_id", mdw.api_auth, userFavoriteCtrl.RemoveFavorite);
router.post("/favorite/toggle", mdw.api_auth, userFavoriteCtrl.ToggleFavorite);
router.get("/favorite/check/:product_id", mdw.api_auth, userFavoriteCtrl.CheckFavorite);
router.get("/favorite/list", mdw.api_auth, userFavoriteCtrl.GetUserFavorites);

// Cart
router.post("/cart/add", mdw.api_auth, cartCtrl.addToCart);
router.get("/cart/list/:id_user", cartCtrl.getCartList);
router.put("/cart/update/:_id", mdw.api_auth, cartCtrl.updateCartQuantity);
router.delete("/cart/delete/:_id", mdw.api_auth, cartCtrl.deleteCartItem);

//router.post('/order/:id_user/place/:address', mdw.api_auth, orderCtrl.PlaceOrder);
router.post("/order", mdw.api_auth, orderCtrl.PlaceOrder);
router.get("/order/history/:id_user", orderCtrl.GetOrderHistory);
router.get("/order/list/admin", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), orderCtrl.GetAllOrders);

// Wallet
router.post("/wallet/create", mdw.api_auth, walletCtrl.CreateWallet);
router.post("/wallet/login", mdw.api_auth, walletCtrl.LoginWallet);
router.get("/wallet/info", mdw.api_auth, walletCtrl.GetWalletInfo);
router.get("/wallet/balance", mdw.api_auth, walletCtrl.CheckBalance);
router.post("/wallet/deposit", mdw.api_auth, walletCtrl.Deposit);
router.post("/wallet/withdraw", mdw.api_auth, walletCtrl.Withdraw);
router.put("/wallet/change-pin", mdw.api_auth, walletCtrl.ChangePin);
router.get("/wallet/history", mdw.api_auth, walletCtrl.GetTransactionHistory);

// Statistics
router.get("/statistics/revenue", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), statisticsCtrl.GetRevenueStatistics);
router.get("/statistics/top-selling", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), statisticsCtrl.GetTopSellingProductsStats);

// Customer Management
router.get("/customer/list", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), customerCtrl.GetAllCustomers);
router.get("/customer/detail/:id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), customerCtrl.GetCustomerDetail);
router.post("/customer/add", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), upload.single("image"), customerCtrl.AddCustomer);
router.put("/customer/update/:id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), upload.single("image"), customerCtrl.UpdateCustomer);
router.delete("/customer/delete/:id", mdw.api_auth, mdw.checkRole(["admin", "engineer"]), customerCtrl.DeleteCustomer);

module.exports = router;