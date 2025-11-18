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

// User
router.post("/account/register", upload.single("image"), accountCtrl.doReg);
router.post("/account/login", upload.none(), accountCtrl.doLogin);
router.post(
  "/account/upload-avatar",
  upload.single("image"),
  accountCtrl.UploadAvatar
);
router.get(
  "/account/list",
  mdw.api_auth,
  mdw.checkRole(["admin"]),
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
  mdw.checkRole(["admin"]),
  upload.single("image"),
  bannerCtrl.AddBanner
);
router.delete(
  "/banner/sale/delete",
  mdw.api_auth,
  mdw.checkRole(["admin"]),
  bannerCtrl.DeleteBanner
);
router.get("/banner/sale/list", bannerCtrl.GetAllBanner);

// Category
router.post(
  "/category/add",
  mdw.api_auth,
  mdw.checkRole(["admin"]),
  catCtrl.addCat
);
router.put(
  "/category/edit/:_id",
  mdw.api_auth,
  mdw.checkRole(["admin"]),
  catCtrl.updateCat
);
router.delete(
  "/category/delete/:_id",
  mdw.api_auth,
  mdw.checkRole(["admin"]),
  catCtrl.deleteCat
);
router.get("/category/list", catCtrl.getListCat);
router.get("/categories/top4", catCtrl.GetTopCategories);

// Product
router.post("/product/add", upload.single("image"), pCtrl.addProduct);
router.put("/product/edit/:_id", pCtrl.EditProduct);
router.delete("/product/delete/:_id", pCtrl.DeleteProduct);
router.get("/product/list", pCtrl.GetListProduct);
router.get("/product/list-by-cat", pCtrl.GetProductByCat);
router.get("/product/list/top-selling", pCtrl.GetTopSellingProducts);
router.put(
  "/product/:_id/edit/favorite/:is_favorite",
  mdw.api_auth,
  pCtrl.UpdateFavorite
);
router.get("/product/list/favorite", mdw.api_auth, pCtrl.GetFavoriteProducts);

// Cart
router.post("/cart/add", mdw.api_auth, cartCtrl.addToCart);
router.put(
  "/cart/:_id/update/:newQuantity",
  mdw.api_auth,
  cartCtrl.UpdateCartQuantity
);
router.delete("/cart/delete/:_id", mdw.api_auth, cartCtrl.DeleteCartItem);
router.get("/getListMyCart/:id_user", cartCtrl.GetListMyCart);

//router.post('/order/:id_user/place/:address', mdw.api_auth, orderCtrl.PlaceOrder);
router.post("/order", mdw.api_auth, orderCtrl.PlaceOrder);
router.get("/order/history/:id_user", orderCtrl.GetOrderHistory);

// Wallet
router.post("/wallet/create", mdw.api_auth, walletCtrl.CreateWallet);

module.exports = router;
