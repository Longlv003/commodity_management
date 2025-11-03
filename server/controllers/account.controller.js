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
        const user = new userModel(req.body);

        user.pass = await bcrypt.hash(req.body.pass, salt);

        const token = await userModel.makeAuthToken(user);

        let newUser = await user.save();

        return res.status(200).json({
            message: 'Register successfully',
            data: {newUser, token}
        })
    } catch (error) {
        console.log(error.message);
        return res.status(400).send(error);
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

