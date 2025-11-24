const db = require("./db");

const pVariantSchema = new db.mongoose.Schema(
  {
    sku: { type: String, required: true, unique: true },
    product_id: {
      type: db.mongoose.Types.ObjectId,
      ref: "pModel",
      required: true,
    },
    size: { type: String, required: true },
    color: { type: String, required: true },
    quantity: { type: Number, required: true, min: 0, default: 0 },
    price: { type: Number, required: true, min: 0, default: 0 },
    image: [{ type: String }],
    is_deleted: { type: Boolean, default: false },
    deleted_at: { type: Date, default: null },
  },
  {
    collection: "product_variant",
  }
);

let pVariantModel = db.mongoose.model("pVariantModel", pVariantSchema);
module.exports = { pVariantModel };
