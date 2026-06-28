const fs = require('fs');
const path = require('path');

const inputPath = path.join(__dirname, 'real-meal-data-input.json');

function fixDuplicatesAndPrices() {
    console.log("Đang xử lý dọn dẹp data: xóa category trùng lặp và dọn field price nếu còn sót...");
    if (!fs.existsSync(inputPath)) {
        console.log("Không tìm thấy file JSON!");
        return;
    }
    
    const rawData = fs.readFileSync(inputPath, 'utf8');
    const meals = JSON.parse(rawData);

    meals.forEach(meal => {
        // Xóa thuộc tính price nếu còn sót
        if (meal.price) {
            delete meal.price;
        }

        // Loại bỏ trùng lặp trong categories
        if (meal.categories && Array.isArray(meal.categories)) {
            meal.categories = [...new Set(meal.categories.map(c => c.toLowerCase().trim()))];
            // Format lại chữ cái đầu viết hoa cho đẹp (Tùy chọn)
            meal.categories = meal.categories.map(c => c.charAt(0).toUpperCase() + c.slice(1));
        }

        // Loại bỏ trùng lặp trong tags
        if (meal.tags && Array.isArray(meal.tags)) {
            meal.tags = [...new Set(meal.tags.map(t => t.toLowerCase().trim()))];
            meal.tags = meal.tags.map(t => t.charAt(0).toUpperCase() + t.slice(1));
        }
    });

    fs.writeFileSync(inputPath, JSON.stringify(meals, null, 2));
    console.log("Đã dọn dẹp sạch sẽ mảng categories/tags và các trường thừa!");
}

fixDuplicatesAndPrices();
