const fs = require('fs');
const path = require('path');

const ingredientsPath = path.join(__dirname, '../ingredient/ingredient_data_input.json');
const categoriesPath = path.join(__dirname, '../category/category_data_input.json');
const tagsPath = path.join(__dirname, '../tag/tag_data_input.json');

// 1. EXPAND INGREDIENTS (Adding 200 variations & exotic ingredients)
let ingredients = JSON.parse(fs.readFileSync(ingredientsPath, 'utf8'));
const newIngredients = [
    "Thịt bò Kobe", "Thịt bò Wagyu", "Thịt bò Úc", "Thịt bò Mỹ", "Thịt bò lúc lắc", 
    "Gà tre", "Gà ác", "Gà ta thả vườn", "Gà đồi", "Gà đen H'Mông",
    "Heo rừng", "Heo Ibérico", "Thịt heo quay", "Thịt heo xông khói", "Ba rọi hun khói",
    "Cá hồi Na Uy", "Cá hồi Sapa", "Cá tầm", "Cá lăng", "Cá mú", "Cá bớp", "Cá chình",
    "Cá mặt quỷ", "Cá mập", "Vi cá mập", "Yến sào", "Bào ngư", "Hải sâm", "Sò điệp Hokkaido",
    "Tôm hùm Alaska", "Tôm hùm bông", "Tôm càng xanh", "Cua Hoàng Đế (King Crab)", "Cua Cà Mau",
    "Nấm Truffle đen", "Nấm Truffle trắng", "Nấm Matsutake", "Nấm mối", "Nấm linh chi",
    "Sâm Ngọc Linh", "Hồng sâm Hàn Quốc", "Đông trùng hạ thảo", "Kỷ tử đỏ", "Táo đỏ Tân Cương",
    "Hạt Macadamia Úc", "Hạt dẻ cười", "Hạt thông", "Hạt lanh Canada", "Hạt chia Úc",
    "Trứng cá muối Caviar", "Trứng cá hồi", "Trứng cá chuồn", "Trứng cút lộn", "Trứng ngỗng",
    "Sữa ong chúa", "Mật ong rừng U Minh", "Mật ong Manuka", "Sữa non", "Phô mai Mozzarella",
    "Phô mai Cheddar", "Phô mai Parmesan", "Phô mai Camembert", "Phô mai Gouda", "Bơ Pháp",
    "Dầu Olive ép lạnh Extra Virgin", "Dầu quả bơ", "Dầu hạt lanh", "Dầu dừa tinh khiết", "Dầu mè đen",
    "Gạo ST25", "Gạo lứt nảy mầm", "Gạo Tám Xoan", "Gạo lứt tím than", "Quinoa ba màu",
    "Yến mạch cán dẹt", "Kiều mạch", "Hạt kê", "Lúa mạch đen", "Bột mì nguyên cám",
    "Bắp cải tím", "Rau chân vịt baby", "Cần tây Mỹ", "Măng tây xanh", "Măng tây trắng",
    "Cà chua Cherry", "Cà chua đen", "Bí ngòi vàng", "Bí ngòi xanh", "Củ dền đỏ",
    "Hoa chuối", "Hoa điên điển", "Rau tiến vua", "Bông thiên lý", "Rau càng cua",
    "Trái Sake", "Khoai môn cao", "Khoai mỡ", "Củ năng", "Củ sen",
    "Sầu riêng Ri6", "Sầu riêng Musang King", "Xoài Cát Hòa Lộc", "Vú sữa Lò Rèn", "Bưởi Da Xanh",
    "Dưa lưới Nhật", "Dưa hấu không hạt", "Lựu đỏ Ấn Độ", "Cherry Mỹ", "Nho mẫu đơn Shine Muscat",
    "Dâu tây Mộc Châu", "Dâu tây Hàn Quốc", "Việt quất New Zealand", "Mâm xôi đen", "Nam việt quất",
    "Chanh leo", "Cam sành", "Cam vàng Navel", "Quýt đường", "Bưởi Năm Roi",
    "Bột than tre", "Bột trà xanh Matcha Uji", "Bột Cacao nguyên chất", "Socola đen 70%", "Socola trắng",
    "Đường phèn", "Đường thốt nốt", "Mật mía", "Đường dừa", "Mật agave",
    "Nước mắm Phú Quốc 40 độ đạm", "Nước mắm cá cơm", "Nước tương Nhật xì dầu", "Tương ớt Sriracha", "Tương đen Hoisin",
    "Sốt Mayonnaise Nhật", "Sốt Teriyaki", "Sốt BBQ", "Sốt chua ngọt", "Sốt mè rang",
    "Sốt Pesto", "Sốt cà chua Marinara", "Sốt kem Nấm", "Sốt tiêu đen", "Sốt phô mai",
    "Rượu vang đỏ", "Rượu vang trắng", "Rượu Sake", "Rượu Soju", "Rượu Mirin",
    "Hành Baro", "Tỏi đen", "Tỏi cô đơn Lý Sơn", "Hành phi", "Tỏi phi",
    "Tiêu sọ Phú Quốc", "Tiêu xanh", "Ớt bột Hàn Quốc", "Ớt sừng trâu", "Ớt hiểm",
    "Bột ngọt Ajinomoto", "Hạt nêm thịt thăn", "Hạt nêm nấm hương", "Muối hồng Himalaya", "Muối Tây Ninh",
    // 50 more abstract ingredients...
    "Bột Gelatin", "Bột Agar", "Baking Soda", "Baking Powder", "Men nở (Yeast)",
    "Tinh chất Vani", "Lá Gelatin", "Màu thực phẩm tự nhiên", "Hoa đậu biếc", "Lá cẩm",
    "Lá dứa", "Lá gai", "Gấc", "Lá giang", "Lá me", 
    "Sả băm", "Gừng xắt lát", "Riềng", "Nghệ tươi", "Tinh bột nghệ"
];

const currentIngredientNames = new Set(ingredients.map(i => i.name));
let addedIng = 0;
newIngredients.forEach(name => {
    if (!currentIngredientNames.has(name)) {
        ingredients.push({ name: name });
        addedIng++;
    }
});
fs.writeFileSync(ingredientsPath, JSON.stringify(ingredients, null, 2));


// 2. EXPAND TAGS
let tags = JSON.parse(fs.readFileSync(tagsPath, 'utf8'));
const newTags = [
    "Dành cho người sành ăn", "Món ăn đường phố 5 sao", "Hương vị tuổi thơ", "Ẩm thực hoàng gia",
    "Món ăn chữa lành (Comfort food)", "Bữa tiệc BBQ", "Buffet gia đình", "Tráng miệng hảo hạng",
    "Đồ uống sáng tạo", "Cà phê đặc sản", "Món chay Fusion", "Thực đơn Ayurvedic",
    "Phục hồi chấn thương", "Tốt cho não bộ", "Làm đẹp da", "Chống lão hóa",
    "Tốt cho thị lực", "Giàu kẽm", "Giàu sắt", "Tăng cường sinh lý",
    "Phát triển chiều cao", "Bữa ăn học đường", "Thực đơn Omad", "Nhịn ăn gián đoạn (IF)",
    "Thực đơn Carnivore", "Thực đơn Pescatarian", "Thực đơn Flexitarian", "Món ăn dã ngoại",
    "Món nhậu lai rai", "Đồ ăn vặt văn phòng"
];

const currentTagNames = new Set(tags.map(t => t.name));
let addedTags = 0;
newTags.forEach(name => {
    if (!currentTagNames.has(name)) {
        tags.push({ name: name });
        addedTags++;
    }
});
fs.writeFileSync(tagsPath, JSON.stringify(tags, null, 2));


// 3. EXPAND CATEGORIES
let categories = JSON.parse(fs.readFileSync(categoriesPath, 'utf8'));
const newCategories = [
    {
        name: "Ẩm thực Fusion (Giao thoa)",
        children: [{ name: "Á - Âu" }, { name: "Việt - Nhật" }, { name: "Thái - Việt" }]
    },
    {
        name: "Đồ nướng BBQ",
        children: [{ name: "Nướng Hàn Quốc" }, { name: "Nướng Churrasco (Brazil)" }, { name: "Nướng BBQ Mỹ" }]
    },
    {
        name: "Món ăn eat clean nâng cao",
        children: [{ name: "Buddha Bowl" }, { name: "Poke Bowl" }, { name: "Açai Bowl" }]
    },
    {
        name: "Lẩu đặc biệt",
        children: [{ name: "Lẩu thái Tomyum" }, { name: "Lẩu Tứ Xuyên" }, { name: "Lẩu nấm Ashima" }, { name: "Lẩu bò nhúng dấm" }]
    },
    {
        name: "Pizza & Pasta",
        children: [{ name: "Pizza đế mỏng" }, { name: "Pizza đế dày" }, { name: "Mì ý sốt kem" }, { name: "Mì ý sốt cà" }]
    },
    {
        name: "Sushi & Sashimi",
        children: [{ name: "Nigiri" }, { name: "Maki" }, { name: "Sashimi cá hồi" }, { name: "Sashimi tổng hợp" }]
    },
    {
        name: "Bánh truyền thống Việt Nam",
        children: [{ name: "Bánh xèo" }, { name: "Bánh khọt" }, { name: "Bánh chưng/Bánh tét" }, { name: "Bánh da lợn" }]
    }
];

// Append categories (simplified check)
let addedCats = 0;
const currentCatNames = new Set(categories.map(c => c.name));
newCategories.forEach(cat => {
    if (!currentCatNames.has(cat.name)) {
        categories.push(cat);
        addedCats++;
    }
});
fs.writeFileSync(categoriesPath, JSON.stringify(categories, null, 2));

console.log(`Successfully added:`);
console.log(`- ${addedIng} new Ingredients (Total: ${ingredients.length})`);
console.log(`- ${addedTags} new Tags (Total: ${tags.length})`);
console.log(`- ${addedCats} new Root Categories (Total: ${categories.length})`);
