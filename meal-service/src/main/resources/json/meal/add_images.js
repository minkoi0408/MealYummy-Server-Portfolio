const fs = require('fs');
const path = require('path');

const inputPath = path.join(__dirname, 'meal_data_input.json');
let meals = [];

try {
    meals = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
} catch (e) {
    console.error("Lỗi đọc file:", e);
    process.exit(1);
}

const IMAGES = {
    SALAD: [
        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&q=80",
        "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=800&q=80",
        "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80",
        "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=800&q=80",
        "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=800&q=80"
    ],
    SMOOTHIE: [
        "https://images.unsplash.com/photo-1556881286-fc6915169721?w=800&q=80",
        "https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=800&q=80",
        "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=800&q=80",
        "https://images.unsplash.com/photo-1502741224143-9038af877c8e?w=800&q=80",
        "https://images.unsplash.com/photo-1553530979-7ee52a2670c4?w=800&q=80"
    ],
    RICE_BOWL: [
        "https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=800&q=80",
        "https://images.unsplash.com/photo-1580476262798-bddd9f4b7369?w=800&q=80",
        "https://images.unsplash.com/photo-1541519227354-08fa5d50c44d?w=800&q=80",
        "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800&q=80",
        "https://images.unsplash.com/photo-1555126634-ba0917eb8521?w=800&q=80"
    ],
    NOODLE: [
        "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&q=80",
        "https://images.unsplash.com/photo-1552611052-33e04de081de?w=800&q=80",
        "https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?w=800&q=80",
        "https://images.unsplash.com/photo-1582878826629-29b7ad1cb438?w=800&q=80",
        "https://images.unsplash.com/photo-1591814468924-caf88d1232e1?w=800&q=80"
    ],
    SOUP: [
        "https://images.unsplash.com/photo-1547592180-85f173990554?w=800&q=80",
        "https://images.unsplash.com/photo-1604152135912-04a022e23696?w=800&q=80",
        "https://images.unsplash.com/photo-1565557612192-3bc1230e7195?w=800&q=80",
        "https://images.unsplash.com/photo-1548943487-a2e4b43b4850?w=800&q=80",
        "https://images.unsplash.com/photo-1614583224978-f05ce51ef5fa?w=800&q=80"
    ],
    GENERAL: [
        "https://images.unsplash.com/photo-1490645935967-10de6ba82830?w=800&q=80",
        "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=80",
        "https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=800&q=80",
        "https://images.unsplash.com/photo-1493770348161-369560ae357d?w=800&q=80",
        "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=800&q=80"
    ]
};

const randomItem = (arr) => arr[Math.floor(Math.random() * arr.length)];

meals = meals.map(meal => {
    let type = 'GENERAL';
    const name = meal.name.toLowerCase();
    
    if (name.includes('salad')) type = 'SALAD';
    else if (name.includes('sinh tố')) type = 'SMOOTHIE';
    else if (name.includes('cơm') || name.includes('bát dinh dưỡng')) type = 'RICE_BOWL';
    else if (name.includes('bún') || name.includes('mì') || name.includes('phở') || name.includes('nui') || name.includes('miến') || name.includes('bánh mì') || name.includes('nướng')) type = 'NOODLE';
    else if (name.includes('súp') || name.includes('canh')) type = 'SOUP';

    const pool = IMAGES[type];
    const selectedUrl = randomItem(pool);

    return {
        ...meal,
        images: [
            {
                url: selectedUrl,
                isThumbnail: true
            }
        ]
    };
});

fs.writeFileSync(inputPath, JSON.stringify(meals, null, 2));
console.log(`Đã gán hình ảnh HD cho ${meals.length} món ăn thành công!`);
