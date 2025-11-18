const db = require("./db");
const jwt = require("jsonwebtoken");
require("dotenv").config();
const token_auto = process.env.TOKEN_SEC_KEY;
const bcrypt = require("bcrypt");

const accSchema = new db.mongoose.Schema(
  {
    name: { type: String },
    email: { type: String, required: true, unique: true },
    pass: { type: String, required: true, unique: true },
    role: {
      type: String,
      enum: ["user", "admin"],
      default: "user",
    },
    is_active: { type: Boolean, default: true },
    has_wallet: { type: Boolean, default: false },
    //is_delete: {type: Boolean, default: false},
    phone: { type: String },
    address: { type: String },
    image: { type: String },
    token: { type: String },
  },
  {
    collection: "accounts",
  }
);

accSchema.statics.makeAuthToken = async (user) => {
  const token = jwt.sign({ _id: user._id, email: user.email }, token_auto);

  user.token = token;
  await user.save();
  return token;
};

accSchema.statics.findByEmailPasswd = async (email, passwd) => {
  const user = await userModel.findOne({ email });
  if (!user) {
    throw new Error("User not found");
  }

  const checkPass = await bcrypt.compare(passwd, user.pass);
  if (!checkPass) {
    throw new Error("Wrong password");
  }

  return user;
};

let userModel = db.mongoose.model("userModel", accSchema);
module.exports = { userModel };
