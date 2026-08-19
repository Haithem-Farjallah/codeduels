package com.codeduels.auth.security;

import com.codeduels.auth.security.jwt.JwtService;
import com.codeduels.match.model.Match;
import com.codeduels.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final MatchRepository matchRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        // ── CONNECT: who are you? ──
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearer = accessor.getFirstNativeHeader("Authorization");

            if (bearer == null || !bearer.startsWith("Bearer ")) {
                throw new MessagingException("Missing authentication token");
            }
            try {
                String email = jwtService.extractEmail(bearer.substring(7));
                UserDetails user = userDetailsService.loadUserByUsername(email);

                accessor.setUser(new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()));      // ← principal bound to the session

                log.info("[WS] {} connected", email);
            } catch (Exception e) {
                throw new MessagingException("Invalid authentication token");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/topic/match/")) {
                UUID userId = currentUserId(accessor);
                UUID matchId = UUID.fromString(
                        destination.substring("/topic/match/".length()));

                Match match = matchRepository.findById(matchId)
                        .orElseThrow(() -> new MessagingException("Match not found"));

                boolean participant = userId.equals(match.getPlayerOneId())
                        || userId.equals(match.getPlayerTwoId());

                if (!participant) {
                    log.warn("[WS] user {} denied subscription to match {}", userId, matchId);
                    throw new MessagingException("Not a participant in this match");
                }
            }
        }

        return message;
    }

    private UUID currentUserId(StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new MessagingException("Not authenticated");
        }
        return principal.getId();
    }
}