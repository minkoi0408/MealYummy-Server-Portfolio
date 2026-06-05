const fs = require('fs');
const path = require('path');

const ingredientsPath = path.join(__dirname, '../ingredient/ingredient_response.json');
const tagsPath = path.join(__dirname, '../tag/tag_response.json');
const categoriesPath = path.join(__dirname, '../category/category_response.json');
const outputPath = path.join(__dirname, 'meal_data_input.json');

const ingredients = JSON.parse(fs.readFileSync(ingredientsPath, 'utf8'));
let allTags = [];
let allCats = [];
try {
    allTags = JSON.parse(fs.readFileSync(tagsPath, 'utf8')).map(t => t.id).filter(id => id);
    allCats = JSON.parse(fs.readFileSync(categoriesPath, 'utf8')).map(c => c.id).filter(id => id);
} catch (e) {
    console.log("Could not load tag or category responses, skipping dynamic tags/cats.");
}

const randomItem = (arr) => arr[Math.floor(Math.random() * arr.length)];
const randomInt = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
const shuffle = (arr) => arr.sort(() => 0.5 - Math.random());

// --- CATEGORIES & TAGS MAPPING ---
const VEGAN_TAG = "6a218a708ddeb43c6690b59d";

const TYPES = {
    SALAD: {
        cats: ["6a218a068ddeb43c6690b490", "6a218a068ddeb43c6690b491", "6a218a078ddeb43c6690b493"],
        tags: ["6a218a708ddeb43c6690b5a0", "6a218a708ddeb43c6690b5b8", "6a218a708ddeb43c6690b5b6", "6a218a708ddeb43c6690b5a4"],
        templates: [
            { name: "Salad {protein} và {veg}", desc: "Món salad thanh mát, kết hợp giữa {protein} giàu đạm và {veg} tươi ngon, phù hợp cho chế độ eat clean." },
            { name: "Salad trộn {veg} và {veg2}", desc: "Salad thuần chay nhẹ bụng, xốt dầu giấm tươi mát kết hợp {veg} và {veg2}." }
        ]
    },
    SMOOTHIE: {
        cats: ["6a218a078ddeb43c6690b495", "6a218a078ddeb43c6690b494"],
        tags: ["6a218a708ddeb43c6690b5c6", "6a218a708ddeb43c6690b5bf", "6a218a708ddeb43c6690b5ba", "6a218a708ddeb43c6690b5bd"],
        templates: [
            { name: "Sinh tố {fruit} và {carb}", desc: "Thức uống meal replacement hoàn hảo, kết hợp {fruit} giàu vitamin và {carb} cung cấp năng lượng nhanh." },
            { name: "Sinh tố detox {veg} và {fruit}", desc: "Đồ uống detox siêu tốc kết hợp {veg} và {fruit} giúp thanh lọc cơ thể hiệu quả." }
        ]
    },
    RICE_BOWL: {
        cats: ["69f1d3e0fe69d1241bcb2843", "6a218a088ddeb43c6690b49b", "69f1d3e2fe69d1241bcb285d"],
        tags: ["6a218a708ddeb43c6690b5b5", "6a218a708ddeb43c6690b5a1", "6a218a708ddeb43c6690b5ae", "6a218a708ddeb43c6690b5af", "6a218a708ddeb43c6690b5b4"],
        templates: [
            { name: "Cơm {carb} trộn {protein} và {veg}", desc: "Bữa trưa năng lượng với {carb} chậm tiêu hóa, ăn kèm {protein} và {veg} giúp no lâu, duy trì vóc dáng." },
            { name: "Bát dinh dưỡng: {carb}, {protein} và {veg}", desc: "Power bowl cân bằng hoàn hảo macros: tinh bột từ {carb}, đạm từ {protein} và chất xơ từ {veg}." }
        ]
    },
    NOODLE: {
        cats: ["69f1d3e0fe69d1241bcb284b", "69f1d3e1fe69d1241bcb284f", "69f1d3e1fe69d1241bcb2853"],
        tags: ["6a218a708ddeb43c6690b5b7", "6a218a708ddeb43c6690b5b2"],
        templates: [
            { name: "Bún {carb} xào {protein} sốt {sauce}", desc: "Món xào healthy ít dầu mỡ, dùng {carb} sợi, {protein} dai ngon và nước sốt {sauce} đậm đà." },
            { name: "{carb} nướng ăn kèm {veg} và {sauce}", desc: "Bữa phụ hoặc bữa tối nhẹ nhàng với {carb} nướng thơm lừng, dùng chung {veg} chấm sốt {sauce}." }
        ]
    },
    SOUP: {
        cats: ["69f1d3e2fe69d1241bcb2858", "69f1d3e2fe69d1241bcb2859", "6a218a088ddeb43c6690b49d"],
        tags: ["6a218a708ddeb43c6690b5ad", "6a218a708ddeb43c6690b5ac"],
        templates: [
            { name: "Súp {veg} nấu {protein}", desc: "Món súp dinh dưỡng, ấm bụng, kết hợp vị ngọt từ {veg} và đạm từ {protein}, rất tốt cho tiêu hóa." },
            { name: "Canh {veg} thanh mát nấu {protein}", desc: "Món canh giải nhiệt dùng {veg} và {protein} tươi ngon, nêm nếm vừa phải tốt cho huyết áp." }
        ]
    }
};

// --- INGREDIENTS CLASSIFICATION ---
const proteins = ingredients.filter(i => /Gà|Bò|Cá|Tôm|Heo|Đùi gà|Mực|Thăn|Cua|Ghẹ|Bạch tuộc|Ốc|Sườn/.test(i.name));
const veganProteins = ingredients.filter(i => /Đậu hũ|Tempeh|Đậu nành|Đậu xanh|Đậu đỏ|Đậu phộng|Whey|Đậu lăng|Đậu gà|Hạt điều|Hạt óc chó|Hạt macca/.test(i.name));
const veggies = ingredients.filter(i => /Rau|Cải|Salad|Bina|Kale|Cà chua|Dưa chuột|Bông cải|Măng tây|Cần tây|Nấm|Bí|Mướp|Xà lách|Khoai tây|Hành|Gừng|Tỏi|Ớt/.test(i.name));
const riceCarbs = ingredients.filter(i => /Gạo|Yến mạch|Quinoa|Kiều mạch/.test(i.name));
const noodleCarbs = ingredients.filter(i => /Bún|Phở|Mì|Nui|Miến|Bánh mì/.test(i.name));
const smoothieCarbs = ingredients.filter(i => /Yến mạch|Sữa|Sữa chua|Matcha|Cacao|Hạt chia/.test(i.name));
const sauces = ingredients.filter(i => /Tương|Sốt|Mắm|Dầu|Giấm|Mayonnaise/.test(i.name));
const veganSauces = ingredients.filter(i => /Tương|Dầu|Giấm/.test(i.name) && !/Mắm/.test(i.name));
const fruits = ingredients.filter(i => /Táo|Chuối|Bơ|Dâu|Cam|Chanh|Việt quất|Xoài|Đu đủ|Mâm xôi/.test(i.name));

let generatedMeals = [];
if (fs.existsSync(outputPath)) {
    try {
        generatedMeals = JSON.parse(fs.readFileSync(outputPath, 'utf8'));
    } catch (e) {
        console.error("Could not read existing meals, starting fresh.");
    }
}
const initialCount = generatedMeals.length;
const numMealsToGenerate = 1000;

for (let i = 1; i <= numMealsToGenerate; i++) {
    // 25% chance of being vegan
    const isVegan = Math.random() < 0.25;

    // Pick a meal type randomly
    const mealTypeKeys = Object.keys(TYPES);
    const selectedTypeKey = mealTypeKeys[Math.floor(Math.random() * mealTypeKeys.length)];
    const mealTypeInfo = TYPES[selectedTypeKey];

    const template = randomItem(mealTypeInfo.templates);

    // Pick categories and tags logically
    const selectedCats = shuffle(mealTypeInfo.cats).slice(0, randomInt(1, 2));
    const selectedTags = shuffle(mealTypeInfo.tags).slice(0, randomInt(2, 3));
    if (isVegan) {
        selectedTags.push(VEGAN_TAG);
    }
    
    // Mix in new dynamic premium tags and categories
    if (allTags.length > 0) {
        selectedTags.push(...shuffle(allTags).slice(0, randomInt(1, 3)));
    }
    if (allCats.length > 0 && Math.random() > 0.5) {
        selectedCats.push(randomItem(allCats));
    }

    // Determine ingredients based on type & vegan
    let pPool = isVegan ? veganProteins : proteins;
    if (pPool.length === 0) pPool = veganProteins; // fallback

    let vPool = veggies;
    let sPool = isVegan ? veganSauces : sauces;
    if (sPool.length === 0) sPool = sauces; // fallback
    
    let fPool = fruits;
    let cPool = riceCarbs;

    if (selectedTypeKey === 'NOODLE') cPool = noodleCarbs;
    else if (selectedTypeKey === 'SMOOTHIE') cPool = smoothieCarbs;

    const p = randomItem(pPool);
    const v = randomItem(vPool);
    const v2 = randomItem(vPool.filter(item => item.id !== v.id)) || v;
    const c = randomItem(cPool);
    const s = randomItem(sPool);
    const f = randomItem(fPool);

    // Replace placeholders
    let mealName = template.name
        .replace('{protein}', p.name)
        .replace('{veg}', v.name)
        .replace('{veg2}', v2.name)
        .replace('{carb}', c.name)
        .replace('{sauce}', s.name)
        .replace('{fruit}', f.name);
    
    let mealDesc = template.desc
        .replace('{protein}', p.name.toLowerCase())
        .replace('{veg}', v.name.toLowerCase())
        .replace('{veg2}', v2.name.toLowerCase())
        .replace('{carb}', c.name.toLowerCase())
        .replace('{sauce}', s.name.toLowerCase())
        .replace('{fruit}', f.name.toLowerCase());

    const minPrice = randomInt(35, 80) * 1000;
    const maxPrice = minPrice + randomInt(10, 50) * 1000;

    let calBase = 300;
    let proBase = 20;
    let fatBase = 5;
    let carbBase = 30;
    
    if (selectedTypeKey === 'SALAD') {
        calBase = 250; proBase = 15; fatBase = 10; carbBase = 20;
    } else if (selectedTypeKey === 'SMOOTHIE') {
        calBase = 200; proBase = 5; fatBase = 2; carbBase = 40;
    } else {
        calBase = 400; proBase = 30; fatBase = 15; carbBase = 50;
    }

    const nutrition = {
        calories: calBase + randomInt(0, 150),
        protein: proBase + randomInt(0, 20),
        fat: fatBase + randomInt(0, 10),
        carbs: carbBase + randomInt(0, 30),
        fiber: randomInt(3, 15)
    };

    const selectedIngs = [];
    const addIng = (ing, valRange, unit) => {
        if(ing && !selectedIngs.find(x => x.ingredientId === ing.id)) {
            selectedIngs.push({
                ingredientId: ing.id,
                value: randomInt(valRange[0], valRange[1]),
                unit: unit
            });
        }
    };

    // Add ingredients logically based on type
    if (selectedTypeKey === 'SALAD') {
        addIng(v, [100, 200], 'g');
        addIng(v2, [50, 100], 'g');
        addIng(p, [50, 150], 'g'); // Protein
        addIng(randomItem(sPool), [1, 3], 'muỗng canh'); // Sauce
    } else if (selectedTypeKey === 'SMOOTHIE') {
        addIng(f, [100, 200], 'g');
        addIng(c, [100, 250], 'ml'); // Milk/Oats
        addIng(randomItem(vPool), [50, 100], 'g'); // Maybe some veggie
    } else {
        addIng(c, [100, 200], 'g');
        addIng(p, [100, 200], 'g');
        addIng(v, [50, 150], 'g');
        addIng(s, [1, 5], 'muỗng canh');
    }

    // Add 1-2 random spices
    const spices = ingredients.filter(i => /Tiêu|Muối|Tỏi|Gừng|Ớt|Hành|Ngò|Chanh/.test(i.name));
    for(let j=0; j<randomInt(1, 2); j++) {
        addIng(randomItem(spices), [1, 5], 'ít/nhúm');
    }

    generatedMeals.push({
        name: mealName,
        description: mealDesc,
        price: {
            minPrice: minPrice,
            maxPrice: maxPrice
        },
        nutrition: nutrition,
        categoryIds: [...new Set(selectedCats)],
        tagIds: [...new Set(selectedTags)],
        ingredients: selectedIngs
    });
}

fs.writeFileSync(outputPath, JSON.stringify(generatedMeals, null, 2));
console.log(`Successfully appended ${numMealsToGenerate} logically accurate meals. Total meals in file: ${generatedMeals.length}`);
