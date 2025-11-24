const { billModel } = require("../models/bill.model");
const { billDetailModel } = require("../models/billDetail.model");
const { pVariantModel } = require("../models/product.variants.model");
const { pModel } = require("../models/product.model");
const { variantSalesModel } = require("../models/variant.sales.model");

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
    
    // Đặt thời gian cho startDate là đầu ngày (00:00:00)
    startDate.setHours(0, 0, 0, 0);
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

    // Tạo sort stage động dựa trên daysDiff
    let sortStage = { "_id.year": 1 };
    if (daysDiff <= 90) {
      // Nếu <= 90 ngày, sort theo year, month, day
      sortStage["_id.month"] = 1;
      sortStage["_id.day"] = 1;
    } else if (daysDiff <= 730) {
      // Nếu <= 730 ngày, sort theo year, month
      sortStage["_id.month"] = 1;
    }
    // Nếu > 730 ngày, chỉ sort theo year (đã có ở trên)

    // Aggregate để tính toán
    const statistics = await billModel.aggregate([
      { $match: matchStage },
      { $group: groupStage },
      { $sort: sortStage }
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
    const { limit = 10, start_date, end_date } = req.query;

    // Tạo match stage - nếu có start_date và end_date thì filter theo ngày
    let matchStage = {};
    if (start_date && end_date) {
      const startDate = new Date(start_date);
      const endDate = new Date(end_date);
      endDate.setHours(23, 59, 59, 999);
      
      if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
        throw new Error("Invalid date format. Use YYYY-MM-DD or ISO date string");
      }

      if (startDate > endDate) {
        throw new Error("start_date must be before end_date");
      }

      matchStage = {
        sale_date: {
          $gte: startDate,
          $lte: endDate,
        },
      };
    }

    // Tính total_sold từ variantSalesModel (thay vì từ variant model)
    const aggregatePipeline = [
      ...(Object.keys(matchStage).length > 0 ? [{ $match: matchStage }] : []),
      {
        $group: {
          _id: "$product_id",
          totalSold: { $sum: "$quantity_sold" },
        },
      },
      { $sort: { totalSold: -1 } },
      { $limit: parseInt(limit) },
    ];

    const salesStats = await variantSalesModel.aggregate(aggregatePipeline);

    if (!salesStats || salesStats.length === 0) {
      dataRes.data = [];
      return res.json(dataRes);
    }

    const productIds = salesStats.map((s) => s._id);

    // Gom nhóm theo product_id để tính tổng quantity, min/max price từ variant
    const variantStats = await pVariantModel.aggregate([
      { 
        $match: { 
          product_id: { $in: productIds },
          is_deleted: { $ne: true } 
        } 
      },
      {
        $group: {
          _id: "$product_id",
          totalQty: { $sum: "$quantity" },
          min_price: { $min: "$price" },
          max_price: { $max: "$price" },
        },
      },
    ]);
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

    // Tạo map từ salesStats để lấy total_sold
    const salesMap = {};
    salesStats.forEach((s) => {
      salesMap[s._id.toString()] = s.totalSold;
    });

    // Map để tra nhanh - kết hợp dữ liệu từ variantStats và salesStats
    const statMap = {};
    variantStats.forEach((v) => {
      statMap[v._id.toString()] = {
        quantity: v.totalQty,
        total_sold: salesMap[v._id.toString()] || 0, // Lấy từ salesStats
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
