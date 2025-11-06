const {cartModel} = require('../models/cart.model');
const {billModel} = require('../models/bill.model');
const {billDetailModel} = require('../models/billDetail.model');
const {productModel} = require('../models/product.model');

exports.PlaceOrder = async (req, res, next) => {
    let dataRes = {msg: 'OK'};
    
    try {
        const {id_user, address} = req.params;

        if (!id_user || !address) {
            throw new Error("Thieu thong tin nguoi dung hoac dia chi");
        }

        const cartItem = await cartModel.find({id_user}).populate('id_product');

        if (cartItem.length === 0) {
            throw new Error("Gio hang trong");
        }

        let totalAmount = 0;
        cartItem.forEach(element => {
            totalAmount += element.id_product.price * element.quantity;
        });

        //const newIDBill = await getNextSequence('Bill');
        const newBill = new billModel({
            id_user,
            address,
            created_date: new Date(),
            total_amount: totalAmount
        });
        await newBill.save();

        for (const item of cartItem) {
            //const newIDBillDetails = await getNextSequence('BillDetail');
            const newBillDetails = new billDetailModel({
                id_bill: newBill._id,
                id_product: item.id_product._id,
                price: item.id_product.price,
                quantity: item.quantity
            });
            await newBillDetails.save();
        }

        await cartModel.deleteMany({id_user});

        dataRes.data = {bill: newBill, totalAmount};

    } catch (error) {
        dataRes.data = null;
        dataRes.msg = error.message;
    }

    res.json(dataRes);
}

exports.GetOrderHistory = async (req, res, next) => {
    let dataRes = { msg: 'OK', data: null };

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
            const details = await billDetailModel.find({ id_bill: bill._id }).populate('id_product');
            
            history.push({
                bill_id: bill._id,
                created_date: bill.created_date,
                total_amount: bill.total_amount,
                address: bill.address,
                products: details.map(item => ({
                    product_id: item.id_product._id,
                    name: item.id_product.name,
                    price: item.price,
                    quantity: item.quantity,
                    image: item.id_product.image
                }))
            });
        }

        dataRes.data = history;

    } catch (error) {
        dataRes.msg = error.message;
        dataRes.data = null;
    }

    res.json(dataRes);
};
