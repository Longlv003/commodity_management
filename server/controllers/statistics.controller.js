const { billModel } = require("../models/bill.model");
const { billDetailModel } = require("../models/billDetail.model");
const { pVariantModel } = require("../models/product.variants.model");
const { pModel } = require("../models/product.model");

// API thống kê doanh thu theo khoảng ngày
exports.GetRevenueStatistics = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { start_date, end_date } = req.query;

    // Kiểm tra tham số
    if (!start_date || !end_date) {
      throw new Error("Vui lòng cung cấp start_date và end_date (định dạng: YYYY-MM-DD)");
    }

    // Chuyển đổi string thành Date object
    const startDate = new Date(start_date);
    const endDate = new Date(end_date);
    
    // Đặt thời gian cho endDate là cuối ngày (23:59:59)
    endDate.setHours(23, 59, 59, 999);

    // Kiểm tra tính hợp lệ của ngày
    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
      throw new Error("Định dạng ngày không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD");
    }

    // Kiểm tra start_date phải nhỏ hơn hoặc bằng end_date
    if (startDate > endDate) {
      throw new Error("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc");
    }

    // Tính số ngày trong khoảng để quyết định group theo ngày hay tháng
    const daysDiff = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24));
    
    let groupStage = {};

    // Nếu khoảng thời gian <= 90 ngày, group theo ngày
    // Nếu > 90 ngày và <= 730 ngày (2 năm), group theo tháng
    // Nếu > 730 ngày, group theo năm
    if (daysDiff <= 90) {
      groupStage = {
        _id: {
          year: { $year: "$created_date" },
          month: { $month: "$created_date" },
          day: { $dayOfMonth: "$created_date" }
        },
        date: { $first: { $dateToString: { format: "%d/%m/%Y", date: "$created_date" } } },
        total_revenue: { $sum: "$total_amount" },
        order_count: { $sum: 1 }
      };
    } else if (daysDiff <= 730) {
      groupStage = {
        _id: {
          year: { $year: "$created_date" },
          month: { $month: "$created_date" }
        },
        date: { $first: { $dateToString: { format: "%m/%Y", date: "$created_date" } } },
        total_revenue: { $sum: "$total_amount" },
        order_count: { $sum: 1 }
      };
    } else {
      groupStage = {
        _id: {
          year: { $year: "$created_date" }
        },
        date: { $first: { $dateToString: { format: "%Y", date: "$created_date" } } },
        total_revenue: { $sum: "$total_amount" },
        order_count: { $sum: 1 }
      };
    }

    // Match stage để lọc theo khoảng ngày
    const matchStage = {
      created_date: {
        $gte: startDate,
        $lte: endDate
      }
    };

    // Aggregate để tính toán
    const statistics = await billModel.aggregate([
      { $match: matchStage },
      { $group: groupStage },
      { 
        $sort: { 
          "_id.year": 1, 
          "_id.month": daysDiff <= 90 ? 1 : undefined, 
          "_id.day": daysDiff <= 90 ? 1 : undefined 
        } 
      }
    ]);

    // Tính tổng doanh thu và số đơn hàng
    const totalRevenue = statistics.reduce((sum, item) => sum + item.total_revenue, 0);
    const totalOrders = statistics.reduce((sum, item) => sum + item.order_count, 0);

    dataRes.data = {
      start_date: start_date,
      end_date: end_date,
      statistics: statistics,
      summary: {
        total_revenue: totalRevenue,
        total_orders: totalOrders,
        average_order_value: totalOrders > 0 ? totalRevenue / totalOrders : 0
      }
    };
  } catch (error) {
    console.error("GetRevenueStatistics Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};

// API lấy top sản phẩm bán chạy (đã có sẵn nhưng tạo lại để đảm bảo không sửa code cũ)
exports.GetTopSellingProductsStats = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { limit = 10 } = req.query;

    // Gom nhóm theo product_id để tính tổng quantity, total_sold, min/max price
    const variantStats = await pVariantModel.aggregate([
      { $match: { is_deleted: { $ne: true } } },
      {
        $group: {
          _id: "$product_id",
          totalQty: { $sum: "$quantity" },
          totalSold: { $sum: "$total_sold" },
          min_price: { $min: "$price" },
          max_price: { $max: "$price" },
        },
      },
      { $sort: { totalSold: -1 } },
      { $limit: parseInt(limit) },
    ]);

    if (!variantStats || variantStats.length === 0) {
      dataRes.data = [];
      return res.json(dataRes);
    }

    const productIds = variantStats.map((v) => v._id);
    const products = await pModel.find({
      _id: { $in: productIds },
      is_deleted: { $ne: true },
    });

    const firstVariants = await pVariantModel.find({
      product_id: { $in: productIds },
      is_deleted: { $ne: true },
    }).sort({ _id: 1 });

    const variantMap = {};
    firstVariants.forEach((v) => {
      const pid = v.product_id.toString();
      if (!variantMap[pid]) {
        variantMap[pid] = v;
      }
    });

    const statMap = {};
    variantStats.forEach((v) => {
      statMap[v._id.toString()] = {
        quantity: v.totalQty,
        total_sold: v.totalSold,
        min_price: v.min_price,
        max_price: v.max_price,
      };
    });

    const result = products
      .map((p) => {
        const variant = variantMap[p._id.toString()];
        return {
          _id: p._id,
          name: p.name,
          quantity: statMap[p._id.toString()]?.quantity || 0,
          total_sold: statMap[p._id.toString()]?.total_sold || 0,
          min_price: statMap[p._id.toString()]?.min_price || 0,
          max_price: statMap[p._id.toString()]?.max_price || 0,
          image: variant && variant.image && Array.isArray(variant.image) && variant.image.length > 0 
            ? variant.image[0] 
            : (variant && typeof variant.image === 'string' ? variant.image : null),
        };
      })
      .filter((p) => p.total_sold > 0)
      .sort((a, b) => b.total_sold - a.total_sold);

    dataRes.data = result;
  } catch (error) {
    console.error("GetTopSellingProductsStats Error:", error);
    dataRes.msg = error.message;
    dataRes.data = null;
  }

  res.json(dataRes);
};
