const { userModel } = require("../models/account.model");
const { billModel } = require("../models/bill.model");
const { billDetailModel } = require("../models/billDetail.model");
const bcrypt = require("bcrypt");
const { uploadSingleFile } = require("../helpers/upload.helper");

// API lấy danh sách khách hàng với thống kê
exports.GetAllCustomers = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { search } = req.query;
    
    let query = {};
    if (search) {
      query = {
        $or: [
          { name: { $regex: search, $options: "i" } },
          { email: { $regex: search, $options: "i" } },
          { phone: { $regex: search, $options: "i" } },
        ],
      };
    }

    const customers = await userModel.find(query).select("-pass -token").sort({ created_at: -1 });

    // Lấy thống kê cho mỗi khách hàng
    const customersWithStats = await Promise.all(
      customers.map(async (customer) => {
        const bills = await billModel.find({ id_user: customer._id });
        const totalOrders = bills.length;
        const totalSpent = bills.reduce((sum, bill) => sum + (bill.total_amount || 0), 0);
        const lastOrderDate = bills.length > 0 
          ? bills.sort((a, b) => new Date(b.created_date) - new Date(a.created_date))[0].created_date 
          : null;

        return {
          ...customer.toObject(),
          stats: {
            total_orders: totalOrders,
            total_spent: totalSpent,
            last_order_date: lastOrderDate,
          },
        };
      })
    );

    dataRes.data = customersWithStats;
  } catch (error) {
    console.error("GetAllCustomers Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// API lấy chi tiết khách hàng với hành vi
exports.GetCustomerDetail = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { id } = req.params;

    const customer = await userModel.findById(id).select("-pass -token");
    if (!customer) {
      throw new Error("Không tìm thấy khách hàng");
    }

    // Lấy tất cả đơn hàng
    const bills = await billModel.find({ id_user: id }).sort({ created_date: -1 });

    // Tính toán thống kê
    const totalOrders = bills.length;
    const totalSpent = bills.reduce((sum, bill) => sum + (bill.total_amount || 0), 0);
    const averageOrderValue = totalOrders > 0 ? totalSpent / totalOrders : 0;
    const firstOrderDate = bills.length > 0 
      ? bills.sort((a, b) => new Date(a.created_date) - new Date(b.created_date))[0].created_date 
      : null;
    const lastOrderDate = bills.length > 0 
      ? bills.sort((a, b) => new Date(b.created_date) - new Date(a.created_date))[0].created_date 
      : null;

    // Lấy sản phẩm đã mua nhiều nhất
    const billDetails = await billDetailModel
      .find({ id_bill: { $in: bills.map(b => b._id) } })
      .populate("id_product")
      .populate("id_variant");

    const productCounts = {};
    billDetails.forEach((detail) => {
      if (detail.id_product) {
        const productId = detail.id_product._id.toString();
        if (!productCounts[productId]) {
          productCounts[productId] = {
            product: detail.id_product,
            quantity: 0,
            total_spent: 0,
          };
        }
        productCounts[productId].quantity += detail.quantity;
        productCounts[productId].total_spent += detail.price * detail.quantity;
      }
    });

    const topProducts = Object.values(productCounts)
      .sort((a, b) => b.quantity - a.quantity)
      .slice(0, 5);

    dataRes.data = {
      customer: customer,
      behavior: {
        total_orders: totalOrders,
        total_spent: totalSpent,
        average_order_value: averageOrderValue,
        first_order_date: firstOrderDate,
        last_order_date: lastOrderDate,
        top_products: topProducts,
      },
    };
  } catch (error) {
    console.error("GetCustomerDetail Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// API thêm khách hàng mới
exports.AddCustomer = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { name, email, phone, address, pass, role = "user" } = req.body;

    if (!email || !pass) {
      throw new Error("Email và mật khẩu là bắt buộc");
    }

    // Kiểm tra email đã tồn tại
    const existed = await userModel.findOne({ email });
    if (existed) {
      throw new Error("Email đã tồn tại");
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPass = await bcrypt.hash(pass, salt);

    const newCustomer = new userModel({
      name,
      email,
      phone,
      address,
      pass: hashedPass,
      role,
      is_active: true,
    });

    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "avatars");
      newCustomer.image = fileName;
    }

    await newCustomer.save();

    const customerData = newCustomer.toObject();
    delete customerData.pass;
    delete customerData.token;

    dataRes.data = customerData;
    dataRes.msg = "Thêm khách hàng thành công";
  } catch (error) {
    console.error("AddCustomer Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// API cập nhật khách hàng
exports.UpdateCustomer = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { id } = req.params;
    const { name, email, phone, address, role, is_active, pass } = req.body;

    const customer = await userModel.findById(id);
    if (!customer) {
      throw new Error("Không tìm thấy khách hàng");
    }

    let updateData = {};

    if (name) updateData.name = name;
    if (phone) updateData.phone = phone;
    if (address) updateData.address = address;
    if (role) updateData.role = role;
    if (typeof is_active === "boolean") updateData.is_active = is_active;

    if (email && email !== customer.email) {
      const existingUser = await userModel.findOne({ email });
      if (existingUser) {
        throw new Error("Email đã tồn tại");
      }
      updateData.email = email;
    }

    if (pass) {
      const salt = await bcrypt.genSalt(10);
      updateData.pass = await bcrypt.hash(pass, salt);
    }

    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "avatars");
      updateData.image = fileName;
    }

    if (Object.keys(updateData).length === 0) {
      throw new Error("Không có dữ liệu để cập nhật");
    }

    const updatedCustomer = await userModel
      .findByIdAndUpdate(id, updateData, { new: true })
      .select("-pass -token");

    dataRes.data = updatedCustomer;
    dataRes.msg = "Cập nhật khách hàng thành công";
  } catch (error) {
    console.error("UpdateCustomer Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// API xóa khách hàng
exports.DeleteCustomer = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { id } = req.params;

    const customer = await userModel.findById(id);
    if (!customer) {
      throw new Error("Không tìm thấy khách hàng");
    }

    // Kiểm tra nếu là admin thì không cho xóa
    if (customer.role === "admin") {
      throw new Error("Không thể xóa tài khoản admin");
    }

    // Kiểm tra xem có đơn hàng không
    const bills = await billModel.find({ id_user: id });
    if (bills.length > 0) {
      // Nếu có đơn hàng, chỉ vô hiệu hóa tài khoản
      customer.is_active = false;
      await customer.save();
      dataRes.msg = "Đã vô hiệu hóa tài khoản khách hàng (có đơn hàng liên quan)";
    } else {
      // Nếu không có đơn hàng, xóa hoàn toàn
      await userModel.findByIdAndDelete(id);
      dataRes.msg = "Đã xóa khách hàng thành công";
    }

    dataRes.data = { id };
  } catch (error) {
    console.error("DeleteCustomer Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

