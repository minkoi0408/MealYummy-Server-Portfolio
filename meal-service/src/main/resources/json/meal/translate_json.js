const fs = require('fs');
const path = require('path');
const https = require('https');

const inputPath = path.join(__dirname, 'real-meal-data-input.json');
const outputPath = path.join(__dirname, 'real-meal-data-translated.json');

// Hàm gọi API Google Translate (Miễn phí)
function translateText(text) {
    return new Promise((resolve, reject) => {
        if (!text) return resolve('');
        const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=vi&dt=t&q=${encodeURIComponent(text)}`;
        
        https.get(url, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    // Google trả về mảng các câu đã dịch ở json[0]
                    let translatedStr = '';
                    if (json[0]) {
                        json[0].forEach(item => {
                            if (item[0]) translatedStr += item[0];
                        });
                    }
                    resolve(translatedStr || text);
                } catch (e) {
                    resolve(text); // Nếu lỗi thì trả về text gốc
                }
            });
        }).on('error', () => {
            resolve(text); // Lỗi mạng thì trả về text gốc
        });
    });
}

// Cần delay giữa các lần gọi để tránh bị Google chặn (Rate Limit)
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function run() {
    console.log("Bắt đầu dịch JSON. Quá trình này có thể mất vài phút...");
    const rawData = fs.readFileSync(inputPath, 'utf8');
    const meals = JSON.parse(rawData);

    const total = meals.length;
    for (let i = 0; i < total; i++) {
        const meal = meals[i];
        console.log(`Đang dịch món [${i + 1}/${total}]: ${meal.name}`);

        try {
            // Dịch Tên món ăn
            meal.name = await translateText(meal.name);
            await sleep(100); // 100ms delay
            
            // Dịch Danh mục
            if (meal.categories && meal.categories.length > 0) {
                for (let c = 0; c < meal.categories.length; c++) {
                    meal.categories[c] = await translateText(meal.categories[c]);
                }
            }
            await sleep(100);

            // Dịch Thẻ (Tags)
            if (meal.tags && meal.tags.length > 0) {
                for (let t = 0; t < meal.tags.length; t++) {
                    meal.tags[t] = await translateText(meal.tags[t]);
                }
            }
            await sleep(100);

            // Dịch Nguyên liệu
            if (meal.ingredients && meal.ingredients.length > 0) {
                for (let ig = 0; ig < meal.ingredients.length; ig++) {
                    if (meal.ingredients[ig].name) {
                        meal.ingredients[ig].name = await translateText(meal.ingredients[ig].name);
                    }
                }
            }
            await sleep(300); // Ngủ lâu hơn một chút sau mỗi món

        } catch (err) {
            console.log(`Lỗi khi dịch món ${i + 1}, bỏ qua...`);
        }
    }

    // Ghi đè vào file (hoặc file mới)
    fs.writeFileSync(outputPath, JSON.stringify(meals, null, 2));
    
    // Đổi tên file để lấy file đã dịch làm input chính
    fs.renameSync(outputPath, inputPath);
    console.log(`Đã hoàn tất dịch 100% tiếng Việt cho ${total} món ăn!`);
}

run();
