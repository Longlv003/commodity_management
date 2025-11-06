const {userModel} = require('../models/account.model');
const bcrypt = require('bcrypt');
const {uploadSingleFile} = require('../helpers/upload.helper');

exports.doLogin = async (req, res, next) => {
    // method luôn là post
    try {
        const { email, pass } = req.body;

        if (!email || !pass) {
            return res.status(400).json({ error: 'Missing email or password' });
        }

        const user = await userModel.findByEmailPasswd(email, pass);
        if (!user) {
            return res.status(401).json({error: 'Incorrect login credentials'})
        }

        if (!user.is_active) {
            return res.status(403).json({ error: 'Account is locked. Please contact admin' });
        }
        
        const token = await userModel.makeAuthToken(user);
        return res.status(200).json({
                message: 'Login successful',
                data: { user, token }
            });
    } catch (error) {
        console.log(error.message);
        return res.status(400).send(error)
    }
};

exports.doReg = async (req, res, next) => {
    try {
        const salt = await bcrypt.genSalt(10);

        const existed = await userModel.findOne({ email: req.body.email });
        if (existed) {
            return res.status(400).json({ error: 'Email already exists' });
        }

        const user = new userModel(req.body);

        user.pass = await bcrypt.hash(req.body.pass, salt);

        if (req.file) {
            const fileName = await uploadSingleFile(req.file, 'avatars');
            user.image = fileName; // lưu tên file vào DB
        }

        const token = await userModel.makeAuthToken(user);

        let newUser = await user.save();

        return res.status(200).json({
            message: 'Register successfully',
            data: {newUser, token}
        })
    } catch (error) {
        console.log(error.message);
        return res.status(400).send(error.message);
    }
};

exports.UploadAvatar = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        const fileName = await uploadSingleFile(req.file, 'avatars');
        dataRes.data = fileName;
    } catch (error) {
        dataRes.msg = error.message;
    }
    res.json(dataRes);
};

exports.updateUserStatus = async (req, res) => {
    try {
        const { _id } = req.params;
        const { role, is_active } = req.body;

        const user = await userModel.findById(_id);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Không cho admin tự khóa mình
        if (req.user._id.equals('admin')) {
            return res.status(400).json({ error: 'admin cannot modify your own status or role' });
        }

        // Chỉ cập nhật nếu có dữ liệu
        if (role) user.role = role;
        if (typeof is_active === 'boolean') user.is_active = is_active;

        await user.save();

        return res.status(200).json({
            message: 'User updated successfully',
            data: user
        });
    } catch (error) {
        console.log(error.message);
        return res.status(400).send(error);
    }
};

exports.GetAllAccount = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        let list = await userModel.find();
        dataRes.data = list;
    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }
    res.json(dataRes);
};