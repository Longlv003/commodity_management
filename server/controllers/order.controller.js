const { cartModel } = require("../models/cart.model");
const { billModel } = require("../models/bill.model");
const { billDetailModel } = require("../models/billDetail.model");
const { pModel } = require("../models/product.model");
const { pVariantModel } = require("../models/product.variants.model");

exports.PlaceOrder = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { id_user, address } = req.body;
    console.log("Request body:", req.body);

    if (!id_user || !address) {
      throw new Error("Thieu thong tin nguoi dung hoac dia chi");
    }

    // KIỂM TRA ID_USER CÓ ĐÚNG ĐỊNH DẠNG KHÔNG?
    if (typeof id_user !== "string" || id_user.length < 1) {
      throw new Error("ID user không hợp lệ");
    }

    const cartItem = await cartModel
      .find({ id_user: id_user })
      .populate("id_product")
      .populate("id_variant");
    console.log("Cart items found:", cartItem);

    if (cartItem.length === 0) {
      throw new Error("Gio hang trong");
    }

    let totalAmount = 0;
    let shippingFee = 0; // Phí vận chuyển

    // Parse phí vận chuyển từ address nếu có
    // Format: "Name - Phone - Address | Giao hàng nhanh (25.000đ) | Payment"
    if (address && address.includes("|")) {
      const parts = address.split("|");
      if (parts.length > 1) {
        const shippingInfo = parts[1].trim();
        // Extract số tiền từ chuỗi như "Giao hàng nhanh (25.000đ)" hoặc "Giao hàng hoả tốc (40.000đ)"
        // Tìm số có thể có dấu chấm ngăn cách hàng nghìn
        const shippingMatch = shippingInfo.match(/\(([\d.]+)đ\)/);
        if (shippingMatch && shippingMatch[1]) {
          // Xóa tất cả dấu chấm và chuyển thành số
          const priceString = shippingMatch[1].replace(/\./g, "");
          shippingFee = parseFloat(priceString) || 0;
          console.log("Shipping info:", shippingInfo);
          console.log("Shipping fee parsed:", shippingFee);
        }
      }
    }

    // KIỂM TRA TỒN KHO VÀ TÍNH TỔNG TIỀN
    for (const item of cartItem) {
      if (!item.id_product) {
        throw new Error("Sản phẩm trong giỏ hàng không tồn tại");
      }

      if (!item.id_variant) {
        throw new Error("Variant trong giỏ hàng không tồn tại");
      }

      const product = await pModel.findById(item.id_product._id);
      if (!product) {
        throw new Error(`Sản phẩm không tồn tại: ${item.id_product._id}`);
      }

      // Kiểm tra tồn kho từ variant, không phải từ product
      const variant = await pVariantModel.findById(item.id_variant._id);
      if (!variant) {
        throw new Error(`Variant không tồn tại: ${item.id_variant._id}`);
      }

      if (variant.is_deleted) {
        throw new Error(`Variant ${variant.sku} đã bị xóa`);
      }

      if (variant.quantity < item.quantity) {
        throw new Error(`Sản phẩm ${product.name} (${variant.size} - ${variant.color}) không đủ số lượng. Chỉ còn ${variant.quantity} sản phẩm`);
      }

      // Lấy price từ variant, không phải từ product
      const price = variant.price || 0;
      const itemTotal = price * item.quantity;
      
      if (isNaN(itemTotal) || itemTotal < 0) {
        throw new Error(`Giá sản phẩm không hợp lệ: ${product.name} - ${variant.sku}`);
      }

      totalAmount += itemTotal;
    }

    // Đảm bảo totalAmount là số hợp lệ
    if (isNaN(totalAmount) || totalAmount <= 0) {
      throw new Error("Tổng tiền không hợp lệ");
    }

    // Tổng tiền cuối cùng = tiền sản phẩm + phí vận chuyển
    const finalTotalAmount = totalAmount + shippingFee;
    
    console.log("Subtotal:", totalAmount, "Shipping:", shippingFee, "Total:", finalTotalAmount);

    // TẠO BILL
    const newBill = new billModel({
      id_user: id_user,
      address: address,
      created_date: new Date(),
      total_amount: finalTotalAmount, // Bao gồm cả phí vận chuyển
    });

    const savedBill = await newBill.save();
    console.log("Bill saved:", savedBill._id);

    // TẠO BILL DETAILS VÀ UPDATE SỐ LƯỢNG
    for (const item of cartItem) {
      // Lấy variant để lấy price
      const variant = await pVariantModel.findById(item.id_variant._id);
      if (!variant) {
        throw new Error(`Variant không tồn tại: ${item.id_variant._id}`);
      }

      const price = variant.price || 0;

      // Tạo bill detail - lưu id_variant, price, size, color từ variant
      const newBillDetails = new billDetailModel({
        id_bill: savedBill._id,
        id_product: item.id_product._id,
        id_variant: item.id_variant._id,
        price: price,
        quantity: item.quantity,
        size: variant.size || "",
        color: variant.color || ""
      });
      await newBillDetails.save();

      // Update số lượng variant (không phải product)
      await pVariantModel.updateOne(
        { _id: item.id_variant._id },
        {
          $inc: {
            quantity: -item.quantity,
            total_sold: item.quantity,
          },
        }
      );
    }

    // XÓA GIỎ HÀNG
    await cartModel.deleteMany({ id_user: id_user });

    dataRes.data = {
      bill: savedBill,
      totalAmount: totalAmount,
      items: cartItem.length,
    };
    dataRes.msg = "Đặt hàng thành công!";
  } catch (error) {
    console.error("PlaceOrder Error:", error);
    dataRes.data = null;
    dataRes.msg = error.message;

    // Trả về status code phù hợp
    res.status(400).json(dataRes);
    return;
  }

  res.json(dataRes);
};

exports.GetOrderHistory = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { id_user } = req.params; // Lấy id_user từ URL, ví dụ: /orders/:id_user

    if (!id_user) {
      throw new Error("Thiếu thông tin id_user");
    }

    // Lấy tất cả hóa đơn của user
    const bills = await billModel.find({ id_user }).sort({ created_date: -1 });

    if (!bills || bills.length === 0) {
      dataRes.data = [];
      return res.json(dataRes); // Trả về mảng rỗng nếu chưa có đơn hàng
    }

    // Lấy chi tiết từng hóa đơn
    const history = [];
    for (const bill of bills) {
      const details = await billDetailModel
        .find({ id_bill: bill._id })
        .populate("id_product")
        .populate("id_variant");

      // Tính subtotal (tổng tiền sản phẩm không bao gồm phí vận chuyển)
      const subtotal = details.reduce((sum, item) => sum + (item.price * item.quantity), 0);
      
      // Parse phí vận chuyển từ address
      let shippingFee = 0;
      if (bill.address && bill.address.includes("|")) {
        const parts = bill.address.split("|");
        if (parts.length > 1) {
          const shippingInfo = parts[1].trim();
          // Extract số tiền từ chuỗi như "Giao hàng nhanh (25.000đ)" hoặc "Giao hàng hoả tốc (40.000đ)"
          const shippingMatch = shippingInfo.match(/\(([\d.]+)đ\)/);
          if (shippingMatch && shippingMatch[1]) {
            // Xóa tất cả dấu chấm và chuyển thành số
            const priceString = shippingMatch[1].replace(/\./g, "");
            shippingFee = parseFloat(priceString) || 0;
          }
        }
      }

      history.push({
        bill_id: bill._id,
        created_date: bill.created_date,
        total_amount: bill.total_amount,
        subtotal: subtotal, // Tổng tiền sản phẩm
        shipping_fee: shippingFee, // Phí vận chuyển
        address: bill.address,
        products: details.map((item) => ({
          product_id: item.id_product._id,
          name: item.id_product.name,
          price: item.price, // Giá từ bill_detail
          quantity: item.quantity, // Số lượng từ bill_detail
          amount: item.price * item.quantity, // Tổng tiền = price * quantity
          image: item.id_variant && item.id_variant.image ? item.id_variant.image : [],
          size: item.size || "", // Size từ bill_detail
          color: item.color || "" // Color từ bill_detail
        })),
      });
    }

    dataRes.data = history;
  } catch (error) {
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};
