//const mongoose = require('mongoose');
const {cartModel} = require('../models/cart.model');
const { mongoose } = require('../models/db');
const {pModel} = require('../models/product.model');

exports.GetListMyCart = async (req, res, next) => {
    let dataRes = {msg: 'OK'};

    try {
        const userId = new mongoose.Types.ObjectId(req.params.id_user);
        const cartItems = await cartModel.find({ id_user: userId })
            .populate("id_product"); 
        dataRes.data = cartItems;

    } catch (error) {
        dataRes.msg = error.message;
    }

    res.json(dataRes);
}

exports.addToCart = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        const {id_user, id_product, quantity} = req.body;
        if (!id_user || !id_product) {
            throw new Error("Thieu thong tin nguoi dung hoa san pham");
        }

        // Kiem tra sp ton tai khong?
        const product = await pModel.findById(id_product);
        if (!product) {
            throw new Error("Khong tim thay san pham");
        }

        // Ktr cart co sp chua?
        let cartItem = await cartModel.findOne({id_user, id_product});
        if (cartItem) {
            cartItem.quantity += (quantity||1);
            await cartItem.save();
        } else {
            cartItem = new cartModel({
                id_user,
                id_product,
                quantity: quantity || 1
            });
            await cartItem.save();
        }

        dataRes.data = cartItem;
    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }

    res.json(dataRes);
}

exports.UpdateCartQuantity = async (req, res, next) => {
    let dataRes = { msg: 'OK' };
    try {
        const {_id, newQuantity} = req.params;
        // const _id = req.body._id;
        // const newQuantity = req.body.quantity;

        if (!newQuantity || newQuantity < 1) {
            throw new Error("Số lượng không hợp lệ");
        }

        const cartItem = await cartModel.findById(_id);
        if (!cartItem) {
            throw new Error("Không tìm thấy sản phẩm trong giỏ hàng");
        }

        const updated = await cartModel.findByIdAndUpdate(
            _id,
            { quantity: newQuantity },
            { new: true }
        );

        //dataRes.data = updated;
         
        const cartItems = await cartModel.find({ _id: _id })
            .populate("id_product"); 
        dataRes.data = cartItems;
    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }
    res.json(dataRes);
};

exports.DeleteCartItem = async (req, res, next) => {
    let dataRes = { msg: 'OK' };
    try {
        const _id = req.params._id; 

        if (!_id) {
            throw new Error("Thiếu ID sản phẩm trong giỏ hàng");
        }

        const deleted = await cartModel.findByIdAndDelete(_id);

        if (!deleted) {
            throw new Error("Không tìm thấy sản phẩm trong giỏ hàng");
        }

        dataRes.data = deleted;
    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }

    res.json(dataRes);
};