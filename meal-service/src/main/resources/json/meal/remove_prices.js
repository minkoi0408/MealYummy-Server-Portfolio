const fs = require('fs');
const path = require('path');

const inputPath = path.join(__dirname, 'real-meal-data-input.json');

function removePrices() {
    console.log("Đang loại bỏ trường price khỏi tất cả món ăn...");
    const rawData = fs.readFileSync(inputPath, 'utf8');
    const meals = JSON.parse(rawData);

    meals.forEach(meal => {
        delete meal.price;
    });

    fs.writeFileSync(inputPath, JSON.stringify(meals, null, 2));
    console.log("Đã loại bỏ trường price thành công!");
}

removePrices();
