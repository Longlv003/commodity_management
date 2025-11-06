const {walletModel} = require('../models/wallet.model');
const bcrypt = require('bcrypt');
const {userModel} = require('../models/account.model');

// wallet.controller.js
exports.CreateWallet = async (req, res) => {
    try {
        console.log("🔍 Request body:", req.body);
        console.log("🔍 User from middleware:", req.user);
        const { pin } = req.body;
        if (pin.length != 6) {
            return res.status(400).json({error: 'pin lenght'});
        }
        const userId = req.user._id; // Lấy từ middleware authentication

        // Kiểm tra user đã có ví chưa
        const user = await userModel.findById(userId);
        if (!user) {
            return res.status(400).json({error: 'User not found'});
        } else if (user.wallet) {
            return res.status(400).json({ error: 'User already has a wallet' });
        }

        // Kiểm tra wallet với user_id đã tồn tại chưa (double check)
        const existingWallet = await walletModel.findOne({ id_user: userId });
        if (existingWallet) {
            return res.status(400).json({ error: 'Wallet already exists for this user' });
        }

        // Tạo wallet number tự động
        const wallet_number = 'W' + Date.now() + Math.floor(Math.random() * 1000);

        // Hash PIN
        const salt = await bcrypt.genSalt(10);
        const pin_hash = await bcrypt.hash(pin, salt);

        // Tạo wallet
        const wallet = new walletModel({
            id_user: userId,
            wallet_number,
            pin_hash,
            balance: 0,
            total_deposits: 0,
            total_withdrawals: 0
        });

        await wallet.save();

        // Cập nhật user - đã có ví
        user.has_wallet = true;
        await user.save();

        return res.status(201).json({
            message: 'Wallet created successfully',
            data: {
                wallet: {
                    wallet_number: wallet.wallet_number,
                    balance: wallet.balance,
                    create_date: wallet.create_date
                }
            }
        });
    } catch (error) {
        console.error(error.message);
        return res.status(500).json({ error: error.message });
    }
};