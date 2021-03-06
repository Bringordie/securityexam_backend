package entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author Frederik Braagaard
 */
@Entity
@NamedQuery(name = "User.deleteAllRows", query = "DELETE from User")
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int id;
    //@Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "user_name", length = 25)
    private String userName;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "user_pass")
    private String userPass;
    @Column(name = "full_name")
    private String fullName;
    @JoinTable(name = "user_roles", joinColumns = {
        @JoinColumn(name = "user_id", referencedColumnName = "user_id")}, inverseJoinColumns = {
        @JoinColumn(name = "role_name", referencedColumnName = "role_name")})
    private Role role;
    // https://docs.oracle.com/javase/10/docs/api/java/util/UUID.html
    // https://www.callicoder.com/distributed-unique-id-sequence-number-generator/
    @Column(name = "profile_picture")
    private String profilePicture;
//    @JoinTable(name = "user_friends", joinColumns = {
//        @JoinColumn(name = "user_id", referencedColumnName = "user_id")}, inverseJoinColumns = {
//        @JoinColumn(name = "user_friend", referencedColumnName = "user_friend")})
    @ManyToMany(cascade = {CascadeType.PERSIST})
    private List<Friends> friendList = new ArrayList();
//    @JoinTable(name = "user_posts", joinColumns = {
//        @JoinColumn(name = "user_id", referencedColumnName = "user_id")}, inverseJoinColumns = {
//        @JoinColumn(name = "user_post", referencedColumnName = "user_post"),
//        @JoinColumn(name = "post_date", referencedColumnName = "post_date")})
    @ManyToMany(cascade = {CascadeType.PERSIST})
    private List<UserPosts> userPosts = new ArrayList();
//    @JoinTable(name = "user_friend_requests", joinColumns = {
//        @JoinColumn(name = "user_id", referencedColumnName = "user_id")}, inverseJoinColumns = {
//        @JoinColumn(name = "requested_friend", referencedColumnName = "requested_friend"),
//        @JoinColumn(name = "full_name", referencedColumnName = "full_name")})/*, 
//        @JoinColumn(name = "picture_url", referencedColumnName = "picture_url")})*/
    @ManyToMany(cascade = {CascadeType.PERSIST})
    private List<FriendRequest> friendRequests = new ArrayList();
    @Column(name = "secret_password")
    private String secretAnswer;

    public User() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public User(String fullName, String userName, String userPass, String secretAnswer, String profilePicture) {
        this.fullName = fullName;
        this.userName = userName;
        this.userPass = BCrypt.hashpw(userPass, BCrypt.gensalt());
        this.profilePicture = profilePicture;
        this.secretAnswer = BCrypt.hashpw(secretAnswer, BCrypt.gensalt());
    }

    public User(String fullName, String userName, String userPass, String secretAnswer) {
        this.fullName = fullName;
        this.userName = userName;
        this.userPass = BCrypt.hashpw(userPass, BCrypt.gensalt());
        this.secretAnswer = BCrypt.hashpw(secretAnswer, BCrypt.gensalt());
    }

    public void addFriendRequest(FriendRequest friendRequest) {
        this.friendRequests.add(friendRequest);
    }

    public void addUserPost(UserPosts userPost) {
        this.userPosts.add(userPost);
    }

    public List<UserPosts> getUserPosts() {
        return userPosts;
    }

    public void addToFriendList(Friends user) {
        this.friendList.add(user);
    }

    public List<Friends> getFriendList() {
        return friendList;
    }

    public boolean verifyPassword(String pw) {
        return BCrypt.checkpw(pw, this.userPass);
    }

    public boolean verifySecretAnswer(String secretAnswer) {
        return BCrypt.checkpw(secretAnswer, this.secretAnswer);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPass() {
        return this.userPass;
    }

    public void setUserPass(String userPass) {
        this.userPass = BCrypt.hashpw(userPass, BCrypt.gensalt());
    }

    public String getSecretAnswer() {
        return secretAnswer;
    }

    public void setSecretAnswer(String secretAnswer) {
        this.secretAnswer = secretAnswer;
    }
    
    

    public void setRoleList(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void addRole(Role userRole) {
        this.role = userRole;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<FriendRequest> getFriendRequests() {
        return friendRequests;
    }
    
    public Boolean deleteSpecificFriendRequest(int requestUserNameID) {
        Boolean response = false;
        for (FriendRequest friendRequest : friendRequests) {
        if (friendRequest.getRequestUsernameID() == requestUserNameID) {
            friendRequests.remove(friendRequest);
            response = true;
            break;
        }
    }
        return response;
    }
    
    public Boolean removeFriend(int requestUserNameID) {
        Boolean response = false;
        for (Friends friendRemove : friendList) {
        if (friendRemove.getFriendUsernameID() == requestUserNameID) {
            friendList.remove(friendRemove);
            response = true;
            break;
        }
    }
        return response;
    }
    
    public Boolean validateSpecificFriendRequest(int requestUserNameID) {
        Boolean response = false;
        for (FriendRequest friendRequest : friendRequests) {
        if (friendRequest.getRequestUsernameID() == requestUserNameID) {
            response = true;
            break;
        }
    }
        return response;
    }

    @Override
    public String toString() {
        return "User{" + "userName=" + userName + ", userPass=" + userPass + ", fullName=" + fullName + ", role=" + role + ", profilePicture=" + profilePicture + ", friendList=" + friendList + ", userPosts=" + userPosts + ", friendRequests=" + friendRequests + ", secretAnswer=" + secretAnswer + '}';
    }

}
