var db = require('./db');
const BillDetailSchema = new db.mongoose.Schema(
    {
        id_product: {type: db.mongoose.Schema.Types.ObjectId, ref: 'pModel'},
        id_bill: {type: db.mongoose.Schema.Types.ObjectId, ref: 'billModel'},
        quantity: {type: Number, required: true},
        price: {type: Number, require: true}
    },
    {
        collection:'bill_detail'
    }
);

let billDetailModel = db.mongoose.model('billDetailModel', BillDetailSchema);
module.exports = {billDetailModel};