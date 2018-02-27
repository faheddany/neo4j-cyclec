package com.isae.neo4j;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Hello world!
 *
 */
public class App {
	public enum Labels implements Label {
		USER, RESTAURANT;
	}

	public enum RelationshipTypes implements RelationshipType {
		IS_FRIEND_OF, HAS_TRIED;
	}

	public static void main(String[] args) {

		// Specifier l'emplacement de la base de données
		GraphDatabaseService graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(new File(
				"C:\\Users\\daf\\AppData\\Roaming\\Neo4j Desktop\\Application\\neo4jDatabases\\database-aa0a2bbb-e1b7-4230-90b7-50c0341a00de\\installation-3.3.2\\data\\databases\\graph.db"));

		try (Transaction tx = graphDB.beginTx()) {
			deleteOldData(graphDB);

			// créer des nodes de types personne et avec des propriétés name
			Node steve = graphDB.createNode(Labels.USER);
			steve.setProperty("name", "Steve");

			Node linda = graphDB.createNode(Labels.USER);
			linda.setProperty("name", "Linda");

			Node michael = graphDB.createNode(Labels.USER);
			michael.setProperty("name", "Michael");

			Node rebecca = graphDB.createNode(Labels.USER);
			rebecca.setProperty("name", "Rebecca");

			Node dany = graphDB.createNode(Labels.USER);
			dany.setProperty("name", "Dany");

			// créer des relations de types ami entre les personnes
			steve.createRelationshipTo(michael, RelationshipTypes.IS_FRIEND_OF);
			steve.createRelationshipTo(rebecca, RelationshipTypes.IS_FRIEND_OF);
			steve.createRelationshipTo(linda, RelationshipTypes.IS_FRIEND_OF);
			dany.createRelationshipTo(rebecca, RelationshipTypes.IS_FRIEND_OF);

			// créer des nodes de types restaurants et avec des propriétés name
			Node mrBrown = graphDB.createNode(Labels.RESTAURANT);
			mrBrown.setProperty("name", "Mr. Brown");
			Node zwz = graphDB.createNode(Labels.RESTAURANT);
			zwz.setProperty("name", "Zaatar w Zeit");
			Node crepaway = graphDB.createNode(Labels.RESTAURANT);
			crepaway.setProperty("name", "Crepaway");
			Node roadster = graphDB.createNode(Labels.RESTAURANT);
			roadster.setProperty("name", "Roadster");

			// créer des relations de type essayer entre personnes et
			// restaurants et possédant des propriétés rating
			triedRestaurant(steve, zwz, 5);
			triedRestaurant(steve, mrBrown, 5);
			triedRestaurant(steve, crepaway, 4);
			triedRestaurant(rebecca, roadster, 5);
			triedRestaurant(rebecca, crepaway, 5);
			triedRestaurant(michael, roadster, 5);
			triedRestaurant(michael, mrBrown, 3);
			triedRestaurant(linda, mrBrown, 4);
			triedRestaurant(linda, roadster, 5);
			triedRestaurant(linda, zwz, 5);
			triedRestaurant(dany, zwz, 5);
			triedRestaurant(dany, mrBrown, 3);

			// Chercher tous les restaurants dans notre base de données
			ResourceIterator<Node> restaurants = graphDB.findNodes(Labels.RESTAURANT);
			System.out.println("RESTAURANTS:");
			while (restaurants.hasNext()) {
				Node restaurant = restaurants.next();
				System.out.println("\t" + restaurant.getProperty("name"));
			}

			// Chercher toutes les personnes dans notre base de données
			ResourceIterator<Node> users = graphDB.findNodes(Labels.USER);
			System.out.println("\n\nPersonnes:");
			while (users.hasNext()) {
				Node user = users.next();
				System.out.println("\t" + user.getProperty("name"));
			}

			// Calculer l'évaluation moyenne des restaurants dans notre base de
			// données basés sur les notes que les utilisateurs ont donnés
			// précédemment
			restaurants = graphDB.findNodes(Labels.RESTAURANT);
			System.out.println("\n\nEvaluation des restaurants:");
			while (restaurants.hasNext()) {
				Node restaurant = restaurants.next();

				// Trouver toutes les relations HAS_TRIED et récupérer les
				// évaluations relatives
				Iterable<Relationship> relationships = restaurant.getRelationships(Direction.INCOMING,
						RelationshipTypes.HAS_TRIED);
				int totalStars = 0;
				int relationshipCount = 0;
				for (Relationship relationship : relationships) {
					Integer stars = (Integer) relationship.getProperty("stars");
					totalStars += stars;
					relationshipCount++;
				}
				System.out.println("\t" + restaurant.getProperty("name") + ", Les personnes qui l'ont essayé: "
						+ relationshipCount + ", Note moyenne: " + (float) totalStars / relationshipCount);
			}

			// Trouver tous les restaurants que les utilisateurs ont essayé
			// groupés par les personnes
			users = graphDB.findNodes(Labels.USER);
			System.out.println("\n\nPersonnes:");
			while (users.hasNext()) {
				Node user = users.next();
				System.out.print("\t" + user.getProperty("name") + " a essayé ");
				for (Relationship relationship : user.getRelationships(RelationshipTypes.HAS_TRIED)) {
					Node restaurant = relationship.getOtherNode(user);
					System.out.print(restaurant.getProperty("name") + ", ");
				}
				System.out.println();
			}

			// Recommender à Dany tous les restaurants qui n'a pas essayé et que
			// ses copains ont et donnés des notes plus grand à 3
			Node danyNode = graphDB.findNode(Labels.USER, "name", "Dany");

			// Trouver tous les restaurants que Dany a essayé
			Set<String> danysRestaurants = new HashSet<String>();
			for (Relationship relationship : danyNode.getRelationships(Direction.OUTGOING,
					RelationshipTypes.HAS_TRIED)) {
				danysRestaurants.add((String) relationship.getOtherNode(danyNode).getProperty("name"));
			}

			// Trouver tous les amis de Dany
			Set<String> friendsRestaurants = new HashSet<String>();
			for (Relationship relationship : danyNode.getRelationships(RelationshipTypes.IS_FRIEND_OF)) {
				Node friend = relationship.getOtherNode(danyNode);

				// Trouver tous les restaurants que les amis de Dany ont essayé
				for (Relationship relationship1 : friend.getRelationships(Direction.OUTGOING,
						RelationshipTypes.HAS_TRIED)) {
					// Récupérer les notes et les inclure seulement si la note
					// etait plus grand que 3
					if ((Integer) relationship1.getProperty("stars") > 3) {
						// Ajouter le restaurant au set restaurants
						friendsRestaurants.add((String) relationship1.getOtherNode(friend).getProperty("name"));
					}
				}
			}
			// Enlever tous les restaurants que Dany a essayé du set deja
			// recupéré
			friendsRestaurants.removeAll(danysRestaurants);

			// recommender à Dany tous les restaurants qu'il n'a pas essayé mais
			// ses copains ont et ont donné des notes plus grande à 3
			System.out.println(
					"\n\nRestaurants que Dany n'a pas essayé, mais ses amis ont et donné une note de 4 ou plus sont: ");
			for (String restaurant : friendsRestaurants) {
				System.out.println("\t" + restaurant);
			}
			tx.success();
		}

		graphDB.shutdown();

	}

	private static void deleteOldData(GraphDatabaseService graphDB) {
		try {
			// Find all users
			ResourceIterator<Node> usersToDelete = graphDB.findNodes(Labels.USER);
			while (usersToDelete.hasNext()) {
				Node userToDelete = usersToDelete.next();
				userToDelete.delete();
			}

			// Find all restaurants
			ResourceIterator<Node> restaurantsToDelete = graphDB.findNodes(Labels.RESTAURANT);
			while (restaurantsToDelete.hasNext()) {
				Node restaurantToDelete = restaurantsToDelete.next();
				restaurantToDelete.delete();
			}

			// Find all relationships
			ResourceIterable<Relationship> relationshipsToDelete = graphDB.getAllRelationships();
			for (Relationship relationship : relationshipsToDelete) {
				relationship.delete();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static Relationship triedRestaurant(Node user, Node restaurant, int stars) {
		Relationship relationship = user.createRelationshipTo(restaurant, RelationshipTypes.HAS_TRIED);
		relationship.setProperty("stars", stars);
		return relationship;
	}
}
