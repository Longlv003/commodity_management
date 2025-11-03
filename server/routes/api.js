var express = require('express');
var router = express.Router();
var mdw = require('../middleware/api.auth');
var accountCtrl = require('../controllers/account.controller');
var multer = require('multer');
var path = require('path');
var upload = multer({dest: path.join(__dirname, '../public/upload')});

router.post('/account/register', upload.single('image'), accountCtrl.doReg);
router.post('/account/login', accountCtrl.doLogin);
router.post('/account/upload-avatar',upload.single("image"), accountCtrl.UploadAvatar);



module.exports = router;
