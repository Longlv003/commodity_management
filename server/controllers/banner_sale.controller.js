const { bannerModel } = require("../models/banner_sale.model");
const { userModel } = require("../models/account.model");
const { uploadSingleFile } = require("../helpers/upload.helper");

exports.AddBanner = async (req, res, next) => {
  let dataRes = { msg: "OK" };
  try {
    console.log("Request body:", req.body);
    console.log("Request file:", req.file);

    const { name } = req.body;

    if (!name) return res.status(400).json({ error: "Chưa có tên banner" });
    if (!req.file) return res.status(400).json({ error: "Chưa có ảnh banner" });

    const existingBanner = await bannerModel.findOne({
      name: name.trim(),
    });

    if (existingBanner) {
      return res.status(400).json({
        error: "Tên banner đã tồn tại. Vui lòng chọn tên khác.",
      });
    }

    // Upload file
    const fileName = await uploadSingleFile(req.file, "banners");

    // Tạo banner mới
    const banner = new bannerModel({
      name: name,
      image: fileName,
    });

    await banner.save();

    dataRes.data = banner;
    dataRes.msg = "Banner_sale added successfully";
  } catch (error) {
    console.error(error);
    dataRes.msg = error.message;
    return res.status(500).json(dataRes);
  }

  res.json(dataRes);
};

exports.GetAllBanner = async (req, res, next) => {
  let dataRes = { msg: "OK" };
  try {
    let list = await bannerModel.find();
    dataRes.data = list;
  } catch (error) {
    dataRes.data = null;
    dataRes.msg = error.message;
  }
  res.json(dataRes);

};

// API lấy danh sách banner cho admin (có filter is_delete)
exports.GetAllBannersAdmin = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };
  try {
    const { search, is_delete } = req.query;
    
    let query = {};
    if (search) {
      query.name = { $regex: search, $options: "i" };
    }
    if (is_delete !== undefined) {
      query.is_delete = is_delete === "true";
    } else {
      // Mặc định chỉ lấy banner chưa bị xóa
      query.is_delete = false;
    }

    const banners = await bannerModel.find(query).sort({ created_date: -1 });
    dataRes.data = banners;
  } catch (error) {
    console.error("GetAllBannersAdmin Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }
  res.json(dataRes);
};

// API cập nhật banner
exports.UpdateBanner = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };
  try {
    const { _id } = req.params;
    const { name } = req.body;

    if (!_id) {
      throw new Error("Missing banner ID");
    }

    const banner = await bannerModel.findById(_id);
    if (!banner) {
      throw new Error("Banner not found");
    }

    let updateData = {};
    if (name && name.trim() !== "") {
      // Kiểm tra tên trùng (trừ chính banner đang sửa)
      const existingBanner = await bannerModel.findOne({
        name: name.trim(),
        _id: { $ne: _id }
      });
      if (existingBanner) {
        throw new Error("Tên banner đã tồn tại. Vui lòng chọn tên khác.");
      }
      updateData.name = name.trim();
    }

    // Xử lý upload ảnh mới nếu có
    if (req.file) {
      const fileName = await uploadSingleFile(req.file, "banners");
      updateData.image = fileName;
    }

    if (Object.keys(updateData).length === 0) {
      throw new Error("Không có dữ liệu để cập nhật");
    }

    updateData.updated_date = new Date();

    const updatedBanner = await bannerModel.findByIdAndUpdate(_id, updateData, { new: true });

    dataRes.data = updatedBanner;
    dataRes.msg = "Cập nhật banner thành công";
  } catch (error) {
    console.error("UpdateBanner Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }
  res.json(dataRes);
};

// API xóa banner (soft delete)
exports.DeleteBanner = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };
  try {
    const { _id } = req.params;

    if (!_id) {
      throw new Error("Missing banner ID");
    }

    const banner = await bannerModel.findById(_id);
    if (!banner) {
      throw new Error("Banner not found");
    }

    // Soft delete
    banner.is_delete = true;
    banner.delete_date = new Date();
    await banner.save();

    dataRes.data = { _id };
    dataRes.msg = "Đã xóa banner thành công";
  } catch (error) {
    console.error("DeleteBanner Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }
  res.json(dataRes);
};