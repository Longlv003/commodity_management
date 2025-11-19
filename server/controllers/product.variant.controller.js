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
