const jwt = require('jsonwebtoken');
var {userModel} = require('../models/account.model')
require('dotenv').config();
const chuoi_bi_mat = process.env.TOKEN_SEC_KEY;
// hàm kiểm tra đăng nhập

const api_auth = async (req, res , next)=>{
   // lấy token trong header hoặc cookie
   let header_token = req.header('Authorization');
   let token = null;
   if (typeof(header_token) != 'undefined' && header_token != null) {
       token = header_token.replace('Bearer ', '');
   } else if (req.cookies && req.cookies.token) {
       token = req.cookies.token;
   } else {
       // Nếu là request HTML -> redirect về trang login; còn lại trả JSON
       if (req.accepts('html')) {
           return res.redirect('/auth/login');
       }
       return res.status(403).json({error: 'Không xác định token'});
   }
   
   try {
       let data = jwt.verify(token, chuoi_bi_mat);
       console.log(data);
       // kiểm tra tồn tại user trong csdl
       let user = await userModel.findOne({_id: data._id, token:token});
       // có thể lấy theo ID sau đó so sánh token bằng code
       if(!user){
           throw new Error('Không xác định người dùng');
       }
       // ok tồn tại thông tin trong csdl
       req.user = user;
       req.token = token;

       next();// xác thực ok, cho phép làm tiếp các công việc tiếp theo
      
   } catch (error) {
       console.error(error);
       res.status(401).send({error: error.message})
   }
};

// const checkRole = async (req, res, next) => {
//     if (req.user.role !== 'admin' && req.user.role !== 'super_admin') {
//         return res.status(403).json({ error: 'Chỉ admin mới được phép thực hiện thao tác này.' });
//     }
//     next();
// };

const checkRole = (roles = []) => {
    // roles: mảng quyền được phép, ví dụ ['admin'] hoặc ['admin', 'super_admin']
    return (req, res, next) => {
        if (!req.user || !roles.includes(req.user.role)) {
            return res.status(403).json({ error: 'Không có quyền truy cập' });
        }
        next();
    };
};

module.exports = {api_auth, checkRole};
