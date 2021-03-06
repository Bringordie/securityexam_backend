package rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import dtos.user.UserDTO;
import entities.UserPosts;
import errorhandling.AuthenticationException;
import errorhandling.NoFriendsException;
import errorhandling.NotFoundException;
import facades.UserFacade;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import security.JWTAuthenticationFilter;
import security.UserPrincipal;
import utils.EMF_Creator;

/**
 *
 * @author Frederik Braagaard
 */
@Path("post")
public class PostResource {

    private static EntityManagerFactory EMF = EMF_Creator.createEntityManagerFactory(EMF_Creator.DbSelector.DEV, EMF_Creator.Strategy.CREATE);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final UserFacade FACADE = UserFacade.getUserFacade(EMF);

    @Context
    private UriInfo context;

    @Context
    SecurityContext securityContext;

    /**
     *
     * @author Frederik Braagaard
     */
    @GET
    @Path("/own")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPosts(@HeaderParam("x-access-token") String accessToken) throws ParseException, JOSEException, AuthenticationException, NotFoundException, IOException {
        JWTAuthenticationFilter authenticate = new JWTAuthenticationFilter();
        UserPrincipal userPrin;
        try {
            userPrin = authenticate.getUserPrincipalFromTokenIfValid(accessToken);
        } catch (JOSEException | AuthenticationException ex) {
            throw new WebApplicationException(ex.getMessage(), 401);
        }

        int username = userPrin.getNameID();
        List<UserPosts> response;
        response = FACADE.getPosts(username);
        if (response.isEmpty()) {
            throw new WebApplicationException("This user has no posts", 404);
        }

        return GSON.toJson(response);
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @GET
    @Path("/friends")
    @Produces(MediaType.APPLICATION_JSON)
    public String getFriendsPosts(@HeaderParam("x-access-token") String accessToken) throws ParseException, JOSEException, AuthenticationException, NotFoundException, NoFriendsException, IOException {
        JWTAuthenticationFilter authenticate = new JWTAuthenticationFilter();
        UserPrincipal userPrin;
        try {
            userPrin = authenticate.getUserPrincipalFromTokenIfValid(accessToken);
        } catch (JOSEException | AuthenticationException ex) {
            throw new WebApplicationException(ex.getMessage(), 401);
        }

        int usernameID = userPrin.getNameID();
        List<UserDTO> response;
        try {
            response = FACADE.friendPosts(usernameID);
        } catch (NoFriendsException ex) {
            throw new WebApplicationException("This user currently has no friends in their friendlist.", 404);
        }

        return GSON.toJson(response);
    }

    /**
     *
     * @author Frederik Braagaard
     */
    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createPost(String jsonString, @HeaderParam("x-access-token") String accessToken) throws ParseException, JOSEException, AuthenticationException, IOException {
        JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
        JWTAuthenticationFilter authenticate = new JWTAuthenticationFilter();
        UserPrincipal userPrin;
        try {
            userPrin = authenticate.getUserPrincipalFromTokenIfValid(accessToken);
        } catch (JOSEException | AuthenticationException ex) {
            throw new WebApplicationException(ex.getMessage(), 401);
        }

        int usernameID = userPrin.getNameID();
        String newPost = json.get("post").getAsString();

        Boolean response = FACADE.createPost(usernameID, newPost);
        if (!response) {
            throw new WebApplicationException("Something unexpected happened. Please try again later", 400);
        }

        return GSON.toJson("Post has successfully been created");
    }

}
