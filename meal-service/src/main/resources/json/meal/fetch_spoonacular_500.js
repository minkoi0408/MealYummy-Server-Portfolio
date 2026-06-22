const fs = require('fs');
const path = require('path');
const https = require('https');

const API_KEY = '8f9418c85c864741be5601cd509d1352';
const QUERIES = [
    'fish', 'smoothie', 'low carb', 'keto', 'mediterranean', 
    'seafood', 'pork', 'lamb', 'rice', 'noodle', 
    'diet', 'protein', 'weight loss', 'clean eating', 'organic',
    'vegan', 'healthy', 'salad', 'soup' // fallback
];
const outputPath = path.join(__dirname, 'real-meal-data-input.json');

// --- HÀM DỊCH GOOGLE (Miễn phí) ---
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
                    let translatedStr = '';
                    if (json[0]) {
                        json[0].forEach(item => {
                            if (item[0]) translatedStr += item[0];
                        });
                    }
                    resolve(translatedStr || text);
                } catch (e) {
                    resolve(text);
                }
            });
        }).on('error', () => resolve(text));
    });
}
const sleep = ms => new Promise(r => setTimeout(r, ms));

// --- HÀM FETCH SPOONACULAR ---
const fetchQuery = (query, offset = 0) => {
    return new Promise((resolve, reject) => {
        // Mỗi query lấy 30 recipes
        const URL = `https://api.spoonacular.com/recipes/complexSearch?apiKey=${API_KEY}&query=${query}&number=30&offset=${offset}&addRecipeNutrition=true&fillIngredients=true&addRecipeInformation=true`;
        https.get(URL, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    const response = JSON.parse(data);
                    if (response.status === 'failure' && response.code === 402) {
                        return reject(new Error("API_LIMIT"));
                    }
                    if (!response.results) resolve([]);
                    else resolve(response.results);
                } catch (e) {
                    resolve([]); // Bỏ qua lỗi
                }
            });
        }).on('error', () => resolve([]));
    });
};

async function fetchAndTranslate() {
    let existingMeals = [];
    if (fs.existsSync(outputPath)) {
        existingMeals = JSON.parse(fs.readFileSync(outputPath, 'utf8'));
    }

    const existingIds = new Set();
    const existingTitles = new Set();
    
    // Lấy ID từ URL ảnh để kiểm tra trùng lặp (vì tên đã bị dịch sang Tiếng Việt)
    // VD: https://img.spoonacular.com/recipes/716426-312x231.jpg -> ID 716426
    existingMeals.forEach(meal => {
        if (meal.images && meal.images.length > 0) {
            const url = meal.images[0].url;
            const match = url.match(/\/(\d+)-/);
            if (match) existingIds.add(match[1]);
        }
        existingTitles.add(meal.name.toLowerCase());
    });

    console.log(`Đã có sẵn ${existingMeals.length} món ăn. Cần tìm thêm ${Math.max(0, 500 - existingMeals.length)} món.`);

    let totalMealsCount = existingMeals.length;
    let queryIndex = 0;
    let newMealsCount = 0;

    while (totalMealsCount < 500 && queryIndex < QUERIES.length) {
        const q = QUERIES[queryIndex];
        console.log(`Đang tìm kiếm với từ khóa: ${q}...`);
        
        try {
            const results = await fetchQuery(q, 0);
            
            for (const recipe of results) {
                if (totalMealsCount >= 500) break;
                
                const recipeIdStr = recipe.id.toString();
                if (existingIds.has(recipeIdStr) || existingTitles.has(recipe.title.toLowerCase())) {
                    continue; // Bỏ qua nếu bị trùng lặp
                }
                
                existingIds.add(recipeIdStr);
                existingTitles.add(recipe.title.toLowerCase());

                // Lấy Dinh dưỡng
                let calories = 0, protein = 0, fat = 0, carbs = 0;
                if (recipe.nutrition && recipe.nutrition.nutrients) {
                    recipe.nutrition.nutrients.forEach(n => {
                        if (n.name === 'Calories') calories = n.amount;
                        if (n.name === 'Protein') protein = n.amount;
                        if (n.name === 'Fat') fat = n.amount;
                        if (n.name === 'Carbohydrates') carbs = n.amount;
                    });
                }

                // Lấy Nguyên liệu
                const ingredients = recipe.extendedIngredients ? recipe.extendedIngredients.map(ing => ({
                    name: ing.name,
                    value: ing.amount,
                    unit: ing.unitShort || ing.unit || 'g'
                })) : [];

                const tags = [];
                if (recipe.vegetarian) tags.push("Vegetarian");
                if (recipe.vegan) tags.push("Vegan");
                if (recipe.glutenFree) tags.push("Gluten Free");
                if (recipe.dairyFree) tags.push("Dairy Free");
                if (recipe.veryHealthy) tags.push("Very Healthy");

                const categories = recipe.dishTypes || [];

                const mainIngredients = ingredients.slice(0, 3).map(i => i.name).join(', ');
                const mealType = categories.length > 0 ? categories[0] : 'bữa ăn';
                const descriptionEn = `Một ${mealType} tuyệt vời và giàu dinh dưỡng, nổi bật với sự kết hợp của ${mainIngredients}. Lựa chọn hoàn hảo với ${calories} kcal và ${protein}g protein giúp cung cấp năng lượng hiệu quả.`;

                // --- BẮT ĐẦU DỊCH TIẾNG VIỆT NGAY TẠI ĐÂY ---
                console.log(`[${totalMealsCount + 1}/500] Tìm thấy món mới: ${recipe.title}. Đang dịch...`);
                
                const transName = await translateText(recipe.title);
                await sleep(50);
                
                const transDesc = await translateText(descriptionEn);
                await sleep(50);

                const transCategories = [];
                for(let cat of categories) {
                    transCategories.push(await translateText(cat));
                }
                
                const transTags = [];
                for(let tag of tags) {
                    transTags.push(await translateText(tag));
                }

                const transIngredients = [];
                for(let ing of ingredients) {
                    transIngredients.push({
                        name: await translateText(ing.name),
                        value: ing.value,
                        unit: ing.unit
                    });
                }
                // --- KẾT THÚC DỊCH ---

                existingMeals.push({
                    name: transName,
                    description: transDesc,
                    nutrition: { calories, protein, fat, carbs },
                    categories: transCategories,
                    tags: transTags,
                    ingredients: transIngredients,
                    images: [{ url: recipe.image, isThumbnail: true }]
                });

                totalMealsCount++;
                newMealsCount++;
                
                // Ghi đè tiến độ mỗi 5 món để tránh mất data nếu lỗi
                if (newMealsCount % 5 === 0) {
                    fs.writeFileSync(outputPath, JSON.stringify(existingMeals, null, 2));
                }
            }
        } catch (err) {
            if (err.message === "API_LIMIT") {
                console.error("❌ API Spoonacular đã HẾT LƯỢT (Giới hạn 150 points/ngày). Bạn cần cung cấp API KEY mới để tiếp tục!");
                break;
            } else {
                console.error(`Lỗi khi fetch query ${q}:`, err.message);
            }
        }
        queryIndex++;
    }

    fs.writeFileSync(outputPath, JSON.stringify(existingMeals, null, 2));
    console.log(`✅ Đã thêm mới ${newMealsCount} món ăn, 100% tiếng Việt.`);
    console.log(`🎉 TỔNG SỐ LƯỢNG MÓN ĂN HIỆN TẠI LÀ: ${existingMeals.length} món!`);
}

fetchAndTranslate();
