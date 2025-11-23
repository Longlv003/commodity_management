var express = require("express");
var router = express.Router();

/* GET home page. */
// router.get("/", function (req, res, next) {
//   res.render("index", { title: "Express" });
// });

// Trang đăng nhập admin
router.get("/login", (req, res) => {
  res.render("login");
});

// Trang dashboard (trang chủ admin)
router.get("/admin/dashboard", (req, res) => {
  res.render("admin_dashboard");
});

// Trang quản lý sản phẩm sau khi đăng nhập
router.get("/admin/products", (req, res) => {
  res.render("admin_products");
});

// Trang quản lý đơn hàng
router.get("/admin/orders", (req, res) => {
  res.render("admin_orders");
});

// Trang thống kê báo cáo doanh thu
router.get("/admin/statistics", (req, res) => {
  res.render("admin_statistics");
});

// Trang quản lý khách hàng
router.get("/admin/customers", (req, res) => {
  res.render("admin_customers");
});

// Trang quản lý banner
router.get("/admin/banners", (req, res) => {
  res.render("admin_banners");
});

module.exports = router;
