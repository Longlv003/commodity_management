var { pModel } = require('../models/product.model');
const { uploadSingleFile } = require('../helpers/upload.helper');
const mongoose = require('mongoose');

exports.addProduct = async (req, res, next) => {
    let dataRes = { msg: 'OK' };

    try {
        const { name, price, catID, description, createdAt } = req.body;

        // Kiểm tra thông tin bắt buộc
        if (name == null || price == null || catID == null) {
            throw new Error("Missing required information");
        }

        // Tạo đối tượng sản phẩm
        const product = new pModel({
            name,
            price,
            catID,
            description: description || "",
            createdAt: createdAt || new Date()
        });

        if (req.file) {
            const fileName = await uploadSingleFile(req.file, 'products');
            product.image = fileName; // lưu tên file vào DB
        }

        // Lưu vào DB
        const savedProduct = await product.save();
        dataRes.data = savedProduct;
        dataRes.msg = "Product added successfully";
    } catch (error) {
        console.error(error);
        dataRes.msg = error.message;
    }

    res.json(dataRes);
};

exports.EditProduct = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        if (typeof(req.params._id) != 'undefined') {
            _id = req.params._id;
        }
        const { name, price, description} = req.body;
        
        // Tạo object update chỉ chứa những trường có giá trị
        let updateData = {};
        if (name) updateData.name = name;
        if (price) updateData.price = price;
        if (description) updateData.description = description;

        // Nếu có file upload, thêm image
        if (req.file) {
            const fileName = await uploadSingleFile(req.file, 'products');
            updateData.image = fileName;
        }

        if (Object.keys(updateData).length === 0) {
            throw new Error("No data to update");
        }

        // Cập nhật sản phẩm
        const updatedProduct = await pModel.findByIdAndUpdate(
            _id,
            updateData,
            { new: true } // trả về bản ghi sau khi update
        );

        if (!updatedProduct) {
            throw new Error("Product not found");
        }

        dataRes.msg = "Product updated successfully";
        dataRes.data = updatedProduct;
    }  catch (error) {
        console.error(error);
        dataRes.msg = error.message;
    }

    res.json(dataRes);
};

exports.DeleteProduct = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        if (typeof(req.params._id) != 'undefined') {
            _id = req.params._id;
        }
        const deleteProduct = await pModel.findByIdAndDelete(_id);
        if (!deleteProduct) {
            throw new Error("Delete operation failed: no products found");
        }
        dataRes.msg = "Xóa sản phẩm thành công!";
        dataRes.data = deleteProduct;
    } catch (error) {
        console.error(error);
        dataRes.msg = error.message;
    }

    res.json(dataRes);
}

exports.UploadImage = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        const fileName = await uploadSingleFile(req.file, 'products');
        dataRes.data = fileName;
    } catch (error) {
        dataRes.msg = error.message;
    }
    res.json(dataRes);
};

exports.GetListProduct = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        let list = await pModel.find();
        dataRes.data = list;
    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }
    res.json(dataRes);
}

exports.GetProductByCat = async (req, res, next) => {
    let dk = null;
    let dataRes = {msg: 'OK'};
    try {
        if (typeof(req.query.catID) != 'undefined') {
            dk = {catID: new mongoose.Types.ObjectId(req.query.catID)};
        }
        
        let list = await pModel.find(dk);
        dataRes.data = list;
    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }
    res.json(dataRes);
}

exports.GetListProductAndByCat = async (req, res, next) => {
    const dataRes = { msg: 'OK' };
    try {
        const filter = {};

        // Nếu query catID có giá trị → lọc theo category
        if (req.query.catID) {
            if (!mongoose.Types.ObjectId.isValid(req.query.catID)) {
                throw new Error("Invalid category ID");
            }
            filter.catID = new mongoose.Types.ObjectId(req.query.catID);
        }

        const list = await pModel.find(filter); // nếu filter rỗng → lấy tất cả
        dataRes.data = list;

    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }

    res.json(dataRes);
};
