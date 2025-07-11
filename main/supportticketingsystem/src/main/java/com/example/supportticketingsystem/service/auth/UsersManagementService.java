package com.example.supportticketingsystem.service.auth;

import com.example.supportticketingsystem.dto.ReqRes;
import com.example.supportticketingsystem.dto.collection.OurUsers;
import com.example.supportticketingsystem.dto.request.ChangePasswordRequest;
import com.example.supportticketingsystem.repository.UsersRepo;
import jakarta.persistence.EntityExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UsersManagementService {

    private static final Logger logger = LoggerFactory.getLogger(UsersManagementService.class);

    @Autowired
    private UsersRepo usersRepo;
    @Autowired
    private JWTUtils jwtUtils;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public ReqRes register(ReqRes registrationRequest) {
        ReqRes resp = new ReqRes();

        try {
            // Check if user already exists
            Optional<OurUsers> existingUser = usersRepo.findByEmail(registrationRequest.getEmail());
            if (existingUser.isPresent()) {
                throw new EntityExistsException("User with email " + registrationRequest.getEmail() + " already exists");
            }

            OurUsers ourUser = new OurUsers();
            String email = registrationRequest.getEmail();
            ourUser.setEmail(email);
            ourUser.setCity(registrationRequest.getCity());
            ourUser.setName(registrationRequest.getName());
            ourUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));

            Set<String> roles = new HashSet<>(registrationRequest.getRoles());
            if (email.endsWith("@kore.com")) {
                if (roles.contains("ADMIN")) {
                    roles.add("ADMIN");
                } else {
                    roles.add("DEFAULT");
                }
                ourUser.setProductGroup(registrationRequest.getProductGroup());
            } else {
                if (roles.contains("ADMIN")) {
                    roles.add("ADMIN");
                } else {
                    roles.add("DEFAULT"); // Set default role if none is provided or role is not ADMIN
                }
            }

            ourUser.setRoles(roles);

            OurUsers ourUsersResult = usersRepo.save(ourUser);
            if (ourUsersResult.getId() > 0) {
                resp.setOurUsers((ourUsersResult));
                resp.setMessage("User Saved Successfully");
                resp.setStatusCode(200);
            }

        } catch (EntityExistsException e) {
            resp.setStatusCode(409); // Conflict
            resp.setError(e.getMessage());
        } catch (Exception e) {
            resp.setStatusCode(500);
            resp.setError(e.getMessage());
        }
        return resp;
    }
    public ReqRes login(ReqRes loginRequest) {
        ReqRes response = new ReqRes();
        try {
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                            loginRequest.getPassword()));
            var user = usersRepo.findByEmail(loginRequest.getEmail()).orElseThrow();
            var jwt = jwtUtils.generateToken(user);
            var refreshToken = jwtUtils.generateRefreshToken(new HashMap<>(), user);
            response.setStatusCode(200);
            response.setToken(jwt);
            response.setRoles(user.getRoles());
            System.out.println("ffffffff"+user.getRoles());
            response.setRefreshToken(refreshToken);
            response.setProductGroup(user.getProductGroup());
            response.setEmail(user.getEmail());
            response.setExpirationTime("24Hrs");
            response.setMessage("Successfully Logged In");

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }


    public ReqRes refreshToken(ReqRes refreshTokenReqiest){
        ReqRes response = new ReqRes();
        try{
            String ourEmail = jwtUtils.extractUsername(refreshTokenReqiest.getToken());
            OurUsers users = usersRepo.findByEmail(ourEmail).orElseThrow();
            if (jwtUtils.isTokenValid(refreshTokenReqiest.getToken(), users)) {
                var jwt = jwtUtils.generateToken(users);
                response.setStatusCode(200);
                response.setToken(jwt);
                response.setRefreshToken(refreshTokenReqiest.getToken());
                response.setExpirationTime("24Hr");
                response.setMessage("Successfully Refreshed Token");
            }
            response.setStatusCode(200);
            return response;

        }catch (Exception e){
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
            return response;
        }
    }


    public ReqRes getAllUsers() {
        ReqRes reqRes = new ReqRes();

        try {
            List<OurUsers> result = usersRepo.findAll();
            if (!result.isEmpty()) {
                reqRes.setOurUsersList(result);
                reqRes.setStatusCode(200);
                reqRes.setMessage("Successful");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("No users found");
            }
            return reqRes;
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred: " + e.getMessage());
            return reqRes;
        }
    }


    public ReqRes getUsersById(Integer id) {
        ReqRes reqRes = new ReqRes();
        try {
            OurUsers usersById = usersRepo.findById(id).orElseThrow(() -> new RuntimeException("User Not found"));
            reqRes.setOurUsers(usersById);
            reqRes.setStatusCode(200);
            reqRes.setMessage("Users with id '" + id + "' found successfully");
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred: " + e.getMessage());
        }
        return reqRes;
    }


    public ReqRes deleteUser(Integer userId) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                usersRepo.deleteById(userId);
                reqRes.setStatusCode(200);
                reqRes.setMessage("User deleted successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found for deletion");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while deleting user: " + e.getMessage());
        }
        return reqRes;
    }

    public ReqRes updateUser(Integer userId, OurUsers updatedUser) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers existingUser = userOptional.get();

                // Update only if fields are present
                if (updatedUser.getEmail() != null && !updatedUser.getEmail().isEmpty()) {
                    existingUser.setEmail(updatedUser.getEmail());
                }
                if (updatedUser.getName() != null && !updatedUser.getName().isEmpty()) {
                    existingUser.setName(updatedUser.getName());
                }
                if (updatedUser.getCity() != null && !updatedUser.getCity().isEmpty()) {
                    existingUser.setCity(updatedUser.getCity());
                }
                if (updatedUser.getRoles() != null && !updatedUser.getRoles().isEmpty()) {
                    existingUser.setRoles(updatedUser.getRoles());
                }

                // Check if password is present in the request
                if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                    // Encode the password and update it
                    existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                }

                OurUsers savedUser = usersRepo.save(existingUser);
                reqRes.setOurUsers(savedUser);
                reqRes.setStatusCode(200);
                reqRes.setMessage("User updated successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found for update");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while updating user: " + e.getMessage());
        }
        return reqRes;
    }



    public ReqRes getMyInfo(String email){
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findByEmail(email);
            if (userOptional.isPresent()) {
                reqRes.setOurUsers(userOptional.get());
                reqRes.setStatusCode(200);
                reqRes.setMessage("successful");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found for update");
            }

        }catch (Exception e){
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while getting user info: " + e.getMessage());
        }
        return reqRes;

    }

    public ReqRes setProductGroup(Integer userId, Set<String> productGroup) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers user = userOptional.get();

                // Replace existing product group with new product group
                user.setProductGroup(new HashSet<>(productGroup));

                usersRepo.save(user);
                reqRes.setOurUsers(user);
                reqRes.setStatusCode(200);
                reqRes.setMessage("Product group updated successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while updating product group: " + e.getMessage());
        }
        return reqRes;
    }


    public ReqRes setRole(Integer userId, Set<String> roles) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers user = userOptional.get();

                // Replace existing roles with new roles
                user.setRoles(new HashSet<>(roles));

                usersRepo.save(user);
                reqRes.setOurUsers(user);
                reqRes.setStatusCode(200);
                reqRes.setMessage("Roles updated successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while updating roles: " + e.getMessage());
        }
        return reqRes;
    }


    public ReqRes changePassword(Integer userId, ChangePasswordRequest changePasswordRequest) {
        ReqRes response = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers user = userOptional.get();

                // Validate current password
                if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
                    System.out.println(user.getPassword());
                    response.setStatusCode(400);
                    response.setMessage("Current password is incorrect");
                    return response;
                }

                // Validate new password (same logic as registration)
                String newPassword = changePasswordRequest.getNewPassword();
                if (newPassword == null || newPassword.isEmpty()) {
                    response.setStatusCode(400);
                    response.setMessage("New password cannot be empty");
                    return response;
                }

                // Validate new password confirmation
                String newPasswordConfirmation = changePasswordRequest.getNewPasswordConfirmation();
                if (!newPassword.equals(newPasswordConfirmation)) {
                    response.setStatusCode(400);
                    response.setMessage("New passwords do not match");
                    return response;
                }

                // Update password with encoded new password
                user.setPassword(passwordEncoder.encode(newPassword));
                usersRepo.save(user);

                response.setStatusCode(200);
                response.setMessage("Password changed successfully");
            } else {
                response.setStatusCode(404);
                response.setMessage("User not found");
            }
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error occurred while changing password: " + e.getMessage());
        }
        return response;
    }
}
