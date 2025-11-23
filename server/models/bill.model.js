var db = require('./db');

const BillSchema = new db.mongoose.Schema(
    {
        created_date: {type: Date, default: Date.now},
        id_user: {type: db.mongoose.Schema.Types.ObjectId, ref: 'userModel', required: true},
        total_amount: {type: Number, required: true},
        address: {type: String},
        payment_method: {type: String, default: "cod", enum: ["cod", "online"]} // Phương thức thanh toán
    },
    {
        collection:'bill'
    }
);

let billModel = db.mongoose.model('billModel', BillSchema);
module.exports = {billModel};