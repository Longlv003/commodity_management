var db = require('./db');
const BillDetailSchema = new db.mongoose.Schema(
    {
        id_product: {type: db.mongoose.Schema.Types.ObjectId, ref: 'pModel'},
        id_variant: {type: db.mongoose.Schema.Types.ObjectId, ref: 'pVariantModel'},
        id_bill: {type: db.mongoose.Schema.Types.ObjectId, ref: 'billModel'},
        quantity: {type: Number, required: true},
        price: {type: Number, require: true},
        size: {type: String},
        color: {type: String}
    },
    {
        collection:'bill_detail'
    }
);

let billDetailModel = db.mongoose.model('billDetailModel', BillDetailSchema);
module.exports = {billDetailModel};