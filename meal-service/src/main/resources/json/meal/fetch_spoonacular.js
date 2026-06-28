const fs = require('fs');
const path = require('path');
const https = require('https');

const API_KEY = '997705b7432f48bca5589915076d8055';
const QUERIES = ['healthy', 'salad', 'chicken', 'soup', 'beef', 'vegan'];
const outputPath = path.join(__dirname, 'real-meal-data-input.json');

const fetchQuery = (query) => {
    return new Promise((resolve, reject) => {
        const URL = `https://api.spoonacular.com/recipes/complexSearch?apiKey=${API_KEY}&query=${query}&number=30&addRecipeNutrition=true&fillIngredients=true&addRecipeInformation=true`;
        https.get(URL, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    const response = JSON.parse(data);
                    if (!response.results) resolve([]);
                    else resolve(response.results);
                } catch (e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
};

async function fetchAll() {
    let allMeals = [];
    const seenTitles = new Set();

    for (const q of QUERIES) {
        console.log(`Fetching query: ${q}...`);
        try {
            const results = await fetchQuery(q);
            for (const recipe of results) {
                if (seenTitles.has(recipe.title)) continue;
                seenTitles.add(recipe.title);

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
                const description = `Một ${mealType} tuyệt vời và giàu dinh dưỡng, nổi bật với sự kết hợp của ${mainIngredients}. Lựa chọn hoàn hảo với ${calories} kcal và ${protein}g protein giúp cung cấp năng lượng hiệu quả.`;

                allMeals.push({
                    name: recipe.title,
                    description: description,
                    nutrition: { calories, protein, fat, carbs },
                    categories: categories,
                    tags: tags,
                    ingredients: ingredients,
                    images: [{ url: recipe.image, isThumbnail: true }]
                });
            }
        } catch (err) {
            console.error(`Lỗi khi fetch query ${q}:`, err.message);
        }
    }

    fs.writeFileSync(outputPath, JSON.stringify(allMeals, null, 2));
    console.log(`Đã kéo thành công tổng cộng ${allMeals.length} món ăn thật từ Spoonacular và lưu vào: ${outputPath}`);
}

fetchAll();
