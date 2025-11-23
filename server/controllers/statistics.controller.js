const { billModel } = require("../models/bill.model");
const { billDetailModel } = require("../models/billDetail.model");
const { pVariantModel } = require("../models/product.variants.model");
const { pModel } = require("../models/product.model");

// API thống kê doanh thu theo ngày/tháng/năm
exports.GetRevenueStatistics = async (req, res, next) => {
  let dataRes = { msg: "OK", data: null };

  try {
    const { period } = req.query; // 'day', 'month', 'year'

    if (!period || !['day', 'month', 'year'].includes(period)) {
      throw new Error("Period phải là 'day', 'month' hoặc 'year'");
    }

    let matchStage = {};
    let groupStage = {};

    // Xác định match và group theo period
    if (period === 'day') {
      // Thống kê theo ngày (30 ngày gần nhất)
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
      matchStage = {
        created_date: { $gte: thirtyDaysAgo }
      };
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
    } else if (period === 'month') {
      // Thống kê theo tháng (12 tháng gần nhất)
      const twelveMonthsAgo = new Date();
      twelveMonthsAgo.setMonth(twelveMonthsAgo.getMonth() - 12);
      matchStage = {
        created_date: { $gte: twelveMonthsAgo }
      };
      groupStage = {
        _id: {
          year: { $year: "$created_date" },
          month: { $month: "$created_date" }
        },
        date: { $first: { $dateToString: { format: "%m/%Y", date: "$created_date" } } },
        total_revenue: { $sum: "$total_amount" },
        order_count: { $sum: 1 }
      };
    } else if (period === 'year') {
      // Thống kê theo năm (5 năm gần nhất)
      const fiveYearsAgo = new Date();
      fiveYearsAgo.setFullYear(fiveYearsAgo.getFullYear() - 5);
      matchStage = {
        created_date: { $gte: fiveYearsAgo }
      };
      groupStage = {
        _id: {
          year: { $year: "$created_date" }
        },
        date: { $first: { $dateToString: { format: "%Y", date: "$created_date" } } },
        total_revenue: { $sum: "$total_amount" },
        order_count: { $sum: 1 }
      };
    }

    // Aggregate để tính toán
    const statistics = await billModel.aggregate([
      { $match: matchStage },
      { $group: groupStage },
      { $sort: { "_id.year": 1, "_id.month": period === 'day' ? 1 : undefined, "_id.day": period === 'day' ? 1 : undefined } }
    ]);

    // Tính tổng doanh thu và số đơn hàng
    const totalRevenue = statistics.reduce((sum, item) => sum + item.total_revenue, 0);
    const totalOrders = statistics.reduce((sum, item) => sum + item.order_count, 0);

    dataRes.data = {
      period: period,
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
