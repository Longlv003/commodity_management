const db = require("./db");

let pSchema = new db.mongoose.Schema(
  {
    productCode: { type: String, required: true, unique: true },
    catID: { type: db.mongoose.Schema.Types.ObjectId, required: true },
    name: { type: String, required: true },
    description: { type: String },
    image: [{ type: String }],
    is_favorite: { type: Boolean, default: false },
    created_at: { type: Date, default: new Date() },
    updated_at: { type: Date, default: new Date() },
    is_deleted: { type: Boolean, default: false },
    deleted_at: { type: Date, default: null },
  },
  { collection: "products" }
);

pSchema.virtual("variants", {
  ref: "pVariantModel", // model được populate tới
  localField: "_id", // field ở product
  foreignField: "product_id", // field ở variant
});

// Bật virtual khi chuyển sang JSON hoặc Object
pSchema.set("toObject", { virtuals: true });
pSchema.set("toJSON", { virtuals: true });

let pModel = db.mongoose.model("pModel", pSchema);
module.exports = { pModel };
