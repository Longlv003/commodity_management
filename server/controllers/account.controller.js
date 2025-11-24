const { userModel } = require("../models/account.model");
const bcrypt = require("bcrypt");
const { uploadSingleFile } = require("../helpers/upload.helper");

exports.doLogin = async (req, res, next) => {
  // method luôn là post
  try {
    const { email, pass } = req.body;

    if (!email || !pass) {
      return res.status(400).json({ error: "Missing email or password" });
    }

    const user = await userModel.findByEmailPasswd(email, pass);
    if (!user) {
      return res.status(401).json({ error: "Incorrect login credentials" });
    }

    if (!user.is_active) {
      return res
        .status(403)
        .json({ error: "Account is locked. Please contact admin" });
    }

    const token = await userModel.makeAuthToken(user);
    return res.status(200).json({
      message: "Login successful",
      data: { user, token },
    });
  } catch (error) {
    console.log(error.message);
    return res.status(400).send(error);
  }
};

// Login cho web admin (chỉ admin và engineer)
exports.doLoginWeb = async (req, res, next) => {
  try {
    const { email, pass } = req.body;

    if (!email || !pass) {
      return res.status(400).json({ error: "Missing email or password" });
    }

    const user = await userModel.findByEmailPasswd(email, pass);
    if (!user) {
      return res.status(401).json({ error: "Incorrect login credentials" });
    }

    if (!user.is_active) {
      return res
        .status(403)
        .json({ error: "Account is locked. Please contact admin" });
    }

    // Chỉ cho phép admin và engineer đăng nhập vào web admin
    if (user.role !== "admin" && user.role !== "engineer") {
      return res
        .status(403)
        .json({ error: "Bạn không có quyền đăng nhập vào hệ thống quản trị" });
    }

    const token = await userModel.makeAuthToken(user);
    return res.status(200).json({
      message: "Login successful",
      data: { user, token },
    });
  } catch (error) {
    console.log(error.message);
    return res.status(400).send(error);
  }
};

exports.doReg = async (req, res, next) => {
  try {
    const salt = await bcrypt.genSalt(10);

    const existed = await userModel.findOne({ email: req.body.email });
    if (existed) {
      return res.status(400).json({ error: "Email already exists" });
    }

    // Tạo user từ req.body nhưng loại bỏ các field không được phép set khi đăng ký
    const userData = { ...req.body };
    // Loại bỏ is_active và role - không cho user tự set khi đăng ký
    delete userData.is_active;
    delete userData.role;
    
    const user = new userModel(userData);
    
    // Force set is_active = true cho user mới đăng ký
    user.is_active = true;
    // Đảm bảo role mặc định là "user"
    if (!user.role) {
      user.role = "user";
    }

    user.pass = await bcrypt.hash(req.body.pass, salt);

    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "avatars");
      user.image = fileName; // lưu tên file vào DB
    }

    const token = await userModel.makeAuthToken(user);

    let newUser = await user.save();

    return res.status(200).json({
      message: "Register successfully",
      data: { newUser, token },
    });
  } catch (error) {
    console.log(error.message);
    return res.status(400).send(error.message);
  }
};

exports.UploadAvatar = async (req, res, next) => {
  let dataRes = { msg: "OK" };
  try {
    const { _id } = req.params;

    if (!req.file) throw new Error("Không có file tải lên");

    const user = await userModel.findById(_id);
    if (!user) throw new Error("Không tìm thấy người dùng");

    const fileName = await uploadSingleFile(req.file, "avatars");
    user.image = fileName;
    await user.save();

    dataRes.msg = "Cập nhật ảnh đại diện thành công";
    dataRes.data = user;
  } catch (error) {
    console.error("UploadAvatar Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }
  res.json(dataRes);
};

exports.updateUserStatus = async (req, res) => {
  try {
    const { _id } = req.params;
    const { role, is_active } = req.body;

    const user = await userModel.findById(_id);
    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }

    // Không cho admin tự khóa mình
    if (req.user._id.equals("admin")) {
      return res
        .status(400)
        .json({ error: "admin cannot modify your own status or role" });
    }

    // Chỉ cập nhật nếu có dữ liệu
    if (role) user.role = role;
    if (typeof is_active === "boolean") user.is_active = is_active;

    await user.save();

    return res.status(200).json({
      message: "User updated successfully",
      data: user,
    });
  } catch (error) {
    console.log(error.message);
    return res.status(400).send(error);
  }
};

exports.UpdateUser = async (req, res, next) => {
  let dataRes = { msg: "OK" };

  try {
    const { _id } = req.params;
    const { email, phone, name, address } = req.body; // đọc text fields từ multipart

    const user = await userModel.findById(_id);
    if (!user) throw new Error("Người dùng không tồn tại");

    let updateData = {};

    if (email && email !== user.email) {
      const existingUser = await userModel.findOne({ email });
      if (existingUser) throw new Error("Email đã tồn tại");
      updateData.email = email;
    }

    if (phone) updateData.phone = phone;

    // ✅ Thêm name và address
    if (name) updateData.name = name;
    if (address) updateData.address = address;

    // ✅ Thêm name và address
    if (name) updateData.name = name;
    if (address) updateData.address = address;

    // ✅ Nếu có file upload
    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "avatars");
      updateData.image = fileName;
    }

    if (Object.keys(updateData).length === 0) {
      throw new Error("Không có dữ liệu để cập nhật");
    }

    const updatedUser = await userModel
      .findByIdAndUpdate(_id, updateData, { new: true })
      .select("-pass -token");

    if (!updatedUser) throw new Error("Cập nhật thất bại");

    dataRes.msg = "Cập nhật thông tin thành công";
    dataRes.data = updatedUser;
  } catch (error) {
    console.error("UpdateUser Error:", error);
    dataRes.data = null;
    dataRes.msg = error.message;
  }

  res.json(dataRes);
};