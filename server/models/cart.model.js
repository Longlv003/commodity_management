var db = require('./db');

const cartSchema = new db.mongoose.Schema(
    {
        id_user: {type: db.mongoose.Schema.Types.ObjectId, ref: 'userModel', required: true},
        id_product: {type: db.mongoose.Schema.Types.ObjectId, ref: 'pModel', required: true},
        quantity: {type: Number, required: true, default: 1},
        added_date: {type: Date, default: Date.now} 
    }, 
    {
        collection: "cart"
    }
);

const cartModel = db.mongoose.model('cartModel', cartSchema);
module.exports = {cartModel};