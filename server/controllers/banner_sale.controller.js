const {bannerModel} = require('../models/banner_sale.model');
const {userModel} = require('../models/account.model');
const {uploadSingleFile} = require('../helpers/upload.helper');

exports.AddBanner = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        console.log('Request body:', req.body);
        console.log('Request file:', req.file);

        const {name} = req.body;

        if (!name) return res.status(400).json({ error: 'Chưa có tên banner' });
        if (!req.file) return res.status(400).json({ error: 'Chưa có ảnh banner' });

        const existingBanner = await bannerModel.findOne({ 
            name: name.trim() 
        });
        
        if (existingBanner) {
            return res.status(400).json({ 
                error: 'Tên banner đã tồn tại. Vui lòng chọn tên khác.' 
            });
        }

        // Upload file
        const fileName = await uploadSingleFile(req.file, 'banners');

        // Tạo banner mới
        const banner = new bannerModel({
            name: name,
            image: fileName
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

exports.DeleteBanner = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        if (typeof(req.params._id) != 'undefined') {
            _id = req.params._id;
        }
        const deleteBanner = await bannerModel.findByIdAndDelete(_id);
        if (!deleteBanner) {
            throw new Error("Delete operation failed: no banner found");
        }
        dataRes.msg = "Delete banner successfully";
        dataRes.data = deleteBanner;
    } catch (error) {
        console.error(error);
        dataRes.msg = error.message;
    }

    res.json(dataRes);
}

exports.GetAllBanner = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    try {
        let list = await bannerModel.find();
        dataRes.data = list;
    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }
    res.json(dataRes);
}