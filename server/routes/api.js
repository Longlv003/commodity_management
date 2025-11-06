var express = require('express');
var router = express.Router();
var mdw = require('../middleware/api.auth');
var accountCtrl = require('../controllers/account.controller');
var multer = require('multer');
var path = require('path');
var upload = multer({dest: path.join(__dirname, '../public/upload')});
var bannerCtrl = require('../controllers/banner_sale.controller');
var catCtrl = require('../controllers/category.controller');
var pModel = require('../controllers/product.controller');
var walletCtrl = require('../controllers/wallet.controller');

// User
router.post('/account/register', upload.single('image'), accountCtrl.doReg);
router.post('/account/login', accountCtrl.doLogin);
router.post('/account/upload-avatar',upload.single("image"), accountCtrl.UploadAvatar);
router.get('/account/list',mdw.api_auth, mdw.checkRole(['admin']), accountCtrl.GetAllAccount);

// Banner
router.post('/banner/sale/add',mdw.api_auth, mdw.checkRole(['admin']), upload.single('image'), bannerCtrl.AddBanner);
router.delete('/banner/sale/delete',mdw.api_auth, mdw.checkRole(['admin']), bannerCtrl.DeleteBanner);
router.get('/banner/sale/list', bannerCtrl.GetAllBanner);

// Category
router.post('/category/add', mdw.api_auth, mdw.checkRole(['admin']), catCtrl.addCat);
router.put('/category/edit/:_id', mdw.api_auth, mdw.checkRole(['admin']), catCtrl.updateCat);
router.delete('/category/delete/:_id', mdw.api_auth, mdw.checkRole(['admin']), catCtrl.deleteCat);
router.get('/category/list', catCtrl.getListCat);

// Product
router.post('/product/add', upload.single('image'), pModel.addProduct);
router.put('/product/edit/:_id', pModel.EditProduct);
router.delete('/product/delete/:_id', pModel.DeleteProduct);
router.get('/product/list', pModel.GetListProduct);
router.get('/product/list-by-cat', pModel.GetProductByCat);

// Wallet
router.post('/wallet/create', mdw.api_auth, walletCtrl.CreateWallet);

module.exports = router;