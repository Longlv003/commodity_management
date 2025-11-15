var { pModel } = require("../models/product.model");
const { uploadSingleFile } = require("../helpers/upload.helper");
const mongoose = require("mongoose");
const { pVariantModel } = require("../models/product.variants.model");

exports.CheckProductAndSKU = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { productCode, sku } = req.body;

    if (!productCode || !sku) {
      throw new Error("Missing parameter: productCode or sku");
    }

    const product = await pModel.findOne({ productCode, is_deleted: false });
    const variant = await pVariantModel.findOne({ sku, is_deleted: false });

    if (!product && !variant) {
      dataRes.status = "CREATE_PRODUCT_AND_VARIANT";
    } else if (product && !variant) {
      dataRes.status = "ADD_VARIANT";
      dataRes.product = product;
    } else if (product && variant) {
      throw new Error("ProductCode and SKU already exist");
    } else {
      dataRes.status = "UNKNOWN_CASE";
    }
  } catch (error) {
    console.error("CheckProductAndSKU Error:", error);
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};

exports.AddOnlyProduct = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { name, productCode, catID, description, image } = req.body;

    if (!name || !productCode || !catID) {
      throw new Error("Missing required fields");
    }

    // Check trùng productCode
    const existed = await pModel.findOne({ productCode, is_deleted: false });
    if (existed) {
      throw new Error("ProductCode already exists");
    }

    const product = new pModel({
      name,
      productCode,
      catID,
      description: description || "",
      image: Array.isArray(image) ? image : [],
    });

    // Nếu upload file (single) thì override mảng ảnh
    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "products");
      product.image = [fileName];
    }

    const saved = await product.save();

    dataRes.msg = "Product created successfully";
    dataRes.data = saved;
  } catch (error) {
    console.error("AddProduct Error:", error);
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};

exports.AddProduct = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  // Bắt đầu transaction
  const session = await pModel.startSession();
  session.startTransaction();

  try {
    const { name, catID, description, productCode, variants } = req.body;

    // ----------- CHECK INPUT -----------
    if (!name || !catID) {
      throw new Error("Missing required fields");
    }

    // ----------- CHECK PRODUCT CODE -----------
    // Kiểm tra xem productCode đã tồn tại hay chưa
    if (productCode) {
      const existed = await pModel.findOne({ productCode });
      if (existed) throw new Error("productCode already exists");
    }

    // ----------- TẠO PRODUCT -----------
    const product = new pModel({
      name,
      catID,
      description: description || "",
      productCode: productCode || "",
    });

    // Nếu upload ảnh
    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "products");
      product.image = fileName;
    }

    const savedProduct = await product.save({ session });

    // ----------- XỬ LÝ VARIANTS -----------
    let variantList = [];

    if (variants) {
      let parsed;

      try {
        parsed = JSON.parse(variants);
      } catch (err) {
        throw new Error("Invalid variant JSON format");
      }

      if (Array.isArray(parsed)) {
        variantList = parsed.map((v) => ({
          ...v,
          product_id: savedProduct._id,
        }));
      }
    }

    // ----------- CHECK TRÙNG SKU -----------
    for (const v of variantList) {
      if (v.sku) {
        const existedSku = await pVariantModel.findOne({ sku: v.sku });
        if (existedSku) {
          throw new Error(`SKU already exists: ${v.sku}`);
        }
      }
    }

    // ----------- INSERT VARIANT -----------
    if (variantList.length > 0) {
      await pVariantModel.insertMany(variantList, { session });
    }

    // Commit transaction
    await session.commitTransaction();
    session.endSession();

    // Populate trả về product + variants
    const fullProduct = await pModel
      .findById(savedProduct._id)
      .populate("variants");

    dataRes.data = fullProduct;
    dataRes.msg = "Product added successfully";
  } catch (error) {
    // Rollback nếu có lỗi
    await session.abortTransaction();
    session.endSession();

    console.error(error);
    dataRes.msg = error.message || "Unknown error";
  }

  res.json(dataRes);
};

exports.EditProduct = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id } = req.params;
    const { name, description } = req.body;

    if (!_id) {
      throw new Error("Missing product ID");
    }

    // Tìm sản phẩm
    const product = await pModel.findById(_id);
    if (!product) {
      throw new Error("Product not found");
    }

    // Kiểm tra xem sản phẩm có bị xóa mềm không
    if (product.is_deleted) {
      throw new Error("Cannot edit a deleted product");
    }

    // Tạo object update
    let updateData = {};
    if (name) updateData.name = name;
    if (description) updateData.description = description;

    // Upload ảnh nếu có
    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "products");
      updateData.image = fileName;
    }

    if (Object.keys(updateData).length === 0) {
      throw new Error("No data to update");
    }

    // Cập nhật thời gian sửa
    updateData.updatedAt = new Date();

    // Update DB
    const updatedProduct = await pModel.findByIdAndUpdate(_id, updateData, {
      new: true, // trả về bản ghi sau khi update
    });

    dataRes.msg = "Product updated successfully";
    dataRes.data = updatedProduct;
  } catch (error) {
    console.error("EditProduct Error:", error);
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};

exports.DeleteProduct = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id } = req.params;

    if (!_id) {
      throw new Error("Missing product ID");
    }

    // Tìm sản phẩm
    const product = await pModel.findById(_id);
    if (!product) {
      throw new Error("Product not found");
    }

    // Nếu đã bị xóa rồi thì báo luôn
    if (product.is_deleted) {
      dataRes.msg = "This product has already been deleted";
      dataRes.data = product;
      return res.json(dataRes);
    }

    // Đánh dấu xóa mềm
    product.is_deleted = true;
    product.deleted_at = new Date();

    await product.save();

    // Nếu bạn muốn xóa mềm luôn cả variants đi kèm:
    await pVariantModel.updateMany(
      { product_id: _id },
      { $set: { is_deleted: true, deleted_at: new Date() } }
    );

    dataRes.msg = "Product soft-deleted successfully";
    dataRes.data = product;
  } catch (error) {
    console.error("DeleteProduct Error:", error);
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};

exports.UploadImage = async (req, res, next) => {
  let dataRes = { msg: "OK" };
  try {
    const fileName = await uploadSingleFile(req.file, "products");
    dataRes.data = fileName;
  } catch (error) {
    dataRes.msg = error.message;
  }
  res.json(dataRes);
};

exports.GetListProduct = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Lấy danh sách sản phẩm (không bị xóa)
    const products = await pModel.find({ is_deleted: { $ne: true } });

    // Lấy tổng quantity và min/max price từ bảng variants
    const variantStats = await pVariantModel.aggregate([
      { $match: { is_deleted: { $ne: true } } },
      {
        $group: {
          _id: "$product_id",
          totalQty: { $sum: "$quantity" },
          minPrice: { $min: "$price" },
          maxPrice: { $max: "$price" },
        },
      },
    ]);

    // Chuyển mảng aggregate thành object { product_id: {totalQty, minPrice, maxPrice} }
    const statsMap = {};
    variantStats.forEach((v) => {
      statsMap[v._id.toString()] = {
        totalQty: v.totalQty,
        minPrice: v.minPrice,
        maxPrice: v.maxPrice,
      };
    });

    // Gắn quantity + min/max price vào từng sản phẩm
    const result = products.map((p) => {
      const stat = statsMap[p._id.toString()] || {};
      return {
        ...p.toObject(),
        quantity: stat.totalQty || 0,
        min_price: stat.minPrice || 0,
        max_price: stat.maxPrice || 0,
      };
    });

    dataRes.msg = "Product List";
    dataRes.data = result;
  } catch (error) {
    dataRes.data = null;
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};

exports.GetProductByCat = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Kiểm tra nếu không có catID thì trả về lỗi
    if (!req.query.catID || req.query.catID.trim() === "") {
      return res.status(400).json({
        msg: "Missing parameter: catID",
        data: null,
      });
    }

    // Kiểm tra định dạng ObjectId có hợp lệ không
    if (!mongoose.Types.ObjectId.isValid(req.query.catID)) {
      return res.status(400).json({
        msg: "Invalid category ID format",
        data: null,
      });
    }

    // Điều kiện tìm kiếm theo catID
    const dk = {
      catID: new mongoose.Types.ObjectId(req.query.catID),
      is_deleted: { $ne: true },
    };

    // Lấy danh sách sản phẩm theo category
    const products = await pModel.find(dk);

    if (!products || products.length === 0) {
      dataRes.data = [];
      dataRes.msg = "No products found for this category";
      return res.json(dataRes);
    }

    // Gom nhóm theo product_id trong bảng variants để lấy quantity + min/max price
    const variantStats = await pVariantModel.aggregate([
      {
        $match: {
          is_deleted: { $ne: true },
          product_id: { $in: products.map((p) => p._id) },
        },
      },
      {
        $group: {
          _id: "$product_id",
          totalQty: { $sum: "$quantity" }, // tổng tồn kho
          minPrice: { $min: "$price" }, // giá thấp nhất
          maxPrice: { $max: "$price" }, // giá cao nhất
        },
      },
    ]);

    // Tạo map để gắn nhanh dữ liệu vào sản phẩm
    const statMap = {};
    variantStats.forEach((v) => {
      statMap[v._id.toString()] = {
        quantity: v.totalQty,
        min_price: v.minPrice,
        max_price: v.maxPrice,
      };
    });

    // Gắn quantity + min/max price vào sản phẩm
    const result = products.map((p) => ({
      ...p.toObject(),
      quantity: statMap[p._id.toString()]?.quantity || 0,
      min_price: statMap[p._id.toString()]?.min_price || 0,
      max_price: statMap[p._id.toString()]?.max_price || 0,
    }));

    dataRes.data = result;
    dataRes.msg = "Products retrieved successfully";
  } catch (error) {
    console.error("Error fetching products by category:", error);
    dataRes.data = null;
    dataRes.msg = "Server error: " + error.message;
    return res.status(500).json(dataRes);
  }

  res.json(dataRes);
};

exports.GetTopSellingProducts = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Gom nhóm theo product_id để tính tổng quantity, total_sold, min/max price
    const variantStats = await pVariantModel.aggregate([
      { $match: { is_deleted: { $ne: true } } }, // Bỏ qua variant bị xóa mềm
      {
        $group: {
          _id: "$product_id",
          totalQty: { $sum: "$quantity" },
          totalSold: { $sum: "$total_sold" },
          min_price: { $min: "$price" },
          max_price: { $max: "$price" },
        },
      },
      { $sort: { totalSold: -1 } }, // Sắp xếp giảm dần theo sản phẩm bán chạy
      { $limit: 10 }, // Lấy top 10
    ]);

    // Nếu không có dữ liệu
    if (!variantStats || variantStats.length === 0) {
      dataRes.data = [];
      dataRes.msg = "No selling data found";
      return res.json(dataRes);
    }

    // Danh sách product_id
    const productIds = variantStats.map((v) => v._id);

    // Lấy thông tin chi tiết sản phẩm
    const products = await pModel.find({
      _id: { $in: productIds },
      is_deleted: { $ne: true },
    });

    // Map để tra nhanh
    const statMap = {};
    variantStats.forEach((v) => {
      statMap[v._id.toString()] = {
        quantity: v.totalQty,
        total_sold: v.totalSold,
        min_price: v.min_price,
        max_price: v.max_price,
      };
    });

    // Gắn dữ liệu thống kê vào từng sản phẩm
    const result = products.map((p) => ({
      ...p.toObject(),
      quantity: statMap[p._id.toString()]?.quantity || 0,
      total_sold: statMap[p._id.toString()]?.total_sold || 0,
      min_price: statMap[p._id.toString()]?.min_price || 0,
      max_price: statMap[p._id.toString()]?.max_price || 0,
    }));

    dataRes.data = result;
    dataRes.msg = "Top-selling products retrieved successfully";
  } catch (error) {
    console.error("Error fetching top-selling products:", error);
    dataRes.data = null;
    dataRes.msg = "Server error: " + error.message;
  }

  res.json(dataRes);
};

exports.UpdateFavorite = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id, is_favorite } = req.params;

    // Kiểm tra id có tồn tại không
    if (!_id) {
      throw new Error("Thiếu thông tin sản phẩm");
    }

    // Kiểm tra is_favorite có hợp lệ không
    if (is_favorite !== "true" && is_favorite !== "false") {
      throw new Error("Trạng thái favorite không hợp lệ");
    }

    // Chuyển is_favorite từ string sang boolean
    const isFavorite = is_favorite === "true";

    // Kiểm tra sản phẩm
    const product = await pModel.findById(_id);
    if (!product) {
      throw new Error("Sản phẩm không tồn tại");
    }

    // Update favorite
    const updatedProduct = await pModel.findByIdAndUpdate(
      _id,
      {
        $set: {
          is_favorite: isFavorite,
        },
      },
      { new: true }
    );

    dataRes.data = updatedProduct;
    dataRes.msg = isFavorite
      ? "Đã thêm vào yêu thích"
      : "Đã xóa khỏi yêu thích";
  } catch (error) {
    console.error("UpdateFavorite Error:", error);
    dataRes.data = null;
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};

exports.GetFavoriteProducts = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Lấy danh sách sản phẩm yêu thích
    const products = await pModel
      .find({ is_favorite: true, is_deleted: { $ne: true } })
      .exec();

    if (!products || products.length === 0) {
      dataRes.data = [];
      dataRes.msg = "No favorite products found";
      return res.json(dataRes);
    }

    // Lấy tổng quantity + min/max price từ variants
    const variantStats = await pVariantModel.aggregate([
      {
        $match: {
          product_id: { $in: products.map((p) => p._id) },
          is_deleted: { $ne: true },
        },
      },
      {
        $group: {
          _id: "$product_id",
          totalQty: { $sum: "$quantity" },
          minPrice: { $min: "$price" },
          maxPrice: { $max: "$price" },
        },
      },
    ]);

    // Tạo map để tra nhanh theo product_id
    const statMap = {};
    variantStats.forEach((v) => {
      statMap[v._id.toString()] = {
        quantity: v.totalQty,
        min_price: v.minPrice,
        max_price: v.maxPrice,
      };
    });

    // Gắn quantity + min/max price vào từng sản phẩm
    const result = products.map((p) => ({
      ...p.toObject(),
      quantity: statMap[p._id.toString()]?.quantity || 0,
      min_price: statMap[p._id.toString()]?.min_price || 0,
      max_price: statMap[p._id.toString()]?.max_price || 0,
    }));

    dataRes.data = result;
    dataRes.msg = "Favorite products retrieved successfully";
  } catch (error) {
    console.error("Error fetching favorite products:", error);
    dataRes.data = null;
    dataRes.msg = "Server error: " + error.message;
  }

  res.json(dataRes);
};

exports.GetProductDetail = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Kiểm tra tham số bắt buộc
    const { _id } = req.params;
    if (!_id || _id.trim() === "") {
      return res.status(400).json({
        msg: "Missing parameter: _id",
        data: null,
      });
    }

    // Tìm sản phẩm theo id
    const product = await pModel
      .findOne({ _id: _id, is_deleted: { $ne: true } })
      .exec();
    if (!product) {
      return res.status(404).json({
        msg: "Product not found",
        data: null,
      });
    }

    // Lấy danh sách variants còn hoạt động
    const variants = await pVariantModel
      .find({ product_id: _id, is_deleted: { $ne: true } })
      .exec();

    // Nếu có variant thì tính min/max price và tổng số lượng
    let minPrice = product.price || 0;
    let maxPrice = product.price || 0;
    let totalQty = 0;

    if (variants.length > 0) {
      const prices = variants.map((v) => v.price || 0);
      minPrice = Math.min(...prices);
      maxPrice = Math.max(...prices);
      totalQty = variants.reduce((sum, v) => sum + (v.quantity || 0), 0);
    }

    // Gộp dữ liệu trả về
    dataRes.data = {
      ...product.toObject(),
      variants,
      price_min: minPrice,
      price_max: maxPrice,
      total_quantity: totalQty,
    };

    dataRes.msg = "Product detail retrieved successfully";
  } catch (error) {
    console.error("Error fetching product detail:", error);
    dataRes.data = null;
    dataRes.msg = "Server error: " + error.message;
  }

  res.json(dataRes);
};
