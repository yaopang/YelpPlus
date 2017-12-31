package db.mongodb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.UpdateOptions;
import static com.mongodb.client.model.Filters.eq;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.YelpAPI;

public class MongoDBConnection implements DBConnection{
	private static MongoDBConnection instance;
	
	public static DBConnection getInstance() {
		if (instance == null) {
			instance = new MongoDBConnection();
			return instance;
		}
		return instance;
	}
	
	private MongoClient mongoClient;
	private MongoDatabase db;
	
	private MongoDBConnection() {
		//Connects to local mongodb server
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	}
	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}
	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$push", new Document("favorite", new Document("$each", itemIds))));
		
		/*
		 * db.user.update({ user_id : 'user_id'}, { $push: {favorite : {$each: itemIds}}})  
		 */
	}
	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pullAll", new Document("favorite", itemIds)));
		
		/*
		 * db.user.update({ user_id : 'user_id'}, { $pullAll: {favorite : itemIds}})  
		 */
	}
	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		Set<String> favoriteItems = new HashSet<String>();
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if (iterable.first().containsKey("favorite")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("favorite");
			favoriteItems.addAll(list);
		}
		return favoriteItems;
	}
	@Override
	public Set<Item> getFavoriteItems(String userId) {
		Set<String> itemIds = getFavoriteItemIds(userId);
		Set<Item> favoriteItems = new HashSet<>();
		for (String itemId : itemIds) {
			FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
			Document doc = iterable.first();
			ItemBuilder builder = new ItemBuilder();
			builder.setItemId(doc.getString("item_id"));
			builder.setName(doc.getString("name"));
			builder.setCity(doc.getString("city"));
			builder.setState(doc.getString("state"));
			builder.setCountry(doc.getString("country"));
			builder.setZipcode(doc.getString("zipcode"));
			builder.setRating(doc.getDouble("rating"));
			builder.setAddress(doc.getString("address"));
			builder.setLatitude(doc.getDouble("latitude"));
			builder.setLongitude(doc.getDouble("longitude"));
			builder.setDescription(doc.getString("description"));
			builder.setSnippet(doc.getString("snippet"));
			builder.setSnippetUrl(doc.getString("snippet_url"));
			builder.setImageUrl(doc.getString("image_url"));
			builder.setUrl(doc.getString("url"));
			favoriteItems.add(builder.build());
		}
		return favoriteItems;
	}
	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));

		if (iterable.first().containsKey("categories")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("categories");
			categories.addAll(list);
		}
		return categories;
	}
	@Override
	public List<Item> searchItems(String userId, double lat, double lon, String term) {
		//Connect to external API
		YelpAPI api =  new YelpAPI();
		List<Item> items = api.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}
	@Override
	public void saveItem(Item item) {
		UpdateOptions options = new UpdateOptions().upsert(true);
		// First document is a condition, second document is an operation, the third one is a real MongoDB document. 
		db.getCollection("items").updateOne(new Document().append("item_id", item.getItemId()),
				new Document("$set",
						new Document().append("item_id", item.getItemId()).append("name", item.getName())
								.append("city", item.getCity()).append("state", item.getState())
								.append("country", item.getCountry()).append("zip_code", item.getZipcode())
								.append("rating", item.getRating()).append("address", item.getAddress())
								.append("latitude", item.getLatitude()).append("longitude", item.getLongitude())
								.append("description", item.getDescription()).append("snippet", item.getSnippet())
								.append("snippet_url", item.getSnippetUrl()).append("image_url", item.getImageUrl())
								.append("url", item.getUrl()).append("categories", item.getCategories())),
				options);
		
		/*
		 *  db.items.update({item_id : 'item_id'}, {$set: {item_id : 'item_id'}, {state : 'state'}, 
		 *  ....
		 *  ....
		 *  , {upsert: true})
		 */

		
	}
}
