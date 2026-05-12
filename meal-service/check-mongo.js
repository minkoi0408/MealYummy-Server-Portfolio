const { MongoClient } = require('mongodb');

async function main() {
  const uri = "mongodb+srv://MealYummy_Sever_DB:jkVgfqQV55HHwI9a@cluster0.stq1pey.mongodb.net/meal-service?appName=Cluster0";
  const client = new MongoClient(uri);

  try {
    await client.connect();
    const db = client.db("meal-service");
    const meals = await db.collection("meals").find({}).toArray();
    
    meals.slice(0, 5).forEach(meal => {
      console.log(`Meal: ${meal.name}`);
      console.log(`Images: ${JSON.stringify(meal.images)}`);
      console.log('---');
    });
  } finally {
    await client.close();
  }
}

main().catch(console.dir);
