const fs = require('fs');
const path = require('path');

exports.uploadSingleFile = async (file, folderName) => {
    let dataRes = {msg: 'OK'};
    try {
        if (!file) {
            throw new Error("File not found");
        }

        const destDir = path.join(__dirname, `../public/images/${folderName}`);

        // Đặt tên file duy nhất (tránh trùng)
        const newFileName = Date.now() + '-' + file.originalname;
        const tempPath = file.path;
        const targetPath = path.join(destDir, newFileName);

        // Di chuyển file
        fs.renameSync(tempPath, targetPath);

        // Trả về tên file (không phải URL đầy đủ)
        dataRes.msg = "Upload thành công!"
        dataRes.data = newFileName;
        return newFileName;
    } catch (error) {
        dataRes.msg = error.message;
    }
    res.json(dataRes);
};