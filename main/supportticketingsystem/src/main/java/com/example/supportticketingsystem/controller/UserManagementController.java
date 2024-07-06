package com.example.supportticketingsystem.controller;

import com.example.supportticketingsystem.dto.ReqRes;
import com.example.supportticketingsystem.dto.collection.OurUsers;
import com.example.supportticketingsystem.dto.request.ChangePasswordRequest;
import com.example.supportticketingsystem.service.auth.UsersManagementService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserManagementController {
    @Autowired
    private UsersManagementService usersManagementService;

    @PostMapping("/auth/register")
    public ResponseEntity<ReqRes> regeister(@RequestBody ReqRes reg){
        return ResponseEntity.ok(usersManagementService.register(reg));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ReqRes> login(@RequestBody ReqRes req){
        return ResponseEntity.ok(usersManagementService.login(req));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ReqRes> refreshToken(@RequestBody ReqRes req){
        return ResponseEntity.ok(usersManagementService.refreshToken(req));
    }

    @GetMapping("/admin/get-all-users")
    public ResponseEntity<ReqRes> getAllUsers(){
        return ResponseEntity.ok(usersManagementService.getAllUsers());

    }

    @GetMapping("/admin/get-users/{userId}")
    public ResponseEntity<ReqRes> getUSerByID(@PathVariable Integer userId){
        return ResponseEntity.ok(usersManagementService.getUsersById(userId));

    }

    @PutMapping("/admin/update/{userId}")
    public ResponseEntity<ReqRes> updateUser(@PathVariable Integer userId, @RequestBody OurUsers reqres){
        return ResponseEntity.ok(usersManagementService.updateUser(userId, reqres));
    }

    @GetMapping("/adminuser/get-profile")
    public ResponseEntity<ReqRes> getMyProfile(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        ReqRes response = usersManagementService.getMyInfo(email);
        return  ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/admin/delete/{userId}")
    public ResponseEntity<ReqRes> deleteUSer(@PathVariable Integer userId){
        return ResponseEntity.ok(usersManagementService.deleteUser(userId));
    }


    @PutMapping("/admin/set-product-group/{userId}")
    public ResponseEntity<ReqRes> setProductGroup(@PathVariable Integer userId, @RequestBody ReqRes req) {
        return ResponseEntity.ok(usersManagementService.setProductGroup(userId, req.getProductGroup()));
    }

    @PutMapping("/admin/set-role/{userId}")
    public ResponseEntity<ReqRes> setRole(@PathVariable Integer userId, @RequestBody ReqRes req) {
        return ResponseEntity.ok(usersManagementService.setRole(userId, req.getRoles()));
    }

    @PutMapping("/auth/change-password/{userId}")
    public ResponseEntity<ReqRes> changePassword(@PathVariable Integer userId, @RequestBody ChangePasswordRequest changePasswordRequest) {
        return ResponseEntity.ok(usersManagementService.changePassword(userId, changePasswordRequest));
    }
}
