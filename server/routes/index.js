var express = require("express");
var router = express.Router();

/* GET home page. */
router.get("/", function (req, res, next) {
  res.render("index", { title: "Express" });
});

// Trang đăng nhập admin
router.get("/login", (req, res) => {
  res.render("login");
});

// Trang quản lý sản phẩm sau khi đăng nhập
router.get("/admin", (req, res) => {
  res.render("admin_products");
});

module.exports = router;
