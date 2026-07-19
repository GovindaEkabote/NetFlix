package watch_together_service.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j

/*
* ChannelInterceptor is used to intercept the messages before they are sent to the destination.
* */
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    // JwtTokenProvider is used to validate the JWT token
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * This method is called before every STOMP message is sent through the
     * WebSocket message channel.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Converts the generic Message into a STOMP-specific accessor
        // so we can read headers like CONNECT, SEND, SUBSCRIBE, etc.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);

        // Authenticate only when the client is trying to establish
        // the WebSocket connection (CONNECT frame).
       if(accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())){
           // Read the Authorization header sent by the client.
           List<String> authHeaders = accessor.getNativeHeader("Authorization");

           // Make sure the Authorization header exists.
           if (authHeaders != null && !authHeaders.isEmpty()){
               // Example:
               // Authorization: Bearer eyJhbGciOiJIUzI1Ni...
               String token = authHeaders.get(0);

               // Remove the "Bearer " prefix to get only the JWT.
               // NOTE: Use "Bearer " (one space), not "Bearer  ".
               if(token.startsWith("Bearer  ")){
                   token = token.substring(7);
               }

               if(jwtTokenProvider.validateToken(token)){
                   String userId = jwtTokenProvider.getUserIdFromToken(token);

                   Map<String,Object> sessionAttributes = accessor.getSessionAttributes();
                   if (sessionAttributes != null) {
                       sessionAttributes.put("userId", Long.parseLong(userId));
                   }
                   log.info("WebSocket authenticated user: {}", userId);
               }else {
                   log.warn("Invalid WebSocket token");
               }
           }
       }
       return message;
    }


}
