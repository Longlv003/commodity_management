const db = require('./db');

const pSchema = new db.mongoose.Schema(
    {
        name: {type: String, required: true},
        price: {type: Number, required: true},
        qty: {type: Number, required: true},
        description: {type: String},
        image: {type: String},
        catID: {type: db.mongoose.Schema.Types.ObjectId, required: true},
        createdAt: {type: Date, default: new Date()}
    },
    {collection: 'products'}
);

let pModel = db.mongoose.model('pModel', pSchema);
module.exports = {pModel};