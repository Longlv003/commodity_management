const { cartModel } = require("../models/cart.model");
const { pModel } = require("../models/product.model");
const { pVariantModel } = require("../models/product.variants.model");

exports.addToCart = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { id_user, id_product, id_variant, quantity } = req.body;
    const qtyToAdd = quantity || 1;

    // Validate input
    if (!id_user || !id_product || !id_variant) {
      throw new Error("Missing required fields");
    }

    // Kiểm tra product có tồn tại không
    const product = await pModel.findById(id_product);
    if (!product) {
      throw new Error("Product not found");
    }

    // Kiểm tra variant
    const variant = await pVariantModel.findById(id_variant);
    if (!variant) {
      throw new Error("Variant not found");
    }

    // Kiểm tra variant có thuộc product không
    if (variant.product_id.toString() !== id_product) {
      throw new Error("Variant does not belong to product");
    }

    // Kiểm tra tồn kho khi add
    if (variant.quantity < qtyToAdd) {
      throw new Error("Insufficient stock");
    }

    // Kiểm tra cart đã có variant này chưa
    let cartItem = await cartModel.findOne({
      id_user,
      id_product,
      id_variant,
    });

    if (cartItem) {
      const newTotalQty = cartItem.quantity + qtyToAdd;

      if (variant.quantity < newTotalQty) {
        throw new Error("Insufficient stock");
      }

      cartItem.quantity = newTotalQty;
      await cartItem.save();
    } else {
      cartItem = await cartModel.create({
        id_user,
        id_product,
        id_variant,
        quantity: qtyToAdd,
      });
    }

    dataRes.data = cartItem;
  } catch (error) {
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

exports.getCartList = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { id_user } = req.params;

    if (!id_user) {
      throw new Error("User ID is required");
    }

    const cartItems = await cartModel
      .find({ id_user: id_user })
      .populate({
        path: "id_product",
        select: "name description productCode",
        match: { is_deleted: false },
      })
      .populate({
        path: "id_variant",
        select: "sku size color price quantity image",
        match: { is_deleted: false },
      })
      .sort({ added_date: -1 });

    const validCartItems = cartItems.filter(
      (item) => item.id_product && item.id_variant
    );

    dataRes.data = validCartItems;
  } catch (error) {
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

exports.updateCartQuantity = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id } = req.params;
    const { quantity } = req.query;

    if (!_id || !quantity) {
      throw new Error("_id and quantity are required");
    }

    const quantityNum = parseInt(quantity);
    if (isNaN(quantityNum) || quantityNum < 0) {
      throw new Error("Quantity must be a number greater than or equal to 0");
    }

    // Tìm cart item và populate cả product lẫn variant
    const cartItem = await cartModel
      .findById(_id)
      .populate({
        path: "id_product",
        select: "name description productCode",
        match: { is_deleted: false },
      })
      .populate({
        path: "id_variant",
        select: "sku size color price quantity image",
        match: { is_deleted: false },
      });

    if (!cartItem) {
      throw new Error("Cart item not found");
    }

    // Kiểm tra product và variant có tồn tại không
    if (!cartItem.id_product || !cartItem.id_variant) {
      throw new Error("Product or variant not found");
    }

    // Nếu quantity = 0 thì xóa item khỏi cart
    if (quantityNum === 0) {
      await cartModel.findByIdAndDelete(_id);
      dataRes.data = { deleted: true, _id };
    } else {
      // Kiểm tra tồn kho
      if (cartItem.id_variant.quantity < quantityNum) {
        throw new Error("Insufficient stock");
      }

      // Cập nhật quantity
      cartItem.quantity = quantityNum;
      await cartItem.save();

      dataRes.data = cartItem;
    }
  } catch (error) {
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

exports.deleteCartItem = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id } = req.params;

    if (!_id) {
      throw new Error("ID is required");
    }

    const result = await cartModel.findByIdAndDelete(_id);

    if (!result) {
      throw new Error("Cart item not found");
    }

    dataRes.data = { deleted: true };
  } catch (error) {
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};