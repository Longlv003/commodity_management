const {catModel} = require('../models/category.model');
const {pModel} = require('../models/product.model');
const {variantSalesModel} = require('../models/variant.sales.model');

exports.addCat = async (req, res, next) => {
    let dataRes = { msg: 'OK' };

    try {
        const { name } = req.body; 
        if (!name || name.trim() === '') {
            throw new Error("Tên danh mục không được để trống");
        }

        const cat = new catModel({
            name: name.trim()
        });

        await cat.save();

        dataRes.data = cat;

    } catch (error) {
        dataRes.msg = error.message;
    }

    res.json(dataRes);
};

exports.deleteCat = async (req, res, next) => {
    let dataRes = { msg: 'OK' };

    try {
        const { _id } = req.params;

        if (!_id) {
            throw new Error("Thiếu ID danh mục cần xóa");
        }

        const deleted = await catModel.findByIdAndDelete(_id);

        if (!deleted) {
            throw new Error("Không tìm thấy danh mục để xóa");
        }

        dataRes.msg = "Xóa danh mục thành công!";
        dataRes.data = deleted;

    } catch (error) {
        console.error(error.message);
        dataRes.msg = error.message;
    }

    res.json(dataRes);
};

exports.getListCat = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        // Lấy danh sách các catID có sản phẩm (chưa bị xóa)
        const categoriesWithProducts = await pModel.aggregate([
            {
                $match: {
                    is_deleted: { $ne: true }
                }
            },
            {
                $group: {
                    _id: "$catID"
                }
            }
        ]);

        // Lấy danh sách catID
        const categoryIds = categoriesWithProducts.map(item => item._id).filter(id => id != null);

        // Chỉ lấy các category có sản phẩm
        let list = [];
        if (categoryIds.length > 0) {
            list = await catModel.find({
                _id: { $in: categoryIds }
            });
        }
        
        dataRes.data = list;
    } catch (error) {
        console.log(error.message);
        dataRes.msg = error.message;
    }
    res.json(dataRes);
};

exports.getAllCategories = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        // Lấy tất cả danh mục (kể cả danh mục không có sản phẩm)
        let list = await catModel.find();
        dataRes.data = list;
    } catch (error) {
        console.log(error.message);
        dataRes.msg = error.message;
    }
    res.json(dataRes);
};

exports.GetTopCategories = async (req, res, next) => {
    let dataRes = { msg: 'OK' };
    
    try {
        // Tính tổng số lượng sản phẩm bán chạy (total_sold) theo từng category
        // Bước 1: Aggregate từ variantSalesModel để lấy product_id và quantity_sold
        // Bước 2: Join với product để lấy catID
        // Bước 3: Group theo catID và tính tổng quantity_sold
        const categorySalesStats = await variantSalesModel.aggregate([
            {
                $lookup: {
                    from: "products",
                    localField: "product_id",
                    foreignField: "_id",
                    as: "product"
                }
            },
            {
                $unwind: "$product"
            },
            {
                $match: {
                    "product.is_deleted": { $ne: true },
                    "product.catID": { $ne: null } // Đảm bảo catID không null
                }
            },
            {
                $group: {
                    _id: "$product.catID",
                    totalSold: { $sum: "$quantity_sold" }
                }
            },
            {
                $sort: { totalSold: -1 }
            },
            {
                $limit: 4
            }
        ]);

        // Chỉ lấy top 4 category có số lượng bán chạy nhất (không có fallback)
        let categoryIds = [];
        
        if (categorySalesStats && categorySalesStats.length > 0) {
            categoryIds = categorySalesStats.map(item => item._id).filter(id => id != null);
        }

        // Nếu không có dữ liệu bán hàng, trả về mảng rỗng
        if (categoryIds.length === 0) {
            dataRes.data = [];
            dataRes.total = 0;
            dataRes.msg = "Không có dữ liệu bán hàng";
            return res.json(dataRes);
        }

        // Lấy thông tin chi tiết của các category
        const categories = await catModel.find({
            _id: { $in: categoryIds }
        });

        // Kết hợp thông tin với totalSold từ sales stats
        const result = categories.map(category => {
            const salesInfo = categorySalesStats.find(item => 
                item._id && item._id.toString() === category._id.toString()
            );
            return {
                _id: category._id,
                name: category.name,
                productCount: salesInfo ? salesInfo.totalSold : 0 // Dùng totalSold (số lượng bán chạy)
            };
        });

        // Sắp xếp lại theo số lượng bán chạy giảm dần
        result.sort((a, b) => b.productCount - a.productCount);

        dataRes.data = result;
        dataRes.total = result.length;
        dataRes.msg = "Lấy danh sách danh mục phổ biến thành công";
        
    } catch (error) {
        console.error('Lỗi khi lấy danh mục phổ biến:', error);
        dataRes.data = null;
        dataRes.msg = 'Lỗi server: ' + error.message;
    }
    
    res.json(dataRes);
};