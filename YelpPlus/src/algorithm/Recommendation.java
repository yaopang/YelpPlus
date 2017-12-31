package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import db.mysql.MySQLConnection;
import entity.Item;

public class Recommendation {
	public List<Item> recommendItems(String userId, double lat, double lon) {
		//MySQLConnection conn =  MySQLConnection.getInstance();
		DBConnection conn = DBConnectionFactory.getDBConnection();
		Set<String> favoriteItems = conn.getFavoriteItemIds(userId);
		
		Set<String> allCategories = new HashSet<>();
		for(String item : favoriteItems) {
			allCategories.addAll(conn.getCategories(item));
		}
		allCategories.remove("Undefined");  // tune category set
	    if (allCategories.isEmpty()) {
	    	allCategories.add("");
	    }
	    
	    Set<Item> recommendedItems = new HashSet<>();
	    for(String category : allCategories) {
	    	List<Item> items = conn.searchItems(userId, lat, lon, category);
	    	recommendedItems.addAll(items);
	    }
	    
	    //use list and prepare for ranking
	    List<Item> selectedItems = new ArrayList<>();
	    for(Item item: recommendedItems) {
	    	if(!favoriteItems.contains(item.getItemId())) {
	    		selectedItems.add(item);
	    	}
	    }
	    
	    
	    //rank selected restaurants based on distance
	    Collections.sort(selectedItems, new Comparator<Item>() {
			@Override
			public int compare(Item item1, Item item2) {
				double d1 = getDistance(item1.getLatitude(), item1.getLongitude(), lat, lon) ;
				double d2 = getDistance(item2.getLatitude(), item2.getLongitude(), lat, lon);
				return Double.compare(d1, d2);
			
			}
	    });
	    return selectedItems;
	}
	
	private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		double dlon = lon2 - lon1;
		double dlat = lat2 - lat1;
		double a = Math.sin(dlat / 2 / 180 * Math.PI) * Math.sin(dlat / 2 / 180 * Math.PI)
		        + Math.cos(lat1 / 180 * Math.PI) * Math.cos(lat2 / 180 * Math.PI)
		            * Math.sin(dlon / 2 / 180 * Math.PI) * Math.sin(dlon / 2 / 180 * Math.PI);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		// Radius of earth in miles.
		double R = 3961;
		return R * c;
	}	
}
