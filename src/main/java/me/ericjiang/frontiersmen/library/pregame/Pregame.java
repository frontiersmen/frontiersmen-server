package me.ericjiang.frontiersmen.library.pregame;

import java.util.Arrays;
import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.ericjiang.frontiersmen.library.MultiplayerModule;
import me.ericjiang.frontiersmen.library.StateEvent;
import me.ericjiang.frontiersmen.library.game.Game;
import me.ericjiang.frontiersmen.library.player.Player;
import me.ericjiang.frontiersmen.library.player.PlayerDisconnectionEvent;

@Slf4j
@Getter
public class Pregame extends MultiplayerModule {

    private transient final Game game;

    private final String name;

    private final String gameId;

    private final Player creator;

    private final Map<String, Object> attributes;

    private final int minimumPlayers;

    private final int maximumPlayers;

    private final Player[] playerSeats;

    public Pregame(Game game, Map<String, Object> attributes) {
        this.game = game;
        this.name = game.getName();
        this.gameId = game.getId();
        this.creator = game.getCreator();
        this.attributes = attributes;
        this.minimumPlayers = game.minimumPlayers();
        this.maximumPlayers = game.maximumPlayers();
        this.playerSeats = new Player[maximumPlayers];

        on(StartGameEvent.class, e -> {
            // Ensure other event handlers aren't triggered on an invalid StartGameEvent.
            if (!creator.getId().equals(e.getPlayerId())) {
                throw new IllegalArgumentException("Only the creator can start the game.");
            }
            if (!readyToStart()) {
                throw new IllegalArgumentException("Not ready to start.");
            }
        });

        on(TakeSeatEvent.class, e -> {
            synchronized(playerSeats) {
                takeSeat(e.getSeat(), e.getPlayerId());
                broadcast(toStateEvent());
            }
        });

        on(LeaveSeatEvent.class, e -> {
            synchronized(playerSeats) {
                leaveSeat(e.getSeat(), e.getPlayerId());
                broadcast(toStateEvent());
            }
        });

        on(PlayerDisconnectionEvent.class, e -> {
            synchronized(playerSeats) {
                evictPlayer(e.getPlayerId());
                broadcast(toStateEvent());
            }
        });

        on(TransitionToGameEvent.class, e -> {
            game.setPlayers(playerSeats);
            game.handleEvent(e);
            broadcast(e);
        });
    }

    @Override
    protected String getIdentifier() {
        return String.format("Game %s", gameId);
    }

    @Override
    protected StateEvent toStateEvent() {
        return new PregameUpdateEvent(this);
    }

    @Override
    protected boolean allowConnection(String playerId) {
        return true;
    }

    private boolean readyToStart() {
        synchronized(playerSeats) {
            return Arrays.stream(playerSeats).filter(p -> p != null).count() >= minimumPlayers;
        }
    }

    private void takeSeat(int seat, String playerId) {
        synchronized(playerSeats) {
            if (seatIsTaken(seat)) {
                throw new IllegalArgumentException(String.format(
                        "Player %s attempted to take seat %d but it was occupied by Player %s",
                        playerId, seat, playerSeats[seat].getId()));
            }
            if (seat >= maximumPlayers) {
                throw new IndexOutOfBoundsException(String.format(
                        "Player %s attempted to take seat %d but maximum players is %d",
                        playerId, seat, maximumPlayers));
            }
            // remove from current seat
            evictPlayer(playerId);
            playerSeats[seat] = new Player(playerId);
        }
    }

    private void leaveSeat(int seat, String playerId) {
        synchronized(playerSeats) {
            if (!playerId.equals(playerSeats[seat].getId())) {
                throw new IllegalArgumentException(String.format(
                        "Player %s attempted to leave unoccupied seat %d",
                        playerId, seat));
            }
            playerSeats[seat] = null;
        }
    }

    private boolean seatIsTaken(int seat) {
        synchronized(playerSeats) {
            return playerSeats[seat] != null;
        }
    }

    private void evictPlayer(String playerId) {
        synchronized(playerSeats) {
            for (int i = 0; i < playerSeats.length; i++) {
                if (playerSeats[i] != null && playerId.equals(playerSeats[i].getId())) {
                    log.debug("Removing Player {} from seat {}", playerId, i);
                    playerSeats[i] = null;
                }
            }
        }
    }

}
