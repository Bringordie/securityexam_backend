package rest;

import entities.FriendRequest;
import entities.Friends;
import entities.User;
import entities.Role;
import entities.UserPosts;
import facades.UserFacade;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.parsing.Parser;
import java.net.URI;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.core.UriBuilder;
import jdk.nashorn.internal.ir.annotations.Ignore;
import mongodb.MongoConnection;
import mongodb.MongoFailedLogin;
import net.minidev.json.JSONObject;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.EMF_Creator;

/**
 *
 * @author Frederik Braagaard
 */
public class LoginEndpointTest {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_URL = "http://localhost/api";

    static final URI BASE_URI = UriBuilder.fromUri(SERVER_URL).port(SERVER_PORT).build();
    private static HttpServer httpServer;
    private static EntityManagerFactory emf;
    private static MongoConnection mongo;
    private static MongoFailedLogin mongoLogin;

    private static UserFacade facade;

    private User u1, u2, u3, u4;
    private Role r1, r2;
    private Friends f1, f2;
    private UserPosts up1, up2;
    private FriendRequest fr1, fr2;

    static HttpServer startServer() {
        ResourceConfig rc = ResourceConfig.forApplication(new ApplicationConfig());
        return GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc);
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @BeforeAll
    public static void setUpClass() {
        //This method must be called before you request the EntityManagerFactory
        EMF_Creator.startREST_TestWithDB();
        emf = EMF_Creator.createEntityManagerFactory(EMF_Creator.DbSelector.TEST, EMF_Creator.Strategy.CREATE);
        facade = UserFacade.getUserFacade(emf);
        facade.serverStatus = false;
        mongo.loggingStatus = false;
        mongoLogin.loggingStatus = false;

        httpServer = startServer();
        //Setup RestAssured
        RestAssured.baseURI = SERVER_URL;
        RestAssured.port = SERVER_PORT;
        RestAssured.defaultParser = Parser.JSON;
    }

    @AfterAll
    public static void closeTestServer() {
        //Don't forget this, if you called its counterpart in @BeforeAll
        EMF_Creator.endREST_TestWithDB();
        httpServer.shutdownNow();
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @BeforeEach
    public void setUp() {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            em.createNamedQuery("User.deleteAllRows").executeUpdate();
            em.createNamedQuery("UserPosts.deleteAllRows").executeUpdate();
            em.createNamedQuery("Friends.deleteAllRows").executeUpdate();
            em.createNamedQuery("FriendRequest.deleteAllRows").executeUpdate();
            em.createNamedQuery("Role.deleteAllRows").executeUpdate();

            r1 = new Role("user");
            r2 = new Role("admin");
            em.persist(r1);
            em.persist(r2);
            em.getTransaction().commit();

            em.getTransaction().begin();
            u1 = new User("User user", "user", "test", "where I was born", UUID.randomUUID().toString());
            u1.addRole(r1);
            u2 = new User("User2 user", "user2", "test", "where I went to school", UUID.randomUUID().toString());
            u2.addRole(r1);
            u3 = new User("User3 user", "user3", "test", "where I first traveled to", UUID.randomUUID().toString());
            u3.addRole(r1);
            u4 = new User("Admin admin", "admin", "test", "where I went to school", UUID.randomUUID().toString());
            u4.addRole(r2);

            em.getTransaction().commit();

            em.getTransaction().begin();
            em.persist(u1);
            em.persist(u2);
            em.persist(u3);
            em.persist(u4);

            em.getTransaction().commit();

            up1 = new UserPosts("This is a post made by a user");
            up2 = new UserPosts("This is a post made by a admin");

            u1.addUserPost(up1);
            u4.addUserPost(up2);

            // Out commented these as it's easier to have an overview of the friend tests.
//            f1 = new Friends(u4.getUserName());
//            f2 = new Friends(u1.getUserName());
//
//            u1.addToFriendList(f1);
//            u2.addToFriendList(f2);
            fr1 = new FriendRequest(u2.getId(), u2.getFullName(), u2.getProfilePicture());
            fr2 = new FriendRequest(u1.getId(), u1.getFullName(), u1.getProfilePicture());

            u1.addFriendRequest(fr1);
            u3.addFriendRequest(fr2);

            em.getTransaction().begin();
            em.persist(u1);
            em.persist(u2);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    //This is how we hold on to the token after login, similar to that a client must store the token somewhere
    public static String securityToken;

    //Utility method to login and set the returned securityToken
    /**
     *
     * @author Frederik Braagaard
     */
    public static void loginUser(String username, String password) {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);
        securityToken = given()
                .contentType("application/json")
                .body(json)
                .when().post("/login")
                .then()
                .extract().path("token");
        System.out.println("TOKEN ---> " + securityToken);
    }

    /**
     *
     * @author Frederik Braagaard
     */
    //Some logic still needs to be implemented
    @Ignore
    public static void loginAdmin(String username, String password) {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);
        securityToken = given()
                .contentType("application/json")
                .body(json)
                .when().post("/login/admin")
                .then()
                .extract().path("token");
        System.out.println("TOKEN ---> " + securityToken);
    }

    private void logOut() {
        securityToken = null;
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @Test
    public void userNotAuthenticatedTest() {
        System.out.println("Testing is server UP");
        JSONObject obj = new JSONObject();
        obj.put("username", "user123");
        obj.put("password", "password");

        given().contentType("application/json")
                .body(obj).when().post("/login")
                .then().statusCode(401);
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @Test
    public void successfullLoginTest() {
        System.out.println("Testing is server UP");
        JSONObject obj = new JSONObject();
        obj.put("username", u1.getUserName());
        obj.put("password", "test");

        given().contentType("application/json")
                .body(obj).when().post("/login")
                .then().assertThat().statusCode(200);
    }

    @Test
    public void testLoginFunctionTest() {
        loginUser(u1.getUserName(), "test");
        assertNotNull(securityToken != null);
        System.out.println("The token is NOT NULL and it is: " + securityToken);
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @Test
    public void resetPasswordPass() {

        JSONObject obj = new JSONObject();
        obj.put("username", u1.getUserName());
        obj.put("secret", "where I was born");
        obj.put("newpassword", "new secure password");

        given() //include object in body
                .contentType("application/json")
                .body(obj)
                .when().put("/login/reset/password").then() //put REQUEST
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode());
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @Test
    public void resetPasswordFail() {
        JSONObject obj = new JSONObject();
        obj.put("username", "not_valid");
        obj.put("secret", "where I was born");
        obj.put("newpassword", "newsecure");

        given() //include object in body
                .contentType("application/json")
                .body(obj)
                .when().put("/login/reset/password").then() //put REQUEST
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED_401.getStatusCode());
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @Test
    public void resetPasswordFailAdmin() {
        JSONObject obj = new JSONObject();
        obj.put("username", u4.getUserName());
        obj.put("secret", "where I went to school");
        obj.put("newpassword", "newsecure");

        given() //include object in body
                .contentType("application/json")
                .body(obj)
                .when().put("/login/reset/password").then() //put REQUEST
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED_401.getStatusCode());
    }

}
