const { userFavoriteModel } = require("../models/userFavorite.model");
const { pModel } = require("../models/product.model");
const { pVariantModel } = require("../models/product.variants.model");

// Thêm sản phẩm vào yêu thích
exports.AddFavorite = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Lấy user_id từ query
    const user_id = req.query.user_id;
    if (!user_id) {
      throw new Error("Missing required field: user_id (query parameter)");
    }
    const { product_id } = req.body;

    // Validate input
    if (!product_id) {
      throw new Error("Missing required field: product_id");
    }

    // Kiểm tra product có tồn tại không
    const product = await pModel.findById(product_id);
    if (!product || product.is_deleted) {
      throw new Error("Product not found or has been deleted");
    }

    // Kiểm tra đã favorite chưa
    const existingFavorite = await userFavoriteModel.findOne({
      user_id,
      product_id,
    });

    if (existingFavorite) {
      dataRes.msg = "Product already in favorites";
      dataRes.data = existingFavorite;
      return res.json(dataRes);
    }

    // Tạo favorite mới
    const favorite = await userFavoriteModel.create({
      user_id,
      product_id,
    });

    dataRes.msg = "Added to favorites successfully";
    dataRes.data = favorite;
  } catch (error) {
    console.error("AddFavorite Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// Xóa sản phẩm khỏi yêu thích
exports.RemoveFavorite = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Lấy user_id từ query
    const user_id = req.query.user_id;
    if (!user_id) {
      throw new Error("Missing required field: user_id (query parameter)");
    }
    const { product_id } = req.params;

    // Validate input
    if (!product_id) {
      throw new Error("Missing required field: product_id");
    }

    // Tìm và xóa favorite
    const favorite = await userFavoriteModel.findOneAndDelete({
      user_id,
      product_id,
    });

    if (!favorite) {
      throw new Error("Favorite not found");
    }

    dataRes.msg = "Removed from favorites successfully";
    dataRes.data = { deleted: true, product_id };
  } catch (error) {
    console.error("RemoveFavorite Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// Toggle favorite (thêm nếu chưa có, xóa nếu đã có)
exports.ToggleFavorite = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Lấy user_id từ query
    const user_id = req.query.user_id;
    if (!user_id) {
      throw new Error("Missing required field: user_id (query parameter)");
    }
    const { product_id } = req.body;

    // Validate input
    if (!product_id) {
      throw new Error("Missing required field: product_id");
    }

    // Kiểm tra product có tồn tại không
    const product = await pModel.findById(product_id);
    if (!product || product.is_deleted) {
      throw new Error("Product not found or has been deleted");
    }

    // Kiểm tra đã favorite chưa
    const existingFavorite = await userFavoriteModel.findOne({
      user_id,
      product_id,
    });

    if (existingFavorite) {
      // Nếu đã có thì xóa
      await userFavoriteModel.findOneAndDelete({
        user_id,
        product_id,
      });
      dataRes.msg = "Removed from favorites";
      dataRes.data = { is_favorite: false, product_id };
    } else {
      // Nếu chưa có thì thêm
      const favorite = await userFavoriteModel.create({
        user_id,
        product_id,
      });
      dataRes.msg = "Added to favorites";
      dataRes.data = { is_favorite: true, favorite };
    }
  } catch (error) {
    console.error("ToggleFavorite Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// Kiểm tra xem product có được favorite bởi user không
exports.CheckFavorite = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Lấy user_id từ query
    const user_id = req.query.user_id;
    if (!user_id) {
      throw new Error("Missing required field: user_id (query parameter)");
    }
    const { product_id } = req.params;

    // Validate input
    if (!product_id) {
      throw new Error("Missing required field: product_id");
    }

    // Kiểm tra favorite
    const favorite = await userFavoriteModel.findOne({
      user_id,
      product_id,
    });

    dataRes.data = {
      is_favorite: !!favorite,
      product_id,
    };
  } catch (error) {
    console.error("CheckFavorite Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// Lấy danh sách sản phẩm yêu thích của user
exports.GetUserFavorites = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    // Lấy user_id từ query
    const user_id = req.query.user_id;
    if (!user_id) {
      throw new Error("Missing required field: user_id (query parameter)");
    }

    // Lấy danh sách favorite của user
    const favorites = await userFavoriteModel
      .find({ user_id })
      .populate({
        path: "product_id",
        match: { is_deleted: { $ne: true } },
      })
      .sort({ created_date: -1 });

    // Filter bỏ các product đã bị xóa
    const validFavorites = favorites.filter((f) => f.product_id);

    if (!validFavorites || validFavorites.length === 0) {
      dataRes.data = [];
      dataRes.msg = "No favorite products found";
      return res.json(dataRes);
    }

    // Lấy product IDs
    const productIds = validFavorites.map((f) => f.product_id._id);

    // Lấy tổng quantity + min/max price từ variants
    const variantStats = await pVariantModel.aggregate([
      {
        $match: {
          product_id: { $in: productIds },
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

    // Lấy variant đầu tiên của mỗi product để lấy image
    const firstVariants = await pVariantModel.find({
      product_id: { $in: productIds },
      is_deleted: { $ne: true },
    }).sort({ _id: 1 });

    // Tạo map để lấy variant đầu tiên của mỗi product
    const variantMap = {};
    firstVariants.forEach((v) => {
      const pid = v.product_id.toString();
      if (!variantMap[pid]) {
        variantMap[pid] = v;
      }
    });

    // Tạo map để tra nhanh theo product_id
    const statMap = {};
    variantStats.forEach((v) => {
      statMap[v._id.toString()] = {
        quantity: v.totalQty,
        min_price: v.minPrice,
        max_price: v.maxPrice,
      };
    });

    // Gắn quantity + min/max price + image + is_favorite vào từng sản phẩm
    // Filter bỏ các sản phẩm hết hàng (quantity = 0)
    const result = validFavorites
      .map((f) => {
        const product = f.product_id;
        const variant = variantMap[product._id.toString()];
        return {
          ...product.toObject(),
          quantity: statMap[product._id.toString()]?.quantity || 0,
          min_price: statMap[product._id.toString()]?.min_price || 0,
          max_price: statMap[product._id.toString()]?.max_price || 0,
          image: variant && variant.image ? variant.image : [],
          is_favorite: true, // Tất cả sản phẩm trong danh sách favorite đều là favorite
        };
      })
      .filter((p) => p.quantity > 0); // Chỉ lấy sản phẩm còn hàng

    dataRes.data = result;
    dataRes.msg = "Favorite products retrieved successfully";
  } catch (error) {
    console.error("GetUserFavorites Error:", error);
    dataRes.data = null;
    dataRes.msg = "Server error: " + error.message;
  }

  res.json(dataRes);
};