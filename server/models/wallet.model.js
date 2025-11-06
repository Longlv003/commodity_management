const db = require('./db');
const jwt = require('jsonwebtoken');
require('dotenv').config();
const token_auto = process.env.TOKEN_SEC_KEY;
const bcrypt = require('bcrypt');

const walletSchema = new db.mongoose.Schema(
    {
        id_user: {type: db.mongoose.Schema.Types.ObjectId, required: true, ref: 'userModel', unique: true},
        wallet_number: {type: String, required: true, unique: true},
        total_deposits: {type: Number, required: true, default: 0},
        total_withdrawals: {type: Number, required: true, default: 0},
        balance: {type: Number, required: true, default: 0},
        pin_hash: {type: String, required: true},
        create_date: {type: Date, default: Date.now}
    },
    {collection: 'wallet'}
)

walletSchema.statics.makeAuthToken = async function(wallet) {
    const token = jwt.sign(
        { 
            _id: wallet._id, 
            wallet_number: wallet.wallet_number 
        }, 
        token_auto,
        { expiresIn: '24h' }
    );
    return token;
};

walletSchema.statics.findByCredentials = async function(wallet_number, pin) {
    const wallet = await this.findOne({ wallet_number });
    if (!wallet) {
        throw new Error("Wallet not found");
    }

    const isPinValid = await bcrypt.compare(pin, wallet.pin_hash);
    if (!isPinValid) {
        throw new Error("Invalid PIN");
    }

    return wallet;
};

let walletModel = db.mongoose.model('walletModel', walletSchema);
module.exports = {walletModel};