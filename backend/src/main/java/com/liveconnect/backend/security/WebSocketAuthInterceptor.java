package com.liveconnect.backend.security;

import com.liveconnect.backend.service.CustomUserDetailsService;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor
        implements ChannelInterceptor {

    private static final String AUTHENTICATION =
            "WEBSOCKET_AUTHENTICATION";

    private final JwtService jwtService;

    private final CustomUserDetailsService
            userDetailsService;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService =
                userDetailsService;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        /*
         * STEP 1:
         * Handle STOMP CONNECT
         */
        if (StompCommand.CONNECT.equals(
                accessor.getCommand())) {

            String authorization =
                    accessor.getFirstNativeHeader(
                            "Authorization"
                    );

            if (authorization == null ||
                    !authorization.startsWith(
                            "Bearer ")) {

                throw new IllegalArgumentException(
                        "Missing Authorization header"
                );
            }

            String token =
                    authorization.substring(7);

            String username =
                    jwtService.extractUsername(token);

            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(username);

            if (!jwtService.isTokenValid(
                    token,
                    userDetails.getUsername())) {

                throw new IllegalArgumentException(
                        "Invalid JWT token"
                );
            }

            UsernamePasswordAuthenticationToken
                    authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            /*
             * Set Principal for CONNECT
             */
            accessor.setUser(authentication);

            /*
             * Explicitly store authentication
             * in the WebSocket session.
             */
            if (accessor.getSessionAttributes()
                    != null) {

                accessor.getSessionAttributes().put(
                        AUTHENTICATION,
                        authentication
                );
            }

            System.out.println(
                    "WebSocket authenticated user: "
                            + username
            );
        }

        /*
         * STEP 2:
         * Handle SEND / SUBSCRIBE / etc.
         *
         * Spring may not automatically expose
         * the Principal here in our current setup.
         *
         * So retrieve the authentication we stored
         * during CONNECT.
         */
        else {

            if (accessor.getUser() == null &&
                    accessor.getSessionAttributes()
                            != null) {

                Object storedAuthentication =
                        accessor.getSessionAttributes()
                                .get(AUTHENTICATION);

                if (storedAuthentication
                        instanceof Authentication) {

                    Authentication authentication =
                            (Authentication)
                                    storedAuthentication;

                    accessor.setUser(
                            authentication
                    );

                    System.out.println(
                            "WebSocket session user restored: "
                                    + authentication.getName()
                    );
                }
            }
        }

        return message;
    }
}