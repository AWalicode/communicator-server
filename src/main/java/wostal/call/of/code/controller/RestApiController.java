package wostal.call.of.code.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import wostal.call.of.code.abstracts.AbstractException;
import wostal.call.of.code.dto.AuthorizeDto;
import wostal.call.of.code.dto.ConversationDto;
import wostal.call.of.code.dto.Response;
import wostal.call.of.code.dto.UserWithoutPassword;
import wostal.call.of.code.entity.Conversation;
import wostal.call.of.code.entity.User;
import wostal.call.of.code.service.ConversationService;
import wostal.call.of.code.service.ExceptionHandler;
import wostal.call.of.code.service.MessageService;
import wostal.call.of.code.service.MyUserPrincipal;
import wostal.call.of.code.service.UserService;

@RestController
@Validated
public class RestApiController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private ConversationService conversationService;

	@Autowired
	private MessageService messageService;
	
	
	@PostMapping(value = { "/authorize" })
	public Response authotize(@RequestBody AuthorizeDto authorizeDto) {
		Response response = new Response();
		try {
			User user = userService.userExist(authorizeDto.nick, authorizeDto.password);
			if(user!=null) {
				response.body = user;
				response.code=200;
				response.message="ok";				
			}else {
				throw new AbstractException("User not exist");
			}
		}catch(Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}

	@GetMapping("/contacts")
	public Response getContacts() {
		Response response = new Response();
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
			response.body = userService.getContacts(user);
			response.code=200;
			response.message="ok";
		}catch(Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}
	
	@GetMapping("/conferences")
	public Response getConferences() {
		Response response = new Response();
		try{
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
			List<Conversation> conferences =conversationService.getUserConferences(user); 
			response.body =  conferences;
			response.code=200;
			response.message="ok";
		}catch(Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}
	
	@GetMapping("/conversations/get/{uuid}")
	public Response getConversation(@PathVariable String uuid) {
		Response response = new Response();
		try{
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
			Conversation conversation =  conversationService.get(uuid);
			if(conversation==null) throw new AbstractException("Nie znaleziono konwersacji");
			List<UserWithoutPassword> users = userService.getConversationUsers(uuid);
			boolean found = false;
			for(UserWithoutPassword u : users) {
				if(user.getId()==u.getId()) {
					found =true;
					break;
				}
			}
			if(found) {
				if (conversation.getName().equals(user.getNick())) {
					if (users.size() == 2) {
						if (users.get(0).getNick().equals(user.getNick())) {
							conversation.setName(users.get(1).getNick());
						} else {
							conversation.setName(users.get(0).getNick());
						}
					} else {
						conversation.setName(users.toString());
					}
				}
				response.body = conversation;
				response.code=200;
				response.message="ok";
			}else {
				throw new AbstractException("Nie jeste� uczestnikiem danej konwersacji");
			}			
		}catch(Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}
	
	@DeleteMapping(value = { "/conferences/users/remove" })
	public Response removeUserFromConversation(@RequestBody String uuidConversation) {
		Response response = new Response();
		try {
			Conversation conversation = conversationService.get(uuidConversation);
			if (conversation == null) throw new AbstractException("Nie znaleziono konwersacji o podanym UUID");
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
			conversationService.deleteUserFromConversation(conversation, user);			
			response.body = "Removed";
			response.code=200;
			response.message="ok";
		}catch(Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}
	
	@PostMapping(value = "/conferences/add", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Response createConversation(@RequestBody ConversationDto conversationDto) {
		Response response = new Response();
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
			Conversation conversation;
			conversation = conversationService.createConversation(user, conversationDto);
			response.body = conversation;
			response.code=200;
			response.message="ok";
		} catch (Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}
	
	@GetMapping(path = { "/messages/get/{uuidConversation}/{offset}" })
	public Response getMessages(@PathVariable String uuidConversation, @PathVariable Integer offset) {
		Response response = new Response();
		try {
			Conversation conversation = conversationService.get(uuidConversation);
			if(conversation==null) throw new AbstractException("Nie znaleziono konwersacji o podanym UUID");
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
			List<UserWithoutPassword> users = userService.getConversationUsers(conversation.getId());
			boolean found = false;
			for(UserWithoutPassword u : users) {
				if(user.getId()==u.getId()) {
					found =true;
					break;
				}
			}
			if(found) {
				response.body = messageService.getMessages(offset, 10, conversation.getId());
				response.code=200;
				response.message="ok";
			}else {
				throw new AbstractException("Nie jeste� uczestnikiem danej konwersacji");
			}			
		}catch(Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}
	
	@GetMapping(path = { "/messages/search/{uuidConversation}/{content}" })
	public Response searchMessage(@PathVariable String uuidConversation, @PathVariable String content) {
		Response response = new Response();
		try {
			Conversation conversation = conversationService.get(uuidConversation);
			if(conversation==null) throw new AbstractException("Nie znaleziono konwersacji o podanym UUID");
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
			List<UserWithoutPassword> users = userService.getConversationUsers(conversation.getId());
			boolean found = false;
			for(UserWithoutPassword u : users) {
				if(user.getId()==u.getId()) {
					found =true;
					break;
				}
			}
			if(found) {
				response.body = messageService.search(conversation.getId(), content);
				response.code=200;
				response.message="ok";
			}else {
				throw new AbstractException("Nie jeste� uczestnikiem danej konwersacji");
			}			
		}catch(Exception e) {
			response.code=500;
			response.message = ExceptionHandler.handle(e);
		}
		return response;
	}

}
