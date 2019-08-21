package com.a6raywa1cher.pasttyspring.rest;

import com.a6raywa1cher.pasttyspring.configs.security.HashingService;
import com.a6raywa1cher.pasttyspring.dao.interfaces.UserService;
import com.a6raywa1cher.pasttyspring.models.User;
import com.a6raywa1cher.pasttyspring.models.enums.Role;
import com.a6raywa1cher.pasttyspring.rest.dto.exceptions.NoEnoughRightsForChangeException;
import com.a6raywa1cher.pasttyspring.rest.dto.exceptions.UserWithProvidedUsernameExistsException;
import com.a6raywa1cher.pasttyspring.rest.dto.mirror.UserMirror;
import com.a6raywa1cher.pasttyspring.rest.dto.request.ChangeUserRoleDTO;
import com.a6raywa1cher.pasttyspring.rest.dto.request.UserRegistrationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {
	private UserService userService;
	private HashingService hashingService;

	@Autowired
	public UserController(UserService userService, HashingService hashingService) {
		this.userService = userService;
		this.hashingService = hashingService;
	}

	@PostMapping("/reg")
	@Transactional
	public ResponseEntity<UserMirror> register(@RequestBody @Valid UserRegistrationDTO dto) {
		if (userService.findByUsername(dto.getUsername()).isPresent()) {
			throw new UserWithProvidedUsernameExistsException();
		}
		User user = new User();
		user.setUsername(dto.getUsername());
		user.setPassword(hashingService.hash(dto.getPassword()));
		user.setRole(userService.isAnyoneElseRegistered() ? Role.USER : Role.ADMIN);
		return ResponseEntity.ok(UserMirror.convert(userService.save(user)));
	}

	@PostMapping("/{username}/role")
	@Transactional
	public ResponseEntity<UserMirror> changeRole(@PathVariable("username")
	                                             @Pattern(regexp = ControllerValidations.USERNAME_REGEX)
	                                             @Valid String username,
	                                             @RequestBody @Valid ChangeUserRoleDTO dto,
	                                             Authentication authentication) {
		Optional<User> optionalUser = userService.findByUsername(username);
		if (optionalUser.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		User targetUser = optionalUser.get();
		User requester = (User) authentication.getPrincipal();
		Role requesterRole = requester.getRole();
		Role targetUserRole = targetUser.getRole();
		Role dtoRole = dto.getRole();
		if (!targetUser.equals(requester) && requesterRole != Role.ADMIN) {
			throw new NoEnoughRightsForChangeException();
		} else if (targetUser.equals(requester) && !targetUserRole.getTree().contains(dtoRole)) {
			throw new NoEnoughRightsForChangeException();
		}
		targetUser.setRole(dtoRole);
		return ResponseEntity.ok(UserMirror.convert(userService.save(targetUser)));
	}

	@GetMapping("/{username}")
	@Transactional
	public ResponseEntity<UserMirror> getUser(@PathVariable String username) {
		Optional<User> optionalUser = userService.findByUsername(username);
		if (optionalUser.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(UserMirror.convert(optionalUser.get()));
	}
}
