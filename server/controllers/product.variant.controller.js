let { pModel } = require("../models/product.model");
let { pVariantModel } = require("../models/product.variants.model");
const { uploadSingleFile } = require("../helpers/upload.helper");

exports.AddVariant = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { product_id, sku, size, color, quantity, price } = req.body;

    if (!product_id || !sku || !size || !color) {
      throw new Error("Missing required fields");
    }

    // Check tồn tại Product trước
    const product = await pModel.findById(product_id);
    if (!product || product.is_deleted) {
      throw new Error("Product does not exist or has been deleted");
    }

    // Check trùng SKU
    const existedSKU = await pVariantModel.findOne({ sku, is_deleted: false });
    if (existedSKU) {
      throw new Error("SKU already exists");
    }

    const variant = new pVariantModel({
      product_id,
      sku,
      size,
      color,
      quantity: quantity || 0,
      price: price || 0,
    });

    // Nếu upload hình variant
    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "products");
      variant.image = Array.isArray(fileName) ? fileName : [fileName];
    }

    const saved = await variant.save();

    dataRes.msg = "Variant created successfully";
    dataRes.data = saved;
  } catch (error) {
    console.error("AddVariant Error:", error);
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};

exports.EditVariant = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id } = req.params;
    const { sku, size, color, quantity, price } = req.body;

    if (!_id) {
      throw new Error("Missing variant ID");
    }

    // Tìm variant
    const variant = await pVariantModel.findById(_id);
    if (!variant) {
      throw new Error("Variant not found");
    }

    // Kiểm tra xem variant có bị xóa mềm không
    if (variant.is_deleted) {
      throw new Error("Cannot edit a deleted variant");
    }

    // Tạo object update
    let updateData = {};
    if (sku && sku !== variant.sku) {
      // Kiểm tra SKU trùng nếu thay đổi
      const existedSKU = await pVariantModel.findOne({ 
        sku, 
        is_deleted: false,
        _id: { $ne: _id }
      });
      if (existedSKU) {
        throw new Error("SKU already exists");
      }
      updateData.sku = sku;
    }
    if (size) updateData.size = size;
    if (color) updateData.color = color;
    if (quantity !== undefined) updateData.quantity = Number(quantity);
    if (price !== undefined) updateData.price = Number(price);

    if (Object.keys(updateData).length === 0 && !req.file) {
      throw new Error("No data to update");
    }

    // Nếu upload hình variant mới
    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "products");
      updateData.image = Array.isArray(fileName) ? fileName : [fileName];
    }

    // Update DB
    const updatedVariant = await pVariantModel.findByIdAndUpdate(_id, updateData, {
      new: true,
    });

    dataRes.msg = "Variant updated successfully";
    dataRes.data = updatedVariant;
  } catch (error) {
    console.error("EditVariant Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

exports.DeleteVariant = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id } = req.params;

    if (!_id) {
      throw new Error("Missing variant ID");
    }

    // Tìm variant
    const variant = await pVariantModel.findById(_id);
    if (!variant) {
      throw new Error("Variant not found");
    }

    // Kiểm tra xem variant đã bị xóa chưa
    if (variant.is_deleted) {
      dataRes.msg = "Variant has already been deleted";
      dataRes.data = variant;
      return res.json(dataRes);
    }

    // Đếm số variant còn lại của product (chưa bị xóa)
    const remainingVariants = await pVariantModel.countDocuments({
      product_id: variant.product_id,
      is_deleted: false,
      _id: { $ne: _id } // Không tính variant đang xóa
    });

    // Xóa mềm variant
    variant.is_deleted = true;
    variant.deleted_at = new Date();
    await variant.save();

    // Nếu chỉ còn 1 variant (variant đang xóa) hoặc không còn variant nào
    // thì xóa luôn product
    if (remainingVariants === 0) {
      const product = await pModel.findById(variant.product_id);
      if (product && !product.is_deleted) {
        product.is_deleted = true;
        product.deleted_at = new Date();
        await product.save();
        dataRes.msg = "Variant and product deleted successfully (no variants remaining)";
      } else {
        dataRes.msg = "Variant deleted successfully";
      }
    } else {
      // Còn nhiều variant, chỉ xóa variant
      dataRes.msg = "Variant deleted successfully (product still has other variants)";
    }

    dataRes.data = {
      variant: variant,
      product_deleted: remainingVariants === 0
    };
  } catch (error) {
    console.error("DeleteVariant Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};