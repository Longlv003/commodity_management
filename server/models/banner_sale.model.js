
const db = require("./db");

const bannerSchema = new db.mongoose.Schema(
  {
    name: { type: String, required: true },
    image: { type: String, required: true },
    created_date: { type: Date, default: Date.now },
    updated_date: { type: Date, default: Date.now },
    is_delete: { type: Boolean, default: false },
    delete_date: { type: Date, default: null },
  },
  { collection: "banner_sale" }
);

let bannerModel = db.mongoose.model("bannerModel", bannerSchema);
module.exports = { bannerModel };
