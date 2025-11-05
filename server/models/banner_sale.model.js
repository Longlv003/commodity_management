const db = require('./db');

const bannerSchema = new db.mongoose.Schema(
    {
        name: {type: String, required: true},
        image: {type: String, required: true}
    },
    {collection: 'banner_sale'}
);

let bannerModel = db.mongoose.model('bannerModel', bannerSchema);
module.exports = {bannerModel};