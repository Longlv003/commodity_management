var { pModel } = require('../models/product.model');
const { uploadSingleFile } = require('../helpers/upload.helper');
const mongoose = require('mongoose');

exports.addProduct = async (req, res, next) => {
    let dataRes = { msg: 'OK' };

    try {
        const { name, price, qty, catID, description, createdAt } = req.body;

        // Kiểm tra thông tin bắt buộc
        if (name == null || price == null || catID == null || qty == null) {
            throw new Error("Missing required information");
        }

        // Tạo đối tượng sản phẩm
        const product = new pModel({
            name,
            price,
            qty,
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
        const { name, price, qty, description} = req.body;
        
        // Tạo object update chỉ chứa những trường có giá trị
        let updateData = {};
        if (name) updateData.name = name;
        if (price) updateData.price = price;
        if (qty) updateData.qty = qty;
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
    let dataRes = { msg: 'OK' };
    
    try {
        // Kiểm tra nếu không có catID thì trả về lỗi
        if (!req.query.catID || req.query.catID.trim() === '') {
            return res.status(400).json({
                msg: 'Thiếu tham số catID',
                data: null
            });
        }
        
        // Validate ObjectId
        if (!mongoose.Types.ObjectId.isValid(req.query.catID)) {
            return res.status(400).json({
                msg: 'ID danh mục không hợp lệ',
                data: null
            });
        }
        
        // Tạo điều kiện tìm kiếm theo catID
        const dk = { catID: new mongoose.Types.ObjectId(req.query.catID) };
        
        // Lấy danh sách sản phẩm theo catID
        let list = await pModel.find(dk);
        
        dataRes.data = list;
        dataRes.total = list.length;
        
    } catch (error) {
        console.error('Lỗi khi lấy sản phẩm theo danh mục:', error);
        dataRes.data = null;
        dataRes.msg = 'Lỗi server: ' + error.message;
        return res.status(500).json(dataRes);
    }
    
    res.json(dataRes);
}
