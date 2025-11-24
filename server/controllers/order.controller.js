const { cartModel } = require("../models/cart.model");
const { billModel } = require("../models/bill.model");
const { billDetailModel } = require("../models/billDetail.model");
const { pModel } = require("../models/product.model");
const { pVariantModel } = require("../models/product.variants.model");
const { walletModel } = require("../models/wallet.model");
const { transactionModel } = require("../models/transaction.model");
const { variantSalesModel } = require("../models/variant.sales.model");

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
        throw new Error(
          `Sản phẩm ${product.name} (${variant.size} - ${variant.color}) không đủ số lượng. Chỉ còn ${variant.quantity} sản phẩm`
        );
      }

      // Lấy price từ variant, không phải từ product
      const price = variant.price || 0;
      const itemTotal = price * item.quantity;

      if (isNaN(itemTotal) || itemTotal < 0) {
        throw new Error(
          `Giá sản phẩm không hợp lệ: ${product.name} - ${variant.sku}`
        );
      }

      totalAmount += itemTotal;
    }

    // Đảm bảo totalAmount là số hợp lệ
    if (isNaN(totalAmount) || totalAmount <= 0) {
      throw new Error("Tổng tiền không hợp lệ");
    }

    // Tổng tiền cuối cùng = tiền sản phẩm + phí vận chuyển
    const finalTotalAmount = totalAmount + shippingFee;

    console.log(
      "Subtotal:",
      totalAmount,
      "Shipping:",
      shippingFee,
      "Total:",
      finalTotalAmount
    );

    // XÁC ĐỊNH PHƯƠNG THỨC THANH TOÁN
    let paymentMethod = "cod"; // Mặc định là COD
    if (address && address.includes("|")) {
      const parts = address.split("|");
      if (parts.length > 2) {
        const paymentInfo = parts[2].trim();
        if (paymentInfo.includes("online") || paymentInfo.includes("Online")) {
          paymentMethod = "online";
        }
      }
    }

    // NẾU THANH TOÁN ONLINE, KIỂM TRA VÀ TRỪ TIỀN TỪ VÍ
    if (paymentMethod === "online") {
      // Tìm ví của user
      const wallet = await walletModel.findOne({ id_user: id_user });

      if (!wallet) {
        throw new Error(
          "Bạn chưa có ví. Vui lòng tạo ví trước khi thanh toán online."
        );
      }

      if (!wallet.is_active) {
        throw new Error("Ví của bạn đã bị khóa. Vui lòng liên hệ admin.");
      }

      // Kiểm tra số dư
      if (wallet.balance < finalTotalAmount) {
        throw new Error(
          `Số dư ví không đủ. Số dư hiện tại: ${wallet.balance.toLocaleString(
            "vi-VN"
          )}đ. Tổng tiền: ${finalTotalAmount.toLocaleString("vi-VN")}đ`
        );
      }

      // Trừ tiền từ ví
      wallet.balance -= finalTotalAmount;
      await wallet.save();

      // Tạo transaction record
      const transaction = new transactionModel({
        id_wallet: wallet._id,
        type: "payment", // Loại giao dịch: payment (thanh toán)
        amount: finalTotalAmount,
        description: `Thanh toán đơn hàng - Tổng: ${finalTotalAmount.toLocaleString(
          "vi-VN"
        )}đ`,
        balance_after: wallet.balance,
      });
      await transaction.save();

      console.log(
        `Đã trừ ${finalTotalAmount}đ từ ví. Số dư còn lại: ${wallet.balance}đ`
      );
    }

    // TẠO BILL
    const newBill = new billModel({
      id_user: id_user,
      address: address,
      created_date: new Date(),
      total_amount: finalTotalAmount, // Bao gồm cả phí vận chuyển
      payment_method: paymentMethod, // Lưu phương thức thanh toán
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
        color: variant.color || "",
      });
      await newBillDetails.save();

      // Tạo sales record để lưu lịch sử bán hàng theo thời gian
      const newSalesRecord = new variantSalesModel({
        variant_id: item.id_variant._id,
        product_id: item.id_product._id,
        quantity_sold: item.quantity,
        sale_date: savedBill.created_date || new Date(),
        bill_id: savedBill._id,
        bill_detail_id: newBillDetails._id,
        price: price,
        size: variant.size || "",
        color: variant.color || "",
      });
      await newSalesRecord.save();

      // Update số lượng variant (không phải product)
      // total_sold đã được tách ra model variantSalesModel riêng
      await pVariantModel.updateOne(
        { _id: item.id_variant._id },
        {
          $inc: {
            quantity: -item.quantity,
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
      const subtotal = details.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
      );

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
          image:
            item.id_variant && item.id_variant.image
              ? item.id_variant.image
              : [],
          size: item.size || "", // Size từ bill_detail
          color: item.color || "", // Color từ bill_detail
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

// API cho admin: Lấy tất cả đơn hàng
exports.GetAllOrders = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { userModel } = require("../models/account.model");

    // Lấy tất cả hóa đơn, sắp xếp theo ngày mới nhất
    const bills = await billModel
      .find()
      .sort({ created_date: -1 })
      .populate("id_user");

    if (!bills || bills.length === 0) {
      dataRes.data = [];
      return res.json(dataRes);
    }

    // Lấy chi tiết từng hóa đơn
    const orders = [];
    for (const bill of bills) {
      const details = await billDetailModel
        .find({ id_bill: bill._id })
        .populate("id_product")
        .populate("id_variant");

      // Tính subtotal (tổng tiền sản phẩm không bao gồm phí vận chuyển)
      const subtotal = details.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
      );

      // Parse phí vận chuyển từ address
      let shippingFee = 0;
      let customerName = "";
      let customerPhone = "";
      let deliveryAddress = "";
      let shippingMethod = "";
      let paymentMethod = "";

      if (bill.address && bill.address.includes("|")) {
        const parts = bill.address.split("|");
        if (parts.length > 0) {
          // Parse thông tin khách hàng: "Name - Phone - Address"
          const customerInfo = parts[0].trim();
          const customerParts = customerInfo.split(" - ");
          if (customerParts.length >= 3) {
            customerName = customerParts[0].trim();
            customerPhone = customerParts[1].trim();
            deliveryAddress = customerParts.slice(2).join(" - ").trim();
          } else {
            deliveryAddress = customerInfo;
          }
        }
        if (parts.length > 1) {
          const shippingInfo = parts[1].trim();
          shippingMethod = shippingInfo;
          // Extract số tiền từ chuỗi như "Giao hàng nhanh (25.000đ)" hoặc "Giao hàng hoả tốc (40.000đ)"
          const shippingMatch = shippingInfo.match(/\(([\d.]+)đ\)/);
          if (shippingMatch && shippingMatch[1]) {
            const priceString = shippingMatch[1].replace(/\./g, "");
            shippingFee = parseFloat(priceString) || 0;
          }
        }
        if (parts.length > 2) {
          paymentMethod = parts[2].trim();
        }
      } else {
        deliveryAddress = bill.address || "";
      }

      // Lấy thông tin user
      const user = bill.id_user;
      const userEmail = user ? (user.email || "") : "";
      const userName = user ? (user.name || customerName || "") : customerName;

      orders.push({
        bill_id: bill._id,
        created_date: bill.created_date,
        total_amount: bill.total_amount,
        subtotal: subtotal,
        shipping_fee: shippingFee,
        address: bill.address,
        customer: {
          id: user ? user._id : null,
          name: userName,
          email: userEmail,
          phone: customerPhone,
        },
        delivery: {
          address: deliveryAddress,
          method: shippingMethod,
        },
        payment: paymentMethod,
        products: details.map((item) => ({
          product_id: item.id_product ? item.id_product._id : null,
          name: item.id_product ? item.id_product.name : "Sản phẩm đã bị xóa",
          price: item.price,
          quantity: item.quantity,
          amount: item.price * item.quantity,
          image:
            item.id_variant &&
            item.id_variant.image &&
            Array.isArray(item.id_variant.image) &&
            item.id_variant.image.length > 0
              ? item.id_variant.image[0]
              : item.id_variant && typeof item.id_variant.image === "string"
              ? item.id_variant.image
              : null,
          size: item.size || "",
          color: item.color || "",
        })),
      });
    }

    dataRes.data = orders;
  } catch (error) {
    console.error("GetAllOrders Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};
